package com.acadex.app.data.repository

import com.acadex.app.data.remote.AnnouncementDto
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.domain.model.Announcement
import com.acadex.app.domain.repository.AnnouncementRepository
import com.acadex.app.utils.DateTimeUtils
import com.acadex.app.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : AnnouncementRepository {

    companion object {
        private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9tYnB6cmN0ZnNxbHBheGJ2amhhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyOTc1MzksImV4cCI6MjA5Nzg3MzUzOX0.jVFqmzTk-E-64PJrPgSZ3cpZDvHBk00vbRXtW1bTGSs"
    }

    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private fun triggerRefresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    override fun getAnnouncementsFlow(): Flow<List<Announcement>> = flow {
        var lastFetchedTime = 0L
        while (true) {
            val triggerTime = refreshTrigger.value
            val currentTime = System.currentTimeMillis()
            if (triggerTime > lastFetchedTime || currentTime - lastFetchedTime >= 30_000) {
                lastFetchedTime = currentTime
                val token = sessionManager.getAccessToken()
                if (token != null) {
                    runCatching {
                        supabaseApiService.getAnnouncements(SUPABASE_API_KEY, "Bearer $token")
                    }.onSuccess { dtos ->
                        emit(dtos.map { it.toDomain() })
                    }.onFailure {
                        emit(emptyList())
                    }
                } else {
                    emit(emptyList())
                }
            }
            delay(1000)
        }
    }

    override suspend fun createAnnouncement(announcement: Announcement): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val id = announcement.id.ifEmpty { UUID.randomUUID().toString() }

        val dto = AnnouncementDto(
            id = id,
            userId = userId,
            title = announcement.title,
            content = announcement.content,
            broadcastTarget = announcement.broadcastTarget,
            authorName = announcement.authorName
        )
        supabaseApiService.createAnnouncement(SUPABASE_API_KEY, "Bearer $token", dto)
        triggerRefresh()
    }

    private fun AnnouncementDto.toDomain() = Announcement(
        id = id ?: "",
        userId = userId,
        title = title,
        content = content,
        broadcastTarget = broadcastTarget,
        authorName = authorName,
        createdAt = DateTimeUtils.parseIsoTimestamp(createdAt)
    )
}
