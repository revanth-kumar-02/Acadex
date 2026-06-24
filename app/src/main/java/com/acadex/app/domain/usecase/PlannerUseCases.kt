package com.acadex.app.domain.usecase

import com.acadex.app.domain.model.PlannerTask
import com.acadex.app.domain.repository.PlannerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(private val repository: PlannerRepository) {
    operator fun invoke(date: Long): Flow<List<PlannerTask>> = repository.getTasksFlow(date)
}

class GetAllTasksUseCase @Inject constructor(private val repository: PlannerRepository) {
    operator fun invoke(): Flow<List<PlannerTask>> = repository.getAllTasksFlow()
}

class GetTaskByIdUseCase @Inject constructor(private val repository: PlannerRepository) {
    suspend operator fun invoke(id: String): PlannerTask? = repository.getTaskById(id)
}

class CreateTaskUseCase @Inject constructor(private val repository: PlannerRepository) {
    suspend operator fun invoke(task: PlannerTask): Result<Unit> = repository.createTask(task)
}

class UpdateTaskUseCase @Inject constructor(private val repository: PlannerRepository) {
    suspend operator fun invoke(task: PlannerTask): Result<Unit> = repository.updateTask(task)
}

class DeleteTaskUseCase @Inject constructor(private val repository: PlannerRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteTask(id)
}

data class PlannerUseCases @Inject constructor(
    val getTasks: GetTasksUseCase,
    val getAllTasks: GetAllTasksUseCase,
    val getTaskById: GetTaskByIdUseCase,
    val createTask: CreateTaskUseCase,
    val updateTask: UpdateTaskUseCase,
    val deleteTask: DeleteTaskUseCase
)
