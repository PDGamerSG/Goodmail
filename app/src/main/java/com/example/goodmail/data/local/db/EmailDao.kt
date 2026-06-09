package com.example.goodmail.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.goodmail.data.local.db.entities.EmailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {

    @Query("SELECT * FROM emails ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE id = :id")
    suspend fun getById(id: String): EmailEntity?

    @Query("SELECT body FROM emails WHERE id = :id")
    suspend fun getBody(id: String): String?

    @Query("SELECT * FROM emails WHERE importance = 'UNCLASSIFIED' ORDER BY timestamp DESC")
    suspend fun getUnclassified(): List<EmailEntity>

    @Query("UPDATE emails SET importance = :importance WHERE id = :id")
    suspend fun updateImportance(id: String, importance: String)

    @Upsert
    suspend fun upsertAll(emails: List<EmailEntity>)

    @Query("UPDATE emails SET body = :body WHERE id = :id")
    suspend fun updateBody(id: String, body: String)

    @Query("UPDATE emails SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("DELETE FROM emails WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM emails")
    suspend fun clearAll()
}
