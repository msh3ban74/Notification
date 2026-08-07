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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * The scheduled-alerts feed: pending reminders (upcoming / overdue) and
 * enabled future alarms, all from the real data layer. Product Completion
 * adds full CRUD from here — single delete per row, plus a multi-select
 * mode to clear several at once. Selection keys are composite ("a-<id>"
 * for alarms, "r-<id>" for reminders) so both types share one selection.
 */
@Composable
fun NotificationsScreen(
    reminders: List<ReminderEntity>,
    alarms: List<AlarmEntity>,
    isArabic: Boolean = false,
    onCreateFirst: (() -> Unit)? = null,
    onDeleteAlarm: ((AlarmEntity) -> Unit)? = null,
    onDeleteReminder: ((ReminderEntity) -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("EEE dd MMM, hh:mm a", Locale.getDefault()) }
    val now = System.currentTimeMillis()

    val pendingReminders = remember(reminders) {
        reminders.filter { !it.isCompleted }.sortedBy { it.dueDate }
    }
    val upcomingReminders = pendingReminders.filter { it.dueDate >= now }
    val overdueReminders = pendingReminders.filter { it.dueDate < now }
    val upcomingAlarms = remember(alarms) {
        alarms.filter { it.isEnabled && it.timeInMillis >= now }.sortedBy { it.timeInMillis }
    }

    val canDelete = onDeleteAlarm != null || onDeleteReminder != null
    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    fun keyForAlarm(a: AlarmEntity) = "a-${a.id}"
    fun keyForReminder(r: ReminderEntity) = "r-${r.id}"
    val allKeys = remember(upcomingAlarms, upcomingReminders, overdueReminders) {
        (upcomingAlarms.map { "a-${it.id}" } +
            upcomingReminders.map { "r-${it.id}" } +
            overdueReminders.map { "r-${it.id}" }).toSet()
    }
    fun toggle(key: String) {
        selected = if (key in selected) selected - key else selected + key
    }
    fun exitSelection() {
        selectionMode = false
        selected = emptySet()
    }
    fun deleteSelected() {
        selected.forEach { key ->
            val id = key.substringAfter('-').toLongOrNull() ?: return@forEach
            if (key.startsWith("a-")) {
                alarms.firstOrNull { it.id == id }?.let { onDeleteAlarm?.invoke(it) }
            } else {
                reminders.firstOrNull { it.id == id }?.let { onDeleteReminder?.invoke(it) }
            }
        }
        exitSelection()
    }

    val isEmpty = upcomingReminders.isEmpty() && overdueReminders.isEmpty() && upcomingAlarms.isEmpty()

    if (isEmpty) {
        // Rafeeq DNA — the «الإشعارات» atmosphere: golden hour.
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            com.notification.app.ui.components.RafeeqAtmosphere(
                palette = com.notification.app.ui.components.RafeeqWeather.Alerts,
                modifier = Modifier.matchParentSize()
            )
            EmptyState(
                icon = Icons.Default.NotificationsNone,
                title = if (isArabic) "لا توجد إشعارات مجدولة" else "No scheduled notifications",
                subtitle = if (isArabic) "التنبيهات المجدولة ستظهر هنا في موعدها"
                else "Scheduled alerts will appear here on time"
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Selection toolbar — appears in place of nothing when the list has
        // deletable rows. Enter/exit multi-select, select-all, delete-N.
        if (canDelete) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppPadding.screen, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Text(
                        text = if (isArabic) "محدد: ${selected.size}" else "${selected.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        selected = if (selected.size == allKeys.size) emptySet() else allKeys
                    }) {
                        Text(
                            if (selected.size == allKeys.size) {
                                if (isArabic) "إلغاء الكل" else "None"
                            } else {
                                if (isArabic) "تحديد الكل" else "All"
                            }
                        )
                    }
                    IconButton(
                        onClick = { if (selected.isNotEmpty()) deleteSelected() },
                        enabled = selected.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف المحدد" else "Delete selected",
                            tint = if (selected.isNotEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { exitSelection() }) {
                        Text(if (isArabic) "تم" else "Done")
                    }
                } else {
                    Text(
                        text = if (isArabic) "الإشعارات المجدولة" else "Scheduled alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { selectionMode = true }) {
                        Text(if (isArabic) "تحديد" else "Select")
                    }
                }
            }
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
                        reminder = reminder, isArabic = isArabic, dateFormat = dateFormat,
                        overdue = true,
                        selectionMode = selectionMode,
                        selected = keyForReminder(reminder) in selected,
                        onSelectToggle = { toggle(keyForReminder(reminder)) },
                        onDelete = onDeleteReminder?.let { d -> { d(reminder) } }
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
                        reminder = reminder, isArabic = isArabic, dateFormat = dateFormat,
                        overdue = false,
                        selectionMode = selectionMode,
                        selected = keyForReminder(reminder) in selected,
                        onSelectToggle = { toggle(keyForReminder(reminder)) },
                        onDelete = onDeleteReminder?.let { d -> { d(reminder) } }
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
                    AlarmFeedCard(
                        alarm = alarm, isArabic = isArabic, dateFormat = dateFormat,
                        selectionMode = selectionMode,
                        selected = keyForAlarm(alarm) in selected,
                        onSelectToggle = { toggle(keyForAlarm(alarm)) },
                        onDelete = onDeleteAlarm?.let { d -> { d(alarm) } }
                    )
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
private fun AlarmFeedCard(
    alarm: AlarmEntity,
    isArabic: Boolean,
    dateFormat: SimpleDateFormat,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    PremiumCard(
        contentPadding = AppPadding.listItem,
        modifier = Modifier.fillMaxWidth(),
        onClick = if (selectionMode) onSelectToggle else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onSelectToggle() })
            }
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
            if (!selectionMode && onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (isArabic) "حذف" else "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderFeedCard(
    reminder: ReminderEntity,
    isArabic: Boolean,
    dateFormat: SimpleDateFormat,
    overdue: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onSelectToggle: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val category = ReminderCategory.fromString(reminder.category)

    PremiumCard(
        contentPadding = AppPadding.listItem,
        modifier = Modifier.fillMaxWidth(),
        onClick = if (selectionMode) onSelectToggle else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onSelectToggle() })
            }
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
                    color = if (overdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!selectionMode) {
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
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف" else "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
