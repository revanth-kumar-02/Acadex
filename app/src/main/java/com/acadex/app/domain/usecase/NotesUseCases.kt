package com.acadex.app.domain.usecase

import com.acadex.app.domain.model.Note
import com.acadex.app.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(private val repository: NotesRepository) {
    operator fun invoke(): Flow<List<Note>> = repository.getNotesFlow()
}

class GetNoteByIdUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(id: String): Note? = repository.getNoteById(id)
}

class CreateNoteUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, pdfStream: InputStream?, pdfName: String?): Result<Unit> =
        repository.createNote(note, pdfStream, pdfName)
}

class UpdateNoteUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, pdfStream: InputStream?, pdfName: String?): Result<Unit> =
        repository.updateNote(note, pdfStream, pdfName)
}

class DeleteNoteUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteNote(id)
}

class ToggleFavoriteUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(id: String, isFavorite: Boolean): Result<Unit> =
        repository.toggleFavorite(id, isFavorite)
}

class SyncNotesUseCase @Inject constructor(private val repository: NotesRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.syncNotes()
}

data class NotesUseCases @Inject constructor(
    val getNotes: GetNotesUseCase,
    val getNoteById: GetNoteByIdUseCase,
    val createNote: CreateNoteUseCase,
    val updateNote: UpdateNoteUseCase,
    val deleteNote: DeleteNoteUseCase,
    val toggleFavorite: ToggleFavoriteUseCase,
    val syncNotes: SyncNotesUseCase
)
