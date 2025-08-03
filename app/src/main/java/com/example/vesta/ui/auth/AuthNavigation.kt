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
                onLoginClick = { email, password ->
                    // TODO: Implement login logic
                    // For now, just navigate to success
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
                onRegisterClick = { firstName, lastName, email, password ->
                    // TODO: Implement registration logic
                    // For now, just navigate to success
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
                },
                onResetClick = { email ->
                    // TODO: Implement password reset logic
                    currentRoute = AuthRoutes.LOGIN
                }
            )
        }
    }
}
