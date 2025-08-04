package com.example.vesta

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.vesta.ui.components.FinvestaBottomBar
import com.example.vesta.ui.dashboard.DashboardScreen
import com.example.vesta.ui.transaction.AddTransactionScreen
import com.example.vesta.ui.theme.VestaTheme

class MainActivity : ComponentActivity() {
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
            when (currentScreen) {
                "home" -> {
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
                "add_transaction" -> {
                    AddTransactionScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            navigateBack()
                        },
                        onSaveTransaction = { amount, type, category, date, note ->
                            navigateBack()
                        }
                    )
                }
                "reports" -> {
                    Text(
                        text = "Reports Screen - Coming Soon",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                "bills" -> {
                    Text(
                        text = "Bills Screen - Coming Soon", 
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                "profile" -> {
                    Text(
                        text = "Profile Screen - Coming Soon",
                        modifier = Modifier.padding(innerPadding)  
                    )
                }
                "budget" -> {
                    Text(
                        text = "Budget Screen - Coming Soon",
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VestaTheme {
        Greeting("Android")
    }
}