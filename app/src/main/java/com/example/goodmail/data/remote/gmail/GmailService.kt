package com.example.goodmail.data.remote.gmail

import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import com.google.api.client.util.Base64
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.ModifyMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** One page of inbox results; [nextPageToken] is null when there are no older messages. */
data class InboxPage(val emails: List<Email>, val nextPageToken: String?)

/**
 * Thin coroutine wrapper over the blocking Gmail Java client. All network calls run on
 * [Dispatchers.IO]. The list fetch pulls metadata only; full bodies load lazily via [fetchBody].
 */
@Singleton
class GmailService @Inject constructor(
    private val gmail: Gmail,
) {
    /** Fetch one page of INBOX messages as metadata-only [Email]s (no body), newest first. */
    suspend fun fetchInbox(maxResults: Long = 20, pageToken: String? = null): InboxPage =
        withContext(Dispatchers.IO) {
            val response = gmail.users().messages().list(USER)
                .setLabelIds(listOf(INBOX))
                .setMaxResults(maxResults)
                .apply { pageToken?.let { setPageToken(it) } }
                .execute()
            val refs = response.messages ?: return@withContext InboxPage(emptyList(), null)
            InboxPage(refs.map { ref -> fetchMetadata(ref.id) }, response.nextPageToken)
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
        val html = GmailParsing.extractBody(message.payload)
        resolveInlineImages(id, message.payload, html)
    }

    /**
     * Replace `cid:` image references with base64 `data:` URIs so inline images render in the
     * WebView. Per-image and total size caps keep the cached body well under SQLite's ~2MB
     * CursorWindow row limit; oversized images are left as broken refs rather than crashing reads.
     */
    private fun resolveInlineImages(messageId: String, payload: MessagePart?, html: String): String {
        val referenced = GmailParsing.referencedCids(html)
        if (referenced.isEmpty()) return html
        val inlineParts = GmailParsing.collectInlineImages(payload)
        var budget = MAX_TOTAL_INLINE_CHARS
        val dataUris = mutableMapOf<String, String>()
        for (cid in referenced) {
            val part = inlineParts[cid] ?: continue
            val bytes = part.body?.decodeData()
                ?: part.body?.attachmentId?.let { attachmentId ->
                    runCatching {
                        gmail.users().messages().attachments()
                            .get(USER, messageId, attachmentId).execute().decodeData()
                    }.getOrNull()
                }
                ?: continue
            if (bytes.size > MAX_INLINE_IMAGE_BYTES) continue
            val uri = "data:${part.mimeType};base64,${Base64.encodeBase64String(bytes)}"
            if (uri.length > budget) continue
            budget -= uri.length
            dataUris[cid] = uri
        }
        return GmailParsing.inlineCidImages(html, dataUris)
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

        /** Skip inline images larger than this (decoded bytes). */
        const val MAX_INLINE_IMAGE_BYTES = 700_000

        /** Stop inlining once the data URIs would add this many characters to the body. */
        const val MAX_TOTAL_INLINE_CHARS = 1_400_000
    }
}
