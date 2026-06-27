package com.acadex.app.data.models

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.ANNOUNCEMENT,
    val broadcastTarget: String? = null,
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

enum class NotificationType(val value: String) {
    ASSIGNMENT("assignment"),
    NOTES("notes"),
    ANNOUNCEMENT("announcement"),
    REMINDER("reminder");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: ANNOUNCEMENT
    }
}

data class NotificationPreferences(
    val userId: String = "",
    val assignmentsEnabled: Boolean = true,
    val notesEnabled: Boolean = true,
    val announcementsEnabled: Boolean = true,
    val remindersEnabled: Boolean = true
)
