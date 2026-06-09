package com.example.goodmail.data.repository

import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import com.example.goodmail.domain.model.ImportanceRule
import com.example.goodmail.domain.model.RuleType

/** Pure rule-matching logic, separated from [RuleRepository] so it can be unit-tested. */
object RuleMatcher {

    /** Returns the action of the first matching rule, or null if none match. */
    fun match(rules: List<ImportanceRule>, email: Email): EmailImportance? {
        for (rule in rules) {
            if (rule.value.isBlank()) continue
            val haystack = when (rule.type) {
                RuleType.SENDER -> email.from
                RuleType.KEYWORD_SUBJECT -> email.subject
                RuleType.KEYWORD_BODY -> email.body ?: email.snippet
            }
            if (haystack.contains(rule.value, ignoreCase = true)) return rule.action
        }
        return null
    }
}
