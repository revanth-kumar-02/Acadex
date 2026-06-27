package com.acadex.app.features.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.data.models.AppNotification
import com.acadex.app.data.models.NotificationPreferences
import com.acadex.app.data.models.NotificationType
import com.acadex.app.data.repositories.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class GroupedNotifications(
    val today: List<AppNotification> = emptyList(),
    val yesterday: List<AppNotification> = emptyList(),
    val earlier: List<AppNotification> = emptyList()
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val notificationsState: StateFlow<List<AppNotification>> =
        notificationRepository.getNotificationsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> =
        notificationRepository.getUnreadCountFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val groupedNotifications: StateFlow<GroupedNotifications> =
        notificationsState.map { notifications ->
            groupByDate(notifications)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupedNotifications())

    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPreferences()
        viewModelScope.launch {
            notificationRepository.startRealtimeSync()
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            notificationRepository.getPreferences()
                .onSuccess { _preferences.value = it }
                .onFailure { Log.e(TAG, "Failed to load preferences", it) }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
                .onFailure { Log.e(TAG, "Failed to mark notification read", it) }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
                .onFailure { Log.e(TAG, "Failed to mark all read", it) }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onFailure { Log.e(TAG, "Failed to delete notification", it) }
        }
    }

    fun updatePreferences(prefs: NotificationPreferences) {
        _preferences.value = prefs
        viewModelScope.launch {
            _isLoading.value = true
            notificationRepository.savePreferences(prefs)
                .onSuccess { Log.i(TAG, "Preferences saved") }
                .onFailure {
                    _errorMessage.value = "Failed to save preferences: ${it.message}"
                    Log.e(TAG, "Failed to save preferences", it)
                }
            _isLoading.value = false
        }
    }

    fun clearError() { _errorMessage.value = null }

    private fun groupByDate(notifications: List<AppNotification>): GroupedNotifications {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        val todayStart = cal.apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayStart = todayStart - 86_400_000L

        val today = notifications.filter { it.createdAt >= todayStart }
        val yesterday = notifications.filter { it.createdAt in yesterdayStart until todayStart }
        val earlier = notifications.filter { it.createdAt < yesterdayStart }

        return GroupedNotifications(today, yesterday, earlier)
    }

    companion object {
        private const val TAG = "NotificationsViewModel"
    }
}
