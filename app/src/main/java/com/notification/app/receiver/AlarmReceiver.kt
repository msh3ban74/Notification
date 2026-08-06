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
        // Per-alarm options (reminders fall back to sensible defaults).
        val vibrate = intent.getBooleanExtra("EXTRA_VIBRATE", true)
        val flashlight = intent.getBooleanExtra("EXTRA_FLASHLIGHT", false)
        val volume = intent.getIntExtra("EXTRA_VOLUME", 100)
        val autoStopMin = intent.getIntExtra("EXTRA_AUTO_STOP_MIN", 5)
        val snoozeMin = intent.getIntExtra("EXTRA_SNOOZE_MIN", 10)

        // Weekly repeat — arm the next occurrence before anything else, so a
        // crash mid-ring can't stop the series. No-op for one-shot alarms.
        com.notification.app.domain.scheduler.AlarmManagerScheduler.scheduleNextRepeat(context, intent)

        // Start Foreground Alarm Service for looping audio/vibration
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_RINGTONE_URI", ringtoneUri)
            putExtra("EXTRA_VIBRATE", vibrate)
            putExtra("EXTRA_FLASHLIGHT", flashlight)
            putExtra("EXTRA_VOLUME", volume)
            putExtra("EXTRA_AUTO_STOP_MIN", autoStopMin)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        val alarmId = intent.getLongExtra("EXTRA_ALARM_ID", -1L)
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", -1L)

        // Full Screen Intent activity launch — carries everything needed to
        // re-schedule on snooze (id, ringtone, whether it's a clock alarm).
        val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_NOTE", note)
            putExtra("EXTRA_CATEGORY", category)
            putExtra("EXTRA_IS_ALARM", isAlarm)
            putExtra("EXTRA_ALARM_ID", alarmId)
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_RINGTONE_URI", ringtoneUri)
            putExtra("EXTRA_SNOOZE_MIN", snoozeMin)
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
                // Pierce Do-Not-Disturb so the full-screen alert still shows.
                setBypassDnd(true)
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
