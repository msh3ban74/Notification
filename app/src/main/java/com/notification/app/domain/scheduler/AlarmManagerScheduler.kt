package com.notification.app.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.receiver.AlarmReceiver

object AlarmManagerScheduler {

    fun scheduleExactAlarm(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.notification.app.ACTION_ALARM_TRIGGER"
            putExtra("EXTRA_ALARM_ID", alarm.id)
            putExtra("EXTRA_TITLE", alarm.title)
            putExtra("EXTRA_RINGTONE_URI", alarm.ringtoneUri)
            putExtra("EXTRA_IS_ALARM", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                alarm.timeInMillis,
                pendingIntent
            )
        }
    }

    fun scheduleReminderAlarm(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.notification.app.ACTION_REMINDER_TRIGGER"
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_TITLE", reminder.title)
            putExtra("EXTRA_NOTE", reminder.note)
            putExtra("EXTRA_CATEGORY", reminder.category)
            putExtra("EXTRA_IS_ALARM", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.dueDate,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.dueDate,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
