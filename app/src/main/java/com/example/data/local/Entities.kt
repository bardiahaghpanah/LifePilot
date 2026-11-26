package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "financial_transactions")
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val amount: Double,
    val currency: String = "IRT", // "IRT" or "USD"
    val type: String = "EXPENSE", // "INCOME" or "EXPENSE"
    val category: String = "عمومی",
    val dateEpochDay: Long,
    val paymentStatus: String = "PAID", // "PAID", "PENDING"
    val historicalRateUsed: Double? = null, // Preserved USD->IRT rate at time of record
    val convertedAmountIrt: Double? = null, // Preserved historical conversion result
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val creditor: String,
    val originalAmount: Double,
    val currency: String = "IRT", // "IRT" or "USD"
    val totalCount: Int,
    val paymentAmount: Double,
    val frequency: String = "MONTHLY", // "MONTHLY", "WEEKLY", "BIWEEKLY", "QUARTERLY"
    val firstDueEpochDay: Long,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "installment_payments",
    indices = [Index(value = ["installmentId", "installmentIndex"], unique = true)]
)
data class InstallmentPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val installmentId: Long,
    val installmentIndex: Int, // 1..totalCount
    val dueEpochDay: Long,
    val expectedAmount: Double,
    val paidAmount: Double = 0.0,
    val status: String = "PENDING", // "PENDING", "PARTIAL", "PAID" (never auto-marked paid!)
    val paidAtMillis: Long? = null,
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rateIrtPerUsd: Double,
    val effectiveAtMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "PERSONAL", // WORK, UNIVERSITY, PERSONAL, PROJECTS, OTHER
    val priority: String = "MEDIUM",   // LOW, MEDIUM, HIGH, URGENT
    val startEpochDay: Long,
    val deadlineEpochDay: Long,
    val estimatedMinutes: Int = 30,
    val actualMinutes: Int = 0,
    val status: String = "PENDING",    // PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    val reminderMinutesBefore: Int = 30,
    val linkedProjectId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startEpochDay: Long,
    val endEpochDay: Long,
    val priority: String = "MEDIUM",
    val status: String = "ACTIVE", // PLANNING, ACTIVE, ON_HOLD, COMPLETED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "project_milestones", indices = [Index("projectId")])
data class ProjectMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val dueEpochDay: Long,
    val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "university_courses")
data class UniversityCourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val code: String = "",
    val professor: String = "",
    val dayOfWeek: Int = 0, // 0=Sat .. 6=Fri
    val startMinuteOfDay: Int = 9 * 60,
    val endMinuteOfDay: Int = 10 * 60 + 30,
    val room: String = "",
    val credits: Int = 3,
    val colorHex: Long = 0xFF1D4ED8,
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "university_items", indices = [Index("courseId")])
data class UniversityItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val type: String = "ASSIGNMENT", // ASSIGNMENT, EXAM, SUBMISSION, STUDY
    val dueEpochDay: Long,
    val dueTimeMinutes: Int = 10 * 60,
    val status: String = "PENDING", // PENDING, COMPLETED
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dateEpochDay: Long,
    val startMinuteOfDay: Int = 10 * 60,
    val endMinuteOfDay: Int = 11 * 60,
    val location: String = "",
    val onlineLink: String = "",
    val participants: String = "",
    val reminderMinutesBefore: Int = 15,
    val status: String = "SCHEDULED", // SCHEDULED, COMPLETED, CANCELLED
    val linkedProjectId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "PERSONAL", // WORK, UNIVERSITY, MEETINGS, PERSONAL, INSTALLMENTS, TASKS, PROJECTS, EXAMS, APPOINTMENTS
    val startEpochDay: Long,
    val endEpochDay: Long,
    val startMinuteOfDay: Int = 9 * 60,
    val endMinuteOfDay: Int = 10 * 60,
    val isAllDay: Boolean = false,
    val location: String = "",
    val participants: String = "",
    val priority: String = "MEDIUM",
    val colorHex: Long = 0xFF1E3A8A,
    val recurrenceRule: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    val recurrenceInterval: Int = 1,
    val recurrenceEndEpochDay: Long? = null,
    val reminderMinutesBefore: Int = 15,
    val isCustomHoliday: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val category: String = "شخصی",
    val tags: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val linkedEntityType: String? = null,
    val linkedEntityId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val scheduleType: String = "DAILY", // DAILY, WEEKLY
    val scheduledDaysMask: Int = 0b1111111, // bit 0=Sat .. bit 6=Fri
    val targetCount: Int = 1,
    val reminderMinutesOfDay: Int = 20 * 60,
    val colorHex: Long = 0xFF0F766E,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "dateEpochDay"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val dateEpochDay: Long,
    val completedCount: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val entityType: String,
    val entityId: Long,
    val description: String,
    val previousValue: String = "",
    val newValue: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val category: String,
    val entityType: String = "",
    val entityId: Long = 0,
    val triggerAtMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class LifePilotBackupPayload(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
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
    val notifications: List<NotificationEntity> = emptyList()
)
