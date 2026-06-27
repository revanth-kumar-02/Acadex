package com.acadex.app.data.repositories

import com.acadex.app.data.models.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    val isInitialized: StateFlow<Boolean>
    
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String, registerNumber: String, department: String, semester: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(name: String, registerNumber: String, department: String, semester: String, profilePicUrl: String): Result<User>
    suspend fun handleDeepLinkSession(accessToken: String, refreshToken: String): Result<User>
}
