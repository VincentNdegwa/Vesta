package com.example.vesta.ui.profile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.components.FinvestaIcon
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.ui.theme.VestaTheme
import java.text.SimpleDateFormat
import java.util.*

private data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onSecuritySettingsClick: () -> Unit = {},
    onSavingsGoalsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onUpgradeToPremiumClick: () -> Unit = {},
    onExportDataClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    securityViewModel: SecurityViewModel = hiltViewModel()
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    val securityUiState by securityViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileTopBar(onBackClick = onBackClick)
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
                // Profile Header Section
                ProfileHeaderSection(
                    userEmail = uiState.userEmail ?: "user@example.com",
                    userDisplayName = uiState.userDisplayName ?: "User",
                    onEditProfileClick = onEditProfileClick
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // Account Section
                ProfileSection(title = "Account") {
                    ProfileMenuItem(
                        icon = Icons.Default.Security,
                        title = "Security Settings",
                        subtitle = "PIN, fingerprint, and privacy",
                        onClick = onSecuritySettingsClick,
                        showArrow = true
                    )

                    ProfileMenuItem(
                        icon = Icons.Default.Savings,
                        title = "Savings Goals",
                        subtitle = "Track your savings progress",
                        onClick = onSavingsGoalsClick,
                        showArrow = true
                    )

                    var showCurrencyDialog by remember { mutableStateOf(false) }
                    val currencies = remember {
                        listOf(
                            CurrencyInfo("USD", "$", "US Dollar"),
                            CurrencyInfo("EUR", "€", "Euro"),
                            CurrencyInfo("GBP", "£", "British Pound"),
                            CurrencyInfo("JPY", "¥", "Japanese Yen"),
                            CurrencyInfo("AUD", "A$", "Australian Dollar"),
                            CurrencyInfo("CAD", "C$", "Canadian Dollar"),
                            CurrencyInfo("CHF", "Fr", "Swiss Franc"),
                            CurrencyInfo("KES", "KSh", "Kenyan Shilling"),
                            CurrencyInfo("UGX", "USh", "Ugandan Shilling"),
                            CurrencyInfo("TZS", "TSh", "Tanzanian Shilling")
                        )
                    }

                    Log.d("ProfileScreen", "Currency ${securityUiState.currency}")
                    val currentCurrency = remember(securityUiState.currency) {
                        currencies.find { it.symbol == securityUiState.currency.trim() }
                            ?: currencies.find { it.symbol.trim() == securityUiState.currency.trim() }
                            ?: currencies.first()
                    }

                    ProfileMenuItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency",
                        subtitle = "Change your preferred currency (${currentCurrency.symbol} - ${currentCurrency.code})",
                        onClick = { showCurrencyDialog = true },
                        showArrow = true
                    )

                    if (showCurrencyDialog) {
                        AlertDialog(
                            onDismissRequest = { showCurrencyDialog = false },
                            title = { Text("Select Currency") },
                            text = {
                                LazyColumn {
                                    items(currencies) { currency ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    securityViewModel.setCurrency(currency.symbol)
                                                    showCurrencyDialog = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = currency.symbol,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Column {
                                                    Text(
                                                        text = currency.code,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    Text(
                                                        text = currency.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                            if (currency.symbol.trim() == securityUiState.currency.trim()) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showCurrencyDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

//                    ProfileMenuItem(
//                        icon = Icons.Default.Notifications,
//                        title = "Notifications",
//                        subtitle = "Customize your alerts",
//                        onClick = onNotificationsClick,
//                        trailingContent = {
//                            Switch(
//                                checked = notificationsEnabled,
//                                onCheckedChange = { notificationsEnabled = it },
//                                colors = SwitchDefaults.colors(
//                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
//                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
//                                )
//                            )
//                        }
//                    )
//
//                    ProfileMenuItem(
//                        icon = Icons.Default.Star,
//                        title = "Upgrade to Premium",
//                        subtitle = "Unlock advanced features",
//                        onClick = onUpgradeToPremiumClick,
//                        showArrow = true,
//                        trailingContent = {
//                            Surface(
//                                shape = RoundedCornerShape(12.dp),
//                                color = MaterialTheme.colorScheme.tertiary
//                            ) {
//                                Text(
//                                    text = "Premium",
//                                    style = MaterialTheme.typography.labelSmall.copy(
//                                        fontWeight = FontWeight.SemiBold
//                                    ),
//                                    color = MaterialTheme.colorScheme.onTertiary,
//                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                                )
//                            }
//                        }
//                    )

                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                // Data & Privacy Section
                ProfileSection(title = "Data & Privacy") {
                    ProfileMenuItem(
                        icon = Icons.Default.Download,
                        title = "Export Data",
                        subtitle = "Download your financial data",
                        onClick = onExportDataClick,
                        showArrow = true
                    )
                    
                    ProfileMenuItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Dark Mode",
                        subtitle = "Toggle dark theme",
                        onClick = { securityViewModel.setDarkMode(!securityUiState.isDarkMode) },
                        trailingContent = {
                            Switch(
                                checked = securityUiState.isDarkMode,
                                onCheckedChange = { securityViewModel.setDarkMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                // App Info Section
                AppInfoSection()
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            item {
                SignOutSection(viewModel,onSignOutClick )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Profile & Settings",
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
private fun ProfileHeaderSection(
    userEmail: String,
    userDisplayName: String,
    onEditProfileClick: () -> Unit
) {
    // Calculate member since date (you can replace this with actual data from user profile)
    val memberSince = remember {
        val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        formatter.format(Date(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000)) // 1 year ago as example
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier=Modifier.fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceAround,
                    ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Show first letter of display name or default icon
                            if (userDisplayName.isNotBlank()) {
                                Text(
                                    text = userDisplayName.first().uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // User Info Column
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // User Name
                            Text(
                                text = userDisplayName.ifBlank { "User" },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            // Email
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Free Plan Badge and Member Since
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text(
                                        text = "Free Plan",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "• Member since $memberSince",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier= Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = onEditProfileClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                width = 1.dp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Edit Profile",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
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
            content()
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showArrow: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Title and Subtitle
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
        
        // Trailing Content
        if (trailingContent != null) {
            trailingContent()
        } else if (showArrow) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AppInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(15.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "F",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Finvesta",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Version 2.1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Links Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = { /* Privacy Policy */ }) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            
            TextButton(onClick = { /* Terms of Service */ }) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
            
            TextButton(onClick = { /* Support */ }) {
                Text(
                    text = "Support",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SignOutSection(
    viewModel: AuthViewModel, onSignOutClick: () -> Unit = {}
) {
    fun signOut() {
        viewModel.signOut()
        onSignOutClick()
    }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { signOut() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Sign Out",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Sign Out",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun ProfileScreenPreview() {
//    VestaTheme {
//        ProfileScreen()
//    }
//}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileScreenDarkPreview() {
    VestaTheme {
        ProfileScreen()
    }
}
