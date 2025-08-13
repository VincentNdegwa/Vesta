package com.example.vesta.utils

import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.utils.BiometricResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Helper class to handle authentication for protected features like data export
 */
object AuthenticationHelper {
    
    /**
     * Authenticate user based on security settings
     * @param activity The current FragmentActivity (needed for biometric prompt)
     * @param securityViewModel The security view model to access security settings
     * @param scope Coroutine scope for async operations
     * @param onShowPinDialog Callback to show PIN dialog UI
     * @param onAuthSuccess Called when authentication succeeds
     * @param onAuthFailure Called when authentication fails or is cancelled
     */
    fun authenticate(
        activity: FragmentActivity?,
        securityViewModel: SecurityViewModel,
        scope: CoroutineScope,
        onShowPinDialog: () -> Unit,
        onAuthSuccess: () -> Unit,
        onAuthFailure: () -> Unit
    ) {
        val securityState = securityViewModel.uiState.value
        
        // Check if authentication is required
        if (!securityViewModel.isSecurityEnabled() || !securityState.requireAuthForExports) {
            onAuthSuccess()
            return
        }
        
        // Try fingerprint first if enabled
        if (securityState.fingerprintEnabled && activity != null) {
            scope.launch {
                val result = BiometricAuthHelper.showBiometricPrompt(
                    activity = activity,
                    title = "Authentication Required",
                    subtitle = "Authenticate to export data"
                )
                
                when (result) {
                    is BiometricResult.Success -> {
                        onAuthSuccess()
                    }
                    is BiometricResult.Error -> {
                        // If fingerprint fails, fall back to PIN if enabled
                        if (securityState.pinEnabled) {
                            onShowPinDialog()
                        } else {
                            onAuthFailure()
                        }
                    }
                }
            }
        } else if (securityState.pinEnabled) {
            // Only PIN is enabled, show PIN dialog
            onShowPinDialog()
        } else {
            // No authentication method enabled
            onAuthSuccess()
        }
    }
}
