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

    private fun part(mime: String, block: MessagePart.() -> Unit): MessagePart =
        MessagePart().apply { mimeType = mime }.apply(block)

    private fun leaf(mime: String, content: String): MessagePart =
        MessagePart().apply {
            mimeType = mime
            body = MessagePartBody().apply { data = Base64.encodeBase64URLSafeString(content.toByteArray()) }
        }
}
