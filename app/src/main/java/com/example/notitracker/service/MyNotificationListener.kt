package com.example.notitracker.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.notitracker.MainActivity
import com.example.notitracker.NotiTrackerApp
import com.example.notitracker.data.remote.NetworkResponse
import com.example.notitracker.data.remote.dto.NotificationDto
import com.example.notitracker.data.repository.NotificationRepository
import com.example.notitracker.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyNotificationListener : NotificationListenerService() {

    private val TAG = "MyNotificationListener"
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var batchJob: Job? = null
    private val notificationBuffer = mutableListOf<NotificationDto>()

    private var lastReplyAction: Notification.Action? = null
    private var lastResultKey: String? = null // Lưu key trả lời

    private lateinit var repository: NotificationRepository

    companion object {
        private const val BATCH_DELAY_MS = 30 * 1000L // Đổi thành 30 giây để test cho nhanh
    }

    override fun onCreate() {
        super.onCreate()
        repository = (application as NotiTrackerApp).networkGraph.notificationRepository
    }

    override fun onDestroy() {
        super.onDestroy()
        batchJob?.cancel()
        scope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // BƯỚC 1: Chỉ lọc bỏ thông báo "Smart Summary" của chính mình dựa trên ID (1001)
        // để tránh vòng lặp vô tận, nhưng vẫn cho phép thông báo TEST (ID khác) đi qua.
        if (packageName == this.packageName && sbn.id == 1001) return

        // BƯỚC 2: Lọc bỏ các thông báo hệ thống đang chạy ngầm
        if (sbn.isOngoing) return

        // Tìm nút Reply và lấy ResultKey (ví dụ: "reply_text" hoặc "result")
        val pair = findReplyActionAndKey(sbn.notification)
        if (pair != null) {
            lastReplyAction = pair.first
            lastResultKey = pair.second
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val pm = packageManager
        val appName = try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            "Unknown App"
        }

        val category = when {
            packageName.contains("zalo") || packageName.contains("orca") || packageName.contains("whatsapp") -> "chat"
            packageName.contains("gm") || packageName.contains("outlook") -> "email"
            packageName.contains("android.system") || packageName.contains("settings") -> "system"
            // Thêm các app tài chính
            packageName.contains("bank") || packageName.contains("momo") || packageName.contains("vnpay") -> "finance"
            // Thêm các app giao hàng
            packageName.contains("shopee") || packageName.contains("grab") || packageName.contains("tiki") || packageName.contains("delivery") -> "logistics"
            else -> "general"
        }

        val dto = NotificationDto(
            id = sbn.id.toString(),
            title = title,
            content = text,
            sender = title,
            packageName = packageName,
            timestamp = sbn.postTime,
            appName = appName,
            category = category
        )

        // Gửi broadcast để cập nhật UI trong MainActivity
        val intent = Intent(MainActivity.ACTION_NOTI_EVENT).apply {
            putExtra("title", title)
            putExtra("text", text)
        }
        sendBroadcast(intent)

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
        synchronized(notificationBuffer) {
            notificationBuffer.add(dto)
        }
        Log.d(TAG, "Added notification to buffer. Current size: ${notificationBuffer.size}")
        triggerBatchProcessing()
    }

    private fun triggerBatchProcessing() {
        if (batchJob?.isActive == true) return

        batchJob = scope.launch {
            Log.d(TAG, "Starting delay for batch processing ($BATCH_DELAY_MS ms)...")
            delay(BATCH_DELAY_MS)

            val currentBatch = synchronized(notificationBuffer) {
                val list = notificationBuffer.toList()
                notificationBuffer.clear()
                list
            }

            if (currentBatch.isEmpty()) return@launch

            Log.d(TAG, "Calling API to summarize ${currentBatch.size} notifications...")
            val response = repository.getSummary(currentBatch)
            when (response) {
                is NetworkResponse.Success -> {
                    Log.d(TAG, "Summary success: ${response.data.summary}")
                    NotificationHelper.showSmartNotification(
                        context = applicationContext,
                        summary = response.data.summary,
                        replies = response.data.suggestedReplies,
                        packageName = currentBatch.lastOrNull()?.packageName ?: "",
                        originalReplyIntent = lastReplyAction?.actionIntent,
                        resultKey = lastResultKey
                    )
                }
                is NetworkResponse.Error -> Log.e(TAG, "Summary failed: ${response.message}")
                else -> Log.d(TAG, "Summary loading or other state")
            }
        }
    }
}
