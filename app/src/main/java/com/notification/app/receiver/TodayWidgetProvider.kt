package com.notification.app.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.notification.app.MainActivity
import com.notification.app.R
import com.notification.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ويدجت «يومك» — رفيق على الشاشة الرئيسية من غير ما تفتح التطبيق:
 * "عليك N حاجات النهاردة" + أول ثلاث عناوين بوقتها. يتحدّث كل ٣٠ دقيقة
 * (نظام الويدجت) وعند فتح التطبيق؛ الضغط عليه يفتح رفيق.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (title, body) = buildContent(context)
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, title, body))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(context: Context, title: String, body: String): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_today).apply {
            setTextViewText(R.id.widget_title, title)
            setTextViewText(R.id.widget_body, body)
            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, 777005,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

    private suspend fun buildContent(context: Context): Pair<String, String> {
        val db = AppDatabase.getDatabase(context)
        val isArabic = Locale.getDefault().language == "ar"
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = dayStart + 24L * 60 * 60 * 1000

        val pending = db.reminderDao().getAllReminders().first()
            .filter { !it.isArchived && !it.isCompleted }
        val todays = pending.filter { it.dueDate in dayStart until dayEnd }.sortedBy { it.dueDate }
        val overdue = pending.count { it.dueDate in 1 until dayStart }

        val title = when {
            todays.isEmpty() && overdue == 0 ->
                if (isArabic) "رفيق ✨ يومك فاضي" else "Rafeeq ✨ nothing today"
            else ->
                if (isArabic) "رفيق ✨ عليك ${todays.size + overdue} ${if (todays.size + overdue == 1) "حاجة" else "حاجات"}"
                else "Rafeeq ✨ ${todays.size + overdue} thing${if (todays.size + overdue == 1) "" else "s"} on you"
        }
        val lines = buildList {
            if (overdue > 0) add(if (isArabic) "⚠️ $overdue متأخرة" else "⚠️ $overdue overdue")
            todays.take(3).forEach {
                add("• ${it.title} — ${timeFormat.format(Date(it.dueDate))}")
            }
            if (todays.isEmpty() && overdue == 0) {
                add(if (isArabic) "استمتع بيوم هادي ✨" else "Enjoy a calm day ✨")
            }
        }
        return title to lines.joinToString("\n")
    }

    companion object {
        /** Refreshes every placed widget — called when the app opens. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, TodayWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
