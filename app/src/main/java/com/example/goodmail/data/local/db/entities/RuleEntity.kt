package com.example.goodmail.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.goodmail.domain.model.EmailImportance
import com.example.goodmail.domain.model.ImportanceRule
import com.example.goodmail.domain.model.RuleType

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: String,
    val action: String,
    val createdAt: Long,
)

fun RuleEntity.toDomain(): ImportanceRule = ImportanceRule(
    id = id,
    type = runCatching { RuleType.valueOf(type) }.getOrDefault(RuleType.SENDER),
    value = value,
    action = runCatching { EmailImportance.valueOf(action) }.getOrDefault(EmailImportance.NOT_IMPORTANT),
    createdAt = createdAt,
)

fun ImportanceRule.toEntity(): RuleEntity = RuleEntity(
    id = id,
    type = type.name,
    value = value,
    action = action.name,
    createdAt = createdAt,
)
