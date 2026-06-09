package com.example.goodmail.domain.model

enum class EmailImportance { IMPORTANT, NOT_IMPORTANT, UNCLASSIFIED }

/**
 * Clean domain representation of a single email. [body] is null until lazily loaded when the
 * detail screen is opened (the inbox list only fetches metadata).
 */
data class Email(
    val id: String,
    val threadId: String,
    val from: String,
    val subject: String,
    val snippet: String,
    val body: String?,
    val timestamp: Long,
    val isRead: Boolean,
    val labelIds: List<String>,
    val importance: EmailImportance = EmailImportance.UNCLASSIFIED,
)
