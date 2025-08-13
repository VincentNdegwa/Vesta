package com.example.vesta.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
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
                                 
            val canAuthenticateResult = biometricManager.canAuthenticate(authenticators)
            
            // Debug information
            val resultString = when (canAuthenticateResult) {
                BiometricManager.BIOMETRIC_SUCCESS -> "BIOMETRIC_SUCCESS"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "BIOMETRIC_ERROR_NO_HARDWARE"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "BIOMETRIC_ERROR_HW_UNAVAILABLE"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "BIOMETRIC_ERROR_NONE_ENROLLED"
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "BIOMETRIC_ERROR_UNSUPPORTED"
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "BIOMETRIC_STATUS_UNKNOWN"
                else -> "UNKNOWN STATUS: $canAuthenticateResult"
            }
            
            println("Biometric authentication status: $resultString")
            
            return canAuthenticateResult == BiometricManager.BIOMETRIC_SUCCESS
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
                    println("Biometric authentication succeeded")
                    continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    println("Biometric authentication failed - not recognized")
                    // We don't complete here as system will show multiple attempts
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    println("Biometric authentication error: [$errorCode] $errString")

                    // Map specific error codes to more user-friendly messages
                    val message = when (errorCode) {
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> "No biometric hardware found on this device"
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable"
                        BiometricPrompt.ERROR_LOCKOUT -> "Too many attempts. Try again later"
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Too many attempts. Biometric authentication disabled"
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> "No biometric features enrolled on this device"
                        BiometricPrompt.ERROR_USER_CANCELED -> "Authentication cancelled"
                        else -> errString.toString()
                    }

                    continuation.resume(BiometricResult.Error(message))
                }
            }

            try {
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText(negativeButtonText)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .setConfirmationRequired(false)
                    .build()

                val biometricPrompt = BiometricPrompt(activity, executor, callback)
                println("Starting biometric authentication...")
                biometricPrompt.authenticate(promptInfo)

                continuation.invokeOnCancellation {
                    println("Biometric authentication was cancelled")
                }
            } catch (e: Exception) {
                println("Exception during biometric authentication: ${e.message}")
                e.printStackTrace()
                continuation.resume(BiometricResult.Error("Error setting up biometric authentication: ${e.message}"))
            }
        }

    }


}

sealed class BiometricResult {
    object Success : BiometricResult()
    data class Error(val message: String) : BiometricResult()
}
