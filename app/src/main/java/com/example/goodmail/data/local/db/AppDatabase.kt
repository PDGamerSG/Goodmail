package com.example.goodmail.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.goodmail.data.local.db.entities.EmailEntity
import com.example.goodmail.data.local.db.entities.RuleEntity

@Database(entities = [EmailEntity::class, RuleEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao
    abstract fun ruleDao(): RuleDao
}
