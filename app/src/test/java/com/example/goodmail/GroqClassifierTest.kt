package com.example.goodmail

import com.example.goodmail.data.remote.groq.GroqClassifier
import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqClassifierTest {

    @Test
    fun parse_cleanJson_important() {
        val content = """{"importance":"IMPORTANT","reason":"deadline"}"""
        assertEquals(EmailImportance.IMPORTANT, GroqClassifier.parseImportance(content))
    }

    @Test
    fun parse_cleanJson_notImportant() {
        val content = """{"importance":"NOT_IMPORTANT","reason":"promo"}"""
        assertEquals(EmailImportance.NOT_IMPORTANT, GroqClassifier.parseImportance(content))
    }

    @Test
    fun parse_jsonWrappedInMarkdownFence() {
        val content = "```json\n{\"importance\": \"IMPORTANT\", \"reason\": \"bill due\"}\n```"
        assertEquals(EmailImportance.IMPORTANT, GroqClassifier.parseImportance(content))
    }

    @Test
    fun parse_extraProseAroundJson() {
        val content = "Here is the result: {\"importance\":\"NOT_IMPORTANT\"} hope that helps"
        assertEquals(EmailImportance.NOT_IMPORTANT, GroqClassifier.parseImportance(content))
    }

    @Test
    fun parse_caseInsensitive() {
        assertEquals(EmailImportance.IMPORTANT, GroqClassifier.parseImportance("""{"importance":"important"}"""))
    }

    @Test
    fun parse_plainTextFallback() {
        assertEquals(EmailImportance.NOT_IMPORTANT, GroqClassifier.parseImportance("This is NOT_IMPORTANT"))
        assertEquals(EmailImportance.IMPORTANT, GroqClassifier.parseImportance("Looks IMPORTANT to me"))
    }

    @Test
    fun parse_garbageReturnsUnclassified() {
        assertEquals(EmailImportance.UNCLASSIFIED, GroqClassifier.parseImportance("no verdict here"))
    }

    @Test
    fun buildUserPrompt_includesSenderSubjectPreview() {
        val email = Email(
            id = "1", threadId = "1", from = "Prof Smith", subject = "Exam Monday",
            snippet = "The exam is rescheduled", body = null, timestamp = 0L,
            isRead = false, labelIds = emptyList(),
        )
        val prompt = GroqClassifier.buildUserPrompt(email)
        assertTrue(prompt.contains("Prof Smith"))
        assertTrue(prompt.contains("Exam Monday"))
        assertTrue(prompt.contains("The exam is rescheduled"))
    }
}
