package com.example.goodmail.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val IMPORTANT = "important_emails"
    const val OTHER = "other_emails"

    /** Create channels on startup. No-op below API 26, where channels don't exist. */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val important = NotificationChannel(
            IMPORTANT,
            "Important emails",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Heads-up alerts for emails classified as important"
            enableVibration(true)
        }

        val other = NotificationChannel(
            OTHER,
            "Other emails",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Silent notifications for everything else"
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(important, other))
    }
}
