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
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthUseCases,
    authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
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
        registerNumber: String,
        department: String,
        semester: String
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || registerNumber.isBlank() || department.isBlank() || semester.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
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
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authUseCases.resetPassword(email)
                .onSuccess {
                    _authState.value = AuthState.Idle
                    // Show confirmation in UI or trigger notification
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Password reset failed")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authUseCases.logout()
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
