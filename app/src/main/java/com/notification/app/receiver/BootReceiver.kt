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
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()

                db.alarmDao().getActiveAlarms().forEach { alarm ->
                    if (alarm.timeInMillis > now) {
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
            }
        }
    }
}
