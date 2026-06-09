package com.example.goodmail.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** App configuration: the Groq API key + model, plus background-sync preferences (Phase 5). */
@Singleton
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val apiKey: Flow<String?> = dataStore.data.map { it[KEY_API] }
    val model: Flow<String> = dataStore.data.map { it[KEY_MODEL] ?: DEFAULT_MODEL }
    val syncMinutes: Flow<Int> = dataStore.data.map { it[KEY_SYNC_MINUTES] ?: DEFAULT_SYNC_MINUTES }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIFY] ?: true }

    suspend fun setApiKey(key: String) = dataStore.edit { it[KEY_API] = key.trim() }
    suspend fun setModel(model: String) = dataStore.edit { it[KEY_MODEL] = model.trim() }
    suspend fun setSyncMinutes(minutes: Int) = dataStore.edit { it[KEY_SYNC_MINUTES] = minutes }
    suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.edit { it[KEY_NOTIFY] = enabled }

    companion object {
        const val DEFAULT_MODEL = "llama-3.1-8b-instant"
        const val DEFAULT_SYNC_MINUTES = 15

        private val KEY_API = stringPreferencesKey("groq_api_key")
        private val KEY_MODEL = stringPreferencesKey("groq_model")
        private val KEY_SYNC_MINUTES = intPreferencesKey("sync_minutes")
        private val KEY_NOTIFY = booleanPreferencesKey("notifications_enabled")
    }
}
