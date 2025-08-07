package com.example.vesta.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finvesta_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        private val IS_OFFLINE_MODE = booleanPreferencesKey("is_offline_mode")
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val LAST_APP_LOCK_TIME = longPreferencesKey("last_app_lock_time")
        private val SELECTED_CURRENCY = stringPreferencesKey("selected_currency")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }
    
    // User Session
    suspend fun setUserSession(userId: String, email: String, displayName: String?) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[USER_ID] = userId
            preferences[USER_EMAIL] = email
            displayName?.let { preferences[USER_DISPLAY_NAME] = it }
        }
    }
    
    suspend fun clearUserSession() {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences.remove(USER_ID)
            preferences.remove(USER_EMAIL)
            preferences.remove(USER_DISPLAY_NAME)
        }
    }
    
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }
    
    val userId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_ID]
    }
    
    val userEmail: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }
    
    val userDisplayName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_DISPLAY_NAME]
    }
    
    // App Configuration
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH] = false
        }
    }
    
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_FIRST_LAUNCH] ?: true
    }
    
    // Sync Management
    suspend fun updateLastSyncTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_SYNC_TIME] = timestamp
        }
    }
    
    val lastSyncTime: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_SYNC_TIME] ?: 0L
    }
    
    // Offline Mode
    suspend fun setOfflineMode(isOffline: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_OFFLINE_MODE] = isOffline
        }
    }
    
    val isOfflineMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_OFFLINE_MODE] ?: false
    }
    
    // Security Settings
    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }
    
    val isAppLockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[APP_LOCK_ENABLED] ?: false
    }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }
    
    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED] ?: false
    }
    
    suspend fun updateLastAppLockTime() {
        dataStore.edit { preferences ->
            preferences[LAST_APP_LOCK_TIME] = System.currentTimeMillis()
        }
    }
    
    val lastAppLockTime: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_APP_LOCK_TIME] ?: 0L
    }
    
    // User Preferences
    suspend fun setCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_CURRENCY] = currency
        }
    }
    
    val selectedCurrency: Flow<String> = dataStore.data.map { preferences ->
        preferences[SELECTED_CURRENCY] ?: "USD"
    }
    
    suspend fun setThemeMode(theme: String) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = theme
        }
    }
    
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "SYSTEM"
    }
}
