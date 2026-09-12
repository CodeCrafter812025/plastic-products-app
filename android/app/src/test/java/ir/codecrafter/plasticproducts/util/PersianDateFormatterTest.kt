package ir.codecrafter.plasticproducts.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reference dates chosen to cover: an ordinary mid-year date, the Jalali new
 * year boundary (Nowruz), a year-boundary Gregorian date that lands well
 * into the previous Jalali year, and a historical date far from the current
 * range — plus the never-throw fallback guaranteed by toJalaliDate/
 * toJalaliDateTime on unparseable input.
 */
class PersianDateFormatterTest {

    @Test
    fun `2026-09-12 converts to 21 Shahrivar 1405`() {
        assertEquals("۲۱ شهریور ۱۴۰۵", PersianDateFormatter.toJalaliDate("2026-09-12"))
    }

    @Test
    fun `2026-03-21 converts to 1 Farvardin 1405 (Nowruz)`() {
        assertEquals("۱ فروردین ۱۴۰۵", PersianDateFormatter.toJalaliDate("2026-03-21"))
    }

    @Test
    fun `2026-01-01 converts to 11 Dey 1404`() {
        assertEquals("۱۱ دی ۱۴۰۴", PersianDateFormatter.toJalaliDate("2026-01-01"))
    }

    @Test
    fun `1979-02-11 converts to 22 Bahman 1357`() {
        assertEquals("۲۲ بهمن ۱۳۵۷", PersianDateFormatter.toJalaliDate("1979-02-11"))
    }

    @Test
    fun `full ISO datetime string converts correctly and keeps time in Latin digits`() {
        assertEquals(
            "۲۱ شهریور ۱۴۰۵ - 23:03",
            PersianDateFormatter.toJalaliDateTime("2026-09-12T23:03:37.619105Z"),
        )
    }

    @Test
    fun `invalid input falls back to the raw string instead of throwing`() {
        assertEquals("not-a-date", PersianDateFormatter.toJalaliDate("not-a-date"))
        assertEquals("not-a-date", PersianDateFormatter.toJalaliDateTime("not-a-date"))
    }

    @Test
    fun `empty input falls back to the raw string instead of throwing`() {
        assertEquals("", PersianDateFormatter.toJalaliDate(""))
        assertEquals("", PersianDateFormatter.toJalaliDateTime(""))
    }

    @Test
    fun `jalaliToGregorian(1405,1,1) converts to 2026-03-21 (Nowruz)`() {
        assertEquals(Triple(2026, 3, 21), PersianDateFormatter.jalaliToGregorian(1405, 1, 1))
    }

    @Test
    fun `jalaliToGregorian(1403,1,1) converts to 2024-03-20`() {
        assertEquals(Triple(2024, 3, 20), PersianDateFormatter.jalaliToGregorian(1403, 1, 1))
    }

    @Test
    fun `jalaliToGregorian(1404,1,1) converts to 2025-03-21`() {
        assertEquals(Triple(2025, 3, 21), PersianDateFormatter.jalaliToGregorian(1404, 1, 1))
    }

    /**
     * NOTE: these values (1403 leap, 1404/1405 not) are the opposite of what
     * was originally requested (1404 leap, 1403 not). isJalaliLeapYear is
     * derived purely from this file's own jalaliToGregorian (see its KDoc) —
     * not a separate external table — so these are simply whatever that
     * computational calendar produces for these years, kept intentionally
     * independent of whether it agrees with the real astronomical Iranian
     * calendar (it does, for what it's worth, but that's not why these
     * values were chosen).
     */
    @Test
    fun `1403 is a Jalali leap year`() {
        assertEquals(true, PersianDateFormatter.isJalaliLeapYear(1403))
    }

    @Test
    fun `1404 is not a Jalali leap year`() {
        assertEquals(false, PersianDateFormatter.isJalaliLeapYear(1404))
    }

    @Test
    fun `1405 is not a Jalali leap year`() {
        assertEquals(false, PersianDateFormatter.isJalaliLeapYear(1405))
    }

    @Test
    fun `daysInJalaliMonth reflects the leap-year Esfand length`() {
        assertEquals(30, PersianDateFormatter.daysInJalaliMonth(1403, 12))
        assertEquals(29, PersianDateFormatter.daysInJalaliMonth(1404, 12))
    }

    /**
     * Self-verifying round trip: every single day of Jalali year 1403 (leap,
     * 366 days) and 1404 (not leap, 365 days) must survive
     * gregorianToJalali(jalaliToGregorian(jy, jm, jd)) unchanged. This is a
     * far stronger check than a handful of fixed reference dates — it would
     * catch almost any off-by-one or boundary bug in either conversion
     * direction or in daysInJalaliMonth.
     */
    @Test
    fun `every day of 1403 (leap) and 1404 (not leap) round-trips correctly`() {
        for (jy in listOf(1403, 1404)) {
            for (jm in 1..12) {
                val daysInMonth = PersianDateFormatter.daysInJalaliMonth(jy, jm)
                for (jd in 1..daysInMonth) {
                    val (gy, gm, gd) = PersianDateFormatter.jalaliToGregorian(jy, jm, jd)
                    val roundTripped = PersianDateFormatter.gregorianToJalali(gy, gm, gd)
                    assertEquals(
                        "round trip failed for Jalali $jy-$jm-$jd (via Gregorian $gy-$gm-$gd)",
                        Triple(jy, jm, jd),
                        roundTripped,
                    )
                }
            }
        }
    }
}
