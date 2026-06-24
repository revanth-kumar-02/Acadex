package com.acadex.app.domain.usecase

import com.acadex.app.domain.model.Resource
import com.acadex.app.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetResourcesUseCase @Inject constructor(private val repository: ResourceRepository) {
    operator fun invoke(department: String, semester: String, category: String?): Flow<List<Resource>> =
        repository.getResourcesFlow(department, semester, category)
}

class SearchResourcesUseCase @Inject constructor(private val repository: ResourceRepository) {
    suspend operator fun invoke(query: String): List<Resource> =
        repository.searchResources(query)
}

class IncrementDownloadCountUseCase @Inject constructor(private val repository: ResourceRepository) {
    suspend operator fun invoke(resourceId: String): Result<Unit> =
        repository.incrementDownloadCount(resourceId)
}

class CreateResourceUseCase @Inject constructor(private val repository: ResourceRepository) {
    suspend operator fun invoke(resource: Resource): Result<Unit> =
        repository.createResource(resource)
}

data class ResourceUseCases @Inject constructor(
    val getResources: GetResourcesUseCase,
    val searchResources: SearchResourcesUseCase,
    val incrementDownloadCount: IncrementDownloadCountUseCase,
    val createResource: CreateResourceUseCase
)
