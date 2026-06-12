package com.example.goodmail

import com.example.goodmail.data.remote.gmail.GmailParsing
import com.google.api.client.util.Base64
import com.google.api.services.gmail.model.MessagePart
import com.google.api.services.gmail.model.MessagePartBody
import org.junit.Assert.assertEquals
import org.junit.Test

class GmailParsingTest {

    @Test
    fun parseSender_nameAndEmail() {
        assertEquals("Alice Smith", GmailParsing.parseSender("Alice Smith <alice@example.com>"))
    }

    @Test
    fun parseSender_quotedName() {
        assertEquals("Alice Smith", GmailParsing.parseSender("\"Alice Smith\" <alice@example.com>"))
    }

    @Test
    fun parseSender_bareEmail() {
        assertEquals("bob@example.com", GmailParsing.parseSender("bob@example.com"))
    }

    @Test
    fun parseSender_angleBracketsOnly() {
        assertEquals("bob@example.com", GmailParsing.parseSender("<bob@example.com>"))
    }

    @Test
    fun parseSender_nullOrBlank() {
        assertEquals("(unknown sender)", GmailParsing.parseSender(null))
        assertEquals("(unknown sender)", GmailParsing.parseSender("   "))
    }

    @Test
    fun extractBody_prefersHtmlOverPlain() {
        val payload = part("multipart/alternative") {
            parts = listOf(
                leaf("text/plain", "plain text"),
                leaf("text/html", "<p>html</p>"),
            )
        }
        assertEquals("<p>html</p>", GmailParsing.extractBody(payload))
    }

    @Test
    fun extractBody_fallsBackToPlain() {
        val payload = part("multipart/alternative") {
            parts = listOf(leaf("text/plain", "just plain"))
        }
        assertEquals("just plain", GmailParsing.extractBody(payload))
    }

    @Test
    fun extractBody_singleLeafBody() {
        assertEquals("hi there", GmailParsing.extractBody(leaf("text/plain", "hi there")))
    }

    @Test
    fun extractBody_nestedMultipart() {
        val payload = part("multipart/mixed") {
            parts = listOf(
                part("multipart/alternative") {
                    parts = listOf(
                        leaf("text/plain", "p"),
                        leaf("text/html", "<b>nested</b>"),
                    )
                },
            )
        }
        assertEquals("<b>nested</b>", GmailParsing.extractBody(payload))
    }

    @Test
    fun extractBody_nullReturnsEmpty() {
        assertEquals("", GmailParsing.extractBody(null))
    }

    @Test
    fun collectInlineImages_findsNestedImagePartByContentId() {
        val image = leaf("image/png", "png-bytes").apply {
            headers = listOf(header("Content-ID", "<logo@goodmail>"))
        }
        val payload = part("multipart/related") {
            parts = listOf(
                part("multipart/alternative") {
                    parts = listOf(leaf("text/html", "<img src=\"cid:logo@goodmail\">"))
                },
                image,
            )
        }
        assertEquals(mapOf("logo@goodmail" to image), GmailParsing.collectInlineImages(payload))
    }

    @Test
    fun collectInlineImages_ignoresPartsWithoutContentIdOrNonImages() {
        val payload = part("multipart/mixed") {
            parts = listOf(
                leaf("image/png", "no content id"),
                leaf("application/pdf", "doc").apply {
                    headers = listOf(header("Content-ID", "<doc1>"))
                },
            )
        }
        assertEquals(emptyMap<String, MessagePart>(), GmailParsing.collectInlineImages(payload))
    }

    @Test
    fun collectInlineImages_nullPayloadReturnsEmpty() {
        assertEquals(emptyMap<String, MessagePart>(), GmailParsing.collectInlineImages(null))
    }

    @Test
    fun referencedCids_findsDoubleSingleQuotedAndMixedCase() {
        val html = """<img src="cid:a@x"><img src='cid:b@y'><IMG SRC="CID:c@z">"""
        assertEquals(setOf("a@x", "b@y", "c@z"), GmailParsing.referencedCids(html))
    }

    @Test
    fun referencedCids_emptyWhenNone() {
        assertEquals(emptySet<String>(), GmailParsing.referencedCids("<p>no images</p>"))
    }

    @Test
    fun inlineCidImages_replacesKnownCidsKeepsUnknown() {
        val html = """<img src="cid:logo@goodmail"><img src="cid:unknown@x">"""
        val result = GmailParsing.inlineCidImages(
            html,
            mapOf("logo@goodmail" to "data:image/png;base64,AAA"),
        )
        assertEquals("""<img src="data:image/png;base64,AAA"><img src="cid:unknown@x">""", result)
    }

    @Test
    fun inlineCidImages_handlesSingleQuotes() {
        val result = GmailParsing.inlineCidImages(
            "<img src='cid:logo@goodmail'>",
            mapOf("logo@goodmail" to "data:image/gif;base64,BBB"),
        )
        assertEquals("<img src='data:image/gif;base64,BBB'>", result)
    }

    @Test
    fun inlineCidImages_noopWithEmptyMap() {
        val html = """<img src="cid:logo@goodmail">"""
        assertEquals(html, GmailParsing.inlineCidImages(html, emptyMap()))
    }

    private fun header(name: String, value: String) =
        com.google.api.services.gmail.model.MessagePartHeader().apply {
            this.name = name
            this.value = value
        }

    private fun part(mime: String, block: MessagePart.() -> Unit): MessagePart =
        MessagePart().apply { mimeType = mime }.apply(block)

    private fun leaf(mime: String, content: String): MessagePart =
        MessagePart().apply {
            mimeType = mime
            body = MessagePartBody().apply { data = Base64.encodeBase64URLSafeString(content.toByteArray()) }
        }
}
