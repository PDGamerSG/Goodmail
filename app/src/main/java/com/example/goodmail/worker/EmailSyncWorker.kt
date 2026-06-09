package com.example.goodmail.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.goodmail.data.local.datastore.SettingsStore
import com.example.goodmail.data.local.db.EmailDao
import com.example.goodmail.data.local.db.entities.toDomain
import com.example.goodmail.data.repository.AuthRepository
import com.example.goodmail.data.repository.EmailRepository
import com.example.goodmail.domain.model.EmailImportance
import com.example.goodmail.domain.usecase.ClassifyEmailUseCase
import com.example.goodmail.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodic background sync: refresh inbox, classify new mail (rules then Groq), and fire a
 * notification for each newly-arrived important email. No-ops when signed out.
 */
@HiltWorker
class EmailSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val authRepository: AuthRepository,
    private val emailRepository: EmailRepository,
    private val emailDao: EmailDao,
    private val classifyEmailUseCase: ClassifyEmailUseCase,
    private val notificationHelper: NotificationHelper,
    private val settingsStore: SettingsStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (authRepository.accountEmail.first() == null) return Result.success()
        return try {
            val existingIds = emailDao.getAllList().map { it.id }.toSet()
            emailRepository.refreshInbox()
            classifyEmailUseCase.classifyUnclassified()

            if (settingsStore.notificationsEnabled.first()) {
                val newImportant = emailDao.getAllList().filter {
                    it.id !in existingIds && it.importance == EmailImportance.IMPORTANT.name
                }
                newImportant.forEach { notificationHelper.showImportantEmail(it.toDomain()) }
                if (newImportant.isNotEmpty()) notificationHelper.showSummary(newImportant.size)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
