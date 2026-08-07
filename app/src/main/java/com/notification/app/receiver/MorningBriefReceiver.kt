package com.notification.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.notification.app.MainActivity
import com.notification.app.R
import com.notification.app.data.local.AppDatabase
import com.notification.app.domain.scheduler.AlarmManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * رسالة رفيق الصباحية ☀️ — the real-companion moment.
 *
 * Every morning Rafeeq greets the user with an HONEST one-glance summary
 * of their day, computed live from the database at fire time: how many
 * things are on them today (with the first few titles), any debt promised
 * back today, and overdue count. A gentle notification — it never rings
 * like an alarm — tapping it opens «يومك». Reschedules itself for
 * tomorrow at the end, and [BootReceiver] re-arms it after reboots.
 */
class MorningBriefReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postBrief(context)
            } finally {
                // Tomorrow, same time — the companion never skips a morning.
                AlarmManagerScheduler.scheduleMorningBrief(context)
                pending.finish()
            }
        }
    }

    private suspend fun postBrief(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val isArabic = java.util.Locale.getDefault().language == "ar"
        val now = System.currentTimeMillis()
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24L * 60 * 60 * 1000

        val reminders = db.reminderDao().getAllReminders().first()
            .filter { !it.isArchived && !it.isCompleted }
        val todays = reminders.filter { it.dueDate in dayStart until dayEnd }.sortedBy { it.dueDate }
        val overdue = reminders.count { it.dueDate in 1 until dayStart }
        val debtsToday = db.personLedgerDao().getAllTransactions().first()
            .count { it.linkedReminderId == null && it.dueDate in dayStart until dayEnd }
        val total = todays.size + debtsToday

        val title: String
        val text: String
        if (total == 0 && overdue == 0) {
            title = if (isArabic) "صباح الخير ☀️" else "Good morning ☀️"
            text = if (isArabic) "مفيش حاجة عليك النهاردة — يوم هادي، استمتع ✨"
            else "Nothing on you today — enjoy a calm day ✨"
        } else {
            title = if (isArabic) "صباح الخير ☀️ عليك $total ${if (total == 1) "حاجة" else "حاجات"} النهاردة"
            else "Good morning ☀️ $total thing${if (total == 1) "" else "s"} on you today"
            val names = todays.take(3).joinToString("، ") { it.title }
            val overduePart = if (overdue > 0) {
                if (isArabic) " • و$overdue متأخرة محتاجة نظرة" else " • plus $overdue overdue"
            } else ""
            text = (if (names.isNotBlank()) names else (if (isArabic) "افتح يومك للتفاصيل" else "Open your day for details")) + overduePart
        }

        val channelId = "rafeeq_morning_brief"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    if (isArabic) "رسالة رفيق الصباحية" else "Rafeeq morning brief",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = if (isArabic) "ملخص يومك كل صباح" else "Your day at a glance, every morning"
                }
            )
        }

        val openIntent = PendingIntent.getActivity(
            context, 777002,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(777002, notification)
    }
}
