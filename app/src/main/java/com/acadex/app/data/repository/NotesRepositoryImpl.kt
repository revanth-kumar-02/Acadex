package com.acadex.app.data.repository

import com.acadex.app.data.local.NoteDao
import com.acadex.app.data.mapper.toDomain
import com.acadex.app.data.mapper.toEntity
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.domain.model.Note
import com.acadex.app.domain.repository.NotesRepository
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : NotesRepository {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
        private const val SUPABASE_BASE_URL = "https://ombpzrctfsqlpaxbvjha.supabase.co"
    }

    override fun getNotesFlow(): Flow<List<Note>> {
        return noteDao.getNotesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun createNote(
        note: Note,
        pdfStream: InputStream?,
        pdfName: String?
    ): Result<Unit> = runCatching {
        val noteId = note.id.ifEmpty { UUID.randomUUID().toString() }
        var pdfUrl = note.pdfUrl

        val token = sessionManager.getAccessToken()
        val userId = sessionManager.getUserId()

        if (pdfStream != null && pdfName != null && token != null && userId != null) {
            val storagePath = "notes/$userId/$noteId/$pdfName"
            val requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/pdf"),
                pdfStream.readBytes()
            )
            runCatching {
                val uploadResponse = supabaseApiService.uploadFile(
                    SUPABASE_API_KEY,
                    "Bearer $token",
                    "notes",
                    storagePath,
                    requestBody
                )
                pdfUrl = "$SUPABASE_BASE_URL/storage/v1/object/public/${uploadResponse.key}"
            }
        }

        val updatedNote = note.copy(id = noteId, pdfUrl = pdfUrl, pdfName = pdfName ?: note.pdfName)
        
        // Save to Room Cache
        noteDao.insertNote(updatedNote.toEntity())
    }

    override suspend fun updateNote(
        note: Note,
        pdfStream: InputStream?,
        pdfName: String?
    ): Result<Unit> = runCatching {
        var pdfUrl = note.pdfUrl

        val token = sessionManager.getAccessToken()
        val userId = sessionManager.getUserId()

        if (pdfStream != null && pdfName != null && token != null && userId != null) {
            val storagePath = "notes/$userId/${note.id}/$pdfName"
            val requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/pdf"),
                pdfStream.readBytes()
            )
            runCatching {
                val uploadResponse = supabaseApiService.uploadFile(
                    SUPABASE_API_KEY,
                    "Bearer $token",
                    "notes",
                    storagePath,
                    requestBody
                )
                pdfUrl = "$SUPABASE_BASE_URL/storage/v1/object/public/${uploadResponse.key}"
            }
        }

        val updatedNote = note.copy(pdfUrl = pdfUrl, pdfName = pdfName ?: note.pdfName, updatedAt = System.currentTimeMillis())
        
        // Save to Room Cache
        noteDao.insertNote(updatedNote.toEntity())
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        // Delete from local cache
        noteDao.deleteNoteById(noteId)
    }

    override suspend fun toggleFavorite(noteId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        val note = noteDao.getNoteById(noteId)?.toDomain()
        if (note != null) {
            val updatedNote = note.copy(isFavorite = isFavorite, updatedAt = System.currentTimeMillis())
            noteDao.insertNote(updatedNote.toEntity())
        }
    }

    override suspend fun syncNotes(): Result<Unit> = runCatching {
        // Sync operation placeholder - can be implemented as required
    }
}
