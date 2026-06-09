package com.example.goodmail.data.repository

import com.example.goodmail.data.local.db.EmailDao
import com.example.goodmail.data.local.db.entities.toDomain
import com.example.goodmail.data.local.db.entities.toEntity
import com.example.goodmail.data.remote.gmail.GmailService
import com.example.goodmail.domain.model.Email
import com.example.goodmail.domain.model.EmailImportance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for emails: the UI always observes Room; the network only refreshes it.
 */
@Singleton
class EmailRepository @Inject constructor(
    private val gmailService: GmailService,
    private val emailDao: EmailDao,
) {
    val emails: Flow<List<Email>> = emailDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    /** Fetch the latest inbox metadata and upsert it, preserving any cached body/importance. */
    suspend fun refreshInbox() {
        val fetched = gmailService.fetchInbox()
        val entities = fetched.map { email ->
            val existing = emailDao.getById(email.id)
            email.copy(
                body = existing?.body,
                importance = existing?.toDomain()?.importance ?: EmailImportance.UNCLASSIFIED,
            ).toEntity()
        }
        emailDao.upsertAll(entities)
    }

    /** Return the cached body, fetching and caching it from Gmail on first access. */
    suspend fun loadBody(id: String): String {
        emailDao.getBody(id)?.takeIf { it.isNotEmpty() }?.let { return it }
        val body = gmailService.fetchBody(id)
        emailDao.updateBody(id, body)
        return body
    }

    /** The most recently deleted row, kept so the next [undoLastDelete] can restore it. */
    private var lastDeleted: com.example.goodmail.data.local.db.entities.EmailEntity? = null

    /** Optimistically remove locally, then trash on Gmail; restore the row if the call fails. */
    suspend fun deleteEmail(id: String) {
        val backup = emailDao.getById(id)
        emailDao.deleteById(id)
        runCatching { gmailService.trash(id) }.onFailure { e ->
            if (backup != null) emailDao.upsertAll(listOf(backup))
            throw e
        }
        lastDeleted = backup
    }

    /** Reverse the most recent [deleteEmail]: untrash on Gmail and re-insert locally. */
    suspend fun undoLastDelete() {
        val backup = lastDeleted ?: return
        runCatching { gmailService.untrash(backup.id) }
        emailDao.upsertAll(listOf(backup))
        lastDeleted = null
    }

    suspend fun markAsRead(id: String) {
        emailDao.markRead(id)
        runCatching { gmailService.markRead(id) }
    }
}
