package com.acadex.app.data.repository

import com.acadex.app.data.remote.FirebaseService
import com.acadex.app.domain.model.Assignment
import com.acadex.app.domain.model.AssignmentPriority
import com.acadex.app.domain.model.AssignmentStatus
import com.acadex.app.domain.repository.AssignmentRepository
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val firebaseService: FirebaseService
) : AssignmentRepository {

    override fun getAssignmentsFlow(): Flow<List<Assignment>> = callbackFlow {
        val uid = firebaseService.currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firebaseService.firestore.collection("assignments")
            .whereEqualTo("userId", uid)
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    Assignment(
                        id = doc.getString("id") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        subject = doc.getString("subject") ?: "",
                        dueDate = doc.getLong("dueDate") ?: 0L,
                        priority = AssignmentPriority.valueOf(doc.getString("priority") ?: "MEDIUM"),
                        status = AssignmentStatus.valueOf(doc.getString("status") ?: "PENDING"),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getAssignmentById(id: String): Assignment? {
        val doc = firebaseService.firestore.collection("assignments").document(id).get().await()
        if (!doc.exists()) return null
        return Assignment(
            id = doc.getString("id") ?: "",
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            subject = doc.getString("subject") ?: "",
            dueDate = doc.getLong("dueDate") ?: 0L,
            priority = AssignmentPriority.valueOf(doc.getString("priority") ?: "MEDIUM"),
            status = AssignmentStatus.valueOf(doc.getString("status") ?: "PENDING"),
            createdAt = doc.getLong("createdAt") ?: 0L
        )
    }

    override suspend fun createAssignment(assignment: Assignment): Result<Unit> = runCatching {
        val uid = firebaseService.currentUserId ?: throw Exception("User not authenticated")
        val id = assignment.id.ifEmpty { UUID.randomUUID().toString() }
        
        val map = mapOf(
            "id" to id,
            "userId" to uid,
            "title" to assignment.title,
            "description" to assignment.description,
            "subject" to assignment.subject,
            "dueDate" to assignment.dueDate,
            "priority" to assignment.priority.name,
            "status" to assignment.status.name,
            "createdAt" to assignment.createdAt
        )

        firebaseService.firestore.collection("assignments")
            .document(id)
            .set(map)
            .await()
    }

    override suspend fun updateAssignment(assignment: Assignment): Result<Unit> = runCatching {
        val map = mapOf(
            "title" to assignment.title,
            "description" to assignment.description,
            "subject" to assignment.subject,
            "dueDate" to assignment.dueDate,
            "priority" to assignment.priority.name,
            "status" to assignment.status.name
        )

        firebaseService.firestore.collection("assignments")
            .document(assignment.id)
            .update(map)
            .await()
    }

    override suspend fun deleteAssignment(id: String): Result<Unit> = runCatching {
        firebaseService.firestore.collection("assignments")
            .document(id)
            .delete()
            .await()
    }
}
