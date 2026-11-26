package com.example.data.repository

import androidx.room.withTransaction
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.PersianFormatters
import com.example.data.local.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

/**
 * Pure domain calculators (unit-testable without Android Context).
 */
object LifePilotDomainLogic {

    /**
     * Generates exact Jalali payment schedule for an installment plan.
     * Payments always start as PENDING and are NEVER auto-marked paid when past due.
     */
    fun generateInstallmentSchedule(
        installmentId: Long,
        totalCount: Int,
        paymentAmount: Double,
        frequency: String,
        firstDueEpochDay: Long
    ): List<InstallmentPaymentEntity> {
        val count = totalCount.coerceAtLeast(1)
        val firstJalali = JalaliCalendarEngine.epochDayToJalali(firstDueEpochDay)
        return (1..count).map { index ->
            val offset = index - 1
            val dueDate = when (frequency) {
                "WEEKLY" -> firstJalali.plusDays(offset * 7L)
                "BIWEEKLY" -> firstJalali.plusDays(offset * 14L)
                "QUARTERLY" -> firstJalali.plusMonths(offset * 3)
                else -> firstJalali.plusMonths(offset) // MONTHLY exact Jalali month stepping
            }
            InstallmentPaymentEntity(
                installmentId = installmentId,
                installmentIndex = index,
                dueEpochDay = dueDate.toEpochDay(),
                expectedAmount = paymentAmount,
                paidAmount = 0.0,
                status = "PENDING",
                paidAtMillis = null
            )
        }
    }

    /**
     * USD -> IRT and IRT -> USD exact conversions using a manually entered rate.
     */
    fun convertUsdToIrt(usdAmount: Double, rateIrtPerUsd: Double): Double {
        if (rateIrtPerUsd <= 0.0) return 0.0
        return usdAmount * rateIrtPerUsd
    }

    fun convertIrtToUsd(irtAmount: Double, rateIrtPerUsd: Double): Double {
        if (rateIrtPerUsd <= 0.0) return 0.0
        return irtAmount / rateIrtPerUsd
    }

    /**
     * Calculates current streak & longest streak for a habit while NEVER penalizing
     * unscheduled days (e.g., if a habit is only scheduled Sat/Mon/Wed, Sun does not break streak).
     */
    data class HabitStreakStats(
        val currentStreak: Int,
        val longestStreak: Int,
        val last30DaysCompletionRate: Float
    )

    fun calculateHabitStats(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        todayEpochDay: Long
    ): HabitStreakStats {
        val completedDays = logs
            .filter { it.habitId == habit.id && it.completedCount >= habit.targetCount }
            .map { it.dateEpochDay }
            .toSet()

        fun isScheduled(epochDay: Long): Boolean {
            if (habit.scheduleType == "DAILY") return true
            val dow = JalaliCalendarEngine.epochDayToPersianDayOfWeek(epochDay)
            return ((habit.scheduledDaysMask shr dow) and 1) == 1
        }

        // Current streak walking backward from today (if today is scheduled and not done yet, check from yesterday)
        var currentStreak = 0
        var cursor = todayEpochDay
        if (isScheduled(cursor) && cursor !in completedDays) {
            cursor -= 1
        }
        var lookbackLimit = 365
        while (lookbackLimit-- > 0) {
            if (isScheduled(cursor)) {
                if (cursor in completedDays) {
                    currentStreak++
                } else {
                    break
                }
            }
            cursor -= 1
        }

        // Longest streak across all recorded days
        var longestStreak = currentStreak
        if (completedDays.isNotEmpty()) {
            val minDay = completedDays.min()
            val maxDay = max(completedDays.max(), todayEpochDay)
            var running = 0
            for (d in minDay..maxDay) {
                if (isScheduled(d)) {
                    if (d in completedDays) {
                        running++
                        if (running > longestStreak) longestStreak = running
                    } else {
                        running = 0
                    }
                }
            }
        }

        // Last 30 days scheduled completion rate
        var scheduledCount = 0
        var completedInWindow = 0
        for (d in (todayEpochDay - 29)..todayEpochDay) {
            if (isScheduled(d)) {
                scheduledCount++
                if (d in completedDays) completedInWindow++
            }
        }
        val rate = if (scheduledCount == 0) 0f else completedInWindow.toFloat() / scheduledCount.toFloat()
        return HabitStreakStats(currentStreak, longestStreak, rate)
    }

    /**
     * Time interval and smart daily planner free-slot & conflict analyzer.
     */
    data class FixedBlock(
        val title: String,
        val sourceType: String,
        val sourceId: Long,
        val startMinute: Int,
        val endMinute: Int,
        val colorHex: Long
    )

    data class TimeConflict(
        val first: FixedBlock,
        val second: FixedBlock
    )

    data class ProposedTaskBlock(
        val task: TaskEntity,
        val suggestedStartMinute: Int,
        val suggestedEndMinute: Int,
        val reasonFa: String
    )

    fun detectConflicts(blocks: List<FixedBlock>): List<TimeConflict> {
        val sorted = blocks.sortedBy { it.startMinute }
        val conflicts = mutableListOf<TimeConflict>()
        for (i in sorted.indices) {
            for (j in i + 1 until sorted.size) {
                if (sorted[j].startMinute < sorted[i].endMinute) {
                    conflicts.add(TimeConflict(sorted[i], sorted[j]))
                }
            }
        }
        return conflicts
    }

    fun buildSmartTaskProposals(
        fixedBlocks: List<FixedBlock>,
        pendingTasks: List<TaskEntity>,
        todayEpochDay: Long,
        dayStartMinute: Int = 8 * 60,
        dayEndMinute: Int = 22 * 60
    ): List<ProposedTaskBlock> {
        // Compute free windows between dayStartMinute and dayEndMinute
        val merged = mutableListOf<IntRange>()
        for (b in fixedBlocks.sortedBy { it.startMinute }) {
            val s = b.startMinute.coerceIn(dayStartMinute, dayEndMinute)
            val e = b.endMinute.coerceIn(dayStartMinute, dayEndMinute)
            if (e <= s) continue
            if (merged.isEmpty() || s > merged.last().last) {
                merged.add(s..e)
            } else {
                val prev = merged.removeAt(merged.lastIndex)
                merged.add(prev.first..max(prev.last, e))
            }
        }

        val freeSlots = mutableListOf<IntRange>()
        var pointer = dayStartMinute
        for (busy in merged) {
            if (busy.first - pointer >= 15) {
                freeSlots.add(pointer..busy.first)
            }
            pointer = max(pointer, busy.last)
        }
        if (dayEndMinute - pointer >= 15) {
            freeSlots.add(pointer..dayEndMinute)
        }

        fun priorityScore(priority: String): Int = when (priority) {
            "URGENT" -> 4
            "HIGH" -> 3
            "MEDIUM" -> 2
            else -> 1
        }

        // Rank open tasks by urgency (overdue first, then due today, then priority)
        val rankedTasks = pendingTasks
            .filter { it.status == "PENDING" || it.status == "IN_PROGRESS" }
            .sortedWith(
                compareBy<TaskEntity> { it.deadlineEpochDay }
                    .thenByDescending { priorityScore(it.priority) }
            )

        val proposals = mutableListOf<ProposedTaskBlock>()
        val remainingSlots = freeSlots.map { it.first to it.last }.toMutableList()

        for (task in rankedTasks) {
            val needed = task.estimatedMinutes.coerceIn(15, 180)
            val slotIndex = remainingSlots.indexOfFirst { (start, end) -> (end - start) >= needed }
            if (slotIndex != -1) {
                val (slotStart, slotEnd) = remainingSlots[slotIndex]
                val proposedEnd = slotStart + needed
                val reason = when {
                    task.deadlineEpochDay < todayEpochDay -> "عقب‌افتاده — اولویت فوری"
                    task.deadlineEpochDay == todayEpochDay -> "سررسید امروز"
                    task.priority == "URGENT" || task.priority == "HIGH" -> "اولویت بالا"
                    else -> "متناسب با زمان آزاد شما"
                }
                proposals.add(
                    ProposedTaskBlock(
                        task = task,
                        suggestedStartMinute = slotStart,
                        suggestedEndMinute = proposedEnd,
                        reasonFa = reason
                    )
                )
                val nextStart = proposedEnd + 10 // 10-minute buffer
                if (slotEnd - nextStart >= 15) {
                    remainingSlots[slotIndex] = nextStart to slotEnd
                } else {
                    remainingSlots.removeAt(slotIndex)
                }
            }
            if (proposals.size >= 8) break
        }
        return proposals
    }
}

/**
 * Unified calendar item referencing an original entity (never creating disconnected duplicates).
 */
data class UnifiedCalendarItem(
    val id: String,
    val sourceType: String, // EVENT, INSTALLMENT, TASK, PROJECT, MEETING, COURSE, EXAM, ASSIGNMENT, HOLIDAY
    val sourceId: Long,
    val title: String,
    val subtitle: String,
    val epochDay: Long,
    val startMinute: Int?,
    val endMinute: Int?,
    val isAllDay: Boolean,
    val category: String,
    val colorHex: Long,
    val isHoliday: Boolean = false,
    val isOverdue: Boolean = false
)

class LifePilotRepository(private val db: LifePilotDatabase) {

    private val financeDao = db.financeDao()
    private val productivityDao = db.productivityDao()
    private val lifeDao = db.lifeModulesDao()
    private val systemDao = db.systemDao()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Reactive Flows
    val transactions: Flow<List<FinancialTransactionEntity>> = financeDao.observeTransactions()
    val installments: Flow<List<InstallmentEntity>> = financeDao.observeInstallments()
    val installmentPayments: Flow<List<InstallmentPaymentEntity>> = financeDao.observeInstallmentPayments()
    val exchangeRates: Flow<List<ExchangeRateEntity>> = financeDao.observeExchangeRates()
    val tasks: Flow<List<TaskEntity>> = productivityDao.observeTasks()
    val projects: Flow<List<ProjectEntity>> = productivityDao.observeProjects()
    val milestones: Flow<List<ProjectMilestoneEntity>> = productivityDao.observeMilestones()
    val courses: Flow<List<UniversityCourseEntity>> = lifeDao.observeCourses()
    val universityItems: Flow<List<UniversityItemEntity>> = lifeDao.observeUniversityItems()
    val meetings: Flow<List<MeetingEntity>> = lifeDao.observeMeetings()
    val calendarEvents: Flow<List<CalendarEventEntity>> = lifeDao.observeCalendarEvents()
    val notes: Flow<List<NoteEntity>> = lifeDao.observeNotes()
    val habits: Flow<List<HabitEntity>> = lifeDao.observeHabits()
    val habitLogs: Flow<List<HabitLogEntity>> = lifeDao.observeHabitLogs()
    val activityLogs: Flow<List<ActivityLogEntity>> = systemDao.observeActivityLogs()
    val notifications: Flow<List<NotificationEntity>> = systemDao.observeNotifications()

    private suspend fun logActivity(
        actionType: String,
        entityType: String,
        entityId: Long,
        description: String,
        previousValue: String = "",
        newValue: String = ""
    ) {
        systemDao.insertActivityLog(
            ActivityLogEntity(
                actionType = actionType,
                entityType = entityType,
                entityId = entityId,
                description = description,
                previousValue = previousValue,
                newValue = newValue
            )
        )
    }

    suspend fun addNotification(
        title: String,
        message: String,
        category: String,
        entityType: String = "",
        entityId: Long = 0
    ): Long {
        return systemDao.insertNotification(
            NotificationEntity(
                title = title,
                message = message,
                category = category,
                entityType = entityType,
                entityId = entityId
            )
        )
    }

    // --- Finance & Exchange Rate ---

    suspend fun saveManualExchangeRate(rateIrtPerUsd: Double, notes: String): Long {
        val id = financeDao.insertExchangeRate(
            ExchangeRateEntity(
                rateIrtPerUsd = rateIrtPerUsd,
                effectiveAtMillis = System.currentTimeMillis(),
                notes = notes.trim()
            )
        )
        logActivity(
            actionType = "EXCHANGE_RATE_UPDATED",
            entityType = "EXCHANGE_RATE",
            entityId = id,
            description = "نرخ دستی دلار ثبت شد: ${PersianFormatters.formatIrt(rateIrtPerUsd)}",
            newValue = rateIrtPerUsd.toString()
        )
        return id
    }

    suspend fun addTransaction(
        title: String,
        description: String,
        amount: Double,
        currency: String,
        type: String,
        category: String,
        dateEpochDay: Long,
        paymentStatus: String,
        rateForHistoricalSnapshot: Double?,
        notes: String
    ): Long {
        val convertedIrt = if (currency == "USD" && rateForHistoricalSnapshot != null && rateForHistoricalSnapshot > 0) {
            LifePilotDomainLogic.convertUsdToIrt(amount, rateForHistoricalSnapshot)
        } else if (currency == "IRT") {
            amount
        } else null

        val id = financeDao.insertTransaction(
            FinancialTransactionEntity(
                title = title.trim(),
                description = description.trim(),
                amount = amount,
                currency = currency,
                type = type,
                category = category.trim().ifBlank { "عمومی" },
                dateEpochDay = dateEpochDay,
                paymentStatus = paymentStatus,
                historicalRateUsed = if (currency == "USD") rateForHistoricalSnapshot else null,
                convertedAmountIrt = convertedIrt,
                notes = notes.trim()
            )
        )
        val typeFa = if (type == "INCOME") "درآمد" else "هزینه"
        logActivity(
            actionType = "TRANSACTION_CREATED",
            entityType = "FINANCE",
            entityId = id,
            description = "ثبت $typeFa «$title» به مبلغ ${PersianFormatters.formatCurrency(amount, currency)}",
            newValue = "$amount $currency"
        )
        return id
    }

    suspend fun deleteTransaction(tx: FinancialTransactionEntity) {
        financeDao.deleteTransaction(tx)
        logActivity(
            actionType = "TRANSACTION_DELETED",
            entityType = "FINANCE",
            entityId = tx.id,
            description = "حذف تراکنش «${tx.title}»",
            previousValue = "${tx.amount} ${tx.currency}"
        )
    }

    // --- Installments ---

    suspend fun createInstallmentPlan(
        title: String,
        creditor: String,
        originalAmount: Double,
        currency: String,
        totalCount: Int,
        paymentAmount: Double,
        frequency: String,
        firstDueEpochDay: Long,
        notes: String
    ): Long {
        var installmentId = 0L
        db.withTransaction {
            installmentId = financeDao.insertInstallment(
                InstallmentEntity(
                    title = title.trim(),
                    creditor = creditor.trim(),
                    originalAmount = originalAmount,
                    currency = currency,
                    totalCount = totalCount.coerceAtLeast(1),
                    paymentAmount = paymentAmount,
                    frequency = frequency,
                    firstDueEpochDay = firstDueEpochDay,
                    notes = notes.trim()
                )
            )
            val schedule = LifePilotDomainLogic.generateInstallmentSchedule(
                installmentId = installmentId,
                totalCount = totalCount,
                paymentAmount = paymentAmount,
                frequency = frequency,
                firstDueEpochDay = firstDueEpochDay
            )
            financeDao.insertPayments(schedule)
        }
        logActivity(
            actionType = "INSTALLMENT_CREATED",
            entityType = "INSTALLMENT",
            entityId = installmentId,
            description = "ایجاد قسط «$title» (${PersianFormatters.digits(totalCount.toString())} قسط)",
            newValue = "$originalAmount $currency"
        )
        addNotification(
            title = "قسط جدید ثبت شد",
            message = "جدول پرداخت «$title» با ${PersianFormatters.digits(totalCount.toString())} سررسید به تقویم جلالی اضافه شد.",
            category = "INSTALLMENTS",
            entityType = "INSTALLMENT",
            entityId = installmentId
        )
        return installmentId
    }

    /**
     * Records a full or partial payment for a single installment item.
     * Prevents duplicate over-payment once PAID.
     */
    suspend fun recordInstallmentPayment(
        payment: InstallmentPaymentEntity,
        installmentTitle: String,
        currency: String,
        additionalPaidAmount: Double,
        paidAtMillis: Long,
        notes: String,
        alsoRecordExpenseTransaction: Boolean,
        currentUsdRate: Double?
    ): Boolean {
        if (payment.status == "PAID") return false // Duplicate payment prevention
        if (additionalPaidAmount <= 0.0) return false

        val newPaid = (payment.paidAmount + additionalPaidAmount).coerceAtMost(payment.expectedAmount)
        val newStatus = when {
            newPaid >= payment.expectedAmount - 0.01 -> "PAID"
            newPaid > 0.0 -> "PARTIAL"
            else -> "PENDING"
        }
        val updated = payment.copy(
            paidAmount = newPaid,
            status = newStatus,
            paidAtMillis = paidAtMillis,
            notes = notes.trim().ifBlank { payment.notes }
        )
        financeDao.updatePayment(updated)

        if (alsoRecordExpenseTransaction) {
            addTransaction(
                title = "پرداخت قسط ${payment.installmentIndex} — $installmentTitle",
                description = "ثبت خودکار از بخش اقساط",
                amount = additionalPaidAmount,
                currency = currency,
                type = "EXPENSE",
                category = "اقساط و بدهی",
                dateEpochDay = JalaliCalendarEngine.todayEpochDay(),
                paymentStatus = "PAID",
                rateForHistoricalSnapshot = currentUsdRate,
                notes = notes
            )
        }

        logActivity(
            actionType = "PAYMENT_RECORDED",
            entityType = "INSTALLMENT_PAYMENT",
            entityId = payment.id,
            description = "ثبت پرداخت قسط شماره ${PersianFormatters.digits(payment.installmentIndex.toString())} از «$installmentTitle»",
            previousValue = "${payment.paidAmount} (${payment.status})",
            newValue = "$newPaid ($newStatus)"
        )
        return true
    }

    suspend fun deleteInstallment(item: InstallmentEntity) {
        db.withTransaction {
            financeDao.deletePaymentsForInstallment(item.id)
            financeDao.deleteInstallment(item)
        }
        logActivity(
            actionType = "INSTALLMENT_DELETED",
            entityType = "INSTALLMENT",
            entityId = item.id,
            description = "حذف قسط «${item.title}»"
        )
    }

    // --- Tasks ---

    suspend fun saveTask(task: TaskEntity): Long {
        val isNew = task.id == 0L
        val id = productivityDao.insertTask(task.copy(updatedAt = System.currentTimeMillis()))
        logActivity(
            actionType = if (isNew) "TASK_CREATED" else "TASK_UPDATED",
            entityType = "TASK",
            entityId = id,
            description = if (isNew) "ایجاد وظیفه «${task.title}»" else "بروزرسانی وظیفه «${task.title}»",
            newValue = task.status
        )
        return id
    }

    suspend fun toggleTaskCompletion(task: TaskEntity) {
        val nextStatus = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"
        productivityDao.updateTask(task.copy(status = nextStatus, updatedAt = System.currentTimeMillis()))
        logActivity(
            actionType = if (nextStatus == "COMPLETED") "TASK_COMPLETED" else "TASK_REOPENED",
            entityType = "TASK",
            entityId = task.id,
            description = if (nextStatus == "COMPLETED") "تکمیل وظیفه «${task.title}»" else "بازگشایی وظیفه «${task.title}»",
            previousValue = task.status,
            newValue = nextStatus
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        productivityDao.deleteTask(task)
        logActivity(
            actionType = "TASK_DELETED",
            entityType = "TASK",
            entityId = task.id,
            description = "حذف وظیفه «${task.title}»"
        )
    }

    // --- Projects ---

    suspend fun saveProject(project: ProjectEntity): Long {
        val isNew = project.id == 0L
        val id = productivityDao.insertProject(project)
        logActivity(
            actionType = if (isNew) "PROJECT_CREATED" else "PROJECT_UPDATED",
            entityType = "PROJECT",
            entityId = id,
            description = if (isNew) "ایجاد پروژه «${project.title}»" else "بروزرسانی پروژه «${project.title}»",
            newValue = project.status
        )
        return id
    }

    suspend fun deleteProject(project: ProjectEntity) {
        db.withTransaction {
            productivityDao.deleteMilestonesForProject(project.id)
            productivityDao.deleteProject(project)
        }
        logActivity(
            actionType = "PROJECT_DELETED",
            entityType = "PROJECT",
            entityId = project.id,
            description = "حذف پروژه «${project.title}»"
        )
    }

    suspend fun addMilestone(milestone: ProjectMilestoneEntity): Long {
        return productivityDao.insertMilestone(milestone)
    }

    suspend fun toggleMilestone(milestone: ProjectMilestoneEntity) {
        productivityDao.updateMilestone(milestone.copy(isCompleted = !milestone.isCompleted))
    }

    suspend fun deleteMilestone(milestone: ProjectMilestoneEntity) {
        productivityDao.deleteMilestone(milestone)
    }

    // --- University ---

    suspend fun saveCourse(course: UniversityCourseEntity): Long {
        val id = lifeDao.insertCourse(course)
        logActivity(
            actionType = "COURSE_SAVED",
            entityType = "UNIVERSITY",
            entityId = id,
            description = "ثبت درس دانشگاهی «${course.title}»"
        )
        return id
    }

    suspend fun deleteCourse(course: UniversityCourseEntity) {
        db.withTransaction {
            lifeDao.deleteItemsForCourse(course.id)
            lifeDao.deleteCourse(course)
        }
        logActivity(
            actionType = "COURSE_DELETED",
            entityType = "UNIVERSITY",
            entityId = course.id,
            description = "حذف درس «${course.title}»"
        )
    }

    suspend fun saveUniversityItem(item: UniversityItemEntity): Long {
        val id = lifeDao.insertUniversityItem(item)
        logActivity(
            actionType = "ACADEMIC_ITEM_SAVED",
            entityType = "UNIVERSITY",
            entityId = id,
            description = "ثبت مورد دانشگاهی «${item.title}» (${item.type})"
        )
        return id
    }

    suspend fun toggleUniversityItem(item: UniversityItemEntity) {
        val next = if (item.status == "COMPLETED") "PENDING" else "COMPLETED"
        lifeDao.updateUniversityItem(item.copy(status = next))
    }

    suspend fun deleteUniversityItem(item: UniversityItemEntity) {
        lifeDao.deleteUniversityItem(item)
    }

    // --- Meetings ---

    suspend fun saveMeeting(meeting: MeetingEntity): Long {
        val isNew = meeting.id == 0L
        val id = lifeDao.insertMeeting(meeting)
        logActivity(
            actionType = if (isNew) "MEETING_SCHEDULED" else "MEETING_UPDATED",
            entityType = "MEETING",
            entityId = id,
            description = if (isNew) "زمان‌بندی جلسه «${meeting.title}»" else "ویرایش جلسه «${meeting.title}»"
        )
        if (isNew) {
            addNotification(
                title = "جلسه جدید ثبت شد",
                message = "«${meeting.title}» در تقویم جلالی و داشبورد ثبت گردید.",
                category = "MEETINGS",
                entityType = "MEETING",
                entityId = id
            )
        }
        return id
    }

    suspend fun deleteMeeting(meeting: MeetingEntity) {
        lifeDao.deleteMeeting(meeting)
        logActivity(
            actionType = "MEETING_DELETED",
            entityType = "MEETING",
            entityId = meeting.id,
            description = "حذف جلسه «${meeting.title}»"
        )
    }

    // --- Calendar Events ---

    suspend fun saveCalendarEvent(event: CalendarEventEntity): Long {
        val isNew = event.id == 0L
        val id = lifeDao.insertCalendarEvent(event)
        logActivity(
            actionType = if (isNew) "EVENT_CREATED" else "EVENT_UPDATED",
            entityType = "CALENDAR",
            entityId = id,
            description = if (isNew) "افزودن رویداد «${event.title}» به تقویم" else "ویرایش رویداد «${event.title}»"
        )
        return id
    }

    suspend fun deleteCalendarEvent(event: CalendarEventEntity) {
        lifeDao.deleteCalendarEvent(event)
        logActivity(
            actionType = "EVENT_DELETED",
            entityType = "CALENDAR",
            entityId = event.id,
            description = "حذف رویداد «${event.title}»"
        )
    }

    // --- Notes ---

    suspend fun saveNote(note: NoteEntity): Long {
        val isNew = note.id == 0L
        val id = lifeDao.insertNote(note.copy(updatedAt = System.currentTimeMillis()))
        logActivity(
            actionType = if (isNew) "NOTE_CREATED" else "NOTE_EDITED",
            entityType = "NOTE",
            entityId = id,
            description = if (isNew) "ایجاد یادداشت «${note.title}»" else "ویرایش یادداشت «${note.title}»"
        )
        return id
    }

    suspend fun deleteNote(note: NoteEntity) {
        lifeDao.deleteNote(note)
        logActivity(
            actionType = "NOTE_DELETED",
            entityType = "NOTE",
            entityId = note.id,
            description = "حذف یادداشت «${note.title}»"
        )
    }

    // --- Habits ---

    suspend fun saveHabit(habit: HabitEntity): Long {
        val id = lifeDao.insertHabit(habit)
        logActivity(
            actionType = "HABIT_SAVED",
            entityType = "HABIT",
            entityId = id,
            description = "ثبت عادت «${habit.title}»"
        )
        return id
    }

    suspend fun adjustHabitProgress(habit: HabitEntity, dateEpochDay: Long, delta: Int) {
        val existing = lifeDao.getHabitLog(habit.id, dateEpochDay)
        val nextCount = ((existing?.completedCount ?: 0) + delta).coerceAtLeast(0)
        if (nextCount == 0) {
            lifeDao.deleteHabitLog(habit.id, dateEpochDay)
        } else {
            lifeDao.upsertHabitLog(
                HabitLogEntity(
                    id = existing?.id ?: 0,
                    habitId = habit.id,
                    dateEpochDay = dateEpochDay,
                    completedCount = nextCount,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        if (nextCount >= habit.targetCount && (existing?.completedCount ?: 0) < habit.targetCount) {
            logActivity(
                actionType = "HABIT_COMPLETED",
                entityType = "HABIT",
                entityId = habit.id,
                description = "تکمیل عادت «${habit.title}» برای تاریخ ${JalaliCalendarEngine.epochDayToJalali(dateEpochDay).formatNumeric()}"
            )
        }
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        db.withTransaction {
            lifeDao.deleteLogsForHabit(habit.id)
            lifeDao.deleteHabit(habit)
        }
        logActivity(
            actionType = "HABIT_DELETED",
            entityType = "HABIT",
            entityId = habit.id,
            description = "حذف عادت «${habit.title}»"
        )
    }

    // --- Notifications ---

    suspend fun markNotificationRead(id: Long) = systemDao.markNotificationRead(id)
    suspend fun markAllNotificationsRead() = systemDao.markAllNotificationsRead()
    suspend fun deleteNotification(id: Long) = systemDao.deleteNotification(id)

    suspend fun logSettingsChange(detail: String) {
        logActivity(
            actionType = "SETTINGS_CHANGED",
            entityType = "SETTINGS",
            entityId = 0,
            description = detail
        )
    }

    // --- Local JSON Backup & Restore ---

    suspend fun exportBackupJson(): String {
        val payload = LifePilotBackupPayload(
            version = 1,
            exportedAtMillis = System.currentTimeMillis(),
            transactions = financeDao.getAllTransactions(),
            installments = financeDao.getAllInstallments(),
            installmentPayments = financeDao.getAllInstallmentPayments(),
            exchangeRates = financeDao.getAllExchangeRates(),
            tasks = productivityDao.getAllTasks(),
            projects = productivityDao.getAllProjects(),
            milestones = productivityDao.getAllMilestones(),
            courses = lifeDao.getAllCourses(),
            universityItems = lifeDao.getAllUniversityItems(),
            meetings = lifeDao.getAllMeetings(),
            calendarEvents = lifeDao.getAllCalendarEvents(),
            notes = lifeDao.getAllNotes(),
            habits = lifeDao.getAllHabits(),
            habitLogs = lifeDao.getAllHabitLogs(),
            activityLogs = systemDao.getAllActivityLogs(),
            notifications = systemDao.getAllNotifications()
        )
        val adapter = moshi.adapter(LifePilotBackupPayload::class.java).indent("  ")
        return adapter.toJson(payload)
    }

    suspend fun restoreBackupJson(json: String): Result<Int> = runCatching {
        val adapter = moshi.adapter(LifePilotBackupPayload::class.java)
        val payload = adapter.fromJson(json) ?: throw IllegalArgumentException("فرمت فایل پشتیبان معتبر نیست.")
        db.withTransaction {
            clearAllTablesInternal()
            payload.transactions.forEach { financeDao.insertTransaction(it) }
            payload.installments.forEach { financeDao.insertInstallment(it) }
            financeDao.upsertPayments(payload.installmentPayments)
            payload.exchangeRates.forEach { financeDao.insertExchangeRate(it) }
            payload.tasks.forEach { productivityDao.insertTask(it) }
            payload.projects.forEach { productivityDao.insertProject(it) }
            payload.milestones.forEach { productivityDao.insertMilestone(it) }
            payload.courses.forEach { lifeDao.insertCourse(it) }
            payload.universityItems.forEach { lifeDao.insertUniversityItem(it) }
            payload.meetings.forEach { lifeDao.insertMeeting(it) }
            payload.calendarEvents.forEach { lifeDao.insertCalendarEvent(it) }
            payload.notes.forEach { lifeDao.insertNote(it) }
            payload.habits.forEach { lifeDao.insertHabit(it) }
            payload.habitLogs.forEach { lifeDao.upsertHabitLog(it) }
            payload.activityLogs.forEach { systemDao.insertActivityLog(it) }
            payload.notifications.forEach { systemDao.insertNotification(it) }
        }
        logActivity(
            actionType = "BACKUP_RESTORED",
            entityType = "SYSTEM",
            entityId = 0,
            description = "بازیابی نسخه پشتیبان محلی با موفقیت انجام شد."
        )
        payload.transactions.size + payload.tasks.size + payload.installments.size + payload.notes.size
    }

    suspend fun clearAllData() {
        db.withTransaction { clearAllTablesInternal() }
    }

    private suspend fun clearAllTablesInternal() {
        financeDao.clearInstallmentPayments()
        financeDao.clearInstallments()
        financeDao.clearTransactions()
        financeDao.clearExchangeRates()
        productivityDao.clearMilestones()
        productivityDao.clearTasks()
        productivityDao.clearProjects()
        lifeDao.clearUniversityItems()
        lifeDao.clearCourses()
        lifeDao.clearMeetings()
        lifeDao.clearCalendarEvents()
        lifeDao.clearNotes()
        lifeDao.clearHabitLogs()
        lifeDao.clearHabits()
        systemDao.clearNotifications()
        systemDao.clearActivityLogs()
    }
}
