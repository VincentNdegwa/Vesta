package com.example.vesta

import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.vesta.ui.auth.AuthNavigation
import com.example.vesta.ui.bills.AddBillScreen
import com.example.vesta.ui.bills.BillsScreen
import com.example.vesta.ui.bills.EditBillScreen
import com.example.vesta.ui.budget.BudgetScreen
import com.example.vesta.ui.budget.BudgetSetupScreen
import com.example.vesta.ui.components.FinvestaBottomBar
import com.example.vesta.ui.dashboard.DashboardScreen
import com.example.vesta.ui.profile.EditProfileScreen
import com.example.vesta.ui.profile.ProfileScreen
import com.example.vesta.ui.profile.SecuritySettingsScreen
import com.example.vesta.ui.profile.UpgradeToPremiumScreen
import com.example.vesta.ui.reports.ExportReportsScreen
import com.example.vesta.ui.reports.ReportsScreen
import com.example.vesta.ui.transaction.AddTransactionScreen
import com.example.vesta.ui.theme.VestaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VestaTheme {
                FinvestaApp()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FinvestaApp() {
    var isAuthenticated by remember { mutableStateOf(false) }
    var navigationStack by remember { mutableStateOf(listOf("home")) }
    var selectedBottomTab by remember { mutableStateOf(0) }
    
    val currentScreen = navigationStack.lastOrNull() ?: "home"
    
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
    
    if (isAuthenticated) {
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
                            isAuthenticated = false
                            navigationStack = listOf("home")
                            selectedBottomTab = 0
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
    } else {
        AuthNavigation(
            onAuthSuccess = {
                isAuthenticated = true
            }
        )
    }
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