package com.acadex.app.data.repositories

import android.util.Log
import com.acadex.app.core.auth.SessionManager
import com.acadex.app.core.constants.AppConstants
import com.acadex.app.data.models.*
import com.acadex.app.data.remote.SupabaseApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : NotificationRepository {

    companion object {
        private const val TAG = "NotificationRepository"
        private const val SUPABASE_API_KEY = AppConstants.SUPABASE_API_KEY
    }

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private fun triggerRefresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    override fun getNotificationsFlow(): Flow<List<AppNotification>> = flow {
        var lastFetchedTime = 0L
        while (true) {
            val triggerTime = refreshTrigger.value
            val currentTime = System.currentTimeMillis()
            if (triggerTime > lastFetchedTime || currentTime - lastFetchedTime >= 30_000) {
                lastFetchedTime = currentTime
                val token = sessionManager.getAccessToken()
                val userId = sessionManager.getUserId()
                if (token != null && userId != null) {
                    runCatching {
                        supabaseApiService.getNotifications(
                            SUPABASE_API_KEY,
                            "Bearer $token",
                            "eq.$userId"
                        )
                    }.onSuccess { dtos ->
                        val mapped = dtos.map { it.toDomain() }
                        _notifications.value = mapped
                        emit(mapped)
                    }.onFailure { exception ->
                        Log.e(TAG, "Failed to fetch notifications", exception)
                        emit(_notifications.value)
                    }
                } else {
                    emit(emptyList())
                }
            }
            delay(1000)
        }
    }

    override fun getUnreadCountFlow(): Flow<Int> = flow {
        while (true) {
            emit(_notifications.value.count { !it.isRead })
            delay(1000)
        }
    }

    override suspend fun loadNotifications(): Result<Unit> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        val userId = sessionManager.getUserId() ?: return Result.failure(Exception("No user ID"))
        return runCatching {
            val dtos = supabaseApiService.getNotifications(
                SUPABASE_API_KEY,
                "Bearer $token",
                "eq.$userId"
            )
            _notifications.value = dtos.map { it.toDomain() }
            triggerRefresh()
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        return runCatching {
            supabaseApiService.markNotificationRead(
                SUPABASE_API_KEY,
                "Bearer $token",
                "eq.$notificationId",
                mapOf("is_read" to true)
            )
            _notifications.value = _notifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
        }
    }

    override suspend fun markAllAsRead(): Result<Unit> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        val userId = sessionManager.getUserId() ?: return Result.failure(Exception("No user ID"))
        return runCatching {
            supabaseApiService.markAllNotificationsRead(
                SUPABASE_API_KEY,
                "Bearer $token",
                "eq.$userId",
                mapOf("is_read" to true)
            )
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        return runCatching {
            supabaseApiService.deleteNotification(
                SUPABASE_API_KEY,
                "Bearer $token",
                "eq.$notificationId"
            )
            _notifications.value = _notifications.value.filter { it.id != notificationId }
        }
    }

    override suspend fun createNotificationsForBroadcast(
        title: String,
        message: String,
        type: String,
        broadcastTarget: String,
        createdBy: String
    ): Result<Unit> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        return runCatching {
            // Fetch all users to fan-out notifications based on target matching
            val allUsers = supabaseApiService.getAllUsers(SUPABASE_API_KEY, "Bearer $token")
            val targetUserIds = allUsers.filter { user ->
                val dept = user.department ?: ""
                val sem = user.semester?.toString() ?: ""
                isTargetMatched(dept, sem, broadcastTarget)
            }.map { it.id }

            if (targetUserIds.isEmpty()) {
                Log.w(TAG, "No users matched broadcast target: $broadcastTarget")
                return@runCatching
            }

            val notificationDtos = targetUserIds.map { userId ->
                NotificationDto(
                    userId = userId,
                    title = title,
                    message = message,
                    type = type,
                    broadcastTarget = broadcastTarget,
                    createdBy = createdBy
                )
            }

            supabaseApiService.createNotifications(SUPABASE_API_KEY, "Bearer $token", body = notificationDtos)
            triggerRefresh()
            Log.i(TAG, "Created ${notificationDtos.size} notifications for target: $broadcastTarget")
        }
    }

    override suspend fun getPreferences(): Result<NotificationPreferences> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        val userId = sessionManager.getUserId() ?: return Result.failure(Exception("No user ID"))
        return runCatching {
            val dtos = supabaseApiService.getNotificationPreferences(
                SUPABASE_API_KEY, "Bearer $token", "eq.$userId"
            )
            dtos.firstOrNull()?.toDomain() ?: NotificationPreferences(userId = userId)
        }
    }

    override suspend fun savePreferences(prefs: NotificationPreferences): Result<Unit> {
        val token = sessionManager.getAccessToken() ?: return Result.failure(Exception("Not logged in"))
        val userId = sessionManager.getUserId() ?: return Result.failure(Exception("No user ID"))
        return runCatching {
            supabaseApiService.upsertNotificationPreferences(
                SUPABASE_API_KEY, "Bearer $token",
                body = NotificationPreferencesDto(
                    userId = userId,
                    assignmentsEnabled = prefs.assignmentsEnabled,
                    notesEnabled = prefs.notesEnabled,
                    announcementsEnabled = prefs.announcementsEnabled,
                    remindersEnabled = prefs.remindersEnabled
                )
            )
        }
    }


    override suspend fun startRealtimeSync() {
        // Realtime polling already handled by the Flow with 30s refresh.
        // Full Supabase Realtime via WebSocket would need the Supabase Kotlin SDK.
        triggerRefresh()
    }

    override fun stopRealtimeSync() {
        // No-op for polling approach
    }

    // --- DTO Mappers ---

    private fun NotificationDto.toDomain(): AppNotification {
        val parsedTime = parseIsoTimestamp(createdAt)
        return AppNotification(
            id = id ?: UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            message = message,
            type = NotificationType.fromValue(type),
            broadcastTarget = broadcastTarget,
            createdBy = createdBy,
            createdAt = parsedTime,
            isRead = isRead
        )
    }

    private fun NotificationPreferencesDto.toDomain() = NotificationPreferences(
        userId = userId,
        assignmentsEnabled = assignmentsEnabled,
        notesEnabled = notesEnabled,
        announcementsEnabled = announcementsEnabled,
        remindersEnabled = remindersEnabled
    )

    private fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(isoString.take(19))?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun isTargetMatched(userDept: String, userSem: String, target: String): Boolean {
        if (target.isBlank()) return true
        val cleaned = target.trim()
        if (cleaned.contains("All", ignoreCase = true)) return true
        if (!cleaned.contains(userDept, ignoreCase = true)) return false
        val yearIndex = cleaned.indexOf(" Year ", ignoreCase = true)
        if (yearIndex == -1) return true  // dept match only
        val yearStr = cleaned.substring(yearIndex + 6).trim()
        val targetYear = yearStr.toIntOrNull() ?: return true
        val semDigit = userSem.firstOrNull { it.isDigit() }?.digitToIntOrNull() ?: return true
        val userYear = if (userSem.contains("sem", ignoreCase = true)) (semDigit + 1) / 2 else semDigit
        return userYear == targetYear
    }
}
