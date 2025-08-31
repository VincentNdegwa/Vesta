package com.example.vesta.ui.security.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.repositories.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()
    
    private var currentPin: String = ""
    
    init {
        viewModelScope.launch {
            userSettingsRepository.getUserSettings()
                .map { settings ->
                    SecurityUiState(
                        pinEnabled = settings.isPinEnabled,
                        pin = settings.pinHash?.let { "****" } ?: "",  // Don't expose actual PIN hash
                        fingerprintEnabled = settings.isBiometricEnabled,
                        autoLockTimeout = userSettingsRepository.minutesToTimeoutString(settings.lockTimeoutMinutes),
                        hideAmounts = settings.hideAmounts,
                        requireAuthForExports = settings.requireAuthForExports,
                        isDarkMode = settings.darkMode,
                        isLoading = false
                    ).also {
                        // Keep track of PIN for validation without exposing it in UI state
                        if (settings.pinHash != null) {
                            currentPin = settings.pinHash
                        }
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }
    
    // PIN management
    fun setPinEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                // When disabling PIN, pass null for the hash
                userSettingsRepository.updatePinSettings(enabled, null)
            }
            // When enabling PIN, the setPin function will be called separately with the new PIN
        }
    }
    
    fun setPin(pin: String) {
        viewModelScope.launch {
            val hashedPin = userSettingsRepository.hashPin(pin)
            userSettingsRepository.updatePinSettings(true, hashedPin)
            currentPin = hashedPin
        }
    }
    
    fun validatePin(pin: String): Boolean {
        // In a real app, we should verify against the hashed value in the database
        return userSettingsRepository.hashPin(pin) == currentPin
    }
    
    // Fingerprint management
    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.updateBiometricSettings(enabled)
        }
    }
    
    // Auto-lock timeout
    fun setAutoLockTimeout(timeout: String) {
        viewModelScope.launch {
            val timeoutMinutes = userSettingsRepository.timeoutStringToMinutes(timeout)
            userSettingsRepository.updateAutoLockTimeout(timeoutMinutes)
        }
    }
    
    // Hide amounts
    fun setHideAmounts(hide: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.updateHideAmounts(hide)
        }
    }
    
    // Require auth for exports
    fun setRequireAuthForExports(require: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.updateRequireAuthForExports(require)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userSettingsRepository.updateDarkMode(enabled)
        }
    }
    
    // Check if PIN or fingerprint is set before enabling export authentication
    fun canEnableExportAuth(): Boolean {
        return _uiState.value.pinEnabled || _uiState.value.fingerprintEnabled
    }
    
    fun isSecurityEnabled(): Boolean {
        val result = _uiState.value.pinEnabled || _uiState.value.fingerprintEnabled
        android.util.Log.d("SecurityViewModel", 
            "isSecurityEnabled check: pin=${_uiState.value.pinEnabled}, " +
            "fingerprint=${_uiState.value.fingerprintEnabled}, result=$result")
        return result
    }
}

data class SecurityUiState(
    val pinEnabled: Boolean = false,
    val pin: String = "",
    val fingerprintEnabled: Boolean = false,
    val autoLockTimeout: String = "Never",
    val hideAmounts: Boolean = false,
    val requireAuthForExports: Boolean = false,
    val isDarkMode: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null
)
