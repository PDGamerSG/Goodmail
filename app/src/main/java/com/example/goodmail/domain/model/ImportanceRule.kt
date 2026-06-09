package com.example.goodmail.domain.model

enum class RuleType { SENDER, KEYWORD_SUBJECT, KEYWORD_BODY }

/**
 * A user-defined rule that overrides AI classification. Checked before any Groq call, so a matching
 * rule both forces the importance and saves an API request.
 */
data class ImportanceRule(
    val id: Long = 0,
    val type: RuleType,
    val value: String,
    val action: EmailImportance,
    val createdAt: Long,
)
