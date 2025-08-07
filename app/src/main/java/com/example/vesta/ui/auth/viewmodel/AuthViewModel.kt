package com.example.vesta.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vesta.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val userId: String? = null,
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val passwordResetEmailSent: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // Check if user is already logged in and load their data
        // Check if user is already logged in and load their data
        checkAuthState()
        
        // Also observe auth state changes
        observeAuthState()
        
        // Observe auth state changes from preferences
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { isLoggedIn ->
                _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
            }
        }
        
        viewModelScope.launch {
            authRepository.currentUserId.collect { userId ->
                _uiState.update { it.copy(userId = userId) }
            }
        }
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { firebaseUser ->
                if (firebaseUser != null) {
                    // User is signed in, load their data
                    loadUserDataFromFirebaseUser(firebaseUser)
                } else {
                    // User is signed out, clear the state
                    _uiState.update { 
                        AuthUiState() // Reset to initial state
                    }
                }
            }
        }
    }
    
    private fun loadUserDataFromFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        _uiState.update { 
            it.copy(
                isLoggedIn = true,
                userEmail = firebaseUser.email,
                userDisplayName = firebaseUser.displayName,
                userId = firebaseUser.uid
            )
        }
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                val result = authRepository.getCurrentUser()
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        _uiState.update { 
                            it.copy(
                                isLoggedIn = true,
                                userEmail = user.email,
                                userDisplayName = user.displayName,
                                userId = user.uid
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // If there's an error checking auth state, just continue with default state
                // The user can still sign in manually
            }
        }
    }
    
    init {
        // Observe auth state changes
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { isLoggedIn ->
                _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
            }
        }
        
        viewModelScope.launch {
            authRepository.currentUserId.collect { userId ->
                _uiState.update { it.copy(userId = userId) }
            }
        }
    }
    
    fun signUp( username:String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val displayName = username.trim()
            val result = authRepository.signUp(email, password, displayName)
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userEmail = email,
                        userDisplayName = displayName
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Sign up failed"
                    )
                }
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = authRepository.signIn(email, password)
            
            if (result.isSuccess) {
                // Load user data after successful sign in
                loadUserData()
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Sign in failed"
                    )
                }
            }
        }
    }
    
    private suspend fun loadUserData() {
        try {
            val result = authRepository.getCurrentUser()
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userEmail = user?.email,
                        userDisplayName = user?.displayName,
                        userId = user?.uid
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true, // Still logged in, just couldn't load user data
                        error = "Failed to load user data"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isLoggedIn = true, // Still logged in, just couldn't load user data
                    error = "Failed to load user data: ${e.message}"
                )
            }
        }
    }
    
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, passwordResetEmailSent = false) }
            
            val result = authRepository.sendPasswordResetEmail(email)
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    passwordResetEmailSent = result.isSuccess,
                    error = if (result.isFailure) {
                        result.exceptionOrNull()?.message ?: "Failed to send reset email"
                    } else null
                )
            }
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { 
                AuthUiState() // Reset to initial state
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun updateProfile(displayName: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val result = authRepository.updateProfile(displayName, null)
                
                if (result.isSuccess) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            userDisplayName = displayName,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to update profile"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to update profile: ${e.message}"
                    )
                }
            }
        }
    }
}
