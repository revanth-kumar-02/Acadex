package com.acadex.app.domain.repository

import com.acadex.app.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String, registerNumber: String, department: String, semester: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun updateProfile(name: String, registerNumber: String, department: String, semester: String, profilePicUrl: String): Result<User>
}
