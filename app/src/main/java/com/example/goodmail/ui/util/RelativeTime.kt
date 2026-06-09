package com.example.goodmail.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a message timestamp the way an inbox does: "Just now", "5m", "2h", "Yesterday",
 * "Jun 5", or "Jun 5, 2024". Pure and deterministic — `now`, zone and locale are parameters so
 * it is fully unit-testable.
 */
object RelativeTime {

    fun format(
        epochMillis: Long,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        val then = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val diffMillis = nowMillis - epochMillis

        if (diffMillis in 0 until ONE_HOUR) {
            val minutes = diffMillis / ONE_MINUTE
            return if (minutes < 1) "Just now" else "${minutes}m"
        }

        val thenDate = then.toLocalDate()
        val nowDate = now.toLocalDate()
        return when {
            thenDate == nowDate -> "${diffMillis / ONE_HOUR}h"
            thenDate == nowDate.minusDays(1) -> "Yesterday"
            thenDate.year == nowDate.year ->
                thenDate.format(DateTimeFormatter.ofPattern("MMM d", locale))
            else ->
                thenDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))
        }
    }

    private const val ONE_MINUTE = 60_000L
    private const val ONE_HOUR = 3_600_000L
}
