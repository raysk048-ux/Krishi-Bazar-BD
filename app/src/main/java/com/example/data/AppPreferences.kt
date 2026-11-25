package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.krishiPrefsDataStore by preferencesDataStore(name = "krishi_bazar_prefs")

class AppPreferences(private val context: Context) {
    private object Keys {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val INITIAL_SESSION_READY = booleanPreferencesKey("initial_session_ready_v3")
    }

    val activeUserIdFlow: Flow<String?> = context.krishiPrefsDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_USER_ID]
    }

    val languageFlow: Flow<AppLanguage> = context.krishiPrefsDataStore.data.map { prefs ->
        when (prefs[Keys.APP_LANGUAGE]) {
            AppLanguage.ENGLISH.code -> AppLanguage.ENGLISH
            else -> AppLanguage.BANGLA
        }
    }

    suspend fun isInitialSessionReady(): Boolean {
        return context.krishiPrefsDataStore.data.first()[Keys.INITIAL_SESSION_READY] == true
    }

    suspend fun markInitialSessionReady() {
        context.krishiPrefsDataStore.edit { prefs ->
            prefs[Keys.INITIAL_SESSION_READY] = true
        }
    }

    suspend fun setActiveUserId(userId: String?) {
        context.krishiPrefsDataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(Keys.ACTIVE_USER_ID)
            } else {
                prefs[Keys.ACTIVE_USER_ID] = userId
            }
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.krishiPrefsDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.code
        }
    }
}
