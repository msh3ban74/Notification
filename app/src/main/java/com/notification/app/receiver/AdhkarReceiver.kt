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
import com.notification.app.domain.scheduler.AlarmManagerScheduler

/**
 * أذكار ونوافل رفيق — a gentle daily nudge for the spiritual routine the user
 * turned on: morning/evening adhkar, Duha, and Qiyam. Each is a quiet
 * notification (never a ringing alarm), posted at its time and then
 * rescheduled for the same time tomorrow. [BootReceiver] re-arms them after a
 * reboot. Which ones are on lives in DataStore; the ViewModel schedules and
 * cancels them as the user toggles.
 */
class AdhkarReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        post(context, kind)
        // Same time tomorrow.
        AlarmManagerScheduler.scheduleAdhkar(context, kind)
    }

    private fun post(context: Context, kind: String) {
        val isArabic = java.util.Locale.getDefault().language == "ar"
        val (titleAr, titleEn, bodyAr, bodyEn, notifyId) = content(kind) ?: return

        val channelId = "rafeeq_adhkar"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    if (isArabic) "الأذكار والنوافل" else "Adhkar & nafl",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = if (isArabic) "تذكيرات لطيفة بأذكارك ونوافلك"
                    else "Gentle reminders for your adhkar and nafl"
                }
            )
        }

        val openIntent = PendingIntent.getActivity(
            context, notifyId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isArabic) titleAr else titleEn)
            .setContentText(if (isArabic) bodyAr else bodyEn)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (isArabic) bodyAr else bodyEn))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(notifyId, notification)
    }

    private data class Content(
        val titleAr: String, val titleEn: String,
        val bodyAr: String, val bodyEn: String,
        val notifyId: Int
    )

    private fun content(kind: String): Content? = when (kind) {
        KIND_MORNING -> Content(
            "أذكار الصباح", "Morning adhkar",
            "ابدأ يومك بذكر الله — أذكار الصباح تحصّنك وتطمئن قلبك.",
            "Start your day with remembrance — the morning adhkar.",
            NOTIFY_MORNING
        )
        KIND_EVENING -> Content(
            "أذكار المساء", "Evening adhkar",
            "اختم يومك بذكر الله — أذكار المساء قبل أن تنام.",
            "Close your day with remembrance — the evening adhkar.",
            NOTIFY_EVENING
        )
        KIND_DUHA -> Content(
            "صلاة الضحى", "Duha prayer",
            "وقت الضحى — ركعتان تكتب لك بها صدقة عن كل مفصل.",
            "It's Duha time — a couple of rak'ahs for a blessed morning.",
            NOTIFY_DUHA
        )
        KIND_QIYAM -> Content(
            "قيام الليل", "Qiyam",
            "جوف الليل — قم لله ولو بركعتين، فهو وقت الإجابة.",
            "The depth of night — stand for a couple of rak'ahs of Qiyam.",
            NOTIFY_QIYAM
        )
        else -> null
    }

    companion object {
        const val EXTRA_KIND = "EXTRA_ADHKAR_KIND"

        const val KIND_MORNING = "morning"
        const val KIND_EVENING = "evening"
        const val KIND_DUHA = "duha"
        const val KIND_QIYAM = "qiyam"

        private const val NOTIFY_MORNING = 779001
        private const val NOTIFY_EVENING = 779002
        private const val NOTIFY_DUHA = 779003
        private const val NOTIFY_QIYAM = 779004
    }
}
