package com.acadex.app.domain.repository

import com.acadex.app.domain.model.Assignment
import kotlinx.coroutines.flow.Flow

interface AssignmentRepository {
    fun getAssignmentsFlow(): Flow<List<Assignment>>
    suspend fun getAssignmentById(id: String): Assignment?
    suspend fun createAssignment(assignment: Assignment): Result<Unit>
    suspend fun updateAssignment(assignment: Assignment): Result<Unit>
    suspend fun deleteAssignment(id: String): Result<Unit>
}
