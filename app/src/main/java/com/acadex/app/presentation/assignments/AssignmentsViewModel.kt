package com.acadex.app.presentation.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.domain.model.Assignment
import com.acadex.app.domain.usecase.AssignmentUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssignmentsViewModel @Inject constructor(
    private val assignmentUseCases: AssignmentUseCases
) : ViewModel() {

    val assignmentsState: StateFlow<List<Assignment>> = assignmentUseCases.getAssignments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createAssignment(assignment: Assignment, onSuccess: () -> Unit) {
        viewModelScope.launch {
            assignmentUseCases.createAssignment(assignment)
                .onSuccess { onSuccess() }
        }
    }

    fun updateAssignment(assignment: Assignment, onSuccess: () -> Unit) {
        viewModelScope.launch {
            assignmentUseCases.updateAssignment(assignment)
                .onSuccess { onSuccess() }
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            assignmentUseCases.deleteAssignment(id)
        }
    }

    suspend fun getAssignmentById(id: String): Assignment? {
        return assignmentUseCases.getAssignmentById(id)
    }
}
