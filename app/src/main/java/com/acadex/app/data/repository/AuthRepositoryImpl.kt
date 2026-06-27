package com.acadex.app.data.repository

import com.acadex.app.data.remote.*
import com.acadex.app.domain.model.User
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.utils.SessionManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
                    val authUser = withTimeoutOrNull(6000) {
                        supabaseApiService.getCurrentUser(SUPABASE_API_KEY, "Bearer $savedToken")
                    }
                    if (authUser != null) {
                        // Fetch details from the public.users database table
                        val profileList = withTimeoutOrNull(4000) {
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
                        // Token invalid/expired or timeout occurred
                        throw java.util.concurrent.TimeoutException("Session check timed out or returned null")
                    }
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Session verification failed during initialization", e)
                    // If it is a credential error (HttpException 400 or 401), clear the session and log out
                    if (e is retrofit2.HttpException && (e.code() == 400 || e.code() == 401)) {
                        Log.w("AuthRepository", "Invalid session credentials, clearing session")
                        sessionManager.clearSession()
                        _currentUser.value = null
                    } else {
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
                    }
                } finally {
                    _isInitialized.value = true
                }
            }
        } else {
            _currentUser.value = null
            _isInitialized.value = true
        }
    }

    private fun parseSupabaseError(throwable: Throwable): String {
        if (throwable is retrofit2.HttpException) {
            try {
                val errorBody = throwable.response()?.errorBody()?.string()
                if (!errorBody.isNullOrEmpty()) {
                    val json = com.google.gson.Gson().fromJson(errorBody, com.google.gson.JsonObject::class.java)
                    if (json.has("error_description")) {
                        return json.get("error_description").asString
                    } else if (json.has("msg")) {
                        return json.get("msg").asString
                    } else if (json.has("message")) {
                        return json.get("message").asString
                    } else if (json.has("error")) {
                        return json.get("error").asString
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to parse Supabase error body", e)
            }
            return when (throwable.code()) {
                400 -> "Invalid credentials or request parameters"
                401 -> "Unauthorized session"
                404 -> "User or resource not found"
                else -> "HTTP ${throwable.code()}: ${throwable.message()}"
            }
        }
        return throwable.localizedMessage ?: "Unknown error occurred"
    }

    override suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Starting login request for email: $email")
            val authResponse = supabaseApiService.signIn(SUPABASE_API_KEY, SignInRequest(email, password))
            val token = authResponse.accessToken ?: throw Exception("Auth failed: Access token is null")
            val authUser = authResponse.user
            Log.d("AuthRepository", "Login response received: user ID = ${authUser.id}")
            
            // Fetch database profile
            Log.d("AuthRepository", "Fetching database profile for user ${authUser.id}")
            val profileList = runCatching {
                supabaseApiService.getUserProfile(SUPABASE_API_KEY, "Bearer $token", "eq.${authUser.id}")
            }.onFailure { e ->
                Log.e("AuthRepository", "Failed to fetch database profile", e)
            }.getOrNull()
            
            val profile = profileList?.firstOrNull()
            if (profile != null) {
                Log.d("AuthRepository", "Database profile found: name=${profile.name}")
            } else {
                Log.w("AuthRepository", "No database profile found for user")
            }
            
            val name = profile?.name ?: authUser.userMetadata?.get("name") ?: email.substringBefore("@")
            
            // Persist session locally
            Log.d("AuthRepository", "Saving session to SessionManager")
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
            
            Log.d("AuthRepository", "Setting _currentUser on login success")
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            val cleanMessage = parseSupabaseError(e)
            Log.e("AuthRepository", "Login failed: $cleanMessage", e)
            Result.failure(Exception(cleanMessage, e))
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        registerNumber: String,
        department: String,
        semester: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Starting registration for email: $email")
            withTimeout(15000) {
                val userMetadata = mapOf(
                    "name" to name,
                    "register_number" to registerNumber,
                    "department" to department,
                    "semester" to semester
                )
                
                Log.d("AuthRepository", "Sending sign up request to Supabase with redirect_to=acadex://auth/callback")
                val signUpResponse = supabaseApiService.signUp(
                    apiKey = SUPABASE_API_KEY,
                    redirectTo = "acadex://auth/callback",
                    body = SignUpRequest(email, password, userMetadata)
                )
                
                Log.d("AuthRepository", "Supabase sign up response received: user ID = ${signUpResponse.user?.id}")
                val authUser = signUpResponse.user ?: throw Exception("Registration failed: User details not returned from Supabase")
                Log.d("AuthRepository", "User created successfully in Auth: ${authUser.id}")

                val accessToken = signUpResponse.accessToken
                if (accessToken == null) {
                    val infoMsg = "Registration successful. A verification email has been sent to $email. Please confirm your email to complete registration."
                    Log.i("AuthRepository", infoMsg)
                    throw Exception(infoMsg)
                }

                Log.d("AuthRepository", "Persisting session to SessionManager (Auto-confirmed)")
                sessionManager.saveSession(
                    accessToken = accessToken,
                    refreshToken = signUpResponse.refreshToken ?: "",
                    userId = authUser.id,
                    email = email,
                    name = name
                )
                
                val user = User(
                    uid = authUser.id,
                    name = name,
                    email = email,
                    registerNumber = registerNumber,
                    department = department,
                    semester = semester,
                    profilePicUrl = ""
                )
                
                Log.d("AuthRepository", "Profile created in memory, setting _currentUser")
                _currentUser.value = user
                Result.success(user)
            }
        } catch (e: Exception) {
            val cleanMessage = parseSupabaseError(e)
            Log.e("AuthRepository", "Registration failed: $cleanMessage", e)
            Result.failure(Exception(cleanMessage, e))
        }
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        Log.d("AuthRepository", "Logging out user: clearing session")
        sessionManager.clearSession()
        _currentUser.value = null
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Requesting password reset for email: $email with redirect_to=acadex://auth/callback")
            supabaseApiService.recoverPassword(
                apiKey = SUPABASE_API_KEY,
                redirectTo = "acadex://auth/callback",
                body = RecoverRequest(email)
            )
            Log.i("AuthRepository", "Password recovery request sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            val cleanMessage = parseSupabaseError(e)
            Log.e("AuthRepository", "Password reset request failed: $cleanMessage", e)
            Result.failure(Exception(cleanMessage, e))
        }
    }

    override suspend fun updateProfile(
        name: String,
        registerNumber: String,
        department: String,
        semester: String,
        profilePicUrl: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            Log.d("AuthRepository", "Updating user profile...")
            val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
            val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
            
            val updates = mapOf(
                "name" to name,
                "register_number" to registerNumber,
                "department" to department,
                "semester" to semester,
                "profile_image_url" to profilePicUrl
            )

            Log.d("AuthRepository", "Sending profile update request to Supabase...")
            supabaseApiService.updateUserProfile(SUPABASE_API_KEY, "Bearer $token", "eq.$userId", updates)
            Log.d("AuthRepository", "Profile updated successfully in remote database")

            val updatedUser = User(
                uid = userId,
                name = name,
                email = sessionManager.getUserEmail() ?: "",
                registerNumber = registerNumber,
                department = department,
                semester = semester,
                profilePicUrl = profilePicUrl
            )
            
            Log.d("AuthRepository", "Setting updated profile as _currentUser")
            _currentUser.value = updatedUser
            Result.success(updatedUser)
        } catch (e: Exception) {
            val cleanMessage = parseSupabaseError(e)
            Log.e("AuthRepository", "Profile update failed: $cleanMessage", e)
            Result.failure(Exception(cleanMessage, e))
        }
    }

    override suspend fun handleDeepLinkSession(accessToken: String, refreshToken: String): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d("AuthRepository", "Handling deep link session token exchange...")
            
            // 1. Fetch Auth user details
            val authUser = try {
                supabaseApiService.getCurrentUser(SUPABASE_API_KEY, "Bearer $accessToken")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to retrieve current user from Auth", e)
                throw e
            }
            Log.d("AuthRepository", "User details retrieved: ID = ${authUser.id}, Email = ${authUser.email}")
            
            // 2. Extract profile details from metadata or public database
            val name = authUser.userMetadata?.get("name") ?: authUser.email.substringBefore("@")
            val regNumber = authUser.userMetadata?.get("register_number") ?: ""
            val department = authUser.userMetadata?.get("department") ?: ""
            val semester = authUser.userMetadata?.get("semester") ?: ""
            
            // Fetch database profile
            Log.d("AuthRepository", "Fetching database profile for verification...")
            val profileList = runCatching {
                supabaseApiService.getUserProfile(SUPABASE_API_KEY, "Bearer $accessToken", "eq.${authUser.id}")
            }.getOrNull()
            val profile = profileList?.firstOrNull()
            
            if (profile != null) {
                Log.d("AuthRepository", "Verified profile in public.users: ${profile.name}")
            } else {
                Log.w("AuthRepository", "No database profile found for verified user. Initiating public.users fallback.")
            }
            
            // 3. Save to local SessionManager
            Log.d("AuthRepository", "Saving session to SessionManager from deep link")
            sessionManager.saveSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = authUser.id,
                email = authUser.email,
                name = profile?.name ?: name
            )
            
            val user = User(
                uid = authUser.id,
                name = profile?.name ?: name,
                email = authUser.email,
                registerNumber = profile?.registerNumber ?: regNumber,
                department = profile?.department ?: department,
                semester = profile?.semester ?: semester,
                profilePicUrl = profile?.profileImageUrl ?: ""
            )
            
            Log.d("AuthRepository", "Setting _currentUser on deep link authentication success")
            _currentUser.value = user
            user
        }.onFailure { error ->
            Log.e("AuthRepository", "Deep link authentication failed with exception", error)
        }
    }
}
