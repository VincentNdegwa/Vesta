package com.example.vesta.utils

import android.content.Context
import com.example.vesta.data.repositories.UserSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage app security state
 */
@Singleton
class AppSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsRepository: UserSettingsRepository
) {
    /**
     * Check if the app should be locked based on security settings
     */
    fun shouldLockApp(): Flow<Boolean> {
        return userSettingsRepository.getUserSettings()
            .map { settings ->
                settings.isPinEnabled || settings.isBiometricEnabled
            }
    }
    
    /**
     * Check if security is enabled (PIN or biometric)
     */
    fun isSecurityEnabled(): Flow<Boolean> {
        return userSettingsRepository.getUserSettings()
            .map { settings ->
                settings.isPinEnabled || settings.isBiometricEnabled
            }
    }
    
    /**
     * Get security status immediately (suspend function)
     */
    suspend fun isSecurityEnabledSync(): Boolean {
        return isSecurityEnabled().first()
    }
    
    /**
     * Check if app has been in background long enough to require locking
     */
    fun shouldRequireAuthAfterBackground(backgroundTimeMillis: Long): Flow<Boolean> {
        return userSettingsRepository.getUserSettings()
            .map { settings ->
                val securityEnabled = settings.isPinEnabled || settings.isBiometricEnabled
                
                if (!securityEnabled) {
                    return@map false
                }
                
                // Convert timeout from minutes to milliseconds
                val timeoutMillis = settings.lockTimeoutMinutes * 60 * 1000L
                
                // Special case: "immediately" (0 minutes)
                if (settings.lockTimeoutMinutes == 0) {
                    return@map true
                }
                
                // Special case: "never" (represented by -1)
                if (settings.lockTimeoutMinutes < 0) {
                    return@map false
                }
                
                // Check if app has been in background longer than timeout
                backgroundTimeMillis >= timeoutMillis
            }
    }
}
