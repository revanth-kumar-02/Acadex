package com.acadex.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.domain.model.User
import com.acadex.app.domain.usecase.AuthUseCases
import com.acadex.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
    val isInitialized: StateFlow<Boolean> = authRepository.isInitialized

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

    init {
        viewModelScope.launch {
            combine(authRepository.currentUser, authRepository.isInitialized) { user, initialized ->
                if (!initialized) {
                    AuthState.Loading
                } else if (user != null) {
                    AuthState.Authenticated(user)
                } else {
                    // Only transition to Unauthenticated automatically if we aren't currently
                    // displaying a transient state like Loading or Error from a user action
                    val current = _authState.value
                    if (current is AuthState.Loading || current is AuthState.Error || current is AuthState.Success) {
                        current
                    } else {
                        AuthState.Unauthenticated
                    }
                }
            }.collect { state ->
                // Overwrite state if it resolves to Authenticated or Unauthenticated,
                // or if it was Idle and initial load is Loading.
                if (state is AuthState.Authenticated || state is AuthState.Unauthenticated || 
                    (state is AuthState.Loading && _authState.value is AuthState.Idle)) {
                    _authState.value = state
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(emailRegex)
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email address")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authUseCases.login(email, password)
                .onSuccess { user ->
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Login failed")
                }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        registerNumber: String,
        department: String,
        semester: String
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() || 
            registerNumber.isBlank() || department.isBlank() || semester.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email address")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authUseCases.register(name, email, password, registerNumber, department, semester)
                .onSuccess { user ->
                    _authState.value = AuthState.Success(user)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Registration failed")
                }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email address")
            return
        }
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email address")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authUseCases.resetPassword(email)
                .onSuccess {
                    _authState.value = AuthState.Idle
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Password reset failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authUseCases.logout().onSuccess {
                _authState.value = AuthState.Unauthenticated
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.localizedMessage ?: "Logout failed")
            }
        }
    }

    fun resetState() {
        // Reset back to resolved session status
        val user = currentUser.value
        _authState.value = if (user != null) AuthState.Authenticated(user) else AuthState.Unauthenticated
    }
}
