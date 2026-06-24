package com.acadex.app.data.repository

import com.acadex.app.data.remote.ExamDto
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.domain.model.Exam
import com.acadex.app.domain.repository.ExamRepository
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : ExamRepository {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
    }

    override fun getExamsFlow(): Flow<List<Exam>> = flow {
        while (true) {
            val token = sessionManager.getAccessToken()
            val userId = sessionManager.getUserId()
            if (token != null && userId != null) {
                runCatching {
                    supabaseApiService.getExams(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                }.onSuccess { dtos ->
                    emit(dtos.map { it.toDomain() })
                }.onFailure {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
            delay(30_000)
        }
    }

    override suspend fun getExamById(id: String): Exam? {
        val token = sessionManager.getAccessToken() ?: return null
        val userId = sessionManager.getUserId() ?: return null
        return runCatching {
            supabaseApiService.getExams(SUPABASE_API_KEY, "Bearer $token", "eq.$userId")
                .firstOrNull { it.id == id }
                ?.toDomain()
        }.getOrNull()
    }

    override suspend fun createExam(exam: Exam): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val id = exam.id.ifEmpty { UUID.randomUUID().toString() }

        val dto = ExamDto(
            id = id,
            userId = userId,
            subject = exam.subject,
            examDate = exam.dateTime,
            examType = exam.examName,
            notes = exam.syllabus
        )
        supabaseApiService.createExam(SUPABASE_API_KEY, "Bearer $token", dto)
    }

    override suspend fun updateExam(exam: Exam): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val updates = mapOf<String, Any>(
            "subject" to exam.subject,
            "exam_type" to exam.examName,
            "date" to exam.dateTime,
            "venue" to exam.room,
            "syllabus" to (exam.syllabus ?: "")
        )
        supabaseApiService.updateExam(SUPABASE_API_KEY, "Bearer $token", "eq.${exam.id}", updates)
    }

    override suspend fun deleteExam(id: String): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        supabaseApiService.deleteExam(SUPABASE_API_KEY, "Bearer $token", "eq.$id")
    }

    private fun ExamDto.toDomain() = Exam(
        id = id ?: "",
        subject = subject,
        examName = examType,
        dateTime = examDate,
        room = "",
        syllabus = notes ?: "",
        maxMarks = 100,
        targetMarks = 90,
        revisionProgress = 0f
    )
}
