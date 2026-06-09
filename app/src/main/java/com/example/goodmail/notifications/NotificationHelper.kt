package com.example.goodmail.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.goodmail.R
import com.example.goodmail.domain.model.Email
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun showImportantEmail(email: Email) {
        if (!hasPermission()) return

        val deepLink = "goodmail://email/${email.id}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, deepLink).setPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            email.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.IMPORTANT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(email.from)
            .setContentText(email.subject)
            .setStyle(NotificationCompat.BigTextStyle().bigText(email.subject))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .build()

        NotificationManagerCompat.from(context).notify(email.id.hashCode(), notification)
    }

    /** Group-summary notification shown when several important emails arrive together. */
    fun showSummary(count: Int) {
        if (!hasPermission() || count < 1) return
        val summary = NotificationCompat.Builder(context, NotificationChannels.IMPORTANT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$count new important email" + if (count > 1) "s" else "")
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(SUMMARY_ID, summary)
    }

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private companion object {
        const val GROUP_KEY = "goodmail.important"
        const val SUMMARY_ID = 1
    }
}
