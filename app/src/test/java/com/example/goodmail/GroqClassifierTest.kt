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
    fun parseBatch_cleanJson() {
        val content = """{"results":[{"index":1,"importance":"IMPORTANT"},{"index":2,"importance":"NOT_IMPORTANT"}]}"""
        val parsed = GroqClassifier.parseBatch(content)
        assertEquals(EmailImportance.IMPORTANT, parsed[1])
        assertEquals(EmailImportance.NOT_IMPORTANT, parsed[2])
    }

    @Test
    fun parseBatch_markdownFenceAndProse() {
        val content = "Sure!\n```json\n{\"results\":[{\"index\":1,\"importance\":\"not_important\"}]}\n```"
        assertEquals(mapOf(1 to EmailImportance.NOT_IMPORTANT), GroqClassifier.parseBatch(content))
    }

    @Test
    fun parseBatch_skipsBadEntries() {
        val content = """{"results":[{"index":0,"importance":"IMPORTANT"},{"index":2,"importance":"MAYBE"},{"index":3,"importance":"IMPORTANT"}]}"""
        assertEquals(mapOf(3 to EmailImportance.IMPORTANT), GroqClassifier.parseBatch(content))
    }

    @Test
    fun parseBatch_garbageReturnsEmpty() {
        assertEquals(emptyMap<Int, EmailImportance>(), GroqClassifier.parseBatch("no json here"))
    }

    @Test
    fun buildBatchPrompt_numbersEveryEmail() {
        val emails = listOf(
            Email(
                id = "a", threadId = "a", from = "Alice", subject = "Hi", snippet = "first",
                body = null, timestamp = 0L, isRead = false, labelIds = emptyList(),
            ),
            Email(
                id = "b", threadId = "b", from = "Bob", subject = "Sale", snippet = "second",
                body = null, timestamp = 0L, isRead = false, labelIds = emptyList(),
            ),
        )
        val prompt = GroqClassifier.buildBatchPrompt(emails)
        assertTrue(prompt.contains("1. From: Alice"))
        assertTrue(prompt.contains("2. From: Bob"))
        assertTrue(prompt.contains("Subject: Sale"))
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
