package ir.codecrafter.plasticproducts.util

import java.util.Locale

/**
 * Self-contained Gregorian→Jalali (Persian) calendar conversion + display
 * formatting. The day-of-month and year in the output use Persian digits
 * (۰-۹) — explicitly requested for these two functions only. The optional
 * hour:minute suffix in [toJalaliDateTime] is kept in plain Latin digits
 * instead, matching every other numeric display in this app (prices,
 * quantities, VisitorPerformanceScreen's average-delivery hours via
 * String.format(Locale.US, ...)); Latin digits elsewhere in the app are
 * untouched by this file — this Persian-digit conversion is local to
 * [jalaliDateLabel] and not exposed for reuse elsewhere. Locale.US is used
 * below for the same reason VisitorPerformanceScreen uses it: String.format's
 * %d is locale-sensitive and would silently emit Persian digits on a device
 * set to a Persian locale with an extended numbering system.
 */
object PersianDateFormatter {

    private val PERSIAN_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** Converts a non-negative Int's Latin-digit string form to Persian digits — used only for the date's day/year. */
    private fun toPersianDigits(value: Int): String =
        value.toString().map { ch -> if (ch in '0'..'9') PERSIAN_DIGITS[ch - '0'] else ch }.joinToString("")

    /** Linear day count for a Gregorian date, shared by [gregorianToJalali] and [isJalaliLeapYear]. */
    private fun gregorianEpochDays(gy: Int, gm: Int, gd: Int): Int {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        return 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
    }

    /** Standard days-since-epoch based Gregorian→Jalali conversion, verified against known reference dates. */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        var days = gregorianEpochDays(gy, gm, gd)
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return Triple(jy, jm, jd)
    }

    /** Jalali→Gregorian companion to [gregorianToJalali], from the same well-known algorithm pairing. */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy2 = jy + 1595
        var days = -355668 + (365 * jy2) + ((jy2 / 33) * 8) + ((jy2 % 33 + 3) / 4) + jd
        days += if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * ((days - 1) / 36524)
            days = (days - 1) % 36524
            if (days >= 365) days += 1
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (isGregorianLeapYear(gy)) gDaysInMonth[1] = 29
        var gm = 0
        while (gm < 12 && gd > gDaysInMonth[gm]) {
            gd -= gDaysInMonth[gm]
            gm++
        }
        gm++
        return Triple(gy, gm, gd)
    }

    private fun isGregorianLeapYear(gy: Int): Boolean = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0

    /**
     * Derived directly from this file's own [jalaliToGregorian] — not a
     * separate external leap-year table or algorithm. A Jalali year is leap
     * exactly when the Gregorian gap between its Farvardin 1st and the next
     * year's Farvardin 1st is 366 days instead of 365. This keeps
     * leap-year/month-length logic permanently self-consistent with whatever
     * gregorianToJalali/jalaliToGregorian actually compute, independent of
     * whether that matches the real astronomical Iranian calendar exactly —
     * this app's own conversion is the single source of truth other code in
     * this file relies on, so this can't silently drift out of sync with it.
     */
    fun isJalaliLeapYear(jy: Int): Boolean {
        val (gy1, gm1, gd1) = jalaliToGregorian(jy, 1, 1)
        val (gy2, gm2, gd2) = jalaliToGregorian(jy + 1, 1, 1)
        val dayCount = gregorianEpochDays(gy2, gm2, gd2) - gregorianEpochDays(gy1, gm1, gd1)
        return dayCount == 366
    }

    /** Farvardin-Shahrivar (1-6): 31 days. Mehr-Bahman (7-11): 30 days. Esfand (12): 29, or 30 in a leap year. */
    fun daysInJalaliMonth(jy: Int, jm: Int): Int = when {
        jm in 1..6 -> 31
        jm in 7..11 -> 30
        jm == 12 -> if (isJalaliLeapYear(jy)) 30 else 29
        else -> throw IllegalArgumentException("invalid Jalali month: $jm")
    }

    private fun parseDateParts(isoString: String): Triple<Int, Int, Int>? {
        if (isoString.length < 10) return null
        val segments = isoString.substring(0, 10).split("-")
        if (segments.size != 3) return null
        val y = segments[0].toIntOrNull() ?: return null
        val m = segments[1].toIntOrNull() ?: return null
        val d = segments[2].toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        return Triple(y, m, d)
    }

    private fun parseTimeParts(isoString: String): Pair<Int, Int>? {
        val tIndex = isoString.indexOf('T')
        if (tIndex == -1 || isoString.length < tIndex + 6) return null
        val segments = isoString.substring(tIndex + 1, tIndex + 6).split(":")
        if (segments.size != 2) return null
        val h = segments[0].toIntOrNull() ?: return null
        val mi = segments[1].toIntOrNull() ?: return null
        if (h !in 0..23 || mi !in 0..59) return null
        return Pair(h, mi)
    }

    private fun jalaliDateLabel(jy: Int, jm: Int, jd: Int): String =
        "${toPersianDigits(jd)} ${PERSIAN_MONTH_NAMES[jm - 1]} ${toPersianDigits(jy)}"

    /**
     * "2026-09-12" or "2026-09-12T23:03:37.619105Z" -> "۲۱ شهریور ۱۴۰۵".
     * Never throws — falls back to [isoString] unchanged if it can't be parsed.
     */
    fun toJalaliDate(isoString: String): String = try {
        val (gy, gm, gd) = parseDateParts(isoString) ?: return isoString
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        jalaliDateLabel(jy, jm, jd)
    } catch (e: Exception) {
        isoString
    }

    /**
     * Same as [toJalaliDate] plus hour:minute, for entries where the exact
     * time matters (e.g. order status history, price/stock change history).
     * Falls back to [toJalaliDate]'s output if there's no parseable time
     * component, and to the raw input if even the date can't be parsed —
     * never throws.
     */
    fun toJalaliDateTime(isoString: String): String = try {
        val (gy, gm, gd) = parseDateParts(isoString) ?: return isoString
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        val dateLabel = jalaliDateLabel(jy, jm, jd)
        val (h, mi) = parseTimeParts(isoString) ?: return dateLabel
        dateLabel + " - " + String.format(Locale.US, "%02d:%02d", h, mi)
    } catch (e: Exception) {
        isoString
    }
}
