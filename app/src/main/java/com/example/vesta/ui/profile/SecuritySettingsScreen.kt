package com.example.vesta.ui.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vesta.ui.theme.VestaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    var pinEnabled by remember { mutableStateOf(false) }
    var fingerprintEnabled by remember { mutableStateOf(true) }
    var hideInAppSwitcher by remember { mutableStateOf(true) }
    var requireAuthForExports by remember { mutableStateOf(true) }
    var autoLockTimeout by remember { mutableStateOf("1 minute") }
    
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
                SecurityStatusHeader()
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
                    enabled = pinEnabled,
                    onEnabledChange = { pinEnabled = it }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // Fingerprint Authentication Card
                SecurityCard(
                    icon = Icons.Default.Fingerprint,
                    title = "Fingerprint Authentication",
                    subtitle = "Use your fingerprint to unlock the app",
                    enabled = fingerprintEnabled,
                    onEnabledChange = { fingerprintEnabled = it },
                    statusText = if (fingerprintEnabled) "Fingerprint authentication is active" else null
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                // Additional Security Section
                AdditionalSecuritySection(
                    autoLockTimeout = autoLockTimeout,
                    onAutoLockTimeoutChange = { autoLockTimeout = it },
                    hideInAppSwitcher = hideInAppSwitcher,
                    onHideInAppSwitcherChange = { hideInAppSwitcher = it },
                    requireAuthForExports = requireAuthForExports,
                    onRequireAuthForExportsChange = { requireAuthForExports = it }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
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
private fun SecurityStatusHeader() {
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
                text = "Your account is secure",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
            
            Text(
                text = "Protected with biometrics",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                                Color(0xFFFFA726).copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (icon == Icons.Default.Fingerprint) 
                            Color(0xFFFFA726) 
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
                    color = Color(0xFFFFA726).copy(alpha = 0.1f)
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
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFFFFA726)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdditionalSecuritySection(
    autoLockTimeout: String,
    onAutoLockTimeoutChange: (String) -> Unit,
    hideInAppSwitcher: Boolean,
    onHideInAppSwitcherChange: (Boolean) -> Unit,
    requireAuthForExports: Boolean,
    onRequireAuthForExportsChange: (Boolean) -> Unit
) {
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
                // Auto-lock timeout
                AdditionalSecurityItem(
                    title = "Auto-lock timeout",
                    subtitle = "Lock app after inactivity",
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.clickable { /* Show timeout picker */ }
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
                
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                // Hide amounts in app switcher
                AdditionalSecurityItem(
                    title = "Hide amounts in app switcher",
                    subtitle = "Protect sensitive information",
                    trailingContent = {
                        Switch(
                            checked = hideInAppSwitcher,
                            onCheckedChange = onHideInAppSwitcherChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                )
                
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                // Require authentication for exports
                AdditionalSecurityItem(
                    title = "Require authentication for exports",
                    subtitle = "Additional security for data export",
                    trailingContent = {
                        Switch(
                            checked = requireAuthForExports,
                            onCheckedChange = onRequireAuthForExportsChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AdditionalSecurityItem(
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
