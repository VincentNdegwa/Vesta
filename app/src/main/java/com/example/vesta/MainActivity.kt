package com.example.vesta

import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.ui.auth.AppLockScreen
import com.example.vesta.ui.auth.AuthNavigation
import com.example.vesta.ui.bills.AddBillScreen
import com.example.vesta.ui.bills.BillsScreen
import com.example.vesta.ui.bills.EditBillScreen
import com.example.vesta.ui.budget.BudgetScreen
import com.example.vesta.ui.budget.BudgetSetupScreen
import com.example.vesta.ui.components.FinvestaBottomBar
import com.example.vesta.ui.components.LoadingScreen
import com.example.vesta.ui.dashboard.DashboardScreen
import com.example.vesta.ui.profile.EditProfileScreen
import com.example.vesta.ui.profile.ProfileScreen
import com.example.vesta.ui.profile.SecuritySettingsScreen
import com.example.vesta.ui.profile.UpgradeToPremiumScreen
import com.example.vesta.ui.reports.ExportReportsScreen
import com.example.vesta.ui.reports.ReportsScreen
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.ui.transaction.AddTransactionScreen
import com.example.vesta.ui.theme.VestaTheme
import com.example.vesta.utils.AppSecurityManager
import com.example.vesta.utils.AuthStateManager
import com.example.vesta.utils.AuthStatus
import com.google.android.datatransport.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var appSecurityManager: AppSecurityManager
    
    @Inject
    lateinit var authStateManager: AuthStateManager
    
    // Track when app went to background
    private var backgroundTime: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Get current security status - this will force the app to check security settings on launch
        val initialSecurityCheck = runBlocking {
            val securityEnabled = appSecurityManager.isSecurityEnabledSync()
            android.util.Log.d("MainActivity", "Initial security check: $securityEnabled")
            securityEnabled
        }
        
        setContent {
            VestaTheme {
                // For the actual app, we want to check auth status properly
                FinvestaApp(
                    debugSessionActive = true, // Force active session for testing
                    initialLockState = initialSecurityCheck // Pass the security status
                )
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Record time when app went to background
        backgroundTime = SystemClock.elapsedRealtime()
    }
    
    override fun onResume() {
        super.onResume()
        // Calculate time spent in background
        val timeInBackgroundMs = if (backgroundTime > 0) {
            SystemClock.elapsedRealtime() - backgroundTime
        } else {
            0
        }
        
        // Reset background time
        backgroundTime = 0
        
        // Get current security status synchronously
        val securityEnabled = runBlocking {
            appSecurityManager.isSecurityEnabledSync()
        }
        
        // Check if timeout has passed
        val shouldLock = runBlocking {
            appSecurityManager.shouldRequireAuthAfterBackground(timeInBackgroundMs).first()
        }
        
        android.util.Log.d("MainActivity", "onResume: securityEnabled=$securityEnabled, shouldLock=$shouldLock, timeInBackground=$timeInBackgroundMs")
        
        // Set to lock the app if timeout has passed
        // This will be picked up by the AppLockScreen
        setContent {
            VestaTheme {
                FinvestaApp(
                    initialLockState = shouldLock && securityEnabled,
                    checkAuthOnStart = false, // Don't check auth again, we already know the state
                    debugSessionActive = true // Always active for testing
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinvestaApp(
    initialLockState: Boolean = false,
    checkAuthOnStart: Boolean = true,
    debugSessionActive: Boolean = true // Debug flag to simulate active session
) {
    // App state
    var appState by remember { mutableStateOf(AppState.LOADING) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var isAppLocked by remember { mutableStateOf(initialLockState) }
    var navigationStack by remember { mutableStateOf(listOf("home")) }
    var selectedBottomTab by remember { mutableStateOf(0) }
    
    val currentScreen = navigationStack.lastOrNull() ?: "home"
    
    // ViewModels and managers
    val securityViewModel: SecurityViewModel = hiltViewModel()
    val securityState by securityViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    // Get auth state manager
    val context = LocalContext.current
    val authStateManager = remember {
        (context as? MainActivity)?.authStateManager
    }
    
    // Authentication status - always use actual security status
    val authStatusFlow = remember { 
        if (context is MainActivity) {
            // Production flow: Get from AuthStateManager
            android.util.Log.d("FinvestaApp", "Using actual security status from AuthStateManager")
            authStateManager?.getAuthStatus() ?: flowOf(AuthStatus())
        } else {
            // Debug implementation for previews only
            // Check if security is enabled based on securityViewModel state
            val securityEnabled = securityState.fingerprintEnabled || securityState.pinEnabled || initialLockState
            android.util.Log.d("FinvestaApp", "Preview security status: fingerprint=${securityState.fingerprintEnabled}, pin=${securityState.pinEnabled}, initialLock=$initialLockState")
            flowOf(AuthStatus(
                hasActiveSession = debugSessionActive,
                securityEnabled = securityEnabled
            ))
        }
    }
    val authStatus by authStatusFlow.collectAsStateWithLifecycle(initialValue = AuthStatus())
    
    // Check authentication on start
    LaunchedEffect(Unit) {
        if (checkAuthOnStart) {
            // Simulate loading for better UX
            delay(1000)
            
            // Log authentication status for debugging
            android.util.Log.d("FinvestaApp", "Auth status: hasSession=${authStatus.hasActiveSession}, securityEnabled=${authStatus.securityEnabled}, securityState={pin=${securityState.pinEnabled}, fingerprint=${securityState.fingerprintEnabled}}")
            
            // Determine app state based on auth status
            val securityEnabled = securityState.fingerprintEnabled || securityState.pinEnabled || initialLockState
            android.util.Log.d("FinvestaApp", "Calculated securityEnabled=$securityEnabled")
            
            val newState = when {
                authStatus.hasActiveSession && securityEnabled -> {
                    // Has session and security enabled - always show security check
                    isAppLocked = true
                    AppState.SECURITY_CHECK
                }
                authStatus.hasActiveSession && !securityEnabled -> {
                    // Has session but no security
                    isAuthenticated = true
                    AppState.AUTHENTICATED
                }
                else -> AppState.LOGIN
            }
            
            android.util.Log.d("FinvestaApp", "Transitioning to state: $newState")
            appState = newState
        } else {
            // Skip loading if we already know the state
            val securityEnabled = securityState.fingerprintEnabled || securityState.pinEnabled || initialLockState
            android.util.Log.d("FinvestaApp", "Direct state transition: initialLock=$initialLockState, security={pin=${securityState.pinEnabled}, fingerprint=${securityState.fingerprintEnabled}}")
            
            val newState = when {
                authStatus.hasActiveSession && securityEnabled -> {
                    // Has session and security enabled
                    isAppLocked = true
                    AppState.SECURITY_CHECK
                }
                authStatus.hasActiveSession && !securityEnabled -> {
                    // Has session but no security
                    isAuthenticated = true
                    AppState.AUTHENTICATED
                }
                isAppLocked -> AppState.SECURITY_CHECK
                isAuthenticated -> AppState.AUTHENTICATED
                else -> AppState.LOGIN
            }
            
            android.util.Log.d("FinvestaApp", "Direct state transition to: $newState")
            appState = newState
        }
    }
    
    // Navigation functions
    fun navigateTo(screen: String) {
        navigationStack = navigationStack + screen
    }
    
    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
            // Update selected tab when navigating back to home
            if (navigationStack.last() == "home") {
                selectedBottomTab = 0
            }
        }
    }
    
    fun navigateToTab(screen: String, tabIndex: Int) {
        // For tab navigation, replace the current screen instead of adding to stack
        if (navigationStack.size > 1) {
            navigationStack = listOf("home", screen)
        } else {
            navigationStack = listOf(screen)
        }
        selectedBottomTab = tabIndex
    }
    
    // Handle back button press automatically
    BackHandler(enabled = navigationStack.size > 1 && isAuthenticated) {
        navigateBack()
    }
    
    // Render the appropriate screen based on app state
    when (appState) {
        AppState.LOADING -> {
            LoadingScreen()
        }
        
        AppState.SECURITY_CHECK -> {
            AppLockScreen(
                onUnlocked = { 
                    isAppLocked = false
                    isAuthenticated = true 
                    // Show loading screen after authentication before going to main content
                    appState = AppState.POST_AUTH_LOADING
                }
            )
        }
        
        AppState.POST_AUTH_LOADING -> {
            LoadingScreen()
            
            // Simulate loading data after authentication
            LaunchedEffect(Unit) {
                // Add artificial delay to show loading screen (adjust time as needed)
                delay(1500)
                
                // Then transition to authenticated state
                appState = AppState.AUTHENTICATED
                android.util.Log.d("MainActivity", "Post-auth loading complete, transitioning to AUTHENTICATED")
            }
        }
        
        AppState.AUTHENTICATED -> {
            // User is authenticated, show main app content
            Scaffold(
                bottomBar = {
                    FinvestaBottomBar(
                        selectedTab = selectedBottomTab,
                        onTabSelected = { selectedBottomTab = it },
                        onAddClick = {
                            navigateTo("add_transaction")
                        },
                        onHomeClick = {
                            navigateToTab("home", 0)
                        },
                        onReportsClick = {
                            navigateToTab("reports", 1)
                        },
                        onBillsClick = {
                            navigateToTab("bills", 2)
                        },
                        onProfileClick = {
                            navigateToTab("profile", 3)
                        }
                    )
                }
            ) { innerPadding ->
                when {
                    currentScreen == "home" -> {
                        DashboardScreen(
                            modifier = Modifier.padding(innerPadding),
                            onAddTransactionClick = {
                                navigateTo("add_transaction")
                            },
                            onSetBudgetClick = {
                                navigateTo("budget")
                            }
                        )
                    }
                    currentScreen == "add_transaction" -> {
                        AddTransactionScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onSaveTransaction = {
                                navigateBack()
                            }
                        )
                    }
                    currentScreen == "reports" -> {
                        ReportsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onExportClick = {
                                navigateTo("export_reports")
                            }
                        )
                    }
                    currentScreen == "export_reports" -> {
                        ExportReportsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onExportClick = {
                                // Handle actual export logic here
                            }
                        )
                    }
                    currentScreen == "bills" -> {
                        BillsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onAddBillClick = {
                                navigateTo("add_bill")
                            },
                            onEditBillClick = { billId ->
                                // Store the billId and navigate to edit screen
                                navigateTo("edit_bill/$billId")
                            }
                        )
                    }
                    currentScreen == "add_bill" -> {
                        AddBillScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onCancelClick = {
                                navigateBack()
                            }
                        )
                    }
                    currentScreen.startsWith("edit_bill/") -> {
                        val billId = currentScreen.substring("edit_bill/".length)
                        EditBillScreen(
                            modifier = Modifier.padding(innerPadding),
                            billId = billId,
                            onBackClick = {
                                navigateBack()
                            },
                            onCancelClick = {
                                navigateBack()
                            },
                            onDeleteClick = {
                                navigateBack() // Return to bills screen after deletion
                            }
                        )
                    }
                    currentScreen == "profile" -> {
                        ProfileScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onEditProfileClick = {
                                navigateTo("edit_profile")
                            },
                            onSecuritySettingsClick = {
                                navigateTo("security_settings")
                            },
                            onNotificationsClick = {
                                // Handle notifications click
                            },
                            onUpgradeToPremiumClick = {
                                navigateTo("upgrade_premium")
                            },
                            onExportDataClick = {
                                navigateTo("export_reports")
                            },
                            onSignOutClick = {
                                scope.launch {
                                    // Clear session data
                                    if (authStateManager != null) {
                                        authStateManager.setSessionActive(false)
                                    }
                                    // Reset app state
                                    isAuthenticated = false
                                    navigationStack = listOf("home")
                                    selectedBottomTab = 0
                                    appState = AppState.LOGIN
                                }
                            }
                        )
                    }
                    currentScreen == "security_settings" -> {
                        SecuritySettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            }
                        )
                    }
                    currentScreen == "upgrade_premium" -> {
                        UpgradeToPremiumScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onUpgradeClick = {
                                // Handle upgrade click
                            }
                        )
                    }
                    currentScreen == "edit_profile" -> {
                        EditProfileScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onSaveClick = { username, email ->
                                navigateBack()
                            }
                        )
                    }
                    currentScreen == "budget" -> {
                        BudgetScreen(
                            modifier = Modifier.padding(innerPadding),
                            onBackClick = {
                                navigateBack()
                            },
                            onStartBudgeting = {
                                navigateTo("budget_setup")
                            },
                            onViewReports = {
                                navigateTo("reports")
                            }
                        )
                    }
                    currentScreen == "budget_setup" -> {
                        BudgetSetupScreen(
                            onBackClick = { navigateBack() }
                        )
                    }
                    else -> {
                        // Fallback for any unhandled screens
                        Text(
                            text = "Screen not found",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        Scaffold(
            bottomBar = {
                FinvestaBottomBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { selectedBottomTab = it },
                    onAddClick = {
                        navigateTo("add_transaction")
                    },
                    onHomeClick = {
                        navigateToTab("home", 0)
                    },
                    onReportsClick = {
                        navigateToTab("reports", 1)
                    },
                    onBillsClick = {
                        navigateToTab("bills", 2)
                    },
                    onProfileClick = {
                        navigateToTab("profile", 3)
                    }
                )
            }
        ) { innerPadding ->
            when {
                currentScreen == "home" -> {
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        onAddTransactionClick = {
                            navigateTo("add_transaction")
                        },
                        onSetBudgetClick = {
                            navigateTo("budget")
                        }
                    )
                }
                currentScreen == "add_transaction" -> {
                    AddTransactionScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onSaveTransaction = {
                            navigateBack()
                        }
                    )
                }
                currentScreen == "reports" -> {
                    ReportsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onExportClick = {
                            navigateTo("export_reports")
                        }
                    )
                }
                currentScreen == "export_reports" -> {
                    ExportReportsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onExportClick = {
                            // Handle actual export logic here
                        }
                    )
                }
                currentScreen == "bills" -> {
                    BillsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onAddBillClick = {
                            navigateTo("add_bill")
                        },
                        onEditBillClick = { billId ->
                            // Store the billId and navigate to edit screen
                            navigateTo("edit_bill/$billId")
                        }
                    )
                }
                currentScreen == "add_bill" -> {
                    AddBillScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onCancelClick = {
                            navigateBack()
                        }
                    )
                }
                currentScreen.startsWith("edit_bill/") -> {
                    val billId = currentScreen.substring("edit_bill/".length)
                    EditBillScreen(
                        modifier = Modifier.padding(innerPadding),
                        billId = billId,
                        onBackClick = {
                            navigateBack()
                        },
                        onCancelClick = {
                            navigateBack()
                        },
                        onDeleteClick = {
                            navigateBack() // Return to bills screen after deletion
                        }
                    )
                }
                currentScreen == "profile" -> {
                    ProfileScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onEditProfileClick = {
                            navigateTo("edit_profile")
                        },
                        onSecuritySettingsClick = {
                            navigateTo("security_settings")
                        },
                        onNotificationsClick = {
                            // Handle notifications click
                        },
                        onUpgradeToPremiumClick = {
                            navigateTo("upgrade_premium")
                        },
                        onExportDataClick = {
                            navigateTo("export_reports")
                        },
                        onSignOutClick = {
                            scope.launch {
                                // Clear session data
                                authStateManager?.setSessionActive(false)
                                // Reset app state
                                isAuthenticated = false
                                navigationStack = listOf("home")
                                selectedBottomTab = 0
                                appState = AppState.LOGIN
                            }
                        }
                    )
                }
                currentScreen == "security_settings" -> {
                    SecuritySettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        }
                    )
                }
                currentScreen == "upgrade_premium" -> {
                    UpgradeToPremiumScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onUpgradeClick = {
                            // Handle upgrade click
                        }
                    )
                }
                currentScreen == "edit_profile" -> {
                    EditProfileScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onSaveClick = { username, email ->
                            navigateBack()
                        }
                    )
                }
                currentScreen == "budget" -> {
                    BudgetScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onStartBudgeting = {
                            navigateTo("budget_setup")
                        },
                        onViewReports = {
                            navigateTo("reports")
                        }
                    )
                }
                currentScreen == "budget_setup" -> {
                    BudgetSetupScreen(
                        onBackClick = { navigateBack() }
                    )
                }
                else -> {
                    // Fallback for any unhandled screens
                    Text(
                        text = "Screen not found",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        }
        
        AppState.LOGIN -> {
            AuthNavigation(
                onAuthSuccess = {
                    // Update session state in the AuthStateManager
                    scope.launch {
                        authStateManager?.setSessionActive(true)
                        
                        // Check if we need to lock the app or go directly to content
                        if (securityViewModel.isSecurityEnabled()) {
                            appState = AppState.SECURITY_CHECK
                            isAppLocked = true
                        } else {
                            appState = AppState.AUTHENTICATED
                            isAuthenticated = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Enum representing the possible states of the app
 */
enum class AppState {
    LOADING,
    LOGIN,
    SECURITY_CHECK,
    POST_AUTH_LOADING,
    AUTHENTICATED
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun GreetingPreview() {
    VestaTheme {
        Greeting("Android")
    }
}