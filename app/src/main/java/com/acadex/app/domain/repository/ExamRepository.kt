package com.acadex.app.domain.repository

import com.acadex.app.domain.model.Exam
import kotlinx.coroutines.flow.Flow

interface ExamRepository {
    fun getExamsFlow(): Flow<List<Exam>>
    suspend fun getExamById(id: String): Exam?
    suspend fun createExam(exam: Exam): Result<Unit>
    suspend fun updateExam(exam: Exam): Result<Unit>
    suspend fun deleteExam(id: String): Result<Unit>
}
