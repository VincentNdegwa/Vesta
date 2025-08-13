package com.example.vesta.data.repositories

import com.example.vesta.data.local.dao.UserSettingsDao
import com.example.vesta.data.local.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) {
    // Temporary user ID until proper auth is implemented
    private val currentUserId = "current_user"
    
    // Get the current user's settings as a Flow
    fun getUserSettings(): Flow<UserSettingsEntity> {
        return userSettingsDao.getUserSettingsFlow(currentUserId)
            .map { it ?: createDefaultSettings() }
    }
    
    // Update PIN settings
    suspend fun updatePinSettings(enabled: Boolean, pinHash: String? = null) {
        val currentSettings = userSettingsDao.getUserSettings(currentUserId) ?: createDefaultSettings()
        val updatedSettings = currentSettings.copy(
            isPinEnabled = enabled,
            pinHash = if (enabled) pinHash else null,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Update biometric settings
    suspend fun updateBiometricSettings(enabled: Boolean) {
        val currentSettings = userSettingsDao.getUserSettings(currentUserId) ?: createDefaultSettings()
        val updatedSettings = currentSettings.copy(
            isBiometricEnabled = enabled,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Update auto lock timeout
    suspend fun updateAutoLockTimeout(timeoutMinutes: Int) {
        val currentSettings = userSettingsDao.getUserSettings(currentUserId) ?: createDefaultSettings()
        val updatedSettings = currentSettings.copy(
            lockTimeoutMinutes = timeoutMinutes,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Update hide amounts setting
    suspend fun updateHideAmounts(hide: Boolean) {
        val currentSettings = userSettingsDao.getUserSettings(currentUserId) ?: createDefaultSettings()
        val updatedSettings = currentSettings.copy(
            hideAmounts = hide,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Update require auth for exports
    suspend fun updateRequireAuthForExports(require: Boolean) {
        val currentSettings = userSettingsDao.getUserSettings(currentUserId) ?: createDefaultSettings()
        val updatedSettings = currentSettings.copy(
            requireAuthForExports = require,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Create default settings for a new user
    private fun createDefaultSettings(): UserSettingsEntity {
        return UserSettingsEntity(
            userId = currentUserId,
            isPinEnabled = false,
            pinHash = null,
            isBiometricEnabled = false,
            lockTimeoutMinutes = 5,
            hideAmounts = false,
            requireAuthForExports = true
        )
    }
    
    // Helper function to convert timeout string to minutes
    fun timeoutStringToMinutes(timeoutString: String): Int {
        return when (timeoutString) {
            "Immediately" -> 0
            "30 seconds" -> 1 // Rounded to 1 minute since our entity uses minutes
            "1 minute" -> 1
            "5 minutes" -> 5
            "30 minutes" -> 30
            "Never" -> -1 // Special value for "never"
            else -> 5 // Default to 5 minutes
        }
    }
    
    // Helper function to convert minutes to timeout string
    fun minutesToTimeoutString(minutes: Int): String {
        return when (minutes) {
            0 -> "Immediately"
            1 -> "1 minute"
            5 -> "5 minutes"
            30 -> "30 minutes"
            -1 -> "Never"
            else -> "5 minutes" // Default
        }
    }
    
    // Helper function to hash PIN - in a real app, use proper cryptographic hashing
    fun hashPin(pin: String): String {
        // Simple "hash" for demo purposes - DON'T USE IN PRODUCTION
        return pin.reversed()
    }
    
    // Validate PIN
    suspend fun validatePin(pin: String): Boolean {
        val settings = userSettingsDao.getUserSettings(currentUserId) ?: return false
        return settings.pinHash == hashPin(pin)
    }
}
