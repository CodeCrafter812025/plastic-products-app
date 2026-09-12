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

    /** Standard days-since-epoch based Gregorian→Jalali conversion, verified against known reference dates. */
    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
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
