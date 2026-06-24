package com.acadex.app.domain.repository

import com.acadex.app.domain.model.PlannerTask
import kotlinx.coroutines.flow.Flow

interface PlannerRepository {
    fun getTasksFlow(date: Long): Flow<List<PlannerTask>>
    fun getAllTasksFlow(): Flow<List<PlannerTask>>
    suspend fun getTaskById(id: String): PlannerTask?
    suspend fun createTask(task: PlannerTask): Result<Unit>
    suspend fun updateTask(task: PlannerTask): Result<Unit>
    suspend fun deleteTask(id: String): Result<Unit>
}
