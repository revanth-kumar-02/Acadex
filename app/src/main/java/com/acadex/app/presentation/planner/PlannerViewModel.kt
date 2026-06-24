package com.acadex.app.presentation.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.domain.model.*
import com.acadex.app.domain.usecase.AssignmentUseCases
import com.acadex.app.domain.usecase.ExamUseCases
import com.acadex.app.domain.usecase.PlannerUseCases
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
    val assignments: List<Assignment> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val allTasks: List<PlannerTask> = emptyList()
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val plannerUseCases: PlannerUseCases,
    private val assignmentUseCases: AssignmentUseCases,
    private val examUseCases: ExamUseCases
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate = _selectedDate.asStateFlow()

    val plannerState: StateFlow<PlannerState> = combine(
        _selectedDate,
        plannerUseCases.getAllTasks(),
        assignmentUseCases.getAssignments(),
        examUseCases.getExams()
    ) { date, tasks, assignments, exams ->
        val selectedDay = date / 86400000
        val dailyTasks = tasks.filter {
            val taskDay = it.date / 86400000
            taskDay == selectedDay
        }
        
        PlannerState(
            selectedDate = date,
            tasksForSelectedDate = dailyTasks,
            assignments = assignments,
            exams = exams,
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
            plannerUseCases.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun createTask(task: PlannerTask, onSuccess: () -> Unit) {
        viewModelScope.launch {
            plannerUseCases.createTask(task).onSuccess { onSuccess() }
        }
    }

    fun updateTask(task: PlannerTask, onSuccess: () -> Unit) {
        viewModelScope.launch {
            plannerUseCases.updateTask(task).onSuccess { onSuccess() }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            plannerUseCases.deleteTask(id)
        }
    }

    // Assignment CRUD
    fun createAssignment(assignment: Assignment, onSuccess: () -> Unit) {
        viewModelScope.launch {
            assignmentUseCases.createAssignment(assignment).onSuccess { onSuccess() }
        }
    }

    fun updateAssignment(assignment: Assignment, onSuccess: () -> Unit) {
        viewModelScope.launch {
            assignmentUseCases.updateAssignment(assignment).onSuccess { onSuccess() }
        }
    }

    fun deleteAssignment(id: String) {
        viewModelScope.launch {
            assignmentUseCases.deleteAssignment(id)
        }
    }

    // Exam CRUD
    fun createExam(exam: Exam, onSuccess: () -> Unit) {
        viewModelScope.launch {
            examUseCases.createExam(exam).onSuccess { onSuccess() }
        }
    }

    fun updateExam(exam: Exam, onSuccess: () -> Unit) {
        viewModelScope.launch {
            examUseCases.updateExam(exam).onSuccess { onSuccess() }
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            examUseCases.deleteExam(id)
        }
    }

    // Fetch details
    suspend fun getTaskById(id: String): PlannerTask? = plannerUseCases.getTaskById(id)
    suspend fun getAssignmentById(id: String): Assignment? = assignmentUseCases.getAssignmentById(id)
    suspend fun getExamById(id: String): Exam? = examUseCases.getExamById(id)
}
