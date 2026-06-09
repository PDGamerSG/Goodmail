package com.example.goodmail.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.goodmail.data.local.db.entities.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules")
    suspend fun getAllList(): List<RuleEntity>

    @Insert
    suspend fun insert(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
