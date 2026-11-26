package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.LocalReminderHelper
import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.JalaliDate
import com.example.core.jalali.PersianFormatters
import com.example.data.local.*
import com.example.data.repository.LifePilotDomainLogic
import com.example.data.repository.LifePilotRepository
import com.example.data.repository.UnifiedCalendarItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LifePilotDataState(
    val settings: UserSettings = UserSettings(),
    val transactions: List<FinancialTransactionEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val installmentPayments: List<InstallmentPaymentEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val milestones: List<ProjectMilestoneEntity> = emptyList(),
    val courses: List<UniversityCourseEntity> = emptyList(),
    val universityItems: List<UniversityItemEntity> = emptyList(),
    val meetings: List<MeetingEntity> = emptyList(),
    val calendarEvents: List<CalendarEventEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val activityLogs: List<ActivityLogEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val rejectedProposalTaskIds: Set<Long> = emptySet(),
    val isUnlocked: Boolean = false
) {
    val currentUsdRate: ExchangeRateEntity? get() = exchangeRates.firstOrNull()

    val unreadNotificationsCount: Int get() = notifications.count { !it.isRead }

    val irtIncome: Double
        get() = transactions.filter { it.currency == "IRT" && it.type == "INCOME" }.sumOf { it.amount }
    val irtExpense: Double
        get() = transactions.filter { it.currency == "IRT" && it.type == "EXPENSE" }.sumOf { it.amount }
    val irtBalance: Double get() = irtIncome - irtExpense

    val usdIncome: Double
        get() = transactions.filter { it.currency == "USD" && it.type == "INCOME" }.sumOf { it.amount }
    val usdExpense: Double
        get() = transactions.filter { it.currency == "USD" && it.type == "EXPENSE" }.sumOf { it.amount }
    val usdBalance: Double get() = usdIncome - usdExpense

    val estimatedCombinedIrt: Double?
        get() = currentUsdRate?.let { rate ->
            irtBalance + LifePilotDomainLogic.convertUsdToIrt(usdBalance, rate.rateIrtPerUsd)
        }

    fun projectProgress(projectId: Long): Float {
        val projTasks = tasks.filter { it.linkedProjectId == projectId && it.status != "CANCELLED" }
        val projMilestones = milestones.filter { it.projectId == projectId }
        val total = projTasks.size + projMilestones.size
        if (total == 0) return 0f
        val done = projTasks.count { it.status == "COMPLETED" } + projMilestones.count { it.isCompleted }
        return done.toFloat() / total.toFloat()
    }

    /**
     * Builds unified Jalali calendar items for a given epoch day referencing real entities
     * (never creating disconnected duplicates).
     */
    fun unifiedItemsForDay(epochDay: Long): List<UnifiedCalendarItem> {
        val result = mutableListOf<UnifiedCalendarItem>()
        val jalali = JalaliCalendarEngine.epochDayToJalali(epochDay)
        val persianDow = jalali.dayOfWeekPersian()
        val today = JalaliCalendarEngine.todayEpochDay(settings.timeZoneId)

        // Official Iranian holidays
        if (settings.showOfficialHolidays) {
            JalaliCalendarEngine.holidaysForDate(jalali).forEachIndexed { idx, holiday ->
                result.add(
                    UnifiedCalendarItem(
                        id = "holiday_${jalali.month}_${jalali.day}_$idx",
                        sourceType = "HOLIDAY",
                        sourceId = 0,
                        title = holiday.title,
                        subtitle = if (holiday.isOfficialHoliday) "تعطیل رسمی ایران" else "مناسبت ملی",
                        epochDay = epochDay,
                        startMinute = null,
                        endMinute = null,
                        isAllDay = true,
                        category = "HOLIDAY",
                        colorHex = if (holiday.isOfficialHoliday) 0xFFC62828 else 0xFFB88946,
                        isHoliday = holiday.isOfficialHoliday
                    )
                )
            }
        }

        // Custom calendar events (including recurring expansion)
        calendarEvents.forEach { ev ->
            if (JalaliCalendarEngine.occursOnDay(
                    startEpochDay = ev.startEpochDay,
                    endEpochDay = ev.endEpochDay,
                    recurrenceRule = ev.recurrenceRule,
                    recurrenceInterval = ev.recurrenceInterval,
                    recurrenceEndEpochDay = ev.recurrenceEndEpochDay,
                    targetEpochDay = epochDay
                )
            ) {
                result.add(
                    UnifiedCalendarItem(
                        id = "event_${ev.id}_$epochDay",
                        sourceType = "EVENT",
                        sourceId = ev.id,
                        title = ev.title,
                        subtitle = ev.location.ifBlank { ev.description.ifBlank { if (ev.isCustomHoliday) "مناسبت شخصی" else "رویداد تقویم" } },
                        epochDay = epochDay,
                        startMinute = if (ev.isAllDay) null else ev.startMinuteOfDay,
                        endMinute = if (ev.isAllDay) null else ev.endMinuteOfDay,
                        isAllDay = ev.isAllDay,
                        category = ev.category,
                        colorHex = if (ev.isCustomHoliday) 0xFFC62828 else ev.colorHex,
                        isHoliday = ev.isCustomHoliday
                    )
                )
            }
        }

        // Installment payments
        val instMap = installments.associateBy { it.id }
        installmentPayments.filter { it.dueEpochDay == epochDay }.forEach { p ->
            val parent = instMap[p.installmentId]
            val cur = parent?.currency ?: "IRT"
            val remaining = (p.expectedAmount - p.paidAmount).coerceAtLeast(0.0)
            val statusFa = when (p.status) {
                "PAID" -> "پرداخت‌شده"
                "PARTIAL" -> "پرداخت ناقص"
                else -> if (p.dueEpochDay < today) "معوق" else "در انتظار پرداخت"
            }
            result.add(
                UnifiedCalendarItem(
                    id = "instpay_${p.id}",
                    sourceType = "INSTALLMENT",
                    sourceId = p.installmentId,
                    title = "قسط ${PersianFormatters.digits(p.installmentIndex.toString())}: ${parent?.title ?: "قسط"}",
                    subtitle = "$statusFa • ${PersianFormatters.formatCurrency(if (p.status == "PAID") p.expectedAmount else remaining, cur, settings.usePersianDigits)}",
                    epochDay = epochDay,
                    startMinute = null,
                    endMinute = null,
                    isAllDay = true,
                    category = "INSTALLMENTS",
                    colorHex = if (p.status == "PAID") 0xFF0D7A5F else if (p.dueEpochDay < today) 0xFFC62828 else 0xFFB88946,
                    isOverdue = p.status != "PAID" && p.dueEpochDay < today
                )
            )
        }

        // Tasks (by deadline)
        tasks.filter { it.deadlineEpochDay == epochDay && it.status != "CANCELLED" }.forEach { t ->
            result.add(
                UnifiedCalendarItem(
                    id = "task_${t.id}",
                    sourceType = "TASK",
                    sourceId = t.id,
                    title = t.title,
                    subtitle = if (t.status == "COMPLETED") "وظیفه تکمیل‌شده" else "سررسید وظیفه (${PersianFormatters.digits(t.estimatedMinutes.toString(), settings.usePersianDigits)} دقیقه)",
                    epochDay = epochDay,
                    startMinute = null,
                    endMinute = null,
                    isAllDay = true,
                    category = "TASKS",
                    colorHex = if (t.status == "COMPLETED") 0xFF0D7A5F else 0xFF1D4ED8,
                    isOverdue = t.status != "COMPLETED" && t.deadlineEpochDay < today
                )
            )
        }

        // Project deadlines
        projects.filter { it.endEpochDay == epochDay }.forEach { proj ->
            result.add(
                UnifiedCalendarItem(
                    id = "proj_${proj.id}",
                    sourceType = "PROJECT",
                    sourceId = proj.id,
                    title = "تحویل پروژه: ${proj.title}",
                    subtitle = "ددلاین پروژه",
                    epochDay = epochDay,
                    startMinute = null,
                    endMinute = null,
                    isAllDay = true,
                    category = "PROJECTS",
                    colorHex = 0xFF6D28D9
                )
            )
        }

        // Meetings
        meetings.filter { it.dateEpochDay == epochDay && it.status != "CANCELLED" }.forEach { m ->
            result.add(
                UnifiedCalendarItem(
                    id = "meeting_${m.id}",
                    sourceType = "MEETING",
                    sourceId = m.id,
                    title = m.title,
                    subtitle = m.location.ifBlank { m.participants.ifBlank { "جلسه" } },
                    epochDay = epochDay,
                    startMinute = m.startMinuteOfDay,
                    endMinute = m.endMinuteOfDay,
                    isAllDay = false,
                    category = "MEETINGS",
                    colorHex = 0xFF0369A1
                )
            )
        }

        // Weekly recurring university classes
        courses.filter { it.dayOfWeek == persianDow }.forEach { c ->
            result.add(
                UnifiedCalendarItem(
                    id = "course_${c.id}_$epochDay",
                    sourceType = "COURSE",
                    sourceId = c.id,
                    title = "کلاس ${c.title}",
                    subtitle = listOfNotNull(c.professor.takeIf { it.isNotBlank() }, c.room.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { "برنامه دانشگاه" },
                    epochDay = epochDay,
                    startMinute = c.startMinuteOfDay,
                    endMinute = c.endMinuteOfDay,
                    isAllDay = false,
                    category = "UNIVERSITY",
                    colorHex = c.colorHex
                )
            )
        }

        // University exams, assignments, submissions
        val courseMap = courses.associateBy { it.id }
        universityItems.filter { it.dueEpochDay == epochDay }.forEach { u ->
            val courseName = courseMap[u.courseId]?.title ?: "دانشگاه"
            val typeFa = when (u.type) {
                "EXAM" -> "امتحان"
                "SUBMISSION" -> "تحویل پروژه"
                "STUDY" -> "جلسه مطالعه"
                else -> "تکلیف"
            }
            result.add(
                UnifiedCalendarItem(
                    id = "uitem_${u.id}",
                    sourceType = if (u.type == "EXAM") "EXAM" else "ASSIGNMENT",
                    sourceId = u.id,
                    title = "$typeFa: ${u.title}",
                    subtitle = courseName,
                    epochDay = epochDay,
                    startMinute = u.dueTimeMinutes,
                    endMinute = (u.dueTimeMinutes + 60).coerceAtMost(23 * 60 + 59),
                    isAllDay = false,
                    category = if (u.type == "EXAM") "EXAMS" else "UNIVERSITY",
                    colorHex = if (u.type == "EXAM") 0xFFB91C1C else 0xFF4338CA
                )
            )
        }

        return result.sortedWith(
            compareBy<UnifiedCalendarItem> { !it.isHoliday }
                .thenBy { !it.isAllDay }
                .thenBy { it.startMinute ?: Int.MAX_VALUE }
        )
    }

    fun fixedBlocksForDay(epochDay: Long): List<LifePilotDomainLogic.FixedBlock> {
        return unifiedItemsForDay(epochDay)
            .filter { !it.isAllDay && it.startMinute != null && it.endMinute != null }
            .map {
                LifePilotDomainLogic.FixedBlock(
                    title = it.title,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    startMinute = it.startMinute!!,
                    endMinute = it.endMinute!!,
                    colorHex = it.colorHex
                )
            }
    }
}

class LifePilotViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LifePilotDatabase.getInstance(application)
    private val repository = LifePilotRepository(db)
    private val preferencesStore = PreferencesStore(application)

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    private val _rejectedProposalIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isUnlocked = MutableStateFlow(false)

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Combine database streams into clean grouped sub-flows then into LifePilotDataState
    private val financeFlow = combine(
        repository.transactions,
        repository.installments,
        repository.installmentPayments,
        repository.exchangeRates
    ) { txs, insts, pays, rates -> Quad(txs, insts, pays, rates) }

    private val productivityFlow = combine(
        repository.tasks,
        repository.projects,
        repository.milestones,
        repository.meetings
    ) { tasks, projs, miles, meets -> Quad(tasks, projs, miles, meets) }

    private val lifeFlow = combine(
        repository.courses,
        repository.universityItems,
        repository.calendarEvents,
        repository.notes
    ) { courses, uItems, events, notes -> Quad(courses, uItems, events, notes) }

    private val habitsAndSystemFlow = combine(
        repository.habits,
        repository.habitLogs,
        repository.activityLogs,
        repository.notifications
    ) { habits, logs, acts, notifs -> Quad(habits, logs, acts, notifs) }

    private val ephemeralFlow = combine(
        preferencesStore.settingsFlow,
        _searchHistory,
        _rejectedProposalIds,
        _isUnlocked
    ) { settings, hist, rejected, unlocked -> Quad(settings, hist, rejected, unlocked) }

    val uiState: StateFlow<LifePilotDataState> = combine(
        financeFlow,
        productivityFlow,
        lifeFlow,
        habitsAndSystemFlow,
        ephemeralFlow
    ) { fin, prod, life, habSys, eph ->
        LifePilotDataState(
            settings = eph.a,
            transactions = fin.a,
            installments = fin.b,
            installmentPayments = fin.c,
            exchangeRates = fin.d,
            tasks = prod.a,
            projects = prod.b,
            milestones = prod.c,
            meetings = prod.d,
            courses = life.a,
            universityItems = life.b,
            calendarEvents = life.c,
            notes = life.d,
            habits = habSys.a,
            habitLogs = habSys.b,
            activityLogs = habSys.c,
            notifications = habSys.d,
            searchHistory = eph.b,
            rejectedProposalTaskIds = eph.c,
            isUnlocked = eph.d
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LifePilotDataState()
    )

    private fun emitMessage(msg: String) {
        _snackbarMessage.tryEmit(msg)
    }

    fun unlockWithPin(pin: String): Boolean {
        val expected = uiState.value.settings.appLockPin
        if (expected.isBlank() || pin == expected) {
            _isUnlocked.value = true
            return true
        }
        return false
    }

    // --- Settings ---
    fun updateSettings(logDescription: String? = null, transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            preferencesStore.update(transform)
            if (!logDescription.isNullOrBlank()) {
                repository.logSettingsChange(logDescription)
            }
        }
    }

    // --- Finance ---
    fun saveManualExchangeRate(rateIrtPerUsd: Double, notes: String) {
        if (rateIrtPerUsd <= 0) {
            emitMessage("نرخ دلار باید عددی مثبت باشد.")
            return
        }
        viewModelScope.launch {
            repository.saveManualExchangeRate(rateIrtPerUsd, notes)
            emitMessage("نرخ جدید دلار با موفقیت ثبت شد.")
        }
    }

    fun addTransaction(
        title: String,
        description: String,
        amount: Double,
        currency: String,
        type: String,
        category: String,
        dateEpochDay: Long,
        paymentStatus: String,
        rateOverride: Double?,
        notes: String
    ) {
        if (title.isBlank() || amount <= 0) {
            emitMessage("عنوان و مبلغ معتبر وارد کنید.")
            return
        }
        viewModelScope.launch {
            val rate = rateOverride ?: uiState.value.currentUsdRate?.rateIrtPerUsd
            repository.addTransaction(
                title = title,
                description = description,
                amount = amount,
                currency = currency,
                type = type,
                category = category,
                dateEpochDay = dateEpochDay,
                paymentStatus = paymentStatus,
                rateForHistoricalSnapshot = rate,
                notes = notes
            )
            emitMessage("تراکنش مالی ثبت شد.")
        }
    }

    fun deleteTransaction(tx: FinancialTransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            emitMessage("تراکنش حذف شد.")
        }
    }

    // --- Installments ---
    fun createInstallmentPlan(
        title: String,
        creditor: String,
        originalAmount: Double,
        currency: String,
        totalCount: Int,
        paymentAmount: Double,
        frequency: String,
        firstDueEpochDay: Long,
        notes: String
    ) {
        if (title.isBlank() || totalCount <= 0 || paymentAmount <= 0) {
            emitMessage("اطلاعات قسط را به‌صورت کامل وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.createInstallmentPlan(
                title = title,
                creditor = creditor,
                originalAmount = originalAmount,
                currency = currency,
                totalCount = totalCount,
                paymentAmount = paymentAmount,
                frequency = frequency,
                firstDueEpochDay = firstDueEpochDay,
                notes = notes
            )
            if (uiState.value.settings.notificationsEnabled) {
                LocalReminderHelper.showLocalNotification(
                    getApplication(),
                    (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    "قسط جدید: $title",
                    "جدول اقساط در تقویم جلالی ثبت شد."
                )
            }
            emitMessage("جدول اقساط ساخته و به تقویم جلالی متصل شد.")
        }
    }

    fun recordInstallmentPayment(
        payment: InstallmentPaymentEntity,
        installmentTitle: String,
        currency: String,
        amountToPay: Double,
        notes: String,
        recordExpense: Boolean
    ) {
        viewModelScope.launch {
            val ok = repository.recordInstallmentPayment(
                payment = payment,
                installmentTitle = installmentTitle,
                currency = currency,
                additionalPaidAmount = amountToPay,
                paidAtMillis = System.currentTimeMillis(),
                notes = notes,
                alsoRecordExpenseTransaction = recordExpense,
                currentUsdRate = uiState.value.currentUsdRate?.rateIrtPerUsd
            )
            emitMessage(if (ok) "پرداخت قسط با موفقیت ثبت شد." else "این قسط قبلاً تسویه شده یا مبلغ نامعتبر است.")
        }
    }

    fun deleteInstallment(item: InstallmentEntity) {
        viewModelScope.launch {
            repository.deleteInstallment(item)
            emitMessage("قسط و سررسیدهای آن حذف شدند.")
        }
    }

    // --- Tasks ---
    fun saveTask(task: TaskEntity) {
        if (task.title.isBlank()) {
            emitMessage("عنوان وظیفه نمی‌تواند خالی باشد.")
            return
        }
        viewModelScope.launch {
            repository.saveTask(task)
            emitMessage("وظیفه ذخیره شد.")
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch { repository.toggleTaskCompletion(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            emitMessage("وظیفه حذف شد.")
        }
    }

    // --- Projects ---
    fun saveProject(project: ProjectEntity) {
        if (project.title.isBlank()) {
            emitMessage("نام پروژه را وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.saveProject(project)
            emitMessage("پروژه ذخیره شد.")
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            emitMessage("پروژه حذف شد.")
        }
    }

    fun addMilestone(projectId: Long, title: String, dueEpochDay: Long) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addMilestone(ProjectMilestoneEntity(projectId = projectId, title = title.trim(), dueEpochDay = dueEpochDay))
            emitMessage("نقطه عطف اضافه شد.")
        }
    }

    fun toggleMilestone(milestone: ProjectMilestoneEntity) {
        viewModelScope.launch { repository.toggleMilestone(milestone) }
    }

    fun deleteMilestone(milestone: ProjectMilestoneEntity) {
        viewModelScope.launch { repository.deleteMilestone(milestone) }
    }

    // --- University ---
    fun saveCourse(course: UniversityCourseEntity) {
        if (course.title.isBlank()) {
            emitMessage("نام درس را وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.saveCourse(course)
            emitMessage("درس دانشگاهی ثبت شد و در تقویم نمایش داده می‌شود.")
        }
    }

    fun deleteCourse(course: UniversityCourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            emitMessage("درس حذف شد.")
        }
    }

    fun saveUniversityItem(item: UniversityItemEntity) {
        if (item.title.isBlank()) {
            emitMessage("عنوان تکلیف یا امتحان را وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.saveUniversityItem(item)
            emitMessage("رویداد دانشگاهی ذخیره شد.")
        }
    }

    fun toggleUniversityItem(item: UniversityItemEntity) {
        viewModelScope.launch { repository.toggleUniversityItem(item) }
    }

    fun deleteUniversityItem(item: UniversityItemEntity) {
        viewModelScope.launch { repository.deleteUniversityItem(item) }
    }

    // --- Meetings ---
    fun saveMeeting(meeting: MeetingEntity) {
        if (meeting.title.isBlank()) {
            emitMessage("عنوان جلسه را وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.saveMeeting(meeting)
            emitMessage("جلسه زمان‌بندی و به تقویم متصل شد.")
        }
    }

    fun deleteMeeting(meeting: MeetingEntity) {
        viewModelScope.launch {
            repository.deleteMeeting(meeting)
            emitMessage("جلسه حذف شد.")
        }
    }

    fun createFollowUpTaskFromMeeting(meeting: MeetingEntity) {
        viewModelScope.launch {
            val today = JalaliCalendarEngine.todayEpochDay(uiState.value.settings.timeZoneId)
            repository.saveTask(
                TaskEntity(
                    title = "پیگیری جلسه: ${meeting.title}",
                    description = meeting.notes.ifBlank { meeting.description },
                    category = "WORK",
                    priority = "HIGH",
                    startEpochDay = today,
                    deadlineEpochDay = maxOf(today, meeting.dateEpochDay + 1),
                    estimatedMinutes = 30,
                    linkedProjectId = meeting.linkedProjectId
                )
            )
            emitMessage("وظیفه پیگیری برای جلسه ساخته شد.")
        }
    }

    // --- Calendar Events ---
    fun saveCalendarEvent(event: CalendarEventEntity) {
        if (event.title.isBlank()) {
            emitMessage("عنوان رویداد را وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.saveCalendarEvent(event)
            emitMessage("رویداد در تقویم جلالی ذخیره شد.")
        }
    }

    fun deleteCalendarEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(event)
            emitMessage("رویداد حذف شد.")
        }
    }

    // --- Notes ---
    fun saveNote(note: NoteEntity) {
        if (note.title.isBlank() && note.content.isBlank()) return
        viewModelScope.launch {
            repository.saveNote(note.copy(title = note.title.ifBlank { "یادداشت بدون عنوان" }))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            emitMessage("یادداشت حذف شد.")
        }
    }

    // --- Habits ---
    fun saveHabit(habit: HabitEntity) {
        if (habit.title.isBlank()) {
            emitMessage("نام عادت را وارد کنید.")
            return
        }
        viewModelScope.launch {
            repository.saveHabit(habit)
            emitMessage("عادت ذخیره شد.")
        }
    }

    fun adjustHabitProgress(habit: HabitEntity, epochDay: Long, delta: Int) {
        viewModelScope.launch { repository.adjustHabitProgress(habit, epochDay, delta) }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            emitMessage("عادت حذف شد.")
        }
    }

    // --- Smart Planner ---
    fun approveTaskProposal(proposal: LifePilotDomainLogic.ProposedTaskBlock, targetEpochDay: Long) {
        viewModelScope.launch {
            repository.saveCalendarEvent(
                CalendarEventEntity(
                    title = "زمان‌بندی: ${proposal.task.title}",
                    description = "تایید شده از برنامه‌ریز هوشمند (${proposal.reasonFa})",
                    category = "TASKS",
                    startEpochDay = targetEpochDay,
                    endEpochDay = targetEpochDay,
                    startMinuteOfDay = proposal.suggestedStartMinute,
                    endMinuteOfDay = proposal.suggestedEndMinute,
                    isAllDay = false,
                    priority = proposal.task.priority,
                    colorHex = 0xFF0D7A5F
                )
            )
            _rejectedProposalIds.update { it + proposal.task.id }
            emitMessage("بازه زمانی وظیفه در تقویم ثبت شد.")
        }
    }

    fun rejectTaskProposal(taskId: Long) {
        _rejectedProposalIds.update { it + taskId }
    }

    fun resetRejectedProposals() {
        _rejectedProposalIds.value = emptySet()
    }

    // --- Search ---
    fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        _searchHistory.update { list -> (listOf(trimmed) + list.filterNot { it == trimmed }).take(10) }
    }

    // --- Notifications ---
    fun markNotificationRead(id: Long) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            emitMessage("همه اعلان‌ها خوانده شدند.")
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch { repository.deleteNotification(id) }
    }

    fun triggerLocalReminderTest() {
        viewModelScope.launch {
            repository.addNotification(
                title = "یادآور محلی لایف‌پایلوت",
                message = "سامانه اعلان‌های محلی بدون ارسال داده به اینترنت فعال است.",
                category = "SYSTEM"
            )
            LocalReminderHelper.showLocalNotification(
                getApplication(),
                9001,
                "یادآور محلی LifePilot",
                "اعلان‌های محلی با موفقیت فعال شدند."
            )
            emitMessage("اعلان آزمایشی ثبت شد.")
        }
    }

    // --- Backup & Restore ---
    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            val res = repository.restoreBackupJson(json)
            res.onSuccess { count ->
                emitMessage("بازیابی با موفقیت انجام شد (${PersianFormatters.digits(count.toString())} رکورد اصلی).")
            }.onFailure { err ->
                emitMessage("خطا در بازیابی پشتیبان: ${err.localizedMessage ?: "فرمت نامعتبر"}")
            }
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            repository.clearAllData()
            emitMessage("تمام داده‌های محلی پاکسازی شدند.")
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
