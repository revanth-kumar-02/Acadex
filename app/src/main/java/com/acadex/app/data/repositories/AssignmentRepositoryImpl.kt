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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager,
    private val notificationRepository: NotificationRepository
) : AssignmentRepository {

    companion object {
        private const val SUPABASE_API_KEY = AppConstants.SUPABASE_API_KEY
        private const val SUPABASE_BASE_URL = AppConstants.SUPABASE_BASE_URL
    }

    private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    private fun triggerRefresh() {
        refreshTrigger.value = System.currentTimeMillis()
    }

    override fun getAssignmentsFlow(): Flow<List<Assignment>> = flow {
        var lastFetchedTime = 0L
        while (true) {
            val triggerTime = refreshTrigger.value
            val currentTime = System.currentTimeMillis()
            if (triggerTime > lastFetchedTime || currentTime - lastFetchedTime >= 30_000) {
                lastFetchedTime = currentTime
                val token = sessionManager.getAccessToken()
                if (token != null) {
                    runCatching {
                        supabaseApiService.getAssignments(SUPABASE_API_KEY, "Bearer $token", null)
                    }.onSuccess { dtos ->
                        emit(dtos.map { it.toDomain() })
                    }.onFailure { exception ->
                        Log.e("AssignmentRepository", "Failed to fetch assignments flow list", exception)
                        emit(emptyList())
                    }
                } else {
                    emit(emptyList())
                }
            }
            delay(1000)
        }
    }

    override suspend fun getAssignmentById(id: String): Assignment? {
        val token = sessionManager.getAccessToken() ?: return null
        return runCatching {
            supabaseApiService.getAssignments(SUPABASE_API_KEY, "Bearer $token", null)
                .firstOrNull { it.id == id }
                ?.toDomain()
        }.onFailure { exception ->
            Log.e("AssignmentRepository", "Failed to get assignment by ID: $id", exception)
        }.getOrNull()
    }

    override suspend fun createAssignment(
        assignment: Assignment,
        attachmentStream: InputStream?,
        attachmentName: String?
    ): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val authorName = sessionManager.getUserName() ?: "Student"
        
        val id = assignment.id.ifEmpty { UUID.randomUUID().toString() }
        var attachmentUrl = assignment.attachmentUrl

        if (attachmentStream != null && attachmentName != null) {
            val storagePath = "$userId/assignments/$id/$attachmentName"
            val requestBody = attachmentStream.readBytes()
                .toRequestBody("*/*".toMediaTypeOrNull())
            val uploadResponse = supabaseApiService.uploadFile(
                SUPABASE_API_KEY,
                "Bearer $token",
                "resources",
                storagePath,
                requestBody
            )
            attachmentUrl = "$SUPABASE_BASE_URL/storage/v1/object/public/${uploadResponse.key}"
        }

        val dto = AssignmentDto(
            id = id,
            userId = userId,
            title = assignment.title,
            subject = assignment.subject,
            description = assignment.description,
            dueDate = assignment.dueDate,
            priority = assignment.priority.name.lowercase(),
            status = assignment.status.name.lowercase(),
            broadcastTarget = assignment.broadcastTarget,
            postedBy = authorName,
            attachmentUrl = attachmentUrl,
            assignedDate = assignment.assignedDate
        )
        supabaseApiService.createAssignment(SUPABASE_API_KEY, "Bearer $token", dto)
        triggerRefresh()

        // Fire notification for broadcast target
        val target = assignment.broadcastTarget.ifBlank { "All" }
        notificationRepository.createNotificationsForBroadcast(
            title = "📋 New Assignment: ${assignment.title}",
            message = "${assignment.subject} - Due ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(assignment.dueDate))}",
            type = "assignment",
            broadcastTarget = target,
            createdBy = userId
        ).onFailure { Log.w("AssignmentRepository", "Failed to create broadcast notification", it) }
    }.onFailure { exception ->
        Log.e("AssignmentRepository", "Failed to create assignment", exception)
    }

    override suspend fun updateAssignment(
        assignment: Assignment,
        attachmentStream: InputStream?,
        attachmentName: String?
    ): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        
        var attachmentUrl = assignment.attachmentUrl

        if (attachmentStream != null && attachmentName != null) {
            val storagePath = "$userId/assignments/${assignment.id}/$attachmentName"
            val requestBody = attachmentStream.readBytes()
                .toRequestBody("*/*".toMediaTypeOrNull())
            val uploadResponse = supabaseApiService.uploadFile(
                SUPABASE_API_KEY,
                "Bearer $token",
                "resources",
                storagePath,
                requestBody
            )
            attachmentUrl = "$SUPABASE_BASE_URL/storage/v1/object/public/${uploadResponse.key}"
        }

        val updates = mapOf<String, Any>(
            "title" to assignment.title,
            "description" to assignment.description,
            "subject" to assignment.subject,
            "due_date" to assignment.dueDate,
            "priority" to assignment.priority.name.lowercase(),
            "status" to assignment.status.name.lowercase(),
            "broadcast_target" to assignment.broadcastTarget,
            "attachment_url" to (attachmentUrl ?: ""),
            "assigned_date" to assignment.assignedDate
        )
        supabaseApiService.updateAssignment(SUPABASE_API_KEY, "Bearer $token", "eq.${assignment.id}", updates)
        triggerRefresh()
    }.onFailure { exception ->
        Log.e("AssignmentRepository", "Failed to update assignment ID: ${assignment.id}", exception)
    }

    override suspend fun deleteAssignment(id: String): Result<Unit> = runCatching {
        val token = sessionManager.getAccessToken() ?: throw Exception("User not authenticated")
        supabaseApiService.deleteAssignment(SUPABASE_API_KEY, "Bearer $token", "eq.$id")
        triggerRefresh()
    }.onFailure { exception ->
        Log.e("AssignmentRepository", "Failed to delete assignment ID: $id", exception)
    }

    private fun AssignmentDto.toDomain() = Assignment(
        id = id ?: "",
        userId = userId,
        title = title,
        description = description,
        subject = subject,
        dueDate = dueDate,
        priority = runCatching { AssignmentPriority.valueOf(priority.uppercase()) }.getOrDefault(AssignmentPriority.MEDIUM),
        status = runCatching { AssignmentStatus.valueOf(status.uppercase()) }.getOrDefault(AssignmentStatus.PENDING),
        broadcastTarget = broadcastTarget ?: "Entire CSE Department",
        postedBy = postedBy ?: "Student",
        attachmentUrl = attachmentUrl,
        assignedDate = assignedDate ?: DateTimeUtils.parseIsoTimestamp(createdAt),
        createdAt = DateTimeUtils.parseIsoTimestamp(createdAt)
    )
}
