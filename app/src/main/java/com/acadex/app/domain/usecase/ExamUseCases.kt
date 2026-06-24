package com.acadex.app.domain.usecase

import com.acadex.app.domain.model.Exam
import com.acadex.app.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExamsUseCase @Inject constructor(private val repository: ExamRepository) {
    operator fun invoke(): Flow<List<Exam>> = repository.getExamsFlow()
}

class GetExamByIdUseCase @Inject constructor(private val repository: ExamRepository) {
    suspend operator fun invoke(id: String): Exam? = repository.getExamById(id)
}

class CreateExamUseCase @Inject constructor(private val repository: ExamRepository) {
    suspend operator fun invoke(exam: Exam): Result<Unit> = repository.createExam(exam)
}

class UpdateExamUseCase @Inject constructor(private val repository: ExamRepository) {
    suspend operator fun invoke(exam: Exam): Result<Unit> = repository.updateExam(exam)
}

class DeleteExamUseCase @Inject constructor(private val repository: ExamRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteExam(id)
}

data class ExamUseCases @Inject constructor(
    val getExams: GetExamsUseCase,
    val getExamById: GetExamByIdUseCase,
    val createExam: CreateExamUseCase,
    val updateExam: UpdateExamUseCase,
    val deleteExam: DeleteExamUseCase
)
