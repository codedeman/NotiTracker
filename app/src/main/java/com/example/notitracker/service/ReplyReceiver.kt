package com.example.notitracker.service

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInputResults = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInputResults?.getCharSequence(KEY_TEXT_REPLY)?.toString()
            ?: intent.getStringExtra(EXTRA_SUGGESTED_REPLY)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val resultKey = intent.getStringExtra(EXTRA_RESULT_KEY) ?: "result" // Key từ app gốc

        val originalIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT)
        }

        if (replyText != null && originalIntent != null) {
            Log.d("ReplyReceiver", "Replying for $packageName using key $resultKey: $replyText")

            try {
                // Đóng gói tin nhắn với đúng ResultKey mà app gốc yêu cầu
                val replyBundle = Bundle().apply {
                    putCharSequence(resultKey, replyText)
                }

                val fillInIntent = Intent().apply {
                    RemoteInput.addResultsToIntent(
                        arrayOf(RemoteInput.Builder(resultKey).build()),
                        this,
                        replyBundle
                    )
                }

                // Thực hiện gửi trả lời thay cho app gốc
                originalIntent.send(context, 0, fillInIntent)
                Log.d("ReplyReceiver", "Reply sent successfully!")
            } catch (e: Exception) {
                Log.e("ReplyReceiver", "Error sending reply: ${e.message}")
            }
        } else {
            packageName?.let {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(it)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
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
