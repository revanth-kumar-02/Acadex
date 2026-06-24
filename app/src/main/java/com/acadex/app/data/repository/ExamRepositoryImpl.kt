package com.acadex.app.data.repository

import com.acadex.app.data.remote.FirebaseService
import com.acadex.app.domain.model.Exam
import com.acadex.app.domain.repository.ExamRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepositoryImpl @Inject constructor(
    private val firebaseService: FirebaseService
) : ExamRepository {

    override fun getExamsFlow(): Flow<List<Exam>> = callbackFlow {
        val uid = firebaseService.currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firebaseService.firestore.collection("exams")
            .whereEqualTo("userId", uid)
            .orderBy("dateTime")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    Exam(
                        id = doc.getString("id") ?: "",
                        subject = doc.getString("subject") ?: "",
                        examName = doc.getString("examName") ?: "",
                        dateTime = doc.getLong("dateTime") ?: 0L,
                        room = doc.getString("room") ?: "",
                        syllabus = doc.getString("syllabus") ?: "",
                        maxMarks = doc.getLong("maxMarks")?.toInt() ?: 100,
                        targetMarks = doc.getLong("targetMarks")?.toInt() ?: 90,
                        revisionProgress = doc.getDouble("revisionProgress")?.toFloat() ?: 0.0f
                    )
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getExamById(id: String): Exam? {
        val doc = firebaseService.firestore.collection("exams").document(id).get().await()
        if (!doc.exists()) return null
        return Exam(
            id = doc.getString("id") ?: "",
            subject = doc.getString("subject") ?: "",
            examName = doc.getString("examName") ?: "",
            dateTime = doc.getLong("dateTime") ?: 0L,
            room = doc.getString("room") ?: "",
            syllabus = doc.getString("syllabus") ?: "",
            maxMarks = doc.getLong("maxMarks")?.toInt() ?: 100,
            targetMarks = doc.getLong("targetMarks")?.toInt() ?: 90,
            revisionProgress = doc.getDouble("revisionProgress")?.toFloat() ?: 0.0f
        )
    }

    override suspend fun createExam(exam: Exam): Result<Unit> = runCatching {
        val uid = firebaseService.currentUserId ?: throw Exception("User not authenticated")
        val id = exam.id.ifEmpty { UUID.randomUUID().toString() }

        val map = mapOf(
            "id" to id,
            "userId" to uid,
            "subject" to exam.subject,
            "examName" to exam.examName,
            "dateTime" to exam.dateTime,
            "room" to exam.room,
            "syllabus" to exam.syllabus,
            "maxMarks" to exam.maxMarks,
            "targetMarks" to exam.targetMarks,
            "revisionProgress" to exam.revisionProgress
        )

        firebaseService.firestore.collection("exams")
            .document(id)
            .set(map)
            .await()
    }

    override suspend fun updateExam(exam: Exam): Result<Unit> = runCatching {
        val map = mapOf(
            "subject" to exam.subject,
            "examName" to exam.examName,
            "dateTime" to exam.dateTime,
            "room" to exam.room,
            "syllabus" to exam.syllabus,
            "maxMarks" to exam.maxMarks,
            "targetMarks" to exam.targetMarks,
            "revisionProgress" to exam.revisionProgress
        )

        firebaseService.firestore.collection("exams")
            .document(exam.id)
            .update(map)
            .await()
    }

    override suspend fun deleteExam(id: String): Result<Unit> = runCatching {
        firebaseService.firestore.collection("exams")
            .document(id)
            .delete()
            .await()
    }
}
