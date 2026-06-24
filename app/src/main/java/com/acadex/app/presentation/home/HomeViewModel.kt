package com.acadex.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.data.remote.AnnouncementDto
import com.acadex.app.data.remote.ApiService
import com.acadex.app.data.remote.QuoteDto
import com.acadex.app.domain.model.*
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.domain.usecase.AssignmentUseCases
import com.acadex.app.domain.usecase.ExamUseCases
import com.acadex.app.domain.usecase.NotesUseCases
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

data class DashboardState(
    val user: User? = null,
    val pendingAssignments: List<Assignment> = emptyList(),
    val upcomingExams: List<Exam> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val todayTasks: List<PlannerTask> = emptyList(),
    val upcomingDeadlines: List<DeadlineItem> = emptyList(),
    val stats: AcademicStats = AcademicStats()
)

data class AcademicStats(
    val completedAssignmentsCount: Int = 0,
    val totalNotesCount: Int = 0,
    val examCount: Int = 0,
    val revisionAverage: Float = 0f
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    authRepository: AuthRepository,
    notesUseCases: NotesUseCases,
    assignmentUseCases: AssignmentUseCases,
    examUseCases: ExamUseCases,
    plannerUseCases: PlannerUseCases,
    private val apiService: ApiService
) : ViewModel() {

    private val _quote = MutableStateFlow<QuoteDto?>(null)
    val quote: StateFlow<QuoteDto?> = _quote.asStateFlow()

    private val _announcements = MutableStateFlow<List<AnnouncementDto>>(emptyList())
    val announcements: StateFlow<List<AnnouncementDto>> = _announcements.asStateFlow()

    init {
        fetchExternalData()
    }

    private fun fetchExternalData() {
        viewModelScope.launch {
            runCatching {
                apiService.getRandomQuote()
            }.onSuccess {
                _quote.value = it
            }.onFailure {
                // Fallback quote
                _quote.value = QuoteDto("The capacity to learn is a gift; the ability to learn is a skill; the willingness to learn is a choice.", "Brian Herbert")
            }

            runCatching {
                apiService.getAnnouncements()
            }.onSuccess {
                _announcements.value = it
            }.onFailure {
                // Fallback announcements
                _announcements.value = listOf(
                    AnnouncementDto("1", "End Semester Schedules Out", "The end semester exams will begin from July 15th. Check Resource Hub for details.", "June 24"),
                    AnnouncementDto("2", "Notes Drive Uploads Open", "Students are encouraged to upload revision notes to the Resource Hub.", "June 23")
                )
            }
        }
    }

    // Combine flows for the UI state
    val dashboardState: StateFlow<DashboardState> = combine(
        authRepository.currentUser,
        notesUseCases.getNotes(),
        assignmentUseCases.getAssignments(),
        examUseCases.getExams(),
        plannerUseCases.getAllTasks()
    ) { user, notes, assignments, exams, tasks ->
        val pendingAssignments = assignments.filter { it.status != AssignmentStatus.COMPLETED }
        val completedCount = assignments.count { it.status == AssignmentStatus.COMPLETED }
        val upcomingExams = exams.filter { it.dateTime > System.currentTimeMillis() }
        val recentNotes = notes.take(5)
        
        val today = System.currentTimeMillis()
        val todayTasks = tasks.filter {
            val taskDay = it.dueDate / (86400000)
            val todayDay = today / (86400000)
            taskDay == todayDay
        }

        // Aggregate upcoming deadlines
        val deadlineItems = mutableListOf<DeadlineItem>()
        
        // 1. Pending Assignments
        pendingAssignments.forEach { assignment ->
            val diff = assignment.dueDate - today
            val days = (diff / (86400000)).toInt()
            deadlineItems.add(
                DeadlineItem(
                    id = assignment.id,
                    title = assignment.title,
                    type = DeadlineType.ASSIGNMENT,
                    dueDate = assignment.dueDate,
                    daysRemaining = days
                )
            )
        }

        // 2. Uncompleted Tasks
        tasks.filter { !it.completed }.forEach { task ->
            val diff = task.dueDate - today
            val days = (diff / (86400000)).toInt()
            deadlineItems.add(
                DeadlineItem(
                    id = task.id,
                    title = task.title,
                    type = DeadlineType.TASK,
                    dueDate = task.dueDate,
                    daysRemaining = days
                )
            )
        }

        // 3. Upcoming Exams
        upcomingExams.forEach { exam ->
            val diff = exam.dateTime - today
            val days = (diff / (86400000)).toInt()
            deadlineItems.add(
                DeadlineItem(
                    id = exam.id,
                    title = "${exam.subject} - ${exam.examName}",
                    type = DeadlineType.EXAM,
                    dueDate = exam.dateTime,
                    daysRemaining = days
                )
            )
        }

        val sortedDeadlines = deadlineItems.sortedBy { it.dueDate }

        val avgRevision = if (exams.isNotEmpty()) {
            exams.map { it.revisionProgress }.average().toFloat()
        } else 0f

        DashboardState(
            user = user,
            pendingAssignments = pendingAssignments,
            upcomingExams = upcomingExams,
            recentNotes = recentNotes,
            todayTasks = todayTasks,
            upcomingDeadlines = sortedDeadlines,
            stats = AcademicStats(
                completedAssignmentsCount = completedCount,
                totalNotesCount = notes.size,
                examCount = exams.size,
                revisionAverage = avgRevision
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
}
