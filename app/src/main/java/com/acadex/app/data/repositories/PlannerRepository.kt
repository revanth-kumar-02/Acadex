package com.acadex.app.data.repositories

import com.acadex.app.data.models.PlannerTask
import kotlinx.coroutines.flow.Flow

interface PlannerRepository {
    fun getTasksFlow(date: Long): Flow<List<PlannerTask>>
    fun getAllTasksFlow(): Flow<List<PlannerTask>>
    suspend fun getTaskById(id: String): PlannerTask?
    suspend fun createTask(task: PlannerTask): Result<Unit>
    suspend fun updateTask(task: PlannerTask): Result<Unit>
    suspend fun deleteTask(id: String): Result<Unit>
}
