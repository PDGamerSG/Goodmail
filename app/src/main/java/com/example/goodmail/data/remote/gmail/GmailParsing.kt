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

    /**
     * Walk the MIME tree and collect image parts that carry a `Content-ID` header,
     * keyed by the Content-ID value with the surrounding angle brackets stripped.
     * These are the inline images that HTML bodies reference via `src="cid:..."`.
     */
    fun collectInlineImages(payload: MessagePart?): Map<String, MessagePart> {
        if (payload == null) return emptyMap()
        val found = mutableMapOf<String, MessagePart>()
        collectInlineImagesInto(payload, found)
        return found
    }

    private fun collectInlineImagesInto(part: MessagePart, into: MutableMap<String, MessagePart>) {
        if (part.mimeType?.startsWith("image/") == true) {
            val contentId = part.headers
                ?.firstOrNull { it.name.equals("Content-ID", ignoreCase = true) }
                ?.value?.trim()?.removeSurrounding("<", ">")
            if (!contentId.isNullOrEmpty()) into.putIfAbsent(contentId, part)
        }
        part.parts?.forEach { collectInlineImagesInto(it, into) }
    }

    private val CID_SRC = Regex("""src\s*=\s*(["'])cid:([^"']+)\1""", RegexOption.IGNORE_CASE)

    /** Content-IDs referenced by `<img src="cid:...">` in [html]. */
    fun referencedCids(html: String): Set<String> =
        CID_SRC.findAll(html).map { it.groupValues[2] }.toSet()

    /** Replace `src="cid:X"` references with [dataUris]`[X]`; unknown cids are left untouched. */
    fun inlineCidImages(html: String, dataUris: Map<String, String>): String {
        if (dataUris.isEmpty()) return html
        return CID_SRC.replace(html) { match ->
            val quote = match.groupValues[1]
            val cid = match.groupValues[2]
            val uri = dataUris[cid] ?: return@replace match.value
            "src=$quote$uri$quote"
        }
    }
}
