package com.acadex.app.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.domain.model.Note
import com.acadex.app.domain.usecase.NotesUseCases
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
    private val notesUseCases: NotesUseCases
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showOnlyFavorites = MutableStateFlow(false)
    val showOnlyFavorites = _showOnlyFavorites.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val notesState: StateFlow<List<Note>> = combine(
        notesUseCases.getNotes(),
        _searchQuery,
        _showOnlyFavorites
    ) { notes, query, favOnly ->
        notes.filter { note ->
            val matchesSearch = note.title.contains(query, ignoreCase = true) || 
                                note.content.contains(query, ignoreCase = true) ||
                                note.subject.contains(query, ignoreCase = true)
            val matchesFav = !favOnly || note.isFavorite
            matchesSearch && matchesFav
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    fun syncNotes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            notesUseCases.syncNotes()
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite(noteId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            notesUseCases.toggleFavorite(noteId, isFavorite)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            notesUseCases.deleteNote(noteId)
        }
    }

    fun createNote(
        title: String,
        content: String,
        subject: String,
        pdfStream: InputStream?,
        pdfName: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val note = Note(
                title = title,
                content = content,
                subject = subject
            )
            notesUseCases.createNote(note, pdfStream, pdfName)
                .onSuccess { onSuccess() }
        }
    }

    fun updateNote(
        noteId: String,
        title: String,
        content: String,
        subject: String,
        pdfUrl: String?,
        pdfName: String?,
        isFavorite: Boolean,
        pdfStream: InputStream?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val note = Note(
                id = noteId,
                title = title,
                content = content,
                subject = subject,
                isFavorite = isFavorite,
                pdfUrl = pdfUrl,
                pdfName = pdfName
            )
            notesUseCases.updateNote(note, pdfStream, pdfName)
                .onSuccess { onSuccess() }
        }
    }

    suspend fun getNoteById(id: String): Note? {
        return notesUseCases.getNoteById(id)
    }
}
