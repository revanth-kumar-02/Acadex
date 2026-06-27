package com.acadex.app.features.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.acadex.app.MainActivity
import com.acadex.app.core.auth.SessionManager
import com.acadex.app.core.constants.AppConstants
import com.acadex.app.data.models.NotificationDto
import com.acadex.app.data.remote.SupabaseApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val supabaseApiService: SupabaseApiService,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "ReminderWorker triggered")
        val token = sessionManager.getAccessToken() ?: return Result.success()
        val userId = sessionManager.getUserId() ?: return Result.success()

        return try {
            // Fetch assignments for the current user
            val assignments = supabaseApiService.getAssignments(
                AppConstants.SUPABASE_API_KEY, "Bearer $token", "eq.$userId"
            )

            val now = System.currentTimeMillis()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + 86_400_000L
            val tomorrowEnd = todayEnd + 86_400_000L

            val reminders = mutableListOf<NotificationDto>()

            for (assignment in assignments) {
                if (assignment.status == "completed") continue
                val dueDate = assignment.dueDate
                val reminderTitle: String?
                val reminderMessage: String?

                when {
                    dueDate in todayStart until todayEnd -> {
                        reminderTitle = "⏰ Due Today: ${assignment.title}"
                        reminderMessage = "This assignment is due today!"
                    }
                    dueDate in todayEnd until tomorrowEnd -> {
                        reminderTitle = "📅 Due Tomorrow: ${assignment.title}"
                        reminderMessage = "This assignment is due tomorrow."
                    }
                    else -> continue
                }

                reminders.add(
                    NotificationDto(
                        userId = userId,
                        title = reminderTitle,
                        message = reminderMessage,
                        type = "reminder",
                        createdBy = userId
                    )
                )
                showLocalReminderNotification(reminderTitle, reminderMessage)
            }

            if (reminders.isNotEmpty()) {
                supabaseApiService.createNotifications(
                    AppConstants.SUPABASE_API_KEY, "Bearer $token", body = reminders
                )
                Log.i(TAG, "Created ${reminders.size} reminder notifications")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ReminderWorker failed", e)
            Result.retry()
        }
    }

    private fun showLocalReminderNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID, "Acadex Reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Assignment due date reminders" }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(("reminder_$title").hashCode(), notification)
    }

    companion object {
        private const val TAG = "ReminderWorker"
        private const val CHANNEL_ID = "acadex_reminders"
        const val WORK_NAME = "acadex_reminder_worker"
    }
}
