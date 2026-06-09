package com.example.goodmail.data.remote.groq

import com.example.goodmail.data.local.datastore.SettingsStore
import com.example.goodmail.data.remote.groq.models.ChatMessage
import com.example.goodmail.data.remote.groq.models.ChatRequest
import com.example.goodmail.data.remote.groq.models.ResponseFormat
import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class Verdict(val importance: String = "", val reason: String = "")

/** Classifies a single email via Groq. Any failure (no key, network, bad response) yields UNCLASSIFIED. */
@Singleton
class GroqClassifier @Inject constructor(
    private val api: GroqApiService,
    private val settingsStore: SettingsStore,
) {
    suspend fun classify(email: Email): EmailImportance {
        val key = settingsStore.apiKey.first()?.takeIf { it.isNotBlank() } ?: return EmailImportance.UNCLASSIFIED
        val model = settingsStore.model.first()
        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", SYSTEM_PROMPT),
                ChatMessage("user", buildUserPrompt(email)),
            ),
            temperature = 0.1,
            responseFormat = ResponseFormat("json_object"),
        )
        val response = runCatching { api.chatCompletions("Bearer $key", request) }
            .getOrElse { return EmailImportance.UNCLASSIFIED }
        val content = response.choices.firstOrNull()?.message?.content
            ?: return EmailImportance.UNCLASSIFIED
        return parseImportance(content)
    }

    companion object {
        const val SYSTEM_PROMPT =
            "You are an email importance classifier. Respond with ONLY a JSON object and nothing else.\n\n" +
                "Classify the email as \"IMPORTANT\" or \"NOT_IMPORTANT\".\n\n" +
                "IMPORTANT: a real person writing to the user; deadlines, bills, payments, account or " +
                "security alerts, appointments, travel, exams or grades, jobs/interviews/placement, or " +
                "anything that needs the user to act or reply soon.\n" +
                "NOT_IMPORTANT: marketing and promotions, newsletters, social or forum digests, automated " +
                "notifications, mass announcements, and no-reply bulk mail that needs no action.\n\n" +
                "Respond exactly: {\"importance\":\"IMPORTANT\" or \"NOT_IMPORTANT\",\"reason\":\"<short reason>\"}"

        private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

        fun buildUserPrompt(email: Email): String = buildString {
            append("From: ").append(email.from).append('\n')
            append("Subject: ").append(email.subject).append('\n')
            append("Preview: ").append(email.snippet).append("\n\n")
            append("Classify this email.")
        }

        fun parseImportance(content: String): EmailImportance {
            val start = content.indexOf('{')
            val end = content.lastIndexOf('}')
            if (start in 0 until end) {
                val verdict = runCatching {
                    lenientJson.decodeFromString<Verdict>(content.substring(start, end + 1))
                }.getOrNull()
                when (verdict?.importance?.uppercase()) {
                    "IMPORTANT" -> return EmailImportance.IMPORTANT
                    "NOT_IMPORTANT" -> return EmailImportance.NOT_IMPORTANT
                }
            }
            return guessFromText(content)
        }

        private fun guessFromText(content: String): EmailImportance {
            val upper = content.uppercase()
            return when {
                "NOT_IMPORTANT" in upper || "NOT IMPORTANT" in upper -> EmailImportance.NOT_IMPORTANT
                "IMPORTANT" in upper -> EmailImportance.IMPORTANT
                else -> EmailImportance.UNCLASSIFIED
            }
        }
    }
}
