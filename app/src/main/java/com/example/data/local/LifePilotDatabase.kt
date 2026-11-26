package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FinancialTransactionEntity::class,
        InstallmentEntity::class,
        InstallmentPaymentEntity::class,
        ExchangeRateEntity::class,
        TaskEntity::class,
        ProjectEntity::class,
        ProjectMilestoneEntity::class,
        UniversityCourseEntity::class,
        UniversityItemEntity::class,
        MeetingEntity::class,
        CalendarEventEntity::class,
        NoteEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        ActivityLogEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LifePilotDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun productivityDao(): ProductivityDao
    abstract fun lifeModulesDao(): LifeModulesDao
    abstract fun systemDao(): SystemDao

    companion object {
        @Volatile
        private var INSTANCE: LifePilotDatabase? = null

        fun getInstance(context: Context): LifePilotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifePilotDatabase::class.java,
                    "lifepilot_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
