package com.example.notihub.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras

        val title = extras.getString("android.title")
        val text = extras.getCharSequence("android.text")
        val packageName = sbn.packageName

        Log.d("NOTI", "App: $packageName")
        Log.d("NOTI", "Title: $title")
        Log.d("NOTI", "Text: $text")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NOTI", "Notification removed")
    }
}