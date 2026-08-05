package com.notification.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.notification.app.R
import com.notification.app.service.AlarmService
import com.notification.app.ui.screens.AlarmRingingActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Notification Alert"
        val note = intent.getStringExtra("EXTRA_NOTE") ?: ""
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "REMINDER"
        val ringtoneUri = intent.getStringExtra("EXTRA_RINGTONE_URI") ?: ""
        val isAlarm = intent.getBooleanExtra("EXTRA_IS_ALARM", true)

        // Start Foreground Alarm Service for looping audio/vibration
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_RINGTONE_URI", ringtoneUri)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Full Screen Intent activity launch
        val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_NOTE", note)
            putExtra("EXTRA_CATEGORY", category)
            putExtra("EXTRA_IS_ALARM", isAlarm)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "full_screen_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rafeeq — Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full screen intent alerts for alarms and categorized reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(if (note.isNotBlank()) note else "Tap to open full card")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
