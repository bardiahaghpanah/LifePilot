package com.example

import com.example.core.jalali.JalaliCalendarEngine
import com.example.core.jalali.JalaliDate
import com.example.data.local.HabitEntity
import com.example.data.local.HabitLogEntity
import com.example.data.local.TaskEntity
import com.example.data.repository.LifePilotDomainLogic
import org.junit.Assert.*
import org.junit.Test

class LifePilotCoreUnitTest {

    @Test
    fun jalaliLeapYearsAndEsfandHandlingAreExact() {
        // Known leap years in Persian calendar
        assertTrue(JalaliCalendarEngine.isJalaliLeapYear(1395))
        assertTrue(JalaliCalendarEngine.isJalaliLeapYear(1399))
        assertTrue(JalaliCalendarEngine.isJalaliLeapYear(1403))
        assertTrue(JalaliCalendarEngine.isJalaliLeapYear(1408))

        // Known common years
        assertFalse(JalaliCalendarEngine.isJalaliLeapYear(1402))
        assertFalse(JalaliCalendarEngine.isJalaliLeapYear(1404))
        assertFalse(JalaliCalendarEngine.isJalaliLeapYear(1405))

        // Esfand length
        assertEquals(30, JalaliCalendarEngine.daysInJalaliMonth(1403, 12))
        assertEquals(29, JalaliCalendarEngine.daysInJalaliMonth(1404, 12))
        assertEquals(29, JalaliCalendarEngine.daysInJalaliMonth(1405, 12))
        assertEquals(31, JalaliCalendarEngine.daysInJalaliMonth(1405, 1))
        assertEquals(30, JalaliCalendarEngine.daysInJalaliMonth(1405, 7))
    }

    @Test
    fun gregorianJalaliConversionMatchesKnownDatesAndRoundTrips() {
        assertEquals(JalaliDate(1403, 12, 30), JalaliCalendarEngine.gregorianToJalali(2025, 3, 20))
        assertEquals(JalaliDate(1404, 1, 1), JalaliCalendarEngine.gregorianToJalali(2025, 3, 21))
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendarEngine.gregorianToJalali(2026, 3, 21))
        assertEquals(JalaliDate(1405, 9, 5), JalaliCalendarEngine.gregorianToJalali(2026, 11, 26))

        val g = JalaliCalendarEngine.jalaliToGregorian(1405, 9, 5)
        assertEquals(2026, g.year)
        assertEquals(11, g.month)
        assertEquals(26, g.day)

        // Round-trip 10,000 consecutive days across leap boundaries
        val start = JalaliDate(1390, 1, 1).toEpochDay()
        for (offset in 0L until 10_000L) {
            val ep = start + offset
            val j = JalaliCalendarEngine.epochDayToJalali(ep)
            assertEquals(ep, j.toEpochDay())
        }
    }

    @Test
    fun recurringEventsExpandAccurately() {
        val start = JalaliDate(1405, 1, 15).toEpochDay()
        // Monthly recurrence on 15th of each month
        assertTrue(JalaliCalendarEngine.occursOnDay(start, start, "MONTHLY", 1, null, JalaliDate(1405, 2, 15).toEpochDay()))
        assertFalse(JalaliCalendarEngine.occursOnDay(start, start, "MONTHLY", 1, null, JalaliDate(1405, 2, 16).toEpochDay()))
        // Weekly recurrence
        assertTrue(JalaliCalendarEngine.occursOnDay(start, start, "WEEKLY", 1, null, start + 14))
        assertFalse(JalaliCalendarEngine.occursOnDay(start, start, "WEEKLY", 1, null, start + 15))
    }

    @Test
    fun manualUsdToIrtConversionIsExact() {
        val rate = 270_000.0
        assertEquals(2_700_000.0, LifePilotDomainLogic.convertUsdToIrt(10.0, rate), 0.001)
        assertEquals(10.0, LifePilotDomainLogic.convertIrtToUsd(2_700_000.0, rate), 0.001)
    }

    @Test
    fun installmentScheduleStepsJalaliMonthsAndNeverAutoMarksPaid() {
        val firstDue = JalaliDate(1404, 10, 30).toEpochDay()
        val schedule = LifePilotDomainLogic.generateInstallmentSchedule(
            installmentId = 1L,
            totalCount = 3,
            paymentAmount = 5_000_000.0,
            frequency = "MONTHLY",
            firstDueEpochDay = firstDue
        )
        assertEquals(3, schedule.size)
        // Month 1: 1404/10/30, Month 2: 1404/11/30, Month 3: 1404/12/29 (non-leap Esfand clamps 30->29!)
        assertEquals(JalaliDate(1404, 10, 30), JalaliCalendarEngine.epochDayToJalali(schedule[0].dueEpochDay))
        assertEquals(JalaliDate(1404, 11, 30), JalaliCalendarEngine.epochDayToJalali(schedule[1].dueEpochDay))
        assertEquals(JalaliDate(1404, 12, 29), JalaliCalendarEngine.epochDayToJalali(schedule[2].dueEpochDay))
        // Never auto-marked paid
        assertTrue(schedule.all { it.status == "PENDING" && it.paidAmount == 0.0 })
    }

    @Test
    fun habitStreakIgnoresUnscheduledDays() {
        // Schedule only Saturday (bit 0) and Monday (bit 2)
        val mask = (1 shl 0) or (1 shl 2)
        val habit = HabitEntity(id = 7L, title = "باشگاه", scheduleType = "WEEKLY", scheduledDaysMask = mask, targetCount = 1)
        // 1405/09/05 is Thursday (dow=5); let's find a Monday and preceding Saturday
        val sat = JalaliDate(1405, 8, 30).toEpochDay() // Saturday (dow=0)
        assertEquals(0, JalaliCalendarEngine.epochDayToPersianDayOfWeek(sat))
        val mon = sat + 2 // Monday (dow=2)
        assertEquals(2, JalaliCalendarEngine.epochDayToPersianDayOfWeek(mon))

        val logs = listOf(
            HabitLogEntity(habitId = 7L, dateEpochDay = sat, completedCount = 1),
            HabitLogEntity(habitId = 7L, dateEpochDay = mon, completedCount = 1)
        )
        // Evaluate on Tuesday (mon + 1, unscheduled): streak must be 2!
        val stats = LifePilotDomainLogic.calculateHabitStats(habit, logs, mon + 1)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun smartPlannerDetectsConflictsAndSchedulesInFreeSlots() {
        val fixed = listOf(
            LifePilotDomainLogic.FixedBlock("جلسه تیم", "MEETING", 1, 9 * 60, 10 * 60 + 30, 0xFF000000),
            LifePilotDomainLogic.FixedBlock("کلاس دانشگاه", "COURSE", 2, 10 * 60, 11 * 60, 0xFF000000)
        )
        val conflicts = LifePilotDomainLogic.detectConflicts(fixed)
        assertEquals(1, conflicts.size)

        val today = JalaliDate(1405, 9, 5).toEpochDay()
        val tasks = listOf(
            TaskEntity(id = 10, title = "تحویل تمرین", startEpochDay = today, deadlineEpochDay = today, priority = "URGENT", estimatedMinutes = 45)
        )
        val proposals = LifePilotDomainLogic.buildSmartTaskProposals(fixed, tasks, today)
        assertEquals(1, proposals.size)
        // First free window starts at 08:00 (480) before 09:00 (540)
        assertEquals(8 * 60, proposals[0].suggestedStartMinute)
        assertEquals(8 * 60 + 45, proposals[0].suggestedEndMinute)
    }
}
