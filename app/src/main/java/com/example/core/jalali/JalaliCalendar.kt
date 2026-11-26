package com.example.core.jalali

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Accurate Jalali (Solar Hijri / Persian) calendar engine using the Kazimierz Borkowski
 * astronomical algorithm (matching jalaali-js / CLDR Persian calendar across years 1..3177).
 * Handles 29/30/31-day months, leap years (Esfand 30), Gregorian <-> Jalali conversion,
 * Persian week numbering (Saturday = 0 .. Friday = 6), Iranian official holidays, and formatting.
 */
data class JalaliDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<JalaliDate> {
    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toEpochDay(): Long = JalaliCalendarEngine.jalaliToEpochDay(year, month, day)

    fun dayOfWeekPersian(): Int = JalaliCalendarEngine.epochDayToPersianDayOfWeek(toEpochDay())

    fun plusDays(days: Long): JalaliDate = JalaliCalendarEngine.epochDayToJalali(toEpochDay() + days)

    fun plusMonths(months: Int): JalaliDate {
        val totalMonths = (year * 12) + (month - 1) + months
        val newYear = Math.floorDiv(totalMonths, 12)
        val newMonth = Math.floorMod(totalMonths, 12) + 1
        val maxDay = JalaliCalendarEngine.daysInJalaliMonth(newYear, newMonth)
        return JalaliDate(newYear, newMonth, day.coerceAtMost(maxDay))
    }

    fun plusYears(years: Int): JalaliDate {
        val newYear = year + years
        val maxDay = JalaliCalendarEngine.daysInJalaliMonth(newYear, month)
        return JalaliDate(newYear, month, day.coerceAtMost(maxDay))
    }

    fun formatNumeric(usePersianDigits: Boolean = true): String {
        val raw = "%04d/%02d/%02d".format(Locale.US, year, month, day)
        return PersianFormatters.digits(raw, usePersianDigits)
    }

    fun formatFull(usePersianDigits: Boolean = true): String {
        val dayName = JalaliCalendarEngine.PERSIAN_WEEKDAYS[dayOfWeekPersian()]
        val monthName = JalaliCalendarEngine.PERSIAN_MONTHS[month - 1]
        return PersianFormatters.digits("$dayName $day $monthName $year", usePersianDigits)
    }

    fun formatShort(usePersianDigits: Boolean = true): String {
        val monthName = JalaliCalendarEngine.PERSIAN_MONTHS[month - 1]
        return PersianFormatters.digits("$day $monthName", usePersianDigits)
    }

    fun formatMonthYear(usePersianDigits: Boolean = true): String {
        val monthName = JalaliCalendarEngine.PERSIAN_MONTHS[month - 1]
        return PersianFormatters.digits("$monthName $year", usePersianDigits)
    }

    fun weekOfYear(): Int {
        val startOfYear = JalaliDate(year, 1, 1)
        val dayOfYear = (toEpochDay() - startOfYear.toEpochDay()).toInt()
        val firstDayOffset = startOfYear.dayOfWeekPersian()
        return ((dayOfYear + firstDayOffset) / 7) + 1
    }
}

data class GregorianDate(val year: Int, val month: Int, val day: Int) {
    fun formatNumeric(): String = "%04d-%02d-%02d".format(Locale.US, year, month, day)
}

data class IranianHoliday(
    val month: Int,
    val day: Int,
    val title: String,
    val isOfficialHoliday: Boolean = true
)

object JalaliCalendarEngine {

    val PERSIAN_MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val PERSIAN_WEEKDAYS = listOf(
        "شنبه", "یک‌شنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    val PERSIAN_WEEKDAYS_SHORT = listOf(
        "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    // Standard Borkowski break years for exact solar Hijri leap year calculation
    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private data class JalaliCalInfo(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): JalaliCalInfo {
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1

        val leapG = (gy / 4) - (((gy / 100) + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG

        if (jump - n < 6) {
            n = n - jump + ((jump + 4) / 33) * 33
        }
        var leap = ((((n + 1) % 33) - 1) % 4)
        if (leap == -1) leap = 4

        return JalaliCalInfo(leap = leap, gy = gy, march = march)
    }

    fun isJalaliLeapYear(jy: Int): Boolean = jalCal(jy).leap == 0

    fun daysInJalaliMonth(jy: Int, jm: Int): Int = when (jm) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isJalaliLeapYear(jy)) 30 else 29
        else -> throw IllegalArgumentException("Invalid Jalali month: $jm")
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Long {
        val y = gy.toLong()
        val m = gm.toLong()
        val d = gd.toLong()
        val d1 = ((y + (m - 8) / 6 + 100100) * 1461) / 4 +
            (153 * ((m + 9) % 12) + 2) / 5 + d - 34840408
        return d1 - (((y + 100100 + (m - 8) / 6) / 100) * 3) / 4 + 752
    }

    private fun d2g(jdn: Long): GregorianDate {
        var j = 4 * jdn + 139361631
        j += (((4 * jdn + 183187720) / 146097) * 3) / 4 * 4 - 3908
        val i = ((j % 1461) / 4) * 5 + 308
        val gd = ((i % 153) / 5 + 1).toInt()
        val gm = (((i / 153) % 12) + 1).toInt()
        val gy = (j / 1461 - 100100 + (8 - gm) / 6).toInt()
        return GregorianDate(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Long {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31L - (jm / 7) * (jm - 7L) + jd - 1
    }

    private fun d2j(jdn: Long): JalaliDate {
        val g = d2g(jdn)
        var jy = g.year - 621
        val r = jalCal(jy)
        val jdn1f = g2d(g.year, 3, r.march)
        var k = jdn - jdn1f
        return if (k >= 0) {
            if (k <= 185) {
                val jm = (1 + k / 31).toInt()
                val jd = ((k % 31) + 1).toInt()
                JalaliDate(jy, jm, jd)
            } else {
                k -= 186
                val jm = (7 + k / 30).toInt()
                val jd = ((k % 30) + 1).toInt()
                JalaliDate(jy, jm, jd)
            }
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
            val jm = (7 + k / 30).toInt()
            val jd = ((k % 30) + 1).toInt()
            JalaliDate(jy, jm, jd)
        }
    }

    // Unix epoch 1970-01-01 corresponds to JDN 2440588
    private const val UNIX_EPOCH_JDN = 2440588L

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate = d2j(g2d(gy, gm, gd))

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): GregorianDate = d2g(j2d(jy, jm, jd))

    fun jalaliToEpochDay(jy: Int, jm: Int, jd: Int): Long = j2d(jy, jm, jd) - UNIX_EPOCH_JDN

    fun epochDayToJalali(epochDay: Long): JalaliDate = d2j(epochDay + UNIX_EPOCH_JDN)

    fun epochDayToGregorian(epochDay: Long): GregorianDate = d2g(epochDay + UNIX_EPOCH_JDN)

    /**
     * 1970-01-01 was Thursday (پنج‌شنبه = index 5 in Saturday-first Persian week).
     * Returns 0 = Saturday (شنبه) .. 6 = Friday (جمعه).
     */
    fun epochDayToPersianDayOfWeek(epochDay: Long): Int {
        return Math.floorMod(epochDay + 5L, 7L).toInt()
    }

    fun todayJalali(timeZoneId: String = "Asia/Tehran"): JalaliDate {
        val tz = TimeZone.getTimeZone(timeZoneId)
        val cal = Calendar.getInstance(tz)
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun todayEpochDay(timeZoneId: String = "Asia/Tehran"): Long = todayJalali(timeZoneId).toEpochDay()

    fun currentTimeFormatted(timeZoneId: String = "Asia/Tehran", usePersianDigits: Boolean = true): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return PersianFormatters.digits("%02d:%02d".format(Locale.US, h, m), usePersianDigits)
    }

    fun currentGreeting(timeZoneId: String = "Asia/Tehran"): String {
        val h = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11 -> "صبح بخیر"
            in 12..15 -> "ظهر بخیر"
            in 16..19 -> "عصر بخیر"
            else -> "شب بخیر"
        }
    }

    fun formatMinuteOfDay(minutes: Int, usePersianDigits: Boolean = true): String {
        val clamped = minutes.coerceIn(0, 23 * 60 + 59)
        val h = clamped / 60
        val m = clamped % 60
        return PersianFormatters.digits("%02d:%02d".format(Locale.US, h, m), usePersianDigits)
    }

    /**
     * Official Solar Hijri Iranian national holidays & key cultural occasions.
     */
    val OFFICIAL_SOLAR_HOLIDAYS: List<IranianHoliday> = listOf(
        IranianHoliday(1, 1, "جشن نوروز (آغاز سال نو)", true),
        IranianHoliday(1, 2, "عید نوروز", true),
        IranianHoliday(1, 3, "عید نوروز", true),
        IranianHoliday(1, 4, "عید نوروز", true),
        IranianHoliday(1, 12, "روز جمهوری اسلامی", true),
        IranianHoliday(1, 13, "جشن سیزده‌بدر (روز طبیعت)", true),
        IranianHoliday(3, 14, "رحلت امام خمینی (ره)", true),
        IranianHoliday(3, 15, "قیام ۱۵ خرداد", true),
        IranianHoliday(9, 30, "شب یلدا (چله)", false),
        IranianHoliday(11, 22, "پیروزی انقلاب اسلامی", true),
        IranianHoliday(12, 29, "روز ملی شدن صنعت نفت ایران", true)
    )

    fun holidaysForDate(date: JalaliDate): List<IranianHoliday> {
        return OFFICIAL_SOLAR_HOLIDAYS.filter { it.month == date.month && it.day == date.day }
    }

    fun isFriday(date: JalaliDate): Boolean = date.dayOfWeekPersian() == 6

    /**
     * Checks whether a recurring event starting at [startEpochDay] occurs on [targetEpochDay].
     */
    fun occursOnDay(
        startEpochDay: Long,
        endEpochDay: Long,
        recurrenceRule: String,
        recurrenceInterval: Int,
        recurrenceEndEpochDay: Long?,
        targetEpochDay: Long
    ): Boolean {
        val interval = recurrenceInterval.coerceAtLeast(1)
        if (targetEpochDay < startEpochDay) return false
        if (recurrenceEndEpochDay != null && targetEpochDay > recurrenceEndEpochDay) return false

        val spanDays = (endEpochDay - startEpochDay).coerceAtLeast(0L)
        if (recurrenceRule == "NONE" || recurrenceRule.isBlank()) {
            return targetEpochDay in startEpochDay..(startEpochDay + spanDays)
        }

        val diff = targetEpochDay - startEpochDay
        return when (recurrenceRule) {
            "DAILY" -> (diff % interval) <= spanDays
            "WEEKLY" -> {
                val period = interval * 7L
                (diff % period) <= spanDays
            }
            "MONTHLY" -> {
                val startJ = epochDayToJalali(startEpochDay)
                val targetJ = epochDayToJalali(targetEpochDay)
                val monthDiff = (targetJ.year - startJ.year) * 12 + (targetJ.month - startJ.month)
                if (monthDiff < 0 || monthDiff % interval != 0) return false
                val expectedDay = startJ.day.coerceAtMost(daysInJalaliMonth(targetJ.year, targetJ.month))
                targetJ.day == expectedDay
            }
            "YEARLY" -> {
                val startJ = epochDayToJalali(startEpochDay)
                val targetJ = epochDayToJalali(targetEpochDay)
                val yearDiff = targetJ.year - startJ.year
                if (yearDiff < 0 || yearDiff % interval != 0) return false
                val expectedDay = startJ.day.coerceAtMost(daysInJalaliMonth(targetJ.year, startJ.month))
                targetJ.month == startJ.month && targetJ.day == expectedDay
            }
            else -> targetEpochDay in startEpochDay..(startEpochDay + spanDays)
        }
    }
}

object PersianFormatters {
    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun digits(input: String, usePersian: Boolean = true): String {
        if (!usePersian) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(PERSIAN_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toLatinDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatIrt(amount: Double, usePersianDigits: Boolean = true, showUnit: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '٬'
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0", symbols)
        val sign = if (amount < 0) "−" else ""
        val formatted = sign + df.format(abs(amount))
        val text = if (showUnit) "$formatted تومان" else formatted
        return digits(text, usePersianDigits)
    }

    fun formatUsd(amount: Double, usePersianDigits: Boolean = true, showUnit: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.##", symbols)
        val sign = if (amount < 0) "−" else ""
        val formatted = sign + df.format(abs(amount))
        val text = if (showUnit) "$$formatted دلار" else "$$formatted"
        return digits(text, usePersianDigits)
    }

    fun formatCurrency(amount: Double, currency: String, usePersianDigits: Boolean = true): String {
        return if (currency == "USD") formatUsd(amount, usePersianDigits) else formatIrt(amount, usePersianDigits)
    }

    fun formatNumber(value: Long, usePersianDigits: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '٬' }
        return digits(DecimalFormat("#,##0", symbols).format(value), usePersianDigits)
    }

    fun formatPercent(ratio: Float, usePersianDigits: Boolean = true): String {
        val pct = (ratio.coerceIn(0f, 1f) * 100f).toInt()
        return digits("$pct٪", usePersianDigits)
    }
}
