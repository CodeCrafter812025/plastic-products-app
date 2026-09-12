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
}
