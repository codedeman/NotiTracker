package com.example.notitracker.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.notitracker.NotiTrackerApp
import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.NotificationDto
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
    
    private var lastReplyAction: Notification.Action? = null
    private var lastResultKey: String? = null // Lưu key trả lời

    private lateinit var repository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        repository = (application as NotiTrackerApp).networkGraph.notificationRepository
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        // Tìm nút Reply và lấy ResultKey (ví dụ: "reply_text" hoặc "result")
        val pair = findReplyActionAndKey(sbn.notification)
        if (pair != null) {
            lastReplyAction = pair.first
            lastResultKey = pair.second
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

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

    private fun findReplyActionAndKey(notification: Notification): Pair<Notification.Action, String>? {
        for (i in 0 until (notification.actions?.size ?: 0)) {
            val action = notification.actions[i]
            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                val resultKey = action.remoteInputs[0].resultKey
                return Pair(action, resultKey)
            }
        }
        return null
    }

    private fun handleNotification(dto: NotificationDto) {
        notificationBuffer.add(dto)
        triggerBatchProcessing()
    }

    private fun triggerBatchProcessing() {
        batchJob?.cancel()
        batchJob = scope.launch {
            delay(5000) 

            val currentBatch = notificationBuffer.toList()
            notificationBuffer.clear()

            val response = repository.getSummary(currentBatch)
            when (response) {
                is NetworkResponse.Success -> {
                    NotificationHelper.showSmartNotification(
                        context = applicationContext,
                        summary = response.data.summary,
                        replies = response.data.suggestedReplies,
                        packageName = currentBatch.lastOrNull()?.packageName ?: "",
                        originalReplyIntent = lastReplyAction?.actionIntent,
                        resultKey = lastResultKey // Truyền key xuống
                    )
                }
                is NetworkResponse.Error -> Log.w(TAG, "Summary failed: ${response.message}")
                else -> Unit
            }
        }
    }
}
