package com.acadex.app.data.repository

import com.acadex.app.data.remote.NoteDto
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.domain.model.Note
import com.acadex.app.domain.repository.NotesRepository
import com.acadex.app.utils.DateTimeUtils
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : NotesRepository {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
        private const val SUPABASE_BASE_URL = "https://ombpzrctfsqlpaxbvjha.supabase.co"
    }

    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private fun triggerRefresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    override fun getNotesFlow(): Flow<List<Note>> = flow {
        var lastFetchedTime = 0L
        while (true) {
            val triggerTime = refreshTrigger.value
            val currentTime = System.currentTimeMillis()
            if (triggerTime > lastFetchedTime || currentTime - lastFetchedTime >= 30_000) {
                lastFetchedTime = currentTime
                val token = sessionManager.getAccessToken()
                if (token != null) {
                    runCatching {
                        supabaseApiService.getNotes(SUPABASE_API_KEY, "Bearer $token")
                    }.onSuccess { dtos ->
                        emit(dtos.map { it.toDomain() })
                    }.onFailure {
                        emit(emptyList())
                    }
                } else {
                    emit(emptyList())
                }
            }
            delay(1000)
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        val token = sessionManager.getAccessToken() ?: return null
        return runCatching {
            supabaseApiService.getNotes(SUPABASE_API_KEY, "Bearer $token")
                .firstOrNull { it.id == id }
                ?.toDomain()
        }.getOrNull()
    }

    override suspend fun createNote(
        note: Note,
        fileStream: InputStream?,
        fileName: String?
    ): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val authorName = sessionManager.getUserName() ?: "Student"
        
        val noteId = note.id.ifEmpty { UUID.randomUUID().toString() }
        var fileUrl = note.fileUrl

        if (fileStream != null && fileName != null) {
            val storagePath = "$userId/$noteId/$fileName"
            val requestBody = fileStream.readBytes()
                .toRequestBody("*/*".toMediaTypeOrNull())
            val uploadResponse = supabaseApiService.uploadFile(
                SUPABASE_API_KEY,
                "Bearer $token",
                "resources",
                storagePath,
                requestBody
            )
            fileUrl = "$SUPABASE_BASE_URL/storage/v1/object/public/${uploadResponse.key}"
        }

        val dto = NoteDto(
            id = noteId,
            title = note.title,
            category = note.category,
            fileUrl = fileUrl,
            subject = note.subject,
            broadcastTarget = note.broadcastTarget,
            uploadedBy = userId,
            uploadedByName = authorName
        )
        supabaseApiService.createNote(SUPABASE_API_KEY, "Bearer $token", dto)
        triggerRefresh()
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        supabaseApiService.deleteNote(SUPABASE_API_KEY, "Bearer $token", "eq.$noteId")
        triggerRefresh()
    }

    private fun NoteDto.toDomain() = Note(
        id = id ?: "",
        title = title,
        category = category,
        subject = subject ?: "",
        fileUrl = fileUrl,
        fileName = fileUrl.substringAfterLast("/"),
        broadcastTarget = broadcastTarget ?: "",
        uploadedBy = uploadedBy ?: "",
        uploadedByName = uploadedByName ?: "",
        createdAt = DateTimeUtils.parseIsoTimestamp(createdAt)
    )
}
