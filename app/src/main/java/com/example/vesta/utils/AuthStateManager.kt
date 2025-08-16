package com.example.vesta.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

private val Context.authDataStore by preferencesDataStore(name = "auth_preferences")

@Singleton
class AuthStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSecurityManager: Lazy<AppSecurityManager>
) {
    // Keys for preferences
    private object PreferencesKeys {
        val SESSION_ACTIVE = booleanPreferencesKey("session_active")
    }
    
    /**
     * Check if the user has an active session
     */
    fun hasActiveSession(): Flow<Boolean> {
        return context.authDataStore.data.map { preferences ->
            // Default to false if not found
            preferences[PreferencesKeys.SESSION_ACTIVE] ?: false
        }
    }
    
    /**
     * Set the session active state
     */
    suspend fun setSessionActive(active: Boolean) {
        context.authDataStore.edit { preferences ->
            preferences[PreferencesKeys.SESSION_ACTIVE] = active
        }
    }
    
    /**
     * Check app auth status - combines session state and security state
     * Returns a pair of (hasSession, requiresSecurity)
     */
    fun getAuthStatus(): Flow<AuthStatus> {
        return appSecurityManager.get().isSecurityEnabled().flatMapLatest { securityEnabled ->
            hasActiveSession().map { hasSession ->
                AuthStatus(
                    hasActiveSession = hasSession,
                    securityEnabled = securityEnabled
                )
            }
        }
    }
    
    /**
     * Helper to check auth state and determine which screen to show
     */
    fun shouldShowSecurityCheck(authStatus: AuthStatus): Boolean {
        return authStatus.hasActiveSession && authStatus.securityEnabled
    }
    
    /**
     * Helper to check if user should go directly to main content
     */
    fun shouldShowMainContent(authStatus: AuthStatus): Boolean {
        return authStatus.hasActiveSession && !authStatus.securityEnabled
    }
    
    /**
     * Helper to check if user should be shown the login screen
     */
    fun shouldShowLogin(authStatus: AuthStatus): Boolean {
        // Log authentication status for debugging
        android.util.Log.d("AuthStateManager", "Checking if should show login: hasActiveSession=${authStatus.hasActiveSession}")
        return !authStatus.hasActiveSession
    }
    
    /**
     * Get current session status immediately (suspend function)
     */
    suspend fun hasActiveSessionSync(): Boolean {
        return hasActiveSession().first()
    }
}

/**
 * Data class representing the current authentication status
 */
data class AuthStatus(
    val hasActiveSession: Boolean = false,
    val securityEnabled: Boolean = false
)
