package com.acadex.app.domain.repository

import com.acadex.app.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface ResourceRepository {
    fun getResourcesFlow(department: String, semester: String, category: String?): Flow<List<Resource>>
    suspend fun searchResources(query: String): List<Resource>
    suspend fun incrementDownloadCount(resourceId: String): Result<Unit>
    suspend fun createResource(resource: Resource): Result<Unit>
}
