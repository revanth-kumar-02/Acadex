package com.acadex.app.data.repositories

import android.util.Log
import com.acadex.app.data.models.*
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.core.auth.SessionManager
import com.acadex.app.core.constants.AppConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        private const val SUPABASE_API_KEY = AppConstants.SUPABASE_API_KEY
    }

    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private fun triggerRefresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    override fun getTasksFlow(date: Long): Flow<List<PlannerTask>> = flow {
        var lastFetchedTime = 0L
        while (true) {
            val triggerTime = refreshTrigger.value
            val currentTime = System.currentTimeMillis()
            if (triggerTime > lastFetchedTime || currentTime - lastFetchedTime >= 30_000) {
                lastFetchedTime = currentTime
                val token = sessionManager.getAccessToken()
                val userId = sessionManager.getUserId()
                if (token != null && userId != null) {
                    runCatching {
                        supabaseApiService.getPlannerTasks(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                    }.onSuccess { dtos ->
                        val filtered = dtos.filter { it.date == date }.map { it.toDomain() }
                        emit(filtered)
                    }.onFailure { exception ->
                        Log.e("PlannerRepository", "Failed to fetch tasks flow list for date $date", exception)
                        emit(emptyList())
                    }
                } else {
                    emit(emptyList())
                }
            }
            delay(1000)
        }
    }

    override fun getAllTasksFlow(): Flow<List<PlannerTask>> = flow {
        var lastFetchedTime = 0L
        while (true) {
            val triggerTime = refreshTrigger.value
            val currentTime = System.currentTimeMillis()
            if (triggerTime > lastFetchedTime || currentTime - lastFetchedTime >= 30_000) {
                lastFetchedTime = currentTime
                val token = sessionManager.getAccessToken()
                val userId = sessionManager.getUserId()
                if (token != null && userId != null) {
                    runCatching {
                        supabaseApiService.getPlannerTasks(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                    }.onSuccess { dtos ->
                        emit(dtos.map { it.toDomain() })
                    }.onFailure { exception ->
                        Log.e("PlannerRepository", "Failed to fetch all tasks flow list", exception)
                        emit(emptyList())
                    }
                } else {
                    emit(emptyList())
                }
            }
            delay(1000)
        }
    }

    override suspend fun getTaskById(id: String): PlannerTask? {
        val token = sessionManager.getAccessToken() ?: return null
        val userId = sessionManager.getUserId() ?: return null
        return runCatching {
            supabaseApiService.getPlannerTasks(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                .firstOrNull { it.id == id }
                ?.toDomain()
        }.onFailure { exception ->
            Log.e("PlannerRepository", "Failed to get task by ID: $id", exception)
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
        triggerRefresh()
    }.onFailure { exception ->
        Log.e("PlannerRepository", "Failed to create task", exception)
    }

    override suspend fun updateTask(task: PlannerTask): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val updates = mapOf<String, Any>(
            "title" to task.title,
            "description" to task.description,
            "date" to task.dueDate,
            "is_completed" to task.completed
        )
        supabaseApiService.updatePlannerTask(SUPABASE_API_KEY, "Bearer $token", "eq.${task.id}", updates)
        triggerRefresh()
    }.onFailure { exception ->
        Log.e("PlannerRepository", "Failed to update task ID: ${task.id}", exception)
    }

    override suspend fun deleteTask(id: String): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        supabaseApiService.deletePlannerTask(SUPABASE_API_KEY, "Bearer $token", "eq.$id")
        triggerRefresh()
    }.onFailure { exception ->
        Log.e("PlannerRepository", "Failed to delete task ID: $id", exception)
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
