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
            putExtra("EXTRA_NOTE", alarm.label)
            putExtra("EXTRA_RINGTONE_URI", alarm.ringtoneUri)
            putExtra("EXTRA_IS_ALARM", true)
            putExtra("EXTRA_VIBRATE", alarm.vibrate)
            putExtra("EXTRA_FLASHLIGHT", alarm.flashlight)
            putExtra("EXTRA_VOLUME", alarm.volumePercent)
            putExtra("EXTRA_SNOOZE_MIN", alarm.snoozeMinutes)
            putExtra("EXTRA_AUTO_STOP_MIN", alarm.autoStopMinutes)
            putExtra("EXTRA_SNOOZE_MIN", alarm.snoozeMinutes)
            putExtra("EXTRA_REPEAT_DAYS", alarm.repeatDays)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrFallback(alarmManager, alarm.timeInMillis, pendingIntent)
    }

    /**
     * Weekly repeat — when a repeating alarm fires, schedule its NEXT
     * occurrence. Called from the receiver with the intent it just got, so
     * every option (sound, volume, vibration…) is preserved verbatim. No
     * DB access needed. A one-shot alarm (empty repeatDays) does nothing.
     */
    fun scheduleNextRepeat(context: Context, firedIntent: Intent) {
        val repeatDays = firedIntent.getStringExtra("EXTRA_REPEAT_DAYS").orEmpty()
        val next = nextOccurrence(repeatDays, System.currentTimeMillis()) ?: return
        val alarmId = firedIntent.getLongExtra("EXTRA_ALARM_ID", -1L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Clone the fired intent so all extras carry over unchanged.
        val intent = Intent(firedIntent).apply { setClass(context, AlarmReceiver::class.java) }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrFallback(alarmManager, next, pendingIntent)
    }

    /** Next matching weekday (Calendar.DAY_OF_WEEK 1..7) after [fromMillis],
     *  keeping the same clock time; null if repeatDays is empty/invalid. */
    private fun nextOccurrence(repeatDays: String, fromMillis: Long): Long? {
        val days = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        if (days.isEmpty()) return null
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        for (i in 1..7) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            if (cal.get(java.util.Calendar.DAY_OF_WEEK) in days) return cal.timeInMillis
        }
        return null
    }

    /**
     * Snooze — re-fires the SAME alert [minutes] from now. Clock alarms
     * keep their id/ringtone; reminders keep theirs. Reuses the existing
     * request-code namespaces so nothing collides.
     */
    fun snooze(
        context: Context,
        minutes: Int,
        isAlarm: Boolean,
        alarmId: Long,
        reminderId: Long,
        title: String,
        note: String,
        category: String,
        ringtoneUri: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (isAlarm) "com.notification.app.ACTION_ALARM_TRIGGER"
            else "com.notification.app.ACTION_REMINDER_TRIGGER"
            putExtra("EXTRA_ALARM_ID", alarmId)
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_NOTE", note)
            putExtra("EXTRA_CATEGORY", category)
            putExtra("EXTRA_RINGTONE_URI", ringtoneUri)
            putExtra("EXTRA_IS_ALARM", isAlarm)
        }
        val requestCode = if (isAlarm) alarmId.toInt() else (reminderId + 100000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrFallback(alarmManager, triggerAt, pendingIntent)
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

        scheduleExactOrFallback(alarmManager, reminder.dueDate, pendingIntent)
    }

    /**
     * Stability sprint — schedules an exact alarm, but NEVER crashes when
     * the OS withholds the exact-alarm permission (Android 12+). If exact
     * scheduling isn't allowed we fall back to an inexact wakeup so the
     * alert still fires (approximately) instead of throwing SecurityException.
     */
    private fun scheduleExactOrFallback(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        // setAlarmClock is the strongest guarantee: it fires at the EXACT
        // time even in Doze, and — unlike setExactAndAllowWhileIdle — it
        // does NOT require the SCHEDULE_EXACT_ALARM permission on Android
        // 12+. This is why a short "in 1 minute" alarm now rings on time
        // instead of being deferred to a ~9-minute idle window.
        try {
            val info = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
            return
        } catch (e: Exception) {
            // Fall through to the exact/inexact ladder below.
        }

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }

    /**
     * Cancels a reminder's scheduled alert — mirrors [scheduleReminderAlarm]
     * exactly (same action + the id+100000 request-code namespace), because
     * AlarmManager matches on the Intent action/class and request code.
     */
    fun cancelReminderAlarm(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.notification.app.ACTION_REMINDER_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
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
