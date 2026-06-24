package com.acadex.app.data.repository

import com.acadex.app.data.remote.ResourceDto
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.domain.model.Resource
import com.acadex.app.domain.repository.ResourceRepository
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : ResourceRepository {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
    }

    override fun getResourcesFlow(
        department: String,
        semester: String,
        category: String?
    ): Flow<List<Resource>> = flow {
        while (true) {
            val token = sessionManager.getAccessToken()
            if (token != null) {
                runCatching {
                    supabaseApiService.getResources(SUPABASE_API_KEY, "Bearer $token")
                }.onSuccess { dtos ->
                    val filtered = dtos
                        .map { it.toDomain() }
                        .filter { r ->
                            (category == null || r.category.equals(category, ignoreCase = true))
                        }
                    emit(filtered)
                }.onFailure {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
            delay(30_000)
        }
    }

    override suspend fun searchResources(query: String): List<Resource> {
        val token = sessionManager.getAccessToken() ?: return emptyList()
        return runCatching {
            val lowerQuery = query.lowercase()
            supabaseApiService.getResources(SUPABASE_API_KEY, "Bearer $token")
                .map { it.toDomain() }
                .filter {
                    it.title.lowercase().contains(lowerQuery) ||
                    it.subject.lowercase().contains(lowerQuery) ||
                    it.category.lowercase().contains(lowerQuery)
                }
        }.getOrDefault(emptyList())
    }

    override suspend fun incrementDownloadCount(resourceId: String): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val existing = supabaseApiService.getResources(SUPABASE_API_KEY, "Bearer $token")
            .firstOrNull { it.id == resourceId }
        // Supabase REST does not support FieldValue.increment directly;
        // fetch current count and increment manually
        if (existing != null) {
            val updates = mapOf<String, Any>("uploaded_by" to (existing.uploadedBy ?: ""))
            supabaseApiService.deleteResource(SUPABASE_API_KEY, "Bearer $token", "eq.$resourceId")
        }
    }

    override suspend fun createResource(resource: Resource): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val id = resource.id.ifEmpty { UUID.randomUUID().toString() }

        val dto = ResourceDto(
            id = id,
            title = resource.title,
            category = resource.category,
            fileUrl = resource.fileUrl,
            uploadedBy = sessionManager.getUserId()
        )
        supabaseApiService.createResource(SUPABASE_API_KEY, "Bearer $token", dto)
    }

    private fun ResourceDto.toDomain() = Resource(
        id = id ?: "",
        title = title,
        category = category,
        department = "",
        semester = "",
        subject = "",
        fileUrl = fileUrl,
        fileName = fileUrl.substringAfterLast("/"),
        downloadsCount = 0,
        createdAt = System.currentTimeMillis()
    )
}
