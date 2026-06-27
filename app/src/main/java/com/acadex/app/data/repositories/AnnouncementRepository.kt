package com.acadex.app.data.repositories

import com.acadex.app.data.models.Announcement
import kotlinx.coroutines.flow.Flow

interface AnnouncementRepository {
    fun getAnnouncementsFlow(): Flow<List<Announcement>>
    suspend fun createAnnouncement(announcement: Announcement): Result<Unit>
}
