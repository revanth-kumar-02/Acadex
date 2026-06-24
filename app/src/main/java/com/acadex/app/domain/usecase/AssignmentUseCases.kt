package com.acadex.app.domain.usecase

import com.acadex.app.domain.model.Assignment
import com.acadex.app.domain.repository.AssignmentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAssignmentsUseCase @Inject constructor(private val repository: AssignmentRepository) {
    operator fun invoke(): Flow<List<Assignment>> = repository.getAssignmentsFlow()
}

class GetAssignmentByIdUseCase @Inject constructor(private val repository: AssignmentRepository) {
    suspend operator fun invoke(id: String): Assignment? = repository.getAssignmentById(id)
}

class CreateAssignmentUseCase @Inject constructor(private val repository: AssignmentRepository) {
    suspend operator fun invoke(assignment: Assignment): Result<Unit> = repository.createAssignment(assignment)
}

class UpdateAssignmentUseCase @Inject constructor(private val repository: AssignmentRepository) {
    suspend operator fun invoke(assignment: Assignment): Result<Unit> = repository.updateAssignment(assignment)
}

class DeleteAssignmentUseCase @Inject constructor(private val repository: AssignmentRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteAssignment(id)
}

data class AssignmentUseCases @Inject constructor(
    val getAssignments: GetAssignmentsUseCase,
    val getAssignmentById: GetAssignmentByIdUseCase,
    val createAssignment: CreateAssignmentUseCase,
    val updateAssignment: UpdateAssignmentUseCase,
    val deleteAssignment: DeleteAssignmentUseCase
)
