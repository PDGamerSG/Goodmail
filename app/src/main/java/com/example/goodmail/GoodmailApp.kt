package com.example.goodmail

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.goodmail.data.local.datastore.SettingsStore
import com.example.goodmail.notifications.NotificationChannels
import com.example.goodmail.worker.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GoodmailApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createChannels(this)
        // KEEP: don't override a frequency the user already chose (WorkManager persists it).
        SyncScheduler.schedule(
            context = this,
            minutes = SettingsStore.DEFAULT_SYNC_MINUTES.toLong(),
            policy = ExistingPeriodicWorkPolicy.KEEP,
        )
    }
}
