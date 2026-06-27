package com.acadex.app.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.data.models.*
import com.acadex.app.data.repositories.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notesState: StateFlow<List<Note>> = combine(
        notesRepository.getNotesFlow(),
        _searchQuery,
        _selectedCategory,
        authRepository.currentUser
    ) { notes, query, category, user ->
        if (user == null) {
            emptyList()
        } else {
            val userDept = user.department.ifEmpty { "CSE" }
            val userSem = user.semester.ifEmpty { "Semester 3" }
            val userId = user.uid

            notes.filter { note ->
                val matchesTarget = note.uploadedBy == userId || isTargetMatched(userDept, userSem, note.broadcastTarget)
                val matchesCategory = category == null || note.category.equals(category, ignoreCase = true)
                val matchesQuery = query.isBlank() || 
                                   note.title.contains(query, ignoreCase = true) ||
                                   note.subject.contains(query, ignoreCase = true)

                matchesTarget && matchesCategory && matchesQuery
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.deleteNote(noteId)
        }
    }

    fun createNote(
        title: String,
        category: String,
        subject: String,
        broadcastTarget: String,
        fileStream: InputStream?,
        fileName: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            val authorName = user?.name ?: "Student"
            val note = Note(
                title = title,
                category = category,
                subject = subject,
                broadcastTarget = broadcastTarget,
                fileUrl = "",
                fileName = fileName ?: "",
                uploadedBy = user?.uid ?: "",
                uploadedByName = authorName
            )
            notesRepository.createNote(note, fileStream, fileName)
                .onSuccess { onSuccess() }
                .onFailure { error -> onFailure(error.message ?: "Failed to upload note") }
        }
    }

    suspend fun getNoteById(id: String): Note? {
        return notesRepository.getNoteById(id)
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
