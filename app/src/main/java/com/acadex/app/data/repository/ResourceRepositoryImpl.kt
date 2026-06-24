package com.acadex.app.data.repository

import com.acadex.app.data.remote.FirebaseService
import com.acadex.app.domain.model.Resource
import com.acadex.app.domain.repository.ResourceRepository
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceRepositoryImpl @Inject constructor(
    private val firebaseService: FirebaseService
) : ResourceRepository {

    override fun getResourcesFlow(
        department: String,
        semester: String,
        category: String?
    ): Flow<List<Resource>> = callbackFlow {
        var query = firebaseService.firestore.collection("resources")
            .whereEqualTo("department", department)
            .whereEqualTo("semester", semester)

        if (category != null) {
            query = query.whereEqualTo("category", category)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.map { doc ->
                Resource(
                    id = doc.getString("id") ?: "",
                    title = doc.getString("title") ?: "",
                    category = doc.getString("category") ?: "",
                    department = doc.getString("department") ?: "",
                    semester = doc.getString("semester") ?: "",
                    subject = doc.getString("subject") ?: "",
                    fileUrl = doc.getString("fileUrl") ?: "",
                    fileName = doc.getString("fileName") ?: "",
                    downloadsCount = doc.getLong("downloadsCount")?.toInt() ?: 0,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } ?: emptyList()

            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun searchResources(query: String): List<Resource> {
        val snapshot = firebaseService.firestore.collection("resources")
            .get()
            .await()

        val list = snapshot.documents.map { doc ->
            Resource(
                id = doc.getString("id") ?: "",
                title = doc.getString("title") ?: "",
                category = doc.getString("category") ?: "",
                department = doc.getString("department") ?: "",
                semester = doc.getString("semester") ?: "",
                subject = doc.getString("subject") ?: "",
                fileUrl = doc.getString("fileUrl") ?: "",
                fileName = doc.getString("fileName") ?: "",
                downloadsCount = doc.getLong("downloadsCount")?.toInt() ?: 0,
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            )
        }

        // Standard local lowercase filtering
        val lowerQuery = query.lowercase()
        return list.filter {
            it.title.lowercase().contains(lowerQuery) ||
            it.subject.lowercase().contains(lowerQuery) ||
            it.category.lowercase().contains(lowerQuery)
        }
    }

    override suspend fun incrementDownloadCount(resourceId: String): Result<Unit> = runCatching {
        firebaseService.firestore.collection("resources")
            .document(resourceId)
            .update("downloadsCount", FieldValue.increment(1))
            .await()
    }

    override suspend fun createResource(resource: Resource): Result<Unit> = runCatching {
        val id = resource.id.ifEmpty { UUID.randomUUID().toString() }
        val map = mapOf(
            "id" to id,
            "title" to resource.title,
            "category" to resource.category,
            "department" to resource.department,
            "semester" to resource.semester,
            "subject" to resource.subject,
            "fileUrl" to resource.fileUrl,
            "fileName" to resource.fileName,
            "downloadsCount" to resource.downloadsCount,
            "createdAt" to resource.createdAt
        )

        firebaseService.firestore.collection("resources")
            .document(id)
            .set(map)
            .await()
    }
}
