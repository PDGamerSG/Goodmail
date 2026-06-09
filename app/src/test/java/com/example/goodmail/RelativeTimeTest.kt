package com.example.goodmail

import com.example.goodmail.ui.util.RelativeTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class RelativeTimeTest {

    private val utc = ZoneId.of("UTC")
    private val us = Locale.US

    // Fixed "now": 2026-06-09 12:00:00 UTC
    private val now = ZonedDateTime.of(2026, 6, 9, 12, 0, 0, 0, utc)
        .toInstant().toEpochMilli()

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, utc).toInstant().toEpochMilli()

    @Test
    fun justNow_underAMinute() {
        assertEquals("Just now", RelativeTime.format(now - 30_000, now, utc, us))
    }

    @Test
    fun minutes() {
        assertEquals("5m", RelativeTime.format(now - 5 * 60_000, now, utc, us))
        assertEquals("59m", RelativeTime.format(now - 59 * 60_000, now, utc, us))
    }

    @Test
    fun hours_sameDay() {
        assertEquals("2h", RelativeTime.format(at(2026, 6, 9, 10, 0), now, utc, us))
    }

    @Test
    fun yesterday() {
        assertEquals("Yesterday", RelativeTime.format(at(2026, 6, 8, 23, 0), now, utc, us))
    }

    @Test
    fun sameYear_showsMonthAndDay() {
        assertEquals("Jun 5", RelativeTime.format(at(2026, 6, 5, 9, 0), now, utc, us))
    }

    @Test
    fun priorYear_includesYear() {
        assertEquals("Dec 25, 2025", RelativeTime.format(at(2025, 12, 25, 9, 0), now, utc, us))
    }
}
