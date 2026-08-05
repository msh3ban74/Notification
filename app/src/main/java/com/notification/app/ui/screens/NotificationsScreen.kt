package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.ui.components.EmptyState
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.AppRadius
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sprint 1 — Application Foundation (placeholder shell).
 * Sprint 5 — REAL notifications feed: the user's scheduled timeline.
 *
 * Shows actual scheduled notifications from the EXISTING data layer only:
 *  - pending reminders (every reminder is scheduled with AlarmManager via
 *    the existing AlarmManagerScheduler when it is created/updated), and
 *  - enabled alarms (the existing AlarmEntity flow, incl. AI-created ones).
 *
 * Grouped into "Upcoming" (due from now on) and "Overdue" (past-due,
 * still not completed). No fake data — an honest EmptyState otherwise.
 */
@Composable
fun NotificationsScreen(
    reminders: List<ReminderEntity>,
    alarms: List<AlarmEntity>,
    isArabic: Boolean = false,
    /** v1.0 — empty-state CTA: jump to Tasks to schedule the first reminder. */
    onCreateFirst: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("EEE dd MMM, hh:mm a", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    // Scheduled reminder notifications — pending only (completed ones no
    // longer fire), split around "now".
    val pendingReminders = remember(reminders) {
        reminders.filter { !it.isCompleted }.sortedBy { it.dueDate }
    }
    val upcomingReminders = pendingReminders.filter { it.dueDate >= now }
    val overdueReminders = pendingReminders.filter { it.dueDate < now }

    // Enabled, still-in-the-future alarms scheduled via the existing
    // AlarmManagerScheduler pipeline.
    val upcomingAlarms = remember(alarms) {
        alarms.filter { it.isEnabled && it.timeInMillis >= now }.sortedBy { it.timeInMillis }
    }

    val isEmpty = upcomingReminders.isEmpty() && overdueReminders.isEmpty() && upcomingAlarms.isEmpty()

    if (isEmpty) {
        EmptyState(
            icon = Icons.Default.NotificationsNone,
            title = if (isArabic) "لا توجد إشعارات مجدولة" else "No scheduled notifications",
            subtitle = if (isArabic) {
                "جدول أول تذكير وستظهر إشعاراته هنا تلقائيًا"
            } else {
                "Schedule your first reminder and its alerts will appear here"
            },
            actionLabel = if (onCreateFirst != null) {
                if (isArabic) "أنشئ تذكيرًا" else "Schedule Reminder"
            } else null,
            onAction = onCreateFirst
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppPadding.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = Spacing.sm,
            bottom = AppDimens.bottomNavHeight
        )
    ) {
        if (overdueReminders.isNotEmpty()) {
            item {
                FeedSectionHeader(
                    text = if (isArabic) "متأخرة" else "Overdue",
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(overdueReminders, key = { "overdue-${it.id}" }) { reminder ->
                ReminderFeedCard(
                    reminder = reminder,
                    isArabic = isArabic,
                    dateFormat = dateFormat,
                    overdue = true
                )
            }
        }

        if (upcomingReminders.isNotEmpty()) {
            item {
                FeedSectionHeader(
                    text = if (isArabic) "القادمة" else "Upcoming",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(upcomingReminders, key = { "upcoming-${it.id}" }) { reminder ->
                ReminderFeedCard(
                    reminder = reminder,
                    isArabic = isArabic,
                    dateFormat = dateFormat,
                    overdue = false
                )
            }
        }

        if (upcomingAlarms.isNotEmpty()) {
            item {
                FeedSectionHeader(
                    text = if (isArabic) "المنبهات" else "Alarms",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(upcomingAlarms, key = { "alarm-${it.id}" }) { alarm ->
                PremiumCard(contentPadding = AppPadding.listItem, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.iconSizeMedium)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alarm.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateFormat.format(Date(alarm.timeInMillis)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedSectionHeader(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs)
    )
}

@Composable
private fun ReminderFeedCard(
    reminder: ReminderEntity,
    isArabic: Boolean,
    dateFormat: SimpleDateFormat,
    overdue: Boolean
) {
    val category = ReminderCategory.fromString(reminder.category)

    PremiumCard(contentPadding = AppPadding.listItem, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (overdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(AppDimens.iconSizeMedium)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(reminder.dueDate)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = AppRadius.small
            ) {
                Text(
                    text = if (isArabic) category.displayNameAr else category.displayNameEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                )
            }
        }
    }
}
