package com.example.goodmail.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the signed-in Google account email. We deliberately store ONLY the email; OAuth tokens
 * are fetched and refreshed on demand by GoogleAccountCredential.
 */
@Singleton
class AccountStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val accountEmail: Flow<String?> = dataStore.data.map { prefs -> prefs[KEY_EMAIL] }

    suspend fun setAccountEmail(email: String) {
        dataStore.edit { prefs -> prefs[KEY_EMAIL] = email }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(KEY_EMAIL) }
    }

    private companion object {
        val KEY_EMAIL = stringPreferencesKey("account_email")
    }
}
