package com.acadex.app.data.repository

import com.acadex.app.data.remote.*
import com.acadex.app.domain.model.User
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
    }

    init {
        // Auto-login session checks on app launch
        val savedToken = sessionManager.getAccessToken()
        val savedUserId = sessionManager.getUserId()
        
        if (savedToken != null && savedUserId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Verify saved session by fetching current user from Auth service
                    val authUser = withTimeoutOrNull(4000) {
                        supabaseApiService.getCurrentUser(SUPABASE_API_KEY, "Bearer $savedToken")
                    }
                    if (authUser != null) {
                        // Fetch details from the public.users database table
                        val profileList = withTimeoutOrNull(3000) {
                            supabaseApiService.getUserProfile(SUPABASE_API_KEY, "Bearer $savedToken", "eq.${authUser.id}")
                        }
                        val profile = profileList?.firstOrNull()
                        val user = User(
                            uid = authUser.id,
                            name = profile?.name ?: authUser.userMetadata?.get("name") ?: sessionManager.getUserName() ?: "",
                            email = authUser.email,
                            registerNumber = profile?.registerNumber ?: authUser.userMetadata?.get("register_number") ?: sessionManager.getUserEmail() ?: "",
                            department = profile?.department ?: authUser.userMetadata?.get("department") ?: "",
                            semester = profile?.semester ?: authUser.userMetadata?.get("semester") ?: "",
                            profilePicUrl = profile?.profileImageUrl ?: ""
                        )
                        _currentUser.value = user
                    } else {
                        // Token invalid/expired
                        sessionManager.clearSession()
                        _currentUser.value = null
                    }
                } catch (e: Exception) {
                    // Fallback to local SharedPreferences credentials if network fails to avoid blocking the user
                    val fallbackUser = User(
                        uid = savedUserId,
                        name = sessionManager.getUserName() ?: "",
                        email = sessionManager.getUserEmail() ?: "",
                        registerNumber = "",
                        department = "",
                        semester = "",
                        profilePicUrl = ""
                    )
                    _currentUser.value = fallbackUser
                } finally {
                    _isInitialized.value = true
                }
            }
        } else {
            _currentUser.value = null
            _isInitialized.value = true
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val authResponse = supabaseApiService.signIn(SUPABASE_API_KEY, SignInRequest(email, password))
        val token = authResponse.accessToken ?: throw Exception("Auth failed: Access token is null")
        val authUser = authResponse.user
        
        // Fetch database profile
        val profileList = runCatching {
            supabaseApiService.getUserProfile(SUPABASE_API_KEY, "Bearer $token", "eq.${authUser.id}")
        }.getOrNull()
        val profile = profileList?.firstOrNull()
        
        val name = profile?.name ?: authUser.userMetadata?.get("name") ?: email.substringBefore("@")
        
        // Persist session locally
        sessionManager.saveSession(
            accessToken = token,
            refreshToken = authResponse.refreshToken ?: "",
            userId = authUser.id,
            email = authUser.email,
            name = name
        )

        val user = User(
            uid = authUser.id,
            name = name,
            email = authUser.email,
            registerNumber = profile?.registerNumber ?: authUser.userMetadata?.get("register_number") ?: "",
            department = profile?.department ?: authUser.userMetadata?.get("department") ?: "",
            semester = profile?.semester ?: authUser.userMetadata?.get("semester") ?: "",
            profilePicUrl = profile?.profileImageUrl ?: ""
        )
        _currentUser.value = user
        user
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        registerNumber: String,
        department: String,
        semester: String
    ): Result<User> = runCatching {
        val userMetadata = mapOf(
            "name" to name,
            "register_number" to registerNumber,
            "department" to department,
            "semester" to semester
        )
        
        val signUpResponse = supabaseApiService.signUp(SUPABASE_API_KEY, SignUpRequest(email, password, userMetadata))
        val authUser = signUpResponse.user ?: throw Exception("Registration failed: User is null")
        
        // Wait briefly for trigger synchronization if auto-login token is not returned directly
        val token = sessionManager.getAccessToken() ?: ""
        
        val user = User(
            uid = authUser.id,
            name = name,
            email = email,
            registerNumber = registerNumber,
            department = department,
            semester = semester,
            profilePicUrl = ""
        )
        
        // If the registration immediately logs in the user, persist that session
        val accessToken = signUpResponse.accessToken
        if (accessToken != null) {
            sessionManager.saveSession(
                accessToken = accessToken,
                refreshToken = signUpResponse.refreshToken ?: "",
                userId = authUser.id,
                email = email,
                name = name
            )
        }

        _currentUser.value = user
        user
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        // Clear local preferences
        sessionManager.clearSession()
        _currentUser.value = null
    }

    override suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        supabaseApiService.recoverPassword(SUPABASE_API_KEY, RecoverRequest(email))
    }

    override suspend fun updateProfile(
        name: String,
        registerNumber: String,
        department: String,
        semester: String,
        profilePicUrl: String
    ): Result<User> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        
        val updates = mapOf(
            "name" to name,
            "register_number" to registerNumber,
            "department" to department,
            "semester" to semester,
            "profile_image_url" to profilePicUrl
        )

        supabaseApiService.updateUserProfile(SUPABASE_API_KEY, "Bearer $token", "eq.$userId", updates)

        val updatedUser = User(
            uid = userId,
            name = name,
            email = sessionManager.getUserEmail() ?: "",
            registerNumber = registerNumber,
            department = department,
            semester = semester,
            profilePicUrl = profilePicUrl
        )
        _currentUser.value = updatedUser
        updatedUser
    }
}
