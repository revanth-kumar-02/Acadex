package com.acadex.app.data.remote

import com.acadex.app.data.models.*
import okhttp3.RequestBody
import retrofit2.http.*

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

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshSession(
        @Header("apikey") apiKey: String,
        @Body body: RefreshSessionRequest
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
