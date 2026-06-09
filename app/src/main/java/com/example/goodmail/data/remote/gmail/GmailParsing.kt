package com.example.goodmail.data.remote.gmail

import com.google.api.client.util.Base64
import com.google.api.services.gmail.model.MessagePart

/**
 * Pure, framework-free parsing helpers for Gmail message payloads. Kept separate from
 * [GmailService] so they can be unit-tested on the JVM without Android or network.
 */
object GmailParsing {

    /** Extract a human display name from a raw `From` header ("Name <email>", "<email>", "email"). */
    fun parseSender(fromHeader: String?): String {
        if (fromHeader.isNullOrBlank()) return "(unknown sender)"
        val match = Regex("""^\s*"?([^"<]*?)"?\s*<([^>]+)>\s*$""").find(fromHeader)
        if (match != null) {
            val name = match.groupValues[1].trim()
            val email = match.groupValues[2].trim()
            return name.ifEmpty { email }
        }
        return fromHeader.trim().trim('<', '>')
    }

    /**
     * Walk a message payload's MIME tree and return the decoded body, preferring `text/html`,
     * then `text/plain`, then the payload's own body. Returns an empty string if none is present.
     */
    fun extractBody(payload: MessagePart?): String {
        if (payload == null) return ""
        val chosen = findPart(payload, "text/html") ?: findPart(payload, "text/plain")
        val data = chosen?.body?.data ?: payload.body?.data
        return if (data != null) String(Base64.decodeBase64(data), Charsets.UTF_8) else ""
    }

    private fun findPart(part: MessagePart, mimeType: String): MessagePart? {
        if (part.mimeType == mimeType && part.body?.data != null) return part
        val parts = part.parts ?: return null
        for (sub in parts) {
            val found = findPart(sub, mimeType)
            if (found != null) return found
        }
        return null
    }
}
