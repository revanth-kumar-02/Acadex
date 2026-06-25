package com.acadex.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.domain.model.*
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.domain.repository.NotesRepository
import com.acadex.app.domain.repository.AssignmentRepository
import com.acadex.app.domain.repository.AnnouncementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FeedItem {
    abstract val id: String
    abstract val timestamp: Long
    abstract val broadcastTarget: String
    
    data class AssignmentItem(
        override val id: String,
        val title: String,
        val subject: String,
        val description: String,
        val dueDate: Long,
        val assignedDate: Long,
        override val timestamp: Long,
        override val broadcastTarget: String,
        val attachmentUrl: String?,
        val postedBy: String,
        val status: AssignmentStatus
    ) : FeedItem()
    
    data class NotesItem(
        override val id: String,
        val title: String,
        val category: String,
        val subject: String,
        val fileUrl: String,
        val fileName: String,
        override val timestamp: Long,
        override val broadcastTarget: String,
        val uploadedBy: String,
        val uploadedByName: String
    ) : FeedItem()
    
    data class AnnouncementItem(
        override val id: String,
        val title: String,
        val content: String,
        override val timestamp: Long,
        override val broadcastTarget: String,
        val authorName: String
    ) : FeedItem()
}

data class DashboardState(
    val user: User? = null,
    val feedItems: List<FeedItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    notesRepository: NotesRepository,
    private val assignmentRepository: AssignmentRepository,
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    val dashboardState: StateFlow<DashboardState> = combine(
        authRepository.currentUser,
        notesRepository.getNotesFlow(),
        assignmentRepository.getAssignmentsFlow(),
        announcementRepository.getAnnouncementsFlow()
    ) { user, notes, assignments, announcements ->
        if (user == null) {
            DashboardState(user = null, feedItems = emptyList(), isLoading = true)
        } else {
            val userDept = user.department.ifEmpty { "CSE" }
            val userSem = user.semester.ifEmpty { "Semester 3" }
            val userId = user.uid

            val assignmentItems = assignments
                .filter { it.postedBy.isNotBlank() && (it.postedBy == user.name || isTargetMatched(userDept, userSem, it.broadcastTarget)) }
                .map {
                    FeedItem.AssignmentItem(
                        id = it.id,
                        title = it.title,
                        subject = it.subject,
                        description = it.description,
                        dueDate = it.dueDate,
                        assignedDate = it.assignedDate,
                        timestamp = it.assignedDate,
                        broadcastTarget = it.broadcastTarget,
                        attachmentUrl = it.attachmentUrl,
                        postedBy = it.postedBy,
                        status = it.status
                    )
                }

            val notesItems = notes
                .filter { it.uploadedByName.isNotBlank() && (it.uploadedBy == userId || isTargetMatched(userDept, userSem, it.broadcastTarget)) }
                .map {
                    FeedItem.NotesItem(
                        id = it.id,
                        title = it.title,
                        category = it.category,
                        subject = it.subject,
                        fileUrl = it.fileUrl,
                        fileName = it.fileName,
                        timestamp = it.createdAt,
                        broadcastTarget = it.broadcastTarget,
                        uploadedBy = it.uploadedBy,
                        uploadedByName = it.uploadedByName
                    )
                }

            val announcementItems = announcements
                .filter { it.authorName.isNotBlank() && (it.userId == userId || isTargetMatched(userDept, userSem, it.broadcastTarget)) }
                .map {
                    FeedItem.AnnouncementItem(
                        id = it.id,
                        title = it.title,
                        content = it.content,
                        timestamp = it.createdAt,
                        broadcastTarget = it.broadcastTarget,
                        authorName = it.authorName
                    )
                }

            val mergedFeed = (assignmentItems + notesItems + announcementItems)
                .sortedByDescending { it.timestamp }

            DashboardState(
                user = user,
                feedItems = mergedFeed,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    fun updateAssignmentStatus(assignmentId: String, newStatus: AssignmentStatus) {
        viewModelScope.launch {
            val assignment = assignmentRepository.getAssignmentById(assignmentId)
            if (assignment != null) {
                assignmentRepository.updateAssignment(assignment.copy(status = newStatus))
            }
        }
    }

    fun createAnnouncement(title: String, content: String, broadcastTarget: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            val authorName = user?.name ?: "Student"
            val ann = Announcement(
                userId = user?.uid ?: "",
                title = title,
                content = content,
                broadcastTarget = broadcastTarget,
                authorName = authorName
            )
            announcementRepository.createAnnouncement(ann)
                .onSuccess { onSuccess() }
        }
    }

    private fun isTargetMatched(userDept: String, userSem: String, target: String): Boolean {
        if (target.isBlank()) return true
        val cleanedTarget = target.trim()
        
        val targetDept = when {
            cleanedTarget.startsWith("Entire ", ignoreCase = true) && cleanedTarget.endsWith(" Department", ignoreCase = true) -> {
                cleanedTarget.substring(7, cleanedTarget.length - 11).trim()
            }
            cleanedTarget.contains(" Year ", ignoreCase = true) -> {
                val yearIdx = cleanedTarget.indexOf(" Year ", ignoreCase = true)
                if (yearIdx != -1) cleanedTarget.substring(0, yearIdx).trim() else cleanedTarget
            }
            else -> cleanedTarget
        }

        if (!userDept.equals(targetDept, ignoreCase = true) && !cleanedTarget.contains(userDept, ignoreCase = true)) {
            return false
        }

        val yearIndex = cleanedTarget.indexOf(" Year ", ignoreCase = true)
        if (yearIndex != -1) {
            val yearStr = cleanedTarget.substring(yearIndex + 6).trim()
            val targetYear = yearStr.toIntOrNull() ?: return true
            
            val userYear = when {
                userSem.contains("Year 1", ignoreCase = true) || userSem.contains("1st Year", ignoreCase = true) || userSem.contains("Semester 1", ignoreCase = true) || userSem.contains("Semester 2", ignoreCase = true) -> 1
                userSem.contains("Year 2", ignoreCase = true) || userSem.contains("2nd Year", ignoreCase = true) || userSem.contains("Semester 3", ignoreCase = true) || userSem.contains("Semester 4", ignoreCase = true) -> 2
                userSem.contains("Year 3", ignoreCase = true) || userSem.contains("3rd Year", ignoreCase = true) || userSem.contains("Semester 5", ignoreCase = true) || userSem.contains("Semester 6", ignoreCase = true) -> 3
                userSem.contains("Year 4", ignoreCase = true) || userSem.contains("4th Year", ignoreCase = true) || userSem.contains("Semester 7", ignoreCase = true) || userSem.contains("Semester 8", ignoreCase = true) -> 4
                else -> {
                    val digit = userSem.firstOrNull { it.isDigit() }?.digitToIntOrNull()
                    if (digit != null) {
                        if (userSem.contains("sem", ignoreCase = true)) {
                            (digit + 1) / 2
                        } else {
                            digit
                        }
                    } else 1
                }
            }
            return userYear == targetYear
        }
        return true
    }
}
