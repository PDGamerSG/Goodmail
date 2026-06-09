package com.example.goodmail.data.remote.gmail

import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.ModifyMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin coroutine wrapper over the blocking Gmail Java client. All network calls run on
 * [Dispatchers.IO]. The list fetch pulls metadata only; full bodies load lazily via [fetchBody].
 */
@Singleton
class GmailService @Inject constructor(
    private val gmail: Gmail,
) {
    /** Fetch the most recent INBOX messages as metadata-only [Email]s (no body). */
    suspend fun fetchInbox(maxResults: Long = 50): List<Email> = withContext(Dispatchers.IO) {
        val response = gmail.users().messages().list(USER)
            .setLabelIds(listOf(INBOX))
            .setMaxResults(maxResults)
            .execute()
        val refs = response.messages ?: return@withContext emptyList()
        refs.map { ref -> fetchMetadata(ref.id) }
    }

    private fun fetchMetadata(id: String): Email {
        val message = gmail.users().messages().get(USER, id)
            .setFormat("metadata")
            .setMetadataHeaders(listOf("From", "Subject", "Date"))
            .execute()
        return message.toEmail(body = null)
    }

    suspend fun fetchBody(id: String): String = withContext(Dispatchers.IO) {
        val message = gmail.users().messages().get(USER, id).setFormat("full").execute()
        GmailParsing.extractBody(message.payload)
    }

    suspend fun trash(id: String): Unit = withContext(Dispatchers.IO) {
        gmail.users().messages().trash(USER, id).execute()
    }

    suspend fun untrash(id: String): Unit = withContext(Dispatchers.IO) {
        gmail.users().messages().untrash(USER, id).execute()
    }

    suspend fun markRead(id: String): Unit = withContext(Dispatchers.IO) {
        val request = ModifyMessageRequest().setRemoveLabelIds(listOf(UNREAD))
        gmail.users().messages().modify(USER, id, request).execute()
    }

    private fun Message.toEmail(body: String?): Email {
        val headers = payload?.headers.orEmpty()
        fun header(name: String): String? =
            headers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.value
        return Email(
            id = id,
            threadId = threadId ?: id,
            from = GmailParsing.parseSender(header("From")),
            subject = header("Subject")?.takeIf { it.isNotBlank() } ?: "(no subject)",
            snippet = snippet.orEmpty(),
            body = body,
            timestamp = internalDate ?: 0L,
            isRead = labelIds?.contains(UNREAD) != true,
            labelIds = labelIds.orEmpty(),
            importance = EmailImportance.UNCLASSIFIED,
        )
    }

    private companion object {
        const val USER = "me"
        const val INBOX = "INBOX"
        const val UNREAD = "UNREAD"
    }
}
