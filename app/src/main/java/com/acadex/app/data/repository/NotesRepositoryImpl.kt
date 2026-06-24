package com.acadex.app.data.repository

import com.acadex.app.data.local.NoteDao
import com.acadex.app.data.mapper.toDomain
import com.acadex.app.data.mapper.toEntity
import com.acadex.app.data.remote.FirebaseService
import com.acadex.app.domain.model.Note
import com.acadex.app.domain.repository.NotesRepository
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val firebaseService: FirebaseService
) : NotesRepository {

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
        val uid = firebaseService.currentUserId ?: throw Exception("User not authenticated")
        val noteId = note.id.ifEmpty { UUID.randomUUID().toString() }

        var pdfUrl: String? = null
        if (pdfStream != null && pdfName != null) {
            val storagePath = "notes/$uid/$noteId/$pdfName"
            pdfUrl = firebaseService.uploadFile(storagePath, pdfStream)
        }

        val updatedNote = note.copy(id = noteId, pdfUrl = pdfUrl ?: note.pdfUrl, pdfName = pdfName ?: note.pdfName)
        
        // Save to Room Cache
        noteDao.insertNote(updatedNote.toEntity())

        // Save to Firestore
        val noteMap = mapOf(
            "id" to updatedNote.id,
            "userId" to uid,
            "title" to updatedNote.title,
            "content" to updatedNote.content,
            "subject" to updatedNote.subject,
            "isFavorite" to updatedNote.isFavorite,
            "pdfUrl" to updatedNote.pdfUrl,
            "pdfName" to updatedNote.pdfName,
            "createdAt" to updatedNote.createdAt,
            "updatedAt" to updatedNote.updatedAt
        )
        
        firebaseService.firestore.collection("notes")
            .document(noteId)
            .set(noteMap)
            .await()
    }

    override suspend fun updateNote(
        note: Note,
        pdfStream: InputStream?,
        pdfName: String?
    ): Result<Unit> = runCatching {
        val uid = firebaseService.currentUserId ?: throw Exception("User not authenticated")
        
        var pdfUrl = note.pdfUrl
        if (pdfStream != null && pdfName != null) {
            val storagePath = "notes/$uid/${note.id}/$pdfName"
            pdfUrl = firebaseService.uploadFile(storagePath, pdfStream)
        }

        val updatedNote = note.copy(pdfUrl = pdfUrl, pdfName = pdfName ?: note.pdfName, updatedAt = System.currentTimeMillis())
        
        // Save to Room Cache
        noteDao.insertNote(updatedNote.toEntity())

        // Save to Firestore
        val updates = mapOf(
            "title" to updatedNote.title,
            "content" to updatedNote.content,
            "subject" to updatedNote.subject,
            "isFavorite" to updatedNote.isFavorite,
            "pdfUrl" to updatedNote.pdfUrl,
            "pdfName" to updatedNote.pdfName,
            "updatedAt" to updatedNote.updatedAt
        )
        
        firebaseService.firestore.collection("notes")
            .document(updatedNote.id)
            .update(updates)
            .await()
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        // Delete from local cache
        noteDao.deleteNoteById(noteId)

        // Delete from Firestore
        firebaseService.firestore.collection("notes")
            .document(noteId)
            .delete()
            .await()
    }

    override suspend fun toggleFavorite(noteId: String, isFavorite: Boolean): Result<Unit> = runCatching {
        val note = noteDao.getNoteById(noteId)?.toDomain()
        if (note != null) {
            val updatedNote = note.copy(isFavorite = isFavorite, updatedAt = System.currentTimeMillis())
            noteDao.insertNote(updatedNote.toEntity())
            
            firebaseService.firestore.collection("notes")
                .document(noteId)
                .update("isFavorite", isFavorite, "updatedAt", updatedNote.updatedAt)
                .await()
        }
    }

    override suspend fun syncNotes(): Result<Unit> = runCatching {
        val uid = firebaseService.currentUserId ?: throw Exception("User not authenticated")
        val snapshots = firebaseService.firestore.collection("notes")
            .whereEqualTo("userId", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val notesList = snapshots.documents.map { doc ->
            Note(
                id = doc.getString("id") ?: "",
                title = doc.getString("title") ?: "",
                content = doc.getString("content") ?: "",
                subject = doc.getString("subject") ?: "",
                isFavorite = doc.getBoolean("isFavorite") ?: false,
                pdfUrl = doc.getString("pdfUrl"),
                pdfName = doc.getString("pdfName"),
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        }

        if (notesList.isNotEmpty()) {
            noteDao.clearNotes()
            noteDao.insertNotes(notesList.map { it.toEntity() })
        }
    }
}
