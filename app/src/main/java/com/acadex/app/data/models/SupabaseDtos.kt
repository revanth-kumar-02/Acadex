package com.acadex.app.data.models

import com.google.gson.annotations.SerializedName

// --- Auth Request/Response DTOs ---

data class SignUpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("data") val data: Map<String, String>
)

data class SignInRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RecoverRequest(
    @SerializedName("email") val email: String
)

data class RefreshSessionRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class SupabaseUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("user_metadata") val userMetadata: Map<String, String>?
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("user") val user: SupabaseUserDto
)

data class SignUpResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("user") val user: SupabaseUserDto?,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

// --- Database DTOs ---

data class UserProfileDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("register_number") val registerNumber: String?,
    @SerializedName("department") val department: String?,
    @SerializedName("semester") val semester: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class NoteDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("broadcast_target") val broadcastTarget: String? = null,
    @SerializedName("user_id") val uploadedBy: String? = null,
    @SerializedName("uploaded_by_name") val uploadedByName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AssignmentDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("description") val description: String,
    @SerializedName("due_date") val dueDate: Long,
    @SerializedName("priority") val priority: String,
    @SerializedName("status") val status: String,
    @SerializedName("broadcast_target") val broadcastTarget: String? = null,
    @SerializedName("posted_by") val postedBy: String? = null,
    @SerializedName("attachment_url") val attachmentUrl: String? = null,
    @SerializedName("assigned_date") val assignedDate: Long? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class PlannerTaskDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("date") val date: Long,
    @SerializedName("is_completed") val completed: Boolean,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AnnouncementDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("broadcast_target") val broadcastTarget: String,
    @SerializedName("author_name") val authorName: String,
    @SerializedName("created_at") val createdAt: String? = null
)

// --- Storage DTOs ---

data class StorageUploadResponse(
    @SerializedName("Key") val key: String
)

// --- Notification DTOs ---

data class NotificationDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("broadcast_target") val broadcastTarget: String? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false
)

data class NotificationPreferencesDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("assignments_enabled") val assignmentsEnabled: Boolean = true,
    @SerializedName("notes_enabled") val notesEnabled: Boolean = true,
    @SerializedName("announcements_enabled") val announcementsEnabled: Boolean = true,
    @SerializedName("reminders_enabled") val remindersEnabled: Boolean = true
)
