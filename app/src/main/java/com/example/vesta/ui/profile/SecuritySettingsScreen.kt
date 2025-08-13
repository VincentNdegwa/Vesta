package com.example.vesta.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.components.AutoLockTimeoutDropdown
import com.example.vesta.ui.components.PinInputDialog
import com.example.vesta.ui.components.PinSetupDialog
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.ui.theme.VestaTheme
import com.example.vesta.utils.BiometricAuthHelper
import com.example.vesta.utils.BiometricResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val fragmentActivity = remember { context as? FragmentActivity }
    
    // Dialog states
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinInputDialog by remember { mutableStateOf(false) }
    
    // Auto-lock timeout options
    val timeoutOptions = listOf("Immediately", "30 seconds", "1 minute", "5 minutes", "30 minutes", "Never")
    
    // Check if device supports biometrics (fingerprint, face ID, etc.)
    val supportsBiometrics = remember { 
        if (fragmentActivity == null) {
            println("FragmentActivity is null, cannot check biometric support properly")
            false
        } else {
            BiometricAuthHelper.canAuthenticate(context)
        }
    }
    
    // Display a message if activity is null
    LaunchedEffect(fragmentActivity) {
        if (fragmentActivity == null) {
            Toast.makeText(context, "Warning: Activity context not available. Some features may not work.", Toast.LENGTH_LONG).show()
        }
    }

    
    Scaffold(
        modifier = modifier,
        topBar = {
            SecurityTopBar(onBackClick = onBackClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                // Security Status Header
                SecurityStatusHeader(isSecured = viewModel.isSecurityEnabled())
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // PIN Protection Card
                SecurityCard(
                    icon = Icons.Default.Lock,
                    title = "PIN Protection",
                    subtitle = "Secure your app with a 4-digit PIN",
                    enabled = uiState.pinEnabled,
                    onEnabledChange = { enabled ->
                        if (enabled) {
                            // Show PIN setup dialog when enabling
                            showPinSetupDialog = true
                        } else {
                            // Verify current PIN before disabling
                            showPinInputDialog = true
                        }
                    },
                    statusText = if (uiState.pinEnabled) "PIN protection is active" else null
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Only show fingerprint option if supported by device
            if (supportsBiometrics) {
                item {
                    // Biometric Authentication Card (Fingerprint, Face ID, etc.)
                    SecurityCard(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = "Use your fingerprint or face ID to unlock the app",
                        enabled = uiState.fingerprintEnabled,
                        onEnabledChange = { enabled ->
                            if (enabled) {
                                // Try authenticating before enabling
                                if (fragmentActivity != null) {
                                    scope.launch {
                                        val result = BiometricAuthHelper.showBiometricPrompt(
                                            activity = fragmentActivity,
                                            title = "Enable Biometric Authentication",
                                            subtitle = "Verify your identity to enable this feature"
                                        )
                                        
                                        when (result) {
                                            is BiometricResult.Success -> {
                                                viewModel.setFingerprintEnabled(true)
                                                Toast.makeText(context, "Biometric authentication enabled", Toast.LENGTH_SHORT).show()
                                            }
                                            is BiometricResult.Error -> {
                                                Toast.makeText(context, "Authentication failed: ${result.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    println("Activity is null, cannot show biometric prompt")
                                    Toast.makeText(
                                        context, 
                                        "Cannot access biometric features. Please try restarting the app.", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                // Allow disabling without verification
                                viewModel.setFingerprintEnabled(false)
                            }
                        },
                        statusText = if (uiState.fingerprintEnabled) "Biometric authentication is active" else null
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // Additional Security Section
                AdditionalSecuritySection(
                    autoLockTimeout = uiState.autoLockTimeout,
                    onAutoLockTimeoutChange = { viewModel.setAutoLockTimeout(it) },
                    hideAmounts = uiState.hideAmounts,
                    onHideAmountsChange = { viewModel.setHideAmounts(it) },
                    requireAuthForExports = uiState.requireAuthForExports,
                    onRequireAuthForExportsChange = { 
                        if (it && !viewModel.canEnableExportAuth()) {
                            Toast.makeText(
                                context, 
                                "Please set up PIN or fingerprint authentication first", 
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            viewModel.setRequireAuthForExports(it) 
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // PIN Setup Dialog
    PinSetupDialog(
        showDialog = showPinSetupDialog,
        onDismiss = { showPinSetupDialog = false },
        onPinConfirmed = { pin ->
            viewModel.setPin(pin)
            viewModel.setPinEnabled(true)
            showPinSetupDialog = false
            Toast.makeText(context, "PIN protection enabled", Toast.LENGTH_SHORT).show()
        }
    )
    
    // PIN Input Dialog for verification when disabling
    PinInputDialog(
        showDialog = showPinInputDialog,
        onDismiss = { showPinInputDialog = false },
        onPinEntered = { enteredPin ->
            val isValid = viewModel.validatePin(enteredPin)
            if (isValid) {
                viewModel.setPinEnabled(false)
                Toast.makeText(context, "PIN protection disabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
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
                        title = "Verify Fingerprint",
                        subtitle = "Use your fingerprint to disable PIN protection"
                    )
                    
                    when (result) {
                        is BiometricResult.Success -> {
                            viewModel.setPinEnabled(false)
                            showPinInputDialog = false
                            Toast.makeText(context, "PIN protection disabled", Toast.LENGTH_SHORT).show()
                        }
                        is BiometricResult.Error -> {
                            Toast.makeText(context, "Authentication failed: ${result.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Security Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun SecurityStatusHeader(isSecured: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Security Shield Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isSecured) "Your account is secure" else "Security not enabled",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Text(
                text = if (isSecured) "Protected with PIN or biometrics" else "Enable PIN or fingerprint to secure your app",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SecurityCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    statusText: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (icon == Icons.Default.Fingerprint) 
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (icon == Icons.Default.Fingerprint) 
                            MaterialTheme.colorScheme.tertiary 
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Enable/Disable Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (icon == Icons.Default.Lock) "Enable PIN" else "Enable Fingerprint",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Status Text (for fingerprint)
            if (statusText != null && enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdditionalSecurityItem(
    title: String,
    subtitle: String,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        trailingContent()
    }
}

@Composable
private fun AdditionalSecuritySection(
    autoLockTimeout: String,
    onAutoLockTimeoutChange: (String) -> Unit,
    hideAmounts: Boolean,
    onHideAmountsChange: (Boolean) -> Unit,
    requireAuthForExports: Boolean,
    onRequireAuthForExportsChange: (Boolean) -> Unit
) {
    // Auto-lock timeout options
    val timeoutOptions = listOf("Immediately", "30 seconds", "1 minute", "5 minutes", "30 minutes", "Never")
    
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Additional Security",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                AdditionalSecurityItem(
                    title = "Auto-lock timeout",
                    subtitle = "Lock app after inactivity",
                    trailingContent = {
                        AutoLockTimeoutDropdown(
                            currentTimeout = autoLockTimeout,
                            timeoutOptions = timeoutOptions,
                            onTimeoutSelected = onAutoLockTimeoutChange,
                            menuContent = { onClick ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    modifier = Modifier.clickable(onClick = onClick)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = autoLockTimeout,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                )
                
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                // Hide amounts
                AdditionalSecurityItem(
                    title = "Hide amounts",
                    subtitle = "Tap to reveal sensitive financial information",
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (hideAmounts) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideAmounts) "Hide Amounts" else "Show Amounts",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Switch(
                                checked = hideAmounts,
                                onCheckedChange = onHideAmountsChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                )
                
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                // Require authentication for exports
                AdditionalSecurityItem(
                    title = "Require authentication for exports",
                    subtitle = "Verify identity before exporting data",
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (requireAuthForExports) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Authentication Required",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Switch(
                                checked = requireAuthForExports,
                                onCheckedChange = onRequireAuthForExportsChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun SecuritySettingsScreenPreview() {
//    VestaTheme {
//        SecuritySettingsScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SecuritySettingsScreenDarkPreview() {
    VestaTheme {
        SecuritySettingsScreen()
    }
}
