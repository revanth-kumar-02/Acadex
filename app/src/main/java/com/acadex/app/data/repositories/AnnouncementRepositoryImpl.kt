package com.acadex.app.data.repositories

import android.util.Log
import com.acadex.app.data.models.*
import com.acadex.app.data.remote.SupabaseApiService
import com.acadex.app.core.auth.SessionManager
import com.acadex.app.core.utils.DateTimeUtils
import com.acadex.app.core.constants.AppConstants
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
    private val sessionManager: SessionManager,
    private val notificationRepository: NotificationRepository
) : AnnouncementRepository {

    companion object {
        private const val SUPABASE_API_KEY = AppConstants.SUPABASE_API_KEY
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
                    }.onFailure { exception ->
                        Log.e("AnnouncementRepository", "Failed to fetch announcements list flow", exception)
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

        // Fire broadcast notification
        notificationRepository.createNotificationsForBroadcast(
            title = "📢 ${announcement.title}",
            message = announcement.content.take(120),
            type = "announcement",
            broadcastTarget = announcement.broadcastTarget,
            createdBy = userId
        ).onFailure { Log.w("AnnouncementRepository", "Failed to create broadcast notification", it) }
        Unit
    }.onFailure { exception ->
        Log.e("AnnouncementRepository", "Failed to create announcement", exception)
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
