package com.example.notitracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.NotificationDto
import com.example.notitracker.data.repository.NotificationRepository
import com.example.notitracker.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyNotificationListener : NotificationListenerService() {

    private val TAG = "MyNotificationListener"
    private val scope = CoroutineScope(Dispatchers.Default)
    private var batchJob: Job? = null
    private val notificationBuffer = mutableListOf<NotificationDto>()

    // In a real app, use Dependency Injection to get the repository
    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        // For demonstration, but use DI in production
        // repository = ...
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        Log.d(TAG, "Notification received from: $packageName")

        if (packageName == "com.example.notitracker") {
            Log.d(TAG, "Ignoring notification from our own app")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Text: $text")

        val dto = NotificationDto(
            id = sbn.id.toString(),
            title = title,
            content = text,
            sender = title,
            packageName = packageName,
            timestamp = sbn.postTime
        )

        handleNotification(dto)
    }

    private fun handleNotification(dto: NotificationDto) {
        notificationBuffer.add(dto)
        Log.d(TAG, "Added to buffer. Current size: ${notificationBuffer.size}")
        triggerBatchProcessing()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "Notification removed from: ${sbn.packageName}")
    }

    private fun triggerBatchProcessing() {
        batchJob?.cancel()
        batchJob = scope.launch {
            Log.d(TAG, "Starting batch timer...")
            delay(5000) // Step F: Time Window (5 seconds)

            val currentBatch = notificationBuffer.toList()
            notificationBuffer.clear()
            Log.d(TAG, "Processing batch of ${currentBatch.size} notifications")

            // Step G: AI Processing via Repository
            if (::repository.isInitialized) {
                val response = repository.getSummary(currentBatch)
                if (response is NetworkResponse.Success) {
                    // Step H: Show Smart Notification
                    NotificationHelper.showSmartNotification(
                        context = applicationContext,
                        summary = response.data.summary,
                        replies = response.data.suggestedReplies,
                        packageName = currentBatch.firstOrNull()?.packageName ?: ""
                    )
                }
            } else {
                Log.w(TAG, "Repository not initialized, using simulation")
                // Simulation if repository not ready
                NotificationHelper.showSmartNotification(
                    context = applicationContext,
                    summary = "You have ${currentBatch.size} new messages. Summary: Busy discussion about the project.",
                    replies = listOf("Got it", "I'm busy", "Call you later"),
                    packageName = currentBatch.firstOrNull()?.packageName ?: ""
                )
            }
        }
    }
}
