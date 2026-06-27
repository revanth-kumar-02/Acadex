package com.acadex.app.features.authentication

import com.acadex.app.data.models.User
import com.acadex.app.data.repositories.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        repository.login(email, password)
}

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        registerNumber: String,
        department: String,
        semester: String
    ): Result<User> =
        repository.register(name, email, password, registerNumber, department, semester)
}

class LogoutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.logout()
}

class ResetPasswordUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> = repository.resetPassword(email)
}

class UpdateProfileUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(
        name: String,
        registerNumber: String,
        department: String,
        semester: String,
        profilePicUrl: String
    ): Result<User> = repository.updateProfile(name, registerNumber, department, semester, profilePicUrl)
}

data class AuthUseCases @Inject constructor(
    val login: LoginUseCase,
    val register: RegisterUseCase,
    val logout: LogoutUseCase,
    val resetPassword: ResetPasswordUseCase,
    val updateProfile: UpdateProfileUseCase
)
