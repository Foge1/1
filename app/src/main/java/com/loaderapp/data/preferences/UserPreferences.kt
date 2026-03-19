package com.loaderapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(
    private val context: Context,
) {
    companion object {
        private val currentUserIdKey = longPreferencesKey("current_user_id")
        private val darkThemeKey = booleanPreferencesKey("dark_theme")
        private val notificationsKey = booleanPreferencesKey("notifications_enabled")
        private val soundKey = booleanPreferencesKey("sound_enabled")
        private val vibrationKey = booleanPreferencesKey("vibration_enabled")
    }

    val currentUserId: Flow<Long?> =
        context.dataStore.data.map { preferences ->
            preferences[currentUserIdKey]
        }

    /**
     * Получить текущий userId синхронно (suspend)
     */
    suspend fun getCurrentUserId(): Long? = currentUserId.first()

    val isDarkTheme: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[darkThemeKey] ?: false
        }

    val isNotificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[notificationsKey] ?: true
        }

    val isSoundEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[soundKey] ?: true
        }

    val isVibrationEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[vibrationKey] ?: true
        }

    suspend fun setCurrentUserId(userId: Long) {
        context.dataStore.edit { preferences ->
            preferences[currentUserIdKey] = userId
        }
    }

    suspend fun clearCurrentUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(currentUserIdKey)
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[darkThemeKey] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[notificationsKey] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[soundKey] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[vibrationKey] = enabled
        }
    }
}
