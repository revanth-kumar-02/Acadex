package com.acadex.app.data.repositories

import com.acadex.app.data.models.Assignment
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface AssignmentRepository {
    fun getAssignmentsFlow(): Flow<List<Assignment>>
    suspend fun getAssignmentById(id: String): Assignment?
    suspend fun createAssignment(assignment: Assignment, attachmentStream: InputStream? = null, attachmentName: String? = null): Result<Unit>
    suspend fun updateAssignment(assignment: Assignment, attachmentStream: InputStream? = null, attachmentName: String? = null): Result<Unit>
    suspend fun deleteAssignment(id: String): Result<Unit>
}
