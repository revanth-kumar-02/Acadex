package com.acadex.app.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.domain.model.Assignment
import com.acadex.app.domain.model.User
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.domain.repository.AssignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val assignmentsState: StateFlow<List<Assignment>> = assignmentRepository.getAssignmentsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createAssignment(
        assignment: Assignment,
        attachmentStream: InputStream? = null,
        attachmentName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            assignmentRepository.createAssignment(assignment, attachmentStream, attachmentName)
                .onSuccess { onSuccess() }
                .onFailure { error -> onFailure(error.message ?: "Failed to create assignment") }
        }
    }

    fun updateAssignment(
        assignment: Assignment,
        attachmentStream: InputStream? = null,
        attachmentName: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            assignmentRepository.updateAssignment(assignment, attachmentStream, attachmentName)
                .onSuccess { onSuccess() }
                .onFailure { error -> onFailure(error.message ?: "Failed to update assignment") }
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            assignmentRepository.deleteAssignment(id)
        }
    }

    suspend fun getAssignmentById(id: String): Assignment? {
        return assignmentRepository.getAssignmentById(id)
    }
}
