package com.acadex.app.domain.usecase

import com.acadex.app.domain.model.Note
import com.acadex.app.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStream

class NotesUseCasesTest {

    private lateinit var mockRepository: FakeNotesRepository
    private lateinit var getNotesUseCase: GetNotesUseCase
    private lateinit var createNoteUseCase: CreateNoteUseCase

    @Before
    fun setUp() {
        mockRepository = FakeNotesRepository()
        getNotesUseCase = GetNotesUseCase(mockRepository)
        createNoteUseCase = CreateNoteUseCase(mockRepository)
    }

    @Test
    fun `getNotes returns correct notes from repository`() = runTest {
        val note1 = Note("1", "Title 1", "Content 1", "Math")
        val note2 = Note("2", "Title 2", "Content 2", "Science")
        mockRepository.notes = listOf(note1, note2)

        getNotesUseCase().collect { resultNotes ->
            assertEquals(2, resultNotes.size)
            assertEquals("Title 1", resultNotes[0].title)
            assertEquals("Title 2", resultNotes[1].title)
        }
    }

    @Test
    fun `createNote calls repository successfully`() = runTest {
        val note = Note("", "New Note", "Content", "History")
        val result = createNoteUseCase(note, null, null)

        assertTrue(result.isSuccess)
        assertEquals(1, mockRepository.notes.size)
        assertEquals("New Note", mockRepository.notes[0].title)
    }
}

class FakeNotesRepository : NotesRepository {
    var notes = listOf<Note>()

    override fun getNotesFlow(): Flow<List<Note>> {
        return flowOf(notes)
    }

    override suspend fun getNoteById(id: String): Note? {
        return notes.find { it.id == id }
    }

    override suspend fun createNote(note: Note, pdfStream: InputStream?, pdfName: String?): Result<Unit> {
        notes = notes + note.copy(id = "mock_id")
        return Result.success(Unit)
    }

    override suspend fun updateNote(note: Note, pdfStream: InputStream?, pdfName: String?): Result<Unit> {
        notes = notes.map { if (it.id == note.id) note else it }
        return Result.success(Unit)
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        notes = notes.filter { it.id != noteId }
        return Result.success(Unit)
    }

    override suspend fun toggleFavorite(noteId: String, isFavorite: Boolean): Result<Unit> {
        notes = notes.map { if (it.id == noteId) it.copy(isFavorite = isFavorite) else it }
        return Result.success(Unit)
    }

    override suspend fun syncNotes(): Result<Unit> {
        return Result.success(Unit)
    }
}
