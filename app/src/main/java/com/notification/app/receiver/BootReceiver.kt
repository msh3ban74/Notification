package com.notification.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notification.app.data.local.AppDatabase
import com.notification.app.domain.scheduler.AlarmManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Reboot wipes AlarmManager — re-arm the daily briefs too.
            AlarmManagerScheduler.scheduleMorningBrief(context)
            AlarmManagerScheduler.scheduleEveningBrief(context)

            // goAsync keeps the process alive past onReceive so the DB read +
            // re-arm loop actually finishes — without it Android could kill us
            // first and reboot would silently lose every alarm/reminder.
            val pending = goAsync()
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val now = System.currentTimeMillis()

                    db.alarmDao().getActiveAlarms().forEach { alarm ->
                        // Recurring alarms must ALWAYS be re-armed — their stored
                        // timeInMillis is the first (now-past) occurrence, and
                        // scheduleExactAlarm recomputes the next matching day.
                        // One-shot alarms only re-arm if still in the future.
                        if (alarm.repeatDays.isNotBlank() || alarm.timeInMillis > now) {
                            AlarmManagerScheduler.scheduleExactAlarm(context, alarm)
                        }
                    }

                    // Reboot wipes EVERY AlarmManager registration — task, bill
                    // and medicine reminders included, not just clock alarms.
                    db.reminderDao().getPendingReminders().first()
                        .filter { !it.isArchived && it.dueDate > now }
                        .forEach { reminder ->
                            AlarmManagerScheduler.scheduleReminderAlarm(context, reminder)
                        }
                } catch (_: Exception) {
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
