package com.acadex.app.data.repositories

import com.acadex.app.data.models.AppNotification
import com.acadex.app.data.models.NotificationPreferences
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotificationsFlow(): Flow<List<AppNotification>>
    fun getUnreadCountFlow(): Flow<Int>
    suspend fun loadNotifications(): Result<Unit>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    suspend fun createNotificationsForBroadcast(
        title: String,
        message: String,
        type: String,
        broadcastTarget: String,
        createdBy: String
    ): Result<Unit>
    suspend fun getPreferences(): Result<NotificationPreferences>
    suspend fun savePreferences(prefs: NotificationPreferences): Result<Unit>
    suspend fun startRealtimeSync()
    fun stopRealtimeSync()
}
