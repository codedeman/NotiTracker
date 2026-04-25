package com.example.notitracker.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReplyReceiver", "--- Bắt đầu xử lý trả lời ---")
        
        // 1. Lấy nội dung trả lời (từ ô nhập liệu hoặc nút gợi ý)
        val remoteInputResults = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInputResults?.getCharSequence(KEY_TEXT_REPLY)?.toString()
            ?: intent.getStringExtra(EXTRA_SUGGESTED_REPLY)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val resultKey = intent.getStringExtra(EXTRA_RESULT_KEY)

        Log.d("ReplyReceiver", "App: $packageName | Key: $resultKey | Content: $replyText")

        val originalIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT)
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (replyText != null && originalIntent != null && resultKey != null) {
            try {
                // 2. Đóng gói kết quả theo chuẩn AndroidX (tương thích tốt nhất với SMS/Zalo)
                val results = Bundle().apply {
                    putCharSequence(resultKey, replyText)
                }
                
                val fillInIntent = Intent()
                RemoteInput.addResultsToIntent(
                    arrayOf(RemoteInput.Builder(resultKey).build()),
                    fillInIntent,
                    results
                )

                // 3. Gửi tín hiệu trả lời đến ứng dụng gốc
                Log.d("ReplyReceiver", "Đang gửi tín hiệu qua PendingIntent...")
                originalIntent.send(context, 0, fillInIntent)
                
                // 4. Cập nhật UI để dừng vòng xoay "quay lòng vòng"
                val repliedNotification = NotificationCompat.Builder(context, "smart_notifications")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Smart Summary")
                    .setContentText("Đã gửi trả lời: $replyText")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .build()
                
                notificationManager.notify(1001, repliedNotification)
                Log.d("ReplyReceiver", "Gửi thành công, đã cập nhật thông báo.")
                
                // Tự động xóa thông báo sau 1.5 giây cho sạch màn hình
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    notificationManager.cancel(1001)
                }, 1500)

            } catch (e: Exception) {
                Log.e("ReplyReceiver", "Lỗi khi gửi trả lời: ${e.message}", e)
                notificationManager.cancel(1001)
            }
        } else {
            Log.w("ReplyReceiver", "Thiếu dữ liệu: Text=$replyText, Intent=${originalIntent != null}, Key=$resultKey")
            notificationManager.cancel(1001)
        }
    }

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_SUGGESTED_REPLY = "extra_suggested_reply"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_ORIGINAL_INTENT = "extra_original_intent"
        const val EXTRA_RESULT_KEY = "extra_result_key"
    }
}
