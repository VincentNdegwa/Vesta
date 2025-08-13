package com.example.vesta.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricAuthHelper {

    companion object {
        /**
         * Check if the device has biometric capabilities
         */
        fun canAuthenticate(context: Context): Boolean {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                 BiometricManager.Authenticators.BIOMETRIC_WEAK
            return when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        }
        
        /**
         * Show biometric authentication dialog
         */
        suspend fun showBiometricPrompt(
            activity: FragmentActivity,
            title: String = "Biometric Authentication",
            subtitle: String = "Log in using your biometric credential",
            negativeButtonText: String = "Cancel"
        ): BiometricResult = suspendCancellableCoroutine { continuation ->
            
            val executor = ContextCompat.getMainExecutor(activity)
            
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    continuation.resume(BiometricResult.Success)
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // User authenticated but fingerprint not recognized
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    continuation.resume(BiometricResult.Error(errString.toString()))
                }
            }
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                         BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
            
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
            
            continuation.invokeOnCancellation {
                // Handle cancellation if needed
            }
        }
    }
}

sealed class BiometricResult {
    object Success : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}
