package com.acadex.app.data.repository

import com.acadex.app.data.remote.AssignmentDto
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.domain.model.Assignment
import com.acadex.app.domain.model.AssignmentPriority
import com.acadex.app.domain.model.AssignmentStatus
import com.acadex.app.domain.repository.AssignmentRepository
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : AssignmentRepository {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
    }

    override fun getAssignmentsFlow(): Flow<List<Assignment>> = flow {
        while (true) {
            val token = sessionManager.getAccessToken()
            val userId = sessionManager.getUserId()
            if (token != null && userId != null) {
                runCatching {
                    supabaseApiService.getAssignments(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                }.onSuccess { dtos ->
                    emit(dtos.map { it.toDomain() })
                }.onFailure {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
            delay(30_000) // poll every 30 seconds
        }
    }

    override suspend fun getAssignmentById(id: String): Assignment? {
        val token = sessionManager.getAccessToken() ?: return null
        val userId = sessionManager.getUserId() ?: return null
        return runCatching {
            supabaseApiService.getAssignments(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                .firstOrNull { it.id == id }
                ?.toDomain()
        }.getOrNull()
    }

    override suspend fun createAssignment(assignment: Assignment): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val id = assignment.id.ifEmpty { UUID.randomUUID().toString() }

        val dto = AssignmentDto(
            id = id,
            userId = userId,
            title = assignment.title,
            subject = assignment.subject,
            description = assignment.description,
            dueDate = assignment.dueDate,
            priority = assignment.priority.name.lowercase(),
            status = assignment.status.name.lowercase()
        )
        supabaseApiService.createAssignment(SUPABASE_API_KEY, "Bearer $token", dto)
    }

    override suspend fun updateAssignment(assignment: Assignment): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val updates = mapOf<String, Any>(
            "title" to assignment.title,
            "description" to assignment.description,
            "subject" to assignment.subject,
            "due_date" to assignment.dueDate,
            "priority" to assignment.priority.name.lowercase(),
            "status" to assignment.status.name.lowercase()
        )
        supabaseApiService.updateAssignment(SUPABASE_API_KEY, "Bearer $token", "eq.${assignment.id}", updates)
    }

    override suspend fun deleteAssignment(id: String): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        supabaseApiService.deleteAssignment(SUPABASE_API_KEY, "Bearer $token", "eq.$id")
    }

    private fun AssignmentDto.toDomain() = Assignment(
        id = id ?: "",
        title = title,
        description = description,
        subject = subject,
        dueDate = dueDate,
        priority = runCatching { AssignmentPriority.valueOf(priority.uppercase()) }.getOrDefault(AssignmentPriority.MEDIUM),
        status = runCatching { AssignmentStatus.valueOf(status.uppercase()) }.getOrDefault(AssignmentStatus.PENDING),
        createdAt = 0L
    )
}
