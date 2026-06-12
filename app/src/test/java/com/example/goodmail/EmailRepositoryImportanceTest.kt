package com.example.goodmail

import com.example.goodmail.data.local.db.EmailDao
import com.example.goodmail.data.local.db.entities.EmailEntity
import com.example.goodmail.data.remote.gmail.GmailService
import com.example.goodmail.data.repository.EmailRepository
import com.example.goodmail.domain.model.EmailImportance
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class EmailRepositoryImportanceTest {

    private val dao = FakeEmailDao()
    private val repository = EmailRepository(unusedGmailService(), dao)

    @Test
    fun setImportance_updatesStoredValue() = runBlocking {
        dao.seed(entity("1", EmailImportance.UNCLASSIFIED))

        repository.setImportance("1", EmailImportance.IMPORTANT)

        assertEquals("IMPORTANT", dao.rows.getValue("1").importance)
    }

    @Test
    fun undoLastImportanceChange_restoresPreviousValue() = runBlocking {
        dao.seed(entity("1", EmailImportance.NOT_IMPORTANT))
        repository.setImportance("1", EmailImportance.IMPORTANT)

        repository.undoLastImportanceChange()

        assertEquals("NOT_IMPORTANT", dao.rows.getValue("1").importance)
    }

    @Test
    fun undoLastImportanceChange_withoutPriorChange_isNoop() = runBlocking {
        dao.seed(entity("1", EmailImportance.IMPORTANT))

        repository.undoLastImportanceChange()

        assertEquals("IMPORTANT", dao.rows.getValue("1").importance)
    }

    @Test
    fun undoLastImportanceChange_twice_appliesOnlyOnce() = runBlocking {
        dao.seed(entity("1", EmailImportance.UNCLASSIFIED))
        repository.setImportance("1", EmailImportance.IMPORTANT)

        repository.undoLastImportanceChange()
        // A later external change must not be clobbered by a second undo.
        dao.updateImportance("1", "IMPORTANT")
        repository.undoLastImportanceChange()

        assertEquals("IMPORTANT", dao.rows.getValue("1").importance)
    }

    private fun entity(id: String, importance: EmailImportance) = EmailEntity(
        id = id,
        threadId = id,
        from = "a@b.c",
        subject = "s",
        snippet = "sn",
        body = null,
        timestamp = 0L,
        isRead = false,
        labelIds = "",
        importance = importance.name,
    )

    /** Never invoked: importance changes are purely local; Gmail has no equivalent flag. */
    private fun unusedGmailService() = GmailService(
        Gmail.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), null)
            .setApplicationName("test")
            .build(),
    )
}

private class FakeEmailDao : EmailDao {
    val rows = mutableMapOf<String, EmailEntity>()

    fun seed(entity: EmailEntity) {
        rows[entity.id] = entity
    }

    override fun getAllFlow(): Flow<List<EmailEntity>> = flowOf(rows.values.toList())
    override suspend fun getById(id: String): EmailEntity? = rows[id]
    override suspend fun getBody(id: String): String? = rows[id]?.body
    override suspend fun getUnclassified(): List<EmailEntity> =
        rows.values.filter { it.importance == "UNCLASSIFIED" }
    override suspend fun getAllList(): List<EmailEntity> = rows.values.toList()
    override suspend fun updateImportance(id: String, importance: String) {
        rows[id] = rows.getValue(id).copy(importance = importance)
    }
    override suspend fun upsertAll(emails: List<EmailEntity>) {
        emails.forEach { rows[it.id] = it }
    }
    override suspend fun updateBody(id: String, body: String) {
        rows[id] = rows.getValue(id).copy(body = body)
    }
    override suspend fun markRead(id: String) {
        rows[id] = rows.getValue(id).copy(isRead = true)
    }
    override suspend fun deleteById(id: String) {
        rows.remove(id)
    }
    override suspend fun clearAll() = rows.clear()
}
