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
import com.example.vesta.ui.dashboard.DashboardScreen
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
    
    if (isAuthenticated) {
        DashboardScreen(
            onAddTransactionClick = {
                // TODO: Navigate to add transaction screen
            },
            onSetBudgetClick = {
                // TODO: Navigate to budget screen
            },
            onNavigateToReports = {
                // TODO: Navigate to reports screen
            },
            onNavigateToBills = {
                // TODO: Navigate to bills screen
            },
            onNavigateToProfile = {
                // TODO: Navigate to profile screen
            }
        )
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