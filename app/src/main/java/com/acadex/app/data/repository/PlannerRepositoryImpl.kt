package com.acadex.app.data.repository

import com.acadex.app.data.remote.PlannerTaskDto
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.domain.model.PlannerTask
import com.acadex.app.domain.model.TaskType
import com.acadex.app.domain.repository.PlannerRepository
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : PlannerRepository {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
    }

    override fun getTasksFlow(date: Long): Flow<List<PlannerTask>> = flow {
        while (true) {
            val token = sessionManager.getAccessToken()
            val userId = sessionManager.getUserId()
            if (token != null && userId != null) {
                runCatching {
                    supabaseApiService.getPlannerTasks(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                }.onSuccess { dtos ->
                    // Filter by the epoch-day (matching stored date field)
                    val filtered = dtos.filter { it.date == date }.map { it.toDomain() }
                    emit(filtered)
                }.onFailure {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
            delay(30_000)
        }
    }

    override fun getAllTasksFlow(): Flow<List<PlannerTask>> = flow {
        while (true) {
            val token = sessionManager.getAccessToken()
            val userId = sessionManager.getUserId()
            if (token != null && userId != null) {
                runCatching {
                    supabaseApiService.getPlannerTasks(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                }.onSuccess { dtos ->
                    emit(dtos.map { it.toDomain() })
                }.onFailure {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
            delay(30_000)
        }
    }

    override suspend fun getTaskById(id: String): PlannerTask? {
        val token = sessionManager.getAccessToken() ?: return null
        val userId = sessionManager.getUserId() ?: return null
        return runCatching {
            supabaseApiService.getPlannerTasks(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                .firstOrNull { it.id == id }
                ?.toDomain()
        }.getOrNull()
    }

    override suspend fun createTask(task: PlannerTask): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val id = task.id.ifEmpty { UUID.randomUUID().toString() }

        val dto = PlannerTaskDto(
            id = id,
            userId = userId,
            title = task.title,
            description = task.description,
            date = task.dueDate,
            completed = task.completed
        )
        supabaseApiService.createPlannerTask(SUPABASE_API_KEY, "Bearer $token", dto)
    }

    override suspend fun updateTask(task: PlannerTask): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val updates = mapOf<String, Any>(
            "title" to task.title,
            "description" to task.description,
            "date" to task.dueDate,
            "completed" to task.completed
        )
        supabaseApiService.updatePlannerTask(SUPABASE_API_KEY, "Bearer $token", "eq.${task.id}", updates)
    }

    override suspend fun deleteTask(id: String): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        supabaseApiService.deletePlannerTask(SUPABASE_API_KEY, "Bearer $token", "eq.$id")
    }

    private fun PlannerTaskDto.toDomain() = PlannerTask(
        id = id ?: "",
        title = title,
        description = description,
        dueDate = date,
        startTime = "",
        endTime = "",
        type = TaskType.TASK,
        completed = completed
    )
}
