package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM financial_transactions ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeTransactions(): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions")
    suspend fun getAllTransactions(): List<FinancialTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: FinancialTransactionEntity): Long

    @Update
    suspend fun updateTransaction(tx: FinancialTransactionEntity)

    @Delete
    suspend fun deleteTransaction(tx: FinancialTransactionEntity)

    @Query("DELETE FROM financial_transactions")
    suspend fun clearTransactions()

    @Query("SELECT * FROM installments ORDER BY createdAt DESC")
    fun observeInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments")
    suspend fun getAllInstallments(): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(item: InstallmentEntity): Long

    @Update
    suspend fun updateInstallment(item: InstallmentEntity)

    @Delete
    suspend fun deleteInstallment(item: InstallmentEntity)

    @Query("DELETE FROM installments")
    suspend fun clearInstallments()

    @Query("SELECT * FROM installment_payments ORDER BY dueEpochDay ASC, installmentIndex ASC")
    fun observeInstallmentPayments(): Flow<List<InstallmentPaymentEntity>>

    @Query("SELECT * FROM installment_payments")
    suspend fun getAllInstallmentPayments(): List<InstallmentPaymentEntity>

    @Query("SELECT * FROM installment_payments WHERE installmentId = :installmentId ORDER BY installmentIndex ASC")
    suspend fun getPaymentsForInstallment(installmentId: Long): List<InstallmentPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayments(payments: List<InstallmentPaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayments(payments: List<InstallmentPaymentEntity>)

    @Update
    suspend fun updatePayment(payment: InstallmentPaymentEntity)

    @Query("DELETE FROM installment_payments WHERE installmentId = :installmentId")
    suspend fun deletePaymentsForInstallment(installmentId: Long)

    @Query("DELETE FROM installment_payments")
    suspend fun clearInstallmentPayments()

    @Query("SELECT * FROM exchange_rates ORDER BY effectiveAtMillis DESC")
    fun observeExchangeRates(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates ORDER BY effectiveAtMillis DESC")
    suspend fun getAllExchangeRates(): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRate(rate: ExchangeRateEntity): Long

    @Query("DELETE FROM exchange_rates")
    suspend fun clearExchangeRates()
}

@Dao
interface ProductivityDao {
    @Query("SELECT * FROM tasks ORDER BY deadlineEpochDay ASC, createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("SELECT * FROM projects ORDER BY endEpochDay ASC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects")
    suspend fun clearProjects()

    @Query("SELECT * FROM project_milestones ORDER BY dueEpochDay ASC")
    fun observeMilestones(): Flow<List<ProjectMilestoneEntity>>

    @Query("SELECT * FROM project_milestones")
    suspend fun getAllMilestones(): List<ProjectMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: ProjectMilestoneEntity): Long

    @Update
    suspend fun updateMilestone(milestone: ProjectMilestoneEntity)

    @Delete
    suspend fun deleteMilestone(milestone: ProjectMilestoneEntity)

    @Query("DELETE FROM project_milestones WHERE projectId = :projectId")
    suspend fun deleteMilestonesForProject(projectId: Long)

    @Query("DELETE FROM project_milestones")
    suspend fun clearMilestones()
}

@Dao
interface LifeModulesDao {
    @Query("SELECT * FROM university_courses ORDER BY dayOfWeek ASC, startMinuteOfDay ASC")
    fun observeCourses(): Flow<List<UniversityCourseEntity>>

    @Query("SELECT * FROM university_courses")
    suspend fun getAllCourses(): List<UniversityCourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: UniversityCourseEntity): Long

    @Update
    suspend fun updateCourse(course: UniversityCourseEntity)

    @Delete
    suspend fun deleteCourse(course: UniversityCourseEntity)

    @Query("DELETE FROM university_courses")
    suspend fun clearCourses()

    @Query("SELECT * FROM university_items ORDER BY dueEpochDay ASC, dueTimeMinutes ASC")
    fun observeUniversityItems(): Flow<List<UniversityItemEntity>>

    @Query("SELECT * FROM university_items")
    suspend fun getAllUniversityItems(): List<UniversityItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUniversityItem(item: UniversityItemEntity): Long

    @Update
    suspend fun updateUniversityItem(item: UniversityItemEntity)

    @Delete
    suspend fun deleteUniversityItem(item: UniversityItemEntity)

    @Query("DELETE FROM university_items WHERE courseId = :courseId")
    suspend fun deleteItemsForCourse(courseId: Long)

    @Query("DELETE FROM university_items")
    suspend fun clearUniversityItems()

    @Query("SELECT * FROM meetings ORDER BY dateEpochDay ASC, startMinuteOfDay ASC")
    fun observeMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings")
    suspend fun getAllMeetings(): List<MeetingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Delete
    suspend fun deleteMeeting(meeting: MeetingEntity)

    @Query("DELETE FROM meetings")
    suspend fun clearMeetings()

    @Query("SELECT * FROM calendar_events ORDER BY startEpochDay ASC, startMinuteOfDay ASC")
    fun observeCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events")
    suspend fun getAllCalendarEvents(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateCalendarEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteCalendarEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("SELECT * FROM habits ORDER BY isArchived ASC, isPaused ASC, createdAt DESC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAllHabits(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    @Query("SELECT * FROM habit_logs ORDER BY dateEpochDay DESC")
    fun observeHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllHabitLogs(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getHabitLog(habitId: Long, dateEpochDay: Long): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay")
    suspend fun deleteHabitLog(habitId: Long, dateEpochDay: Long)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: Long)

    @Query("DELETE FROM habit_logs")
    suspend fun clearHabitLogs()
}

@Dao
interface SystemDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestampMillis DESC LIMIT 500")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs ORDER BY timestampMillis DESC")
    suspend fun getAllActivityLogs(): List<ActivityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()

    @Query("SELECT * FROM notifications ORDER BY isRead ASC, createdAt DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotifications(): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()
}
