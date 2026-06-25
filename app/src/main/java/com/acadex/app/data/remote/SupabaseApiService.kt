package com.acadex.app.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import retrofit2.http.*

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

// NoteDto maps to public.resources table in V2
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

// --- Retrofit Endpoint Definitions ---

interface SupabaseApiService {

    // --- Authentication ---
    
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Query("redirect_to") redirectTo: String,
        @Body body: SignUpRequest
    ): SignUpResponse

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body body: SignInRequest
    ): AuthResponse

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Query("redirect_to") redirectTo: String,
        @Body body: RecoverRequest
    )

    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): SupabaseUserDto

    // --- Profiles (public.users) ---
    
    @GET("rest/v1/users")
    suspend fun getUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): List<UserProfileDto>

    @PATCH("rest/v1/users")
    suspend fun updateUserProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, String>
    ): List<UserProfileDto>

    // --- Notes/Shared Resources (public.resources) ---
    
    @GET("rest/v1/resources")
    suspend fun getNotes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): List<NoteDto>

    @POST("rest/v1/resources")
    suspend fun createNote(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: NoteDto
    ): List<NoteDto>

    @DELETE("rest/v1/resources")
    suspend fun deleteNote(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    )

    // --- Assignments (public.assignments) ---
    
    @GET("rest/v1/assignments")
    suspend fun getAssignments(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String? = null
    ): List<AssignmentDto>

    @POST("rest/v1/assignments")
    suspend fun createAssignment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: AssignmentDto
    ): List<AssignmentDto>

    @PATCH("rest/v1/assignments")
    suspend fun updateAssignment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Body body: Map<String, Any>
    ): List<AssignmentDto>

    @DELETE("rest/v1/assignments")
    suspend fun deleteAssignment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    )

    // --- Planner Tasks (public.planner_tasks) ---
    
    @GET("rest/v1/planner_tasks")
    suspend fun getPlannerTasks(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String
    ): List<PlannerTaskDto>

    @POST("rest/v1/planner_tasks")
    suspend fun createPlannerTask(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: PlannerTaskDto
    ): List<PlannerTaskDto>

    @PATCH("rest/v1/planner_tasks")
    suspend fun updatePlannerTask(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Body body: Map<String, Any>
    ): List<PlannerTaskDto>

    @DELETE("rest/v1/planner_tasks")
    suspend fun deletePlannerTask(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    )

    // --- Announcements (public.announcements) ---
    
    @GET("rest/v1/announcements")
    suspend fun getAnnouncements(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): List<AnnouncementDto>

    @POST("rest/v1/announcements")
    suspend fun createAnnouncement(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: AnnouncementDto
    ): List<AnnouncementDto>

    // --- Storage Upload ---
    
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadFile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Path("bucket") bucket: String,
        @Path("path") path: String,
        @Body body: RequestBody
    ): StorageUploadResponse

    @DELETE("storage/v1/object/{bucket}/{path}")
    suspend fun deleteFile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Path("bucket") bucket: String,
        @Path("path") path: String
    ): Unit
}
