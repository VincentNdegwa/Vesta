package com.example.vesta

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vesta.data.sync.AccountSyncWorker
import com.example.vesta.data.sync.BudgetSyncWorker
import com.example.vesta.data.sync.CategorySyncWorker
import com.example.vesta.data.sync.TransactionSyncWorker
import com.example.vesta.ui.auth.AppLockScreen
import com.example.vesta.ui.auth.AuthNavigation
import com.example.vesta.ui.auth.viewmodel.AuthViewModel
import com.example.vesta.ui.bills.*
import com.example.vesta.ui.budget.*
import com.example.vesta.ui.components.FinvestaBottomBar
import com.example.vesta.ui.components.LoadingScreen
import com.example.vesta.ui.dashboard.DashboardScreen
import com.example.vesta.ui.profile.*
import com.example.vesta.ui.reports.*
import com.example.vesta.ui.security.viewmodel.SecurityViewModel
import com.example.vesta.ui.sync.SyncViewModel
import com.example.vesta.ui.transaction.AddTransactionScreen
import com.example.vesta.ui.theme.VestaTheme
import com.example.vesta.utils.AppSecurityManager
import com.example.vesta.utils.AuthStateManager
import com.example.vesta.utils.AuthStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var appSecurityManager: AppSecurityManager
    @Inject lateinit var authStateManager: AuthStateManager

    private var backgroundTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val securityEnabled = runBlocking { appSecurityManager.isSecurityEnabledSync() }
        setContent {
            val securityViewModel: SecurityViewModel = hiltViewModel()
            val securityUiState by securityViewModel.uiState.collectAsStateWithLifecycle()
            
            VestaTheme(darkTheme = securityUiState.isDarkMode) {
                FinvestaApp(
                    initialLockState = securityEnabled,
                    authStateManager = authStateManager,
                    appSecurityManager = appSecurityManager
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        backgroundTime = SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        val timeInBackground = if (backgroundTime > 0) {
            SystemClock.elapsedRealtime() - backgroundTime
        } else 0
        backgroundTime = 0

        val securityEnabled = runBlocking { appSecurityManager.isSecurityEnabledSync() }
        val shouldLock = runBlocking {
            appSecurityManager.shouldRequireAuthAfterBackground(timeInBackground).first()
        }

        setContent {
            val securityViewModel: SecurityViewModel = hiltViewModel()
            val securityUiState by securityViewModel.uiState.collectAsStateWithLifecycle()
            
            VestaTheme(darkTheme = securityUiState.isDarkMode) {
                FinvestaApp(
                    initialLockState = shouldLock && securityEnabled,
                    authStateManager = authStateManager,
                    appSecurityManager = appSecurityManager
                )
            }
        }
    }
}

@Composable
fun FinvestaApp(
    initialLockState: Boolean,
    authStateManager: AuthStateManager,
    appSecurityManager: AppSecurityManager,
    authViewModel: AuthViewModel = hiltViewModel(),
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    var appState by remember { mutableStateOf(AppState.LOADING) }
    var isAppLocked by remember { mutableStateOf(initialLockState) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var navStack by remember { mutableStateOf(listOf("home")) }
    var selectedTab by remember { mutableStateOf(0) }
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val currentScreen = navStack.lastOrNull() ?: "home"
    val scope = rememberCoroutineScope()
    val securityViewModel: SecurityViewModel = hiltViewModel()
    val securityState by securityViewModel.uiState.collectAsStateWithLifecycle()
    val authStatusFlow = authStateManager.getAuthStatus()
    val authStatus by authStatusFlow.collectAsStateWithLifecycle(initialValue = AuthStatus())

    // Navigation
    fun navigateTo(screen: String) { navStack = navStack + screen }
    fun navigateBack() {
        if (navStack.size > 1) navStack = navStack.dropLast(1)
        if (navStack.last() == "home") selectedTab = 0
    }
    fun navigateTab(screen: String, index: Int) {
        navStack = listOf(screen)
        selectedTab = index
    }
    BackHandler(enabled = navStack.size > 1 && isAuthenticated) { navigateBack() }

    authState.userId.let {
        SyncData(it,syncViewModel)
    }

    when (appState) {
        AppState.LOADING -> {
            LoadingScreen()
            LaunchedEffect(Unit) {
                delay(800)
                Log.d("MainActivityAuthState", "AuthStatus ${authStatus}")
                if (authStatus.hasActiveSession) {
                    if (securityState.fingerprintEnabled || securityState.pinEnabled || initialLockState) {
                        isAppLocked = true
                        appState = AppState.SECURITY_CHECK
                    } else {
                        isAuthenticated = true
                        appState = AppState.AUTHENTICATED
                    }
                } else {
                    appState = AppState.LOGIN
                }
            }
        }

        AppState.SECURITY_CHECK -> {
            AppLockScreen(onUnlocked = {
                isAppLocked = false
                isAuthenticated = true
                appState = AppState.AUTHENTICATED
            })
        }

        AppState.LOGIN -> {
            AuthNavigation(
                onAuthSuccess = {
                    scope.launch {
                        authStateManager.setSessionActive(true)
                        if (securityViewModel.isSecurityEnabled()) {
                            appState = AppState.SECURITY_CHECK
                            isAppLocked = true
                        } else {
                            isAuthenticated = true
                            appState = AppState.AUTHENTICATED
                        }
                        isAuthenticated = true
                        appState = AppState.AUTHENTICATED
                    }
                }
            )
        }

        AppState.AUTHENTICATED -> {
            Scaffold(
                bottomBar = {
                    FinvestaBottomBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onAddClick = { navigateTo("add_transaction") },
                        onHomeClick = { navigateTab("home", 0) },
                        onReportsClick = { navigateTab("reports", 1) },
                        onBudgetClick = { navigateTab("budget", 2) },
                        onProfileClick = { navigateTab("profile", 3) }
                    )
                }
            ) { padding ->
                when {
                    currentScreen == "home" -> DashboardScreen(
                        modifier = Modifier.padding(padding),
                        onAddTransactionClick = { navigateTo("add_transaction") },
                        onSetBudgetClick = { navigateTo("budget") }
                    )
                    currentScreen == "add_transaction" -> AddTransactionScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onSaveTransaction = { navigateBack() }
                    )
                    currentScreen == "reports" -> ReportsScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onExportClick = { navigateTo("export_reports") }
                    )
                    currentScreen == "export_reports" -> ExportReportsScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onExportClick = { }
                    )
                    currentScreen == "bills" -> BillsScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onAddBillClick = { navigateTo("add_bill") },
                        onEditBillClick = { id -> navigateTo("edit_bill/$id") }
                    )
                    currentScreen == "add_bill" -> AddBillScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onCancelClick = { navigateBack() }
                    )
                    currentScreen.startsWith("edit_bill/") -> EditBillScreen(
                        modifier = Modifier.padding(padding),
                        billId = currentScreen.removePrefix("edit_bill/"),
                        onBackClick = { navigateBack() },
                        onCancelClick = { navigateBack() },
                        onDeleteClick = { navigateBack() }
                    )
                    currentScreen == "profile" -> ProfileScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onEditProfileClick = { navigateTo("edit_profile") },
                        onSecuritySettingsClick = { navigateTo("security_settings") },
                        onNotificationsClick = { },
                        onUpgradeToPremiumClick = { navigateTo("upgrade_premium") },
                        onExportDataClick = { navigateTo("export_reports") },
                        onSignOutClick = {
                            scope.launch {
                                authStateManager.setSessionActive(false)
                                isAuthenticated = false
                                navStack = listOf("home")
                                selectedTab = 0
                                appState = AppState.LOGIN
                            }
                        }
                    )
                    currentScreen == "security_settings" -> SecuritySettingsScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() }
                    )
                    currentScreen == "upgrade_premium" -> UpgradeToPremiumScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() }
                    )
                    currentScreen == "edit_profile" -> EditProfileScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onSaveClick = { _, _ -> navigateBack() }
                    )
                    currentScreen == "budget" -> BudgetScreen(
                        modifier = Modifier.padding(padding),
                        onBackClick = { navigateBack() },
                        onStartBudgeting = { navigateTo("budget_setup") },
                        onViewReports = { navigateTo("reports") }
                    )
                    currentScreen == "budget_setup" -> BudgetSetupScreen(
                        onBackClick = { navigateBack() }
                    )
                    else -> Text("Screen not found", modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

fun SyncData(userId: String? = null, syncViewModel: SyncViewModel) {
    Log.d("SyncData", "SyncData called")
        userId?.let {
            syncViewModel.sync<TransactionSyncWorker>(
                process = "DOWNLOAD",
                userId = it,
                uniqueName = "sync_transactions_download"
            )
            syncViewModel.sync<AccountSyncWorker>(
                process = "DOWNLOAD",
                userId = it,
                uniqueName = "sync_account_download"
            )
            syncViewModel.sync<CategorySyncWorker>(
                process = "DOWNLOAD",
                userId = it,
                uniqueName = "sync_category_download"
            )

            syncViewModel.sync<BudgetSyncWorker>(
                process = "DOWNLOAD",
                userId = it,
                uniqueName = "sync_budget_download"
            )

            // Uploads
            syncViewModel.sync<TransactionSyncWorker>(
                process = "UPLOAD",
                userId = it,
                uniqueName = "sync_transactions_upload"
            )
            syncViewModel.sync<AccountSyncWorker>(
                process = "UPLOAD",
                userId = it,
                uniqueName = "sync_account_upload"
            )
            syncViewModel.sync<CategorySyncWorker>(
                process = "UPLOAD",
                userId = it,
                uniqueName = "sync_category_upload"
            )
            syncViewModel.sync<BudgetSyncWorker>(
                process = "UPLOAD",
                userId = it,
                uniqueName = "sync_budget_upload"
            )
        }

}

enum class AppState {
    LOADING, LOGIN, SECURITY_CHECK, AUTHENTICATED
}
