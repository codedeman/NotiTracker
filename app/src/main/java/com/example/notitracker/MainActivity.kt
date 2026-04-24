package com.example.notitracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notitracker.ui.theme.NotiTrackerTheme
import com.example.notitracker.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val messages = mutableStateListOf<String>()
    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val title = intent.getStringExtra("title") ?: ""
            val text = intent.getStringExtra("text") ?: ""
            messages.add("$title: $text")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isNotificationServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        enableEdgeToEdge()

        setContent {
            NotiTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                        
                        // Nút bấm để test thông báo
                        Button(
                            onClick = {
                                NotificationHelper.sendTestNotification(
                                    this@MainActivity,
                                    "Hello motherfucker",
                                    "Hello motherfucker"
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Text("Gửi Thông Báo Test")
                        }

                        Text("Danh sách thông báo bắt được:", modifier = Modifier.padding(bottom = 8.dp))
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(messages) { msg ->
                                Text(text = msg, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isRegistered) {
            val filter = IntentFilter("NOTI_EVENT")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            isRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRegistered) {
            unregisterReceiver(receiver)
            isRegistered = false
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
}
