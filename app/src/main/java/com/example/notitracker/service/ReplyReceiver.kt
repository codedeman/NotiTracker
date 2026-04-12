package com.example.notitracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.RemoteInput

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString() 
            ?: intent.getStringExtra(EXTRA_SUGGESTED_REPLY)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)

        if (replyText != null && packageName != null) {
            Log.d("ReplyReceiver", "Replying to $packageName with: $replyText")
            // In a real app, you would use PendingIntent from the original notification
            // to send the actual reply back to the target app.
        } else {
            // Fallback: Open target app
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
    }
}
