package com.example.goodmail

import com.example.goodmail.data.repository.RuleMatcher
import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import com.example.goodmail.domain.model.ImportanceRule
import com.example.goodmail.domain.model.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleMatcherTest {

    private fun email(from: String = "", subject: String = "", snippet: String = "", body: String? = null) =
        Email("1", "1", from, subject, snippet, body, 0L, false, emptyList())

    private fun rule(type: RuleType, value: String, action: EmailImportance = EmailImportance.IMPORTANT) =
        ImportanceRule(type = type, value = value, action = action, createdAt = 0L)

    @Test
    fun senderRuleMatches() {
        val rules = listOf(rule(RuleType.SENDER, "Prof Smith"))
        assertEquals(EmailImportance.IMPORTANT, RuleMatcher.match(rules, email(from = "Prof Smith")))
    }

    @Test
    fun subjectKeywordMatches() {
        val rules = listOf(rule(RuleType.KEYWORD_SUBJECT, "invoice", EmailImportance.IMPORTANT))
        assertEquals(EmailImportance.IMPORTANT, RuleMatcher.match(rules, email(subject = "Your INVOICE is ready")))
    }

    @Test
    fun bodyKeywordFallsBackToSnippetWhenNoBody() {
        val rules = listOf(rule(RuleType.KEYWORD_BODY, "unsubscribe", EmailImportance.NOT_IMPORTANT))
        assertEquals(
            EmailImportance.NOT_IMPORTANT,
            RuleMatcher.match(rules, email(snippet = "click to unsubscribe", body = null)),
        )
    }

    @Test
    fun firstMatchingRuleWins() {
        val rules = listOf(
            rule(RuleType.SENDER, "newsletter", EmailImportance.NOT_IMPORTANT),
            rule(RuleType.SENDER, "news", EmailImportance.IMPORTANT),
        )
        assertEquals(EmailImportance.NOT_IMPORTANT, RuleMatcher.match(rules, email(from = "newsletter@x.com")))
    }

    @Test
    fun noMatchReturnsNull() {
        val rules = listOf(rule(RuleType.SENDER, "boss"))
        assertNull(RuleMatcher.match(rules, email(from = "spammer@x.com")))
    }

    @Test
    fun blankRuleValueIsIgnored() {
        val rules = listOf(rule(RuleType.SENDER, ""))
        assertNull(RuleMatcher.match(rules, email(from = "anyone")))
    }
}
