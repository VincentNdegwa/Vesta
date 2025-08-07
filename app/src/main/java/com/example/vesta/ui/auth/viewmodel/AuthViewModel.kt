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
    
    fun signUp(firstName: String, lastName: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val displayName = "$firstName $lastName".trim()
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
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                }
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
}
