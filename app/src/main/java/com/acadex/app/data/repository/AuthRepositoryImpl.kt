package com.acadex.app.data.repository

import com.acadex.app.data.remote.FirebaseService
import com.acadex.app.domain.model.User
import com.acadex.app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseService: FirebaseService
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        firebaseService.auth.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                // Fetch details from Firestore in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val snapshot = firebaseService.firestore.collection("users")
                            .document(firebaseUser.uid)
                            .get()
                            .await()
                        if (snapshot.exists()) {
                            val user = User(
                                uid = firebaseUser.uid,
                                name = snapshot.getString("name") ?: "",
                                email = firebaseUser.email ?: "",
                                registerNumber = snapshot.getString("registerNumber") ?: "",
                                department = snapshot.getString("department") ?: "",
                                semester = snapshot.getString("semester") ?: "",
                                profilePicUrl = snapshot.getString("profilePicUrl") ?: ""
                            )
                            _currentUser.value = user
                        } else {
                            // Document doesn't exist yet, build placeholder
                            _currentUser.value = User(firebaseUser.uid, firebaseUser.displayName ?: "", firebaseUser.email ?: "")
                        }
                    } catch (e: Exception) {
                        _currentUser.value = User(firebaseUser.uid, firebaseUser.displayName ?: "", firebaseUser.email ?: "")
                    }
                }
            } else {
                _currentUser.value = null
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val result = firebaseService.auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Auth failed: User is null")
        
        val snapshot = firebaseService.firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()
            
        val user = User(
            uid = firebaseUser.uid,
            name = snapshot.getString("name") ?: firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: email,
            registerNumber = snapshot.getString("registerNumber") ?: "",
            department = snapshot.getString("department") ?: "",
            semester = snapshot.getString("semester") ?: "",
            profilePicUrl = snapshot.getString("profilePicUrl") ?: ""
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
        val result = firebaseService.auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Registration failed: User is null")
        
        val user = User(
            uid = firebaseUser.uid,
            name = name,
            email = email,
            registerNumber = registerNumber,
            department = department,
            semester = semester,
            profilePicUrl = ""
        )

        // Save to firestore
        val userMap = mapOf(
            "uid" to user.uid,
            "name" to user.name,
            "email" to user.email,
            "registerNumber" to user.registerNumber,
            "department" to user.department,
            "semester" to user.semester,
            "profilePicUrl" to user.profilePicUrl
        )
        
        firebaseService.firestore.collection("users")
            .document(firebaseUser.uid)
            .set(userMap)
            .await()

        _currentUser.value = user
        user
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        firebaseService.auth.signOut()
        _currentUser.value = null
    }

    override suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        firebaseService.auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateProfile(
        name: String,
        registerNumber: String,
        department: String,
        semester: String,
        profilePicUrl: String
    ): Result<User> = runCatching {
        val uid = firebaseService.currentUserId ?: throw Exception("User not authenticated")
        
        val updates = mapOf(
            "name" to name,
            "registerNumber" to registerNumber,
            "department" to department,
            "semester" to semester,
            "profilePicUrl" to profilePicUrl
        )

        firebaseService.firestore.collection("users")
            .document(uid)
            .update(updates)
            .await()

        val updatedUser = User(
            uid = uid,
            name = name,
            email = firebaseService.auth.currentUser?.email ?: "",
            registerNumber = registerNumber,
            department = department,
            semester = semester,
            profilePicUrl = profilePicUrl
        )
        _currentUser.value = updatedUser
        updatedUser
    }
}
