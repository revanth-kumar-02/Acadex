package com.acadex.app.features.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlannerState(
    val selectedDate: Long = System.currentTimeMillis(),
    val tasksForSelectedDate: List<PlannerTask> = emptyList(),
    val allTasks: List<PlannerTask> = emptyList()
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val plannerUseCases: PlannerUseCases
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate = _selectedDate.asStateFlow()

    val plannerState: StateFlow<PlannerState> = combine(
        _selectedDate,
        plannerUseCases.getAllTasks()
    ) { date, tasks ->
        val selectedDay = date / 86400000
        val dailyTasks = tasks.filter {
            val taskDay = it.dueDate / 86400000
            taskDay == selectedDay
        }
        
        PlannerState(
            selectedDate = date,
            tasksForSelectedDate = dailyTasks,
            allTasks = tasks
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerState()
    )

    fun selectDate(timeInMillis: Long) {
        _selectedDate.value = timeInMillis
    }

    // Task CRUD
    fun toggleTaskCompletion(task: PlannerTask) {
        viewModelScope.launch {
            plannerUseCases.updateTask(task.copy(completed = !task.completed))
        }
    }

    fun createTask(task: PlannerTask, onSuccess: () -> Unit, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            plannerUseCases.createTask(task)
                .onSuccess { onSuccess() }
                .onFailure { error -> onFailure(error.message ?: "Failed to create task") }
        }
    }

    fun updateTask(task: PlannerTask, onSuccess: () -> Unit, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            plannerUseCases.updateTask(task)
                .onSuccess { onSuccess() }
                .onFailure { error -> onFailure(error.message ?: "Failed to update task") }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            plannerUseCases.deleteTask(id)
        }
    }

    // Fetch details
    suspend fun getTaskById(id: String): PlannerTask? = plannerUseCases.getTaskById(id)
}
