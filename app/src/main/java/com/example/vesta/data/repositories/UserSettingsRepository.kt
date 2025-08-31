package com.example.vesta.data.repositories

import com.example.vesta.data.local.dao.UserSettingsDao
import com.example.vesta.data.local.entities.UserSettingsEntity
import com.example.vesta.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao,
    private val authRepository: AuthRepository
) {
    
    fun getUserSettings(): Flow<UserSettingsEntity> {
        return authRepository.currentUserId.flatMapLatest { userId ->
            if (userId.isNullOrEmpty()) {
                kotlinx.coroutines.flow.flowOf(createDefaultSettings(""))
            } else {
                userSettingsDao.getUserSettingsFlow(userId)
                    .map { it ?: createDefaultSettings(userId) }
            }
        }
    }
    
    // Update PIN settings
    suspend fun updatePinSettings(enabled: Boolean, pinHash: String? = null) {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return
        
        val currentSettings = userSettingsDao.getUserSettings(userId) ?: createDefaultSettings(userId)
        val updatedSettings = currentSettings.copy(
            isPinEnabled = enabled,
            pinHash = if (enabled) pinHash else null,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Helper method to get the current user ID
    private suspend fun getCurrentUserId(): String? {
        return authRepository.currentUserId.first()
    }
    
    // Update biometric settings
    suspend fun updateBiometricSettings(enabled: Boolean) {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return
        
        val currentSettings = userSettingsDao.getUserSettings(userId) ?: createDefaultSettings(userId)
        val updatedSettings = currentSettings.copy(
            isBiometricEnabled = enabled,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Update auto lock timeout
    suspend fun updateAutoLockTimeout(timeoutMinutes: Int) {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return
        
        val currentSettings = userSettingsDao.getUserSettings(userId) ?: createDefaultSettings(userId)
        val updatedSettings = currentSettings.copy(
            lockTimeoutMinutes = timeoutMinutes,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Update hide amounts setting
    suspend fun updateHideAmounts(hide: Boolean) {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return
        
        val currentSettings = userSettingsDao.getUserSettings(userId) ?: createDefaultSettings(userId)
        val updatedSettings = currentSettings.copy(
            hideAmounts = hide,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    suspend fun updateDarkMode(enabled: Boolean) {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return

        val currentSettings = userSettingsDao.getUserSettings(userId) ?: createDefaultSettings(userId)
        val updatedSettings = currentSettings.copy(
            darkMode = enabled,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }

    suspend fun updateCurrency(currency: String) {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return

        val currentSettings = userSettingsDao.getUserSettings(userId) ?: createDefaultSettings(userId)
        val updatedSettings = currentSettings.copy(
            currency = currency,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Update require auth for exports
    suspend fun updateRequireAuthForExports(require: Boolean) {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return
        
        val currentSettings = userSettingsDao.getUserSettings(userId) ?: createDefaultSettings(userId)
        val updatedSettings = currentSettings.copy(
            requireAuthForExports = require,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        userSettingsDao.insertUserSettings(updatedSettings)
    }
    
    // Create default settings for a new user
    private fun createDefaultSettings(userId: String): UserSettingsEntity {
        return UserSettingsEntity(
            userId = userId,
            isPinEnabled = false,
            pinHash = null,
            isBiometricEnabled = false,
            lockTimeoutMinutes = 5,
            hideAmounts = false,
            requireAuthForExports = true,
            darkMode = false
        )
    }
    
    fun timeoutStringToMinutes(timeoutString: String): Int {
        return when (timeoutString) {
            "Immediately" -> 0
            "1 minute" -> 1
            "5 minutes" -> 5
            "30 minutes" -> 30
            "Never" -> -1 
            else -> 5 
        }
    }
    
    fun minutesToTimeoutString(minutes: Int): String {
        return when (minutes) {
            0 -> "Immediately"
            1 -> "1 minute"
            5 -> "5 minutes"
            30 -> "30 minutes"
            -1 -> "Never"
            else -> "5 minutes" 
        }
    }
    
    fun hashPin(pin: String): String {
        // Simple "hash" for demo purposes - DON'T USE IN PRODUCTION
        return pin.reversed()
    }
    
    suspend fun validatePin(pin: String): Boolean {
        val userId = getCurrentUserId()
        if (userId.isNullOrEmpty()) return false
        
        val settings = userSettingsDao.getUserSettings(userId) ?: return false
        return settings.pinHash == hashPin(pin)
    }
}
