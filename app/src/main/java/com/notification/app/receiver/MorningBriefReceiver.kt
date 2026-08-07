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
 * رسائل رفيق اليومية — the real-companion moments.
 *
 *  • الصباح ☀️ (8:00): "عليك N حاجات النهاردة" + أول عناوين + المتأخر.
 *  • المساء 🌙 (21:00): "قبل ما تنام — بكرة عليك كذا" حتى تنام مطمّن.
 *
 * Both are computed LIVE from the database at fire time, posted as gentle
 * notifications (never ringing alarms), tap opens «يومك». Each firing
 * reschedules itself for tomorrow; [BootReceiver] re-arms after reboots.
 */
class MorningBriefReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val evening = intent.getBooleanExtra("EXTRA_EVENING", false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postBrief(context, evening)
            } finally {
                // Same time tomorrow — the companion never skips a day.
                if (evening) AlarmManagerScheduler.scheduleEveningBrief(context)
                else AlarmManagerScheduler.scheduleMorningBrief(context)
                pending.finish()
            }
        }
    }

    private suspend fun postBrief(context: Context, evening: Boolean) {
        val db = AppDatabase.getDatabase(context)
        val isArabic = java.util.Locale.getDefault().language == "ar"
        val now = System.currentTimeMillis()
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24L * 60 * 60 * 1000
        // Morning summarizes TODAY; evening looks ahead at TOMORROW.
        val (windowStart, windowEnd) = if (evening) dayEnd to (dayEnd + 24L * 60 * 60 * 1000)
        else dayStart to dayEnd

        val reminders = db.reminderDao().getAllReminders().first()
            .filter { !it.isArchived && !it.isCompleted }
        val inWindow = reminders.filter { it.dueDate in windowStart until windowEnd }.sortedBy { it.dueDate }
        val overdue = reminders.count { it.dueDate in 1 until dayStart }
        val debtsInWindow = db.personLedgerDao().getAllTransactions().first()
            .count { it.linkedReminderId == null && it.dueDate in windowStart until windowEnd }
        val total = inWindow.size + debtsInWindow

        val title: String
        val text: String
        if (evening) {
            if (total == 0) {
                title = if (isArabic) "تصبح على خير 🌙" else "Good night 🌙"
                text = if (isArabic) "بكرة فاضي لحد دلوقتي — نام مطمّن ✨"
                else "Tomorrow is clear so far — sleep easy ✨"
            } else {
                title = if (isArabic) "قبل ما تنام 🌙 بكرة عليك $total ${if (total == 1) "حاجة" else "حاجات"}"
                else "Before you sleep 🌙 $total thing${if (total == 1) "" else "s"} tomorrow"
                text = inWindow.take(3).joinToString("، ") { it.title }
                    .ifBlank { if (isArabic) "افتح يومك للتفاصيل" else "Open your day for details" }
            }
        } else {
            if (total == 0 && overdue == 0) {
                title = if (isArabic) "صباح الخير ☀️" else "Good morning ☀️"
                text = if (isArabic) "مفيش حاجة عليك النهاردة — يوم هادي، استمتع ✨"
                else "Nothing on you today — enjoy a calm day ✨"
            } else {
                title = if (isArabic) "صباح الخير ☀️ عليك $total ${if (total == 1) "حاجة" else "حاجات"} النهاردة"
                else "Good morning ☀️ $total thing${if (total == 1) "" else "s"} on you today"
                val names = inWindow.take(3).joinToString("، ") { it.title }
                val overduePart = if (overdue > 0) {
                    if (isArabic) " • و$overdue متأخرة محتاجة نظرة" else " • plus $overdue overdue"
                } else ""
                text = (if (names.isNotBlank()) names else (if (isArabic) "افتح يومك للتفاصيل" else "Open your day for details")) + overduePart
            }
        }

        val channelId = "rafeeq_daily_brief"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    if (isArabic) "رسائل رفيق اليومية" else "Rafeeq daily briefs",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = if (isArabic) "ملخص يومك كل صباح ومساء" else "Your day at a glance, morning and evening"
                }
            )
        }

        val notifyId = if (evening) 777004 else 777002
        val openIntent = PendingIntent.getActivity(
            context, notifyId,
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

        nm.notify(notifyId, notification)
    }
}
