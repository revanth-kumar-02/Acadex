package com.acadex.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.data.models.*
import com.acadex.app.data.repositories.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val user: User? = null,
    val totalNotes: Int = 0,
    val completedAssignments: Int = 0,
    val isUpdating: Boolean = false,
    val updateError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notesRepository: NotesRepository,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val _isUpdating = MutableStateFlow(false)
    private val _updateError = MutableStateFlow<String?>(null)

    val profileState: StateFlow<ProfileState> = combine(
        authRepository.currentUser,
        notesRepository.getNotesFlow(),
        assignmentRepository.getAssignmentsFlow(),
        _isUpdating,
        _updateError
    ) { user, notes, assignments, updating, error ->
        ProfileState(
            user = user,
            totalNotes = notes.size,
            completedAssignments = assignments.count { it.status == AssignmentStatus.COMPLETED },
            isUpdating = updating,
            updateError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileState()
    )

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun updateProfile(
        name: String,
        registerNumber: String,
        department: String,
        semester: String,
        profilePicUrl: String
    ) {
        viewModelScope.launch {
            _isUpdating.value = true
            _updateError.value = null
            authRepository.updateProfile(name, registerNumber, department, semester, profilePicUrl)
                .onFailure {
                    _updateError.value = it.localizedMessage ?: "Failed to update profile"
                }
            _isUpdating.value = false
        }
    }
}
