package com.example.goodmail.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance

@Entity(tableName = "emails")
data class EmailEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val from: String,
    val subject: String,
    val snippet: String,
    val body: String?,
    val timestamp: Long,
    val isRead: Boolean,
    val labelIds: String,
    val importance: String,
)

fun EmailEntity.toDomain(): Email = Email(
    id = id,
    threadId = threadId,
    from = from,
    subject = subject,
    snippet = snippet,
    body = body,
    timestamp = timestamp,
    isRead = isRead,
    labelIds = if (labelIds.isEmpty()) emptyList() else labelIds.split(","),
    importance = runCatching { EmailImportance.valueOf(importance) }
        .getOrDefault(EmailImportance.UNCLASSIFIED),
)

fun Email.toEntity(): EmailEntity = EmailEntity(
    id = id,
    threadId = threadId,
    from = from,
    subject = subject,
    snippet = snippet,
    body = body,
    timestamp = timestamp,
    isRead = isRead,
    labelIds = labelIds.joinToString(","),
    importance = importance.name,
)
