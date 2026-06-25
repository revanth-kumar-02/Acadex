package com.acadex.app.domain.repository

import com.acadex.app.domain.model.Announcement
import kotlinx.coroutines.flow.Flow

interface AnnouncementRepository {
    fun getAnnouncementsFlow(): Flow<List<Announcement>>
    suspend fun createAnnouncement(announcement: Announcement): Result<Unit>
}
