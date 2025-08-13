package com.example.vesta.ui.security.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.preferences.SecurityPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityPreferences: SecurityPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            combine(
                securityPreferences.isPinEnabled,
                securityPreferences.pinValue,
                securityPreferences.isFingerprintEnabled,
                securityPreferences.autoLockTimeout,
                securityPreferences.hideAmounts,
                securityPreferences.requireAuthForExports
            ) { array ->
                SecurityUiState(
                    pinEnabled = array[0] as Boolean,
                    pin = array[1] as String,
                    fingerprintEnabled = array[2] as Boolean,
                    autoLockTimeout = array[3] as String,
                    hideAmounts = array[4] as Boolean,
                    requireAuthForExports = array[5] as Boolean,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    // PIN management
    fun setPinEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setPinEnabled(enabled)
        }
    }
    
    fun setPin(pin: String) {
        viewModelScope.launch {
            securityPreferences.setPinValue(pin)
        }
    }
    
    fun validatePin(pin: String): Boolean {
        return pin == _uiState.value.pin
    }
    
    // Fingerprint management
    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setFingerprintEnabled(enabled)
        }
    }
    
    // Auto-lock timeout
    fun setAutoLockTimeout(timeout: String) {
        viewModelScope.launch {
            securityPreferences.setAutoLockTimeout(timeout)
        }
    }
    
    // Hide amounts
    fun setHideAmounts(hide: Boolean) {
        viewModelScope.launch {
            securityPreferences.setHideAmounts(hide)
        }
    }
    
    // Require auth for exports
    fun setRequireAuthForExports(require: Boolean) {
        viewModelScope.launch {
            securityPreferences.setRequireAuthForExports(require)
        }
    }
    
    // Check if PIN or fingerprint is set before enabling export authentication
    fun canEnableExportAuth(): Boolean {
        return _uiState.value.pinEnabled || _uiState.value.fingerprintEnabled
    }
    
    fun isSecurityEnabled(): Boolean {
        return _uiState.value.pinEnabled || _uiState.value.fingerprintEnabled
    }
}

data class SecurityUiState(
    val pinEnabled: Boolean = false,
    val pin: String = "",
    val fingerprintEnabled: Boolean = false,
    val autoLockTimeout: String = "1 minute",
    val hideAmounts: Boolean = false,
    val requireAuthForExports: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null
)
