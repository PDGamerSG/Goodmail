package com.example.goodmail.domain.usecase

import com.example.goodmail.data.local.datastore.SettingsStore
import com.example.goodmail.data.local.db.EmailDao
import com.example.goodmail.data.local.db.entities.toDomain
import com.example.goodmail.data.remote.groq.GroqClassifier
import com.example.goodmail.data.repository.RuleRepository
import com.example.goodmail.domain.model.EmailImportance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classifies still-UNCLASSIFIED cached emails. User rules are checked first (and apply even with
 * no API key); only when no rule matches and a key is set do we call Groq. A failed classification
 * leaves the email UNCLASSIFIED so it is retried on the next pass.
 */
@Singleton
class ClassifyEmailUseCase @Inject constructor(
    private val emailDao: EmailDao,
    private val groqClassifier: GroqClassifier,
    private val ruleRepository: RuleRepository,
    private val settingsStore: SettingsStore,
) {
    suspend fun classifyUnclassified() {
        val hasKey = !settingsStore.apiKey.first().isNullOrBlank()
        // First apply user rules (works without an API key); collect the rest for Groq.
        val needsApi = buildList {
            for (entity in emailDao.getUnclassified()) {
                val byRule = ruleRepository.matchRule(entity.toDomain())
                if (byRule != null) emailDao.updateImportance(entity.id, byRule.name) else add(entity)
            }
        }
        if (!hasKey) return
        // Classify the remainder in small batches to minimise API calls.
        for (chunk in needsApi.chunked(BATCH_SIZE)) {
            val verdicts = groqClassifier.classifyBatch(chunk.map { it.toDomain() })
            for (entity in chunk) {
                val importance = verdicts[entity.id] ?: continue
                if (importance != EmailImportance.UNCLASSIFIED) {
                    emailDao.updateImportance(entity.id, importance.name)
                }
            }
            delay(RATE_LIMIT_DELAY_MS)
        }
    }

    /** Re-evaluate every cached email against the current rules (used after rules change). */
    suspend fun reapplyRules() {
        val rules = ruleRepository.allRules()
        if (rules.isEmpty()) return
        for (entity in emailDao.getAllList()) {
            val match = com.example.goodmail.data.repository.RuleMatcher.match(rules, entity.toDomain())
            if (match != null) emailDao.updateImportance(entity.id, match.name)
        }
    }

    private companion object {
        const val RATE_LIMIT_DELAY_MS = 250L
        const val BATCH_SIZE = 5
    }
}
