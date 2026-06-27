package com.acadex.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.acadex.app.features.notifications.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AcadexApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleReminderWorker()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                "acadex_notifications",
                "Acadex Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Academic updates - assignments, notes, announcements" },
            NotificationChannel(
                "acadex_reminders",
                "Assignment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Due date reminders for assignments and tasks" }
        )
        channels.forEach { notificationManager.createNotificationChannel(it) }
        Log.d("AcadexApp", "Notification channels created")
    }

    private fun scheduleReminderWorker() {
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            8, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
        Log.d("AcadexApp", "Reminder worker scheduled")
    }
}
