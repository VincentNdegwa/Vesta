package com.example.vesta.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security_preferences")

@Singleton
class SecurityPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.securityDataStore
    
    // Define preference keys
    companion object {
        private val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        private val PIN_VALUE = stringPreferencesKey("pin_value")
        private val FINGERPRINT_ENABLED = booleanPreferencesKey("fingerprint_enabled")
        private val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        private val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
        private val REQUIRE_AUTH_FOR_EXPORTS = booleanPreferencesKey("require_auth_for_exports")
    }
    
    // PIN Enabled
    val isPinEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PIN_ENABLED] ?: false
    }
    
    suspend fun setPinEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PIN_ENABLED] = enabled
        }
    }
    
    // PIN Value (encrypted in a real app, but we'll use plain text for simplicity)
    val pinValue: Flow<String> = dataStore.data.map { preferences ->
        preferences[PIN_VALUE] ?: ""
    }
    
    suspend fun setPinValue(pin: String) {
        dataStore.edit { preferences ->
            preferences[PIN_VALUE] = pin
        }
    }
    
    // Fingerprint Enabled
    val isFingerprintEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FINGERPRINT_ENABLED] ?: false
    }
    
    suspend fun setFingerprintEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FINGERPRINT_ENABLED] = enabled
        }
    }
    
    // Auto Lock Timeout
    val autoLockTimeout: Flow<String> = dataStore.data.map { preferences ->
        preferences[AUTO_LOCK_TIMEOUT] ?: "1 minute"
    }
    
    suspend fun setAutoLockTimeout(timeout: String) {
        dataStore.edit { preferences ->
            preferences[AUTO_LOCK_TIMEOUT] = timeout
        }
    }
    
    // Hide Amounts
    val hideAmounts: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HIDE_AMOUNTS] ?: false
    }
    
    suspend fun setHideAmounts(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIDE_AMOUNTS] = hide
        }
    }
    
    // Require Auth for Exports
    val requireAuthForExports: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[REQUIRE_AUTH_FOR_EXPORTS] ?: true
    }
    
    suspend fun setRequireAuthForExports(require: Boolean) {
        dataStore.edit { preferences ->
            preferences[REQUIRE_AUTH_FOR_EXPORTS] = require
        }
    }
}
