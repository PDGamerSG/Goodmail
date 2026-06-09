package com.example.goodmail.data.repository

import com.example.goodmail.data.local.db.RuleDao
import com.example.goodmail.data.local.db.entities.toDomain
import com.example.goodmail.data.local.db.entities.toEntity
import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import com.example.goodmail.domain.model.ImportanceRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepository @Inject constructor(
    private val ruleDao: RuleDao,
) {
    val rules: Flow<List<ImportanceRule>> =
        ruleDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    suspend fun addRule(rule: ImportanceRule) = ruleDao.insert(rule.toEntity())

    suspend fun deleteRule(id: Long) = ruleDao.deleteById(id)

    /** First matching rule's action for this email, or null. Checked before any Groq call. */
    suspend fun matchRule(email: Email): EmailImportance? =
        RuleMatcher.match(ruleDao.getAllList().map { it.toDomain() }, email)

    suspend fun allRules(): List<ImportanceRule> = ruleDao.getAllList().map { it.toDomain() }
}
