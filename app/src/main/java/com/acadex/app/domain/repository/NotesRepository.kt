package com.acadex.app.domain.repository

import com.acadex.app.domain.model.Note
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface NotesRepository {
    fun getNotesFlow(): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    suspend fun createNote(note: Note, fileStream: InputStream?, fileName: String?): Result<Unit>
    suspend fun deleteNote(noteId: String): Result<Unit>
}
