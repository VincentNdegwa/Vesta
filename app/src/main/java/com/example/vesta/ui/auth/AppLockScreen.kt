package com.example.vesta.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.components.PinInputDialog
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.utils.BiometricAuthHelper
import com.example.vesta.utils.BiometricResult
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fragmentActivity = remember { context as? FragmentActivity }
    
    // Dialog states
    var showPinInputDialog by remember { mutableStateOf(false) }
    
    // Background color
    val backgroundColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Lock icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "App locked",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "App Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Please authenticate to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Fingerprint button (if enabled)
            if (uiState.fingerprintEnabled && fragmentActivity != null) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = BiometricAuthHelper.showBiometricPrompt(
                                activity = fragmentActivity,
                                title = "Unlock App",
                                subtitle = "Use your fingerprint to unlock the app"
                            )
                            
                            when (result) {
                                is BiometricResult.Success -> onUnlocked()
                                is BiometricResult.Error -> {
                                    // If fingerprint fails and PIN is enabled, show PIN dialog
                                    if (uiState.pinEnabled) {
                                        showPinInputDialog = true
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Use Fingerprint")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // PIN button (if enabled)
            if (uiState.pinEnabled) {
                Button(
                    onClick = { showPinInputDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.fingerprintEnabled) 
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Use PIN", 
                        color = if (uiState.fingerprintEnabled) 
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // PIN Input Dialog
    PinInputDialog(
        showDialog = showPinInputDialog,
        onDismiss = { showPinInputDialog = false },
        onPinEntered = { enteredPin ->
            val isValid = viewModel.validatePin(enteredPin)
            if (isValid) {
                onUnlocked()
            } else {
                // Show error message
            }
            showPinInputDialog = false
        },
        onPinValidated = { /* Nothing to do */ },
        showFingerprint = uiState.fingerprintEnabled,
        onUseFingerprintClick = {
            if (fragmentActivity != null) {
                scope.launch {
                    val result = BiometricAuthHelper.showBiometricPrompt(
                        activity = fragmentActivity,
                        title = "Unlock App",
                        subtitle = "Use your fingerprint to unlock the app"
                    )
                    
                    when (result) {
                        is BiometricResult.Success -> {
                            onUnlocked()
                            showPinInputDialog = false
                        }
                        is BiometricResult.Error -> {
                            // Keep PIN dialog open for retry
                        }
                    }
                }
            }
        },
        title = "Enter PIN",
        subtitle = "Enter your PIN to unlock the app"
    )
    
    // If biometric is enabled, show the prompt immediately
    LaunchedEffect(Unit) {
        if (uiState.fingerprintEnabled && fragmentActivity != null) {
            scope.launch {
                val result = BiometricAuthHelper.showBiometricPrompt(
                    activity = fragmentActivity,
                    title = "Unlock App",
                    subtitle = "Use your fingerprint to unlock the app"
                )
                
                when (result) {
                    is BiometricResult.Success -> onUnlocked()
                    is BiometricResult.Error -> {
                        // If fingerprint fails and PIN is enabled, show PIN dialog
                        if (uiState.pinEnabled) {
                            showPinInputDialog = true
                        }
                    }
                }
            }
        } else if (uiState.pinEnabled) {
            // If only PIN is enabled, show PIN dialog immediately
            showPinInputDialog = true
        }
    }
}
