package com.acadex.app.data.repository

import com.acadex.app.data.remote.FirebaseService
import com.acadex.app.domain.model.PlannerTask
import com.acadex.app.domain.model.TaskType
import com.acadex.app.domain.repository.PlannerRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannerRepositoryImpl @Inject constructor(
    private val firebaseService: FirebaseService
) : PlannerRepository {

    override fun getTasksFlow(date: Long): Flow<List<PlannerTask>> = callbackFlow {
        val uid = firebaseService.currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // We filter in Compose or in Query. Filtering in Query requires matching exact date
        val listener = firebaseService.firestore.collection("planner_tasks")
            .whereEqualTo("userId", uid)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    PlannerTask(
                        id = doc.getString("id") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getLong("date") ?: 0L,
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        type = TaskType.valueOf(doc.getString("type") ?: "TASK"),
                        isCompleted = doc.getBoolean("isCompleted") ?: false
                    )
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override fun getAllTasksFlow(): Flow<List<PlannerTask>> = callbackFlow {
        val uid = firebaseService.currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firebaseService.firestore.collection("planner_tasks")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.map { doc ->
                    PlannerTask(
                        id = doc.getString("id") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        date = doc.getLong("date") ?: 0L,
                        startTime = doc.getString("startTime") ?: "",
                        endTime = doc.getString("endTime") ?: "",
                        type = TaskType.valueOf(doc.getString("type") ?: "TASK"),
                        isCompleted = doc.getBoolean("isCompleted") ?: false
                    )
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getTaskById(id: String): PlannerTask? {
        val doc = firebaseService.firestore.collection("planner_tasks").document(id).get().await()
        if (!doc.exists()) return null
        return PlannerTask(
            id = doc.getString("id") ?: "",
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            date = doc.getLong("date") ?: 0L,
            startTime = doc.getString("startTime") ?: "",
            endTime = doc.getString("endTime") ?: "",
            type = TaskType.valueOf(doc.getString("type") ?: "TASK"),
            isCompleted = doc.getBoolean("isCompleted") ?: false
        )
    }

    override suspend fun createTask(task: PlannerTask): Result<Unit> = runCatching {
        val uid = firebaseService.currentUserId ?: throw Exception("User not authenticated")
        val id = task.id.ifEmpty { UUID.randomUUID().toString() }

        val map = mapOf(
            "id" to id,
            "userId" to uid,
            "title" to task.title,
            "description" to task.description,
            "date" to task.date,
            "startTime" to task.startTime,
            "endTime" to task.endTime,
            "type" to task.type.name,
            "isCompleted" to task.isCompleted
        )

        firebaseService.firestore.collection("planner_tasks")
            .document(id)
            .set(map)
            .await()
    }

    override suspend fun updateTask(task: PlannerTask): Result<Unit> = runCatching {
        val map = mapOf(
            "title" to task.title,
            "description" to task.description,
            "date" to task.date,
            "startTime" to task.startTime,
            "endTime" to task.endTime,
            "type" to task.type.name,
            "isCompleted" to task.isCompleted
        )

        firebaseService.firestore.collection("planner_tasks")
            .document(task.id)
            .update(map)
            .await()
    }

    override suspend fun deleteTask(id: String): Result<Unit> = runCatching {
        firebaseService.firestore.collection("planner_tasks")
            .document(id)
            .delete()
            .await()
    }
}
