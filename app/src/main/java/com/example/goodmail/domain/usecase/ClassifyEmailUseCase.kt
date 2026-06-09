package com.example.goodmail.domain.usecase

import com.example.goodmail.data.local.datastore.SettingsStore
import com.example.goodmail.data.local.db.EmailDao
import com.example.goodmail.data.local.db.entities.toDomain
import com.example.goodmail.data.remote.groq.GroqClassifier
import com.example.goodmail.domain.model.EmailImportance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classifies every still-UNCLASSIFIED cached email. No-ops when no API key is set. A failed
 * classification leaves the email UNCLASSIFIED so it is retried on the next pass.
 */
@Singleton
class ClassifyEmailUseCase @Inject constructor(
    private val emailDao: EmailDao,
    private val groqClassifier: GroqClassifier,
    private val settingsStore: SettingsStore,
) {
    suspend fun classifyUnclassified() {
        if (settingsStore.apiKey.first().isNullOrBlank()) return
        val pending = emailDao.getUnclassified()
        for (entity in pending) {
            val importance = groqClassifier.classify(entity.toDomain())
            if (importance != EmailImportance.UNCLASSIFIED) {
                emailDao.updateImportance(entity.id, importance.name)
            }
            delay(RATE_LIMIT_DELAY_MS)
        }
    }

    private companion object {
        const val RATE_LIMIT_DELAY_MS = 250L
    }
}
