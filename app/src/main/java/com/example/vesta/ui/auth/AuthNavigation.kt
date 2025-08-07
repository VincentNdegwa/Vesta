package com.example.vesta.ui.auth

import androidx.compose.runtime.*

/**
 * Authentication navigation routes
 */
object AuthRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
}

/**
 * Main authentication navigation composable
 * Simplified version without navigation-compose dependency
 */
@Composable
fun AuthNavigation(
    onAuthSuccess: () -> Unit = {}
) {
    var currentRoute by remember { mutableStateOf(AuthRoutes.LOGIN) }
    
    when (currentRoute) {
        AuthRoutes.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    onAuthSuccess()
                },
                onSignUpClick = {
                    currentRoute = AuthRoutes.REGISTER
                },
                onForgotPasswordClick = {
                    currentRoute = AuthRoutes.FORGOT_PASSWORD
                }
            )
        }
        
        AuthRoutes.REGISTER -> {
            RegisterScreen(
                onRegisterSuccess = {
                    onAuthSuccess()
                },
                onSignInClick = {
                    currentRoute = AuthRoutes.LOGIN
                },
                onBackClick = {
                    currentRoute = AuthRoutes.LOGIN
                }
            )
        }
        
        AuthRoutes.FORGOT_PASSWORD -> {
            ForgotPasswordScreen(
                onBackClick = {
                    currentRoute = AuthRoutes.LOGIN
                }
            )
        }
    }
}
