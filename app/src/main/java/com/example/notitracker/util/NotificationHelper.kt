package com.example.notitracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.notitracker.service.ReplyReceiver

object NotificationHelper {
    private const val CHANNEL_ID = "smart_notifications"
    private const val NOTIFICATION_ID = 1001

    fun showSmartNotification(
        context: Context,
        summary: String,
        replies: List<String>,
        packageName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Smart Summary")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Add Suggested Replies as Actions
        replies.forEachIndexed { index, reply ->
            val intent = Intent(context, ReplyReceiver::class.java).apply {
                putExtra(ReplyReceiver.EXTRA_SUGGESTED_REPLY, reply)
                putExtra(ReplyReceiver.EXTRA_PACKAGE_NAME, packageName)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val action = NotificationCompat.Action.Builder(
                0,
                reply,
                pendingIntent
            ).build()
            
            builder.addAction(action)
        }

        // Add Inline Reply Action
        val remoteInput = RemoteInput.Builder(ReplyReceiver.KEY_TEXT_REPLY)
            .setLabel("Type your reply...")
            .build()

        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            putExtra(ReplyReceiver.EXTRA_PACKAGE_NAME, packageName)
        }
        
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            200,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val inlineAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        builder.addAction(inlineAction)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
