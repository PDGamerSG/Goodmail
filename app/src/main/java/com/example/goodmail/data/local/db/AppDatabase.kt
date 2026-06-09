package com.example.goodmail.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.goodmail.data.local.db.entities.EmailEntity

@Database(entities = [EmailEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao
}
