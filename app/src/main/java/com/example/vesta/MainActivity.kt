package com.example.vesta

import android.os.Bundle
import androidx.activity.ComponentActivity
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
    var currentScreen by remember { mutableStateOf("home") }
    var selectedBottomTab by remember { mutableStateOf(0) }
    
    if (isAuthenticated) {
        Scaffold(
            bottomBar = {
                FinvestaBottomBar(
                    selectedTab = selectedBottomTab,
                    onTabSelected = { selectedBottomTab = it },
                    onAddClick = {
                        currentScreen = "add_transaction"
                    },
                    onHomeClick = {
                        currentScreen = "home"
                        selectedBottomTab = 0
                    },
                    onReportsClick = {
                        currentScreen = "reports"
                        selectedBottomTab = 1
                        // TODO: Navigate to reports screen
                    },
                    onBillsClick = {
                        currentScreen = "bills"
                        selectedBottomTab = 2
                        // TODO: Navigate to bills screen
                    },
                    onProfileClick = {
                        currentScreen = "profile"
                        selectedBottomTab = 3
                        // TODO: Navigate to profile screen
                    }
                )
            }
        ) { innerPadding ->
            when (currentScreen) {
                "home" -> {
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        onAddTransactionClick = {
                            currentScreen = "add_transaction"
                        },
                        onSetBudgetClick = {
                            // TODO: Navigate to budget screen
                        }
                    )
                }
                "add_transaction" -> {
                    AddTransactionScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = {
                            currentScreen = "home"
                            selectedBottomTab = 0
                        },
                        onSaveTransaction = { amount, type, category, date, note ->
                            // TODO: Save transaction to database
                            currentScreen = "home"
                            selectedBottomTab = 0
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