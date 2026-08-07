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
import kotlinx.coroutines.launch

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

        // Pre-alerts (قبل يوم / قبل ساعة): a quiet heads-up notification —
        // no ringing service, no full-screen takeover, no recurrence logic.
        if (intent.getBooleanExtra("EXTRA_PRE_ALERT", false)) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "rafeeq_pre_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        "Rafeeq — Upcoming reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
            val isArabic = java.util.Locale.getDefault().language == "ar"
            val leadLabel = when (intent.getStringExtra("EXTRA_PRE_ALERT_LABEL")) {
                "ONE_MONTH" -> if (isArabic) "بعد شهر" else "In a month"
                "ONE_WEEK" -> if (isArabic) "بعد أسبوع" else "In a week"
                "ONE_DAY" -> if (isArabic) "غدًا" else "Tomorrow"
                else -> if (isArabic) "بعد ساعة" else "In an hour"
            }
            val n = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$leadLabel: $title")
                .setContentText(note)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .build()
            nm.notify((System.currentTimeMillis() % 100000).toInt(), n)
            return
        }

        // Weekly repeat — arm the next occurrence before anything else, so a
        // crash mid-ring can't stop the series. No-op for one-shot alarms.
        com.notification.app.domain.scheduler.AlarmManagerScheduler.scheduleNextRepeat(context, intent)

        // Recurring REMINDERS (DAILY/WEEKLY/MONTHLY/YEARLY — e.g. daily
        // medicines) re-arm themselves: advance dueDate one period past now,
        // persist, and schedule the next fire. Without this they rang once.
        val recurringReminderId = intent.getLongExtra("EXTRA_REMINDER_ID", -1L)
        if (recurringReminderId > 0) {
            val pending = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val db = com.notification.app.data.local.AppDatabase.getDatabase(context)
                    val r = db.reminderDao().getReminderById(recurringReminderId)
                    if (r != null && r.recurrence != "NONE" && r.recurrence.isNotBlank()) {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = r.dueDate }
                        val now = System.currentTimeMillis()
                        do {
                            when (r.recurrence) {
                                "WEEKLY" -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                                "MONTHLY" -> cal.add(java.util.Calendar.MONTH, 1)
                                "YEARLY" -> cal.add(java.util.Calendar.YEAR, 1)
                                else -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            }
                        } while (cal.timeInMillis <= now)
                        val next = r.copy(dueDate = cal.timeInMillis, isCompleted = false)
                        db.reminderDao().updateReminder(next)
                        com.notification.app.domain.scheduler.AlarmManagerScheduler
                            .scheduleReminderAlarm(context, next)
                    }
                } catch (_: Exception) {
                } finally {
                    pending.finish()
                }
            }
        }

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
