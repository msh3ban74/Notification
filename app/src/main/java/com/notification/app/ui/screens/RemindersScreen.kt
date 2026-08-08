package com.notification.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.domain.model.PreAlertOption
import com.notification.app.domain.model.RecurrenceType
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.OnPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    isArabic: Boolean,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    // Sprint 5 — edit flow. Optional so existing call sites keep working;
    // when provided, every card shows an Edit action that hands the
    // reminder back to the caller (which opens the pre-filled form).
    onEditReminder: ((ReminderEntity) -> Unit)? = null,
    // Phase D — CRUD extras. Optional for the same reason.
    onTogglePin: ((ReminderEntity) -> Unit)? = null,
    onSetArchived: ((ReminderEntity, Boolean) -> Unit)? = null,
    onDuplicate: ((ReminderEntity) -> Unit)? = null,
    // When provided, "+" opens the RICH Smart Task form instead of the
    // legacy quick-add dialog (checklist/tags/progress/location live there).
    onCreateTask: (() -> Unit)? = null
) {
    var selectedCategoryFilter by remember { mutableStateOf<ReminderCategory?>(null) }

    // Phase D — search / sort / archive view state.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(ReminderSortMode.DUE_DATE.name) }
    var showArchived by rememberSaveable { mutableStateOf(false) }

    val filteredReminders = remember(reminders, selectedCategoryFilter, searchQuery, sortMode, showArchived) {
        val sort = ReminderSortMode.valueOf(sortMode)
        val query = searchQuery.trim()
        reminders.asSequence()
            .filter { it.isArchived == showArchived }
            .filter { selectedCategoryFilter == null || it.category == selectedCategoryFilter!!.name }
            .filter {
                query.isEmpty() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.note.contains(query, ignoreCase = true) ||
                    it.tags.contains(query, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<ReminderEntity> { it.isPinned }.let { pinnedFirst ->
                    when (sort) {
                        ReminderSortMode.DUE_DATE -> pinnedFirst.thenBy { it.dueDate }
                        ReminderSortMode.TITLE -> pinnedFirst.thenBy { it.title.lowercase() }
                        ReminderSortMode.NEWEST -> pinnedFirst.thenByDescending { it.createdAt }
                    }
                }
            )
            .toList()
    }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTask?.invoke() },
                containerColor = MaroonPrimary,
                contentColor = OnPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        // Rafeeq DNA — the «المهام» atmosphere: mint morning.
        com.notification.app.ui.components.RafeeqAtmosphere(
            palette = com.notification.app.ui.components.RafeeqWeather.Tasks,
            modifier = Modifier.fillMaxWidth().height(230.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // Headline (product mock).
            Text(
                text = if (isArabic) "التذكيرات والمهام" else "Reminders & Tasks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            // Phase D — search + sort + archive controls.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isArabic) "ابحث في مهامك…" else "Search your tasks…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = if (isArabic) "مسح" else "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                ReminderSortMode.entries.forEach { mode ->
                    FilterChip(
                        selected = sortMode == mode.name,
                        onClick = { sortMode = mode.name },
                        label = {
                            Text(
                                if (isArabic) mode.labelAr else mode.labelEn,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Archive view toggle — archived items are hidden by default.
                IconButton(onClick = { showArchived = !showArchived }) {
                    Icon(
                        imageVector = if (showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = if (isArabic) "الأرشيف" else "Archive",
                        tint = if (showArchived) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (showArchived) {
                Text(
                    text = if (isArabic) "تعرض الآن: المؤرشفة" else "Now showing: archived",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text(if (isArabic) "الكل" else "All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaroonPrimary,
                            selectedLabelColor = OnPrimary
                        )
                    )
                }
                items(ReminderCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(if (isArabic) cat.displayNameAr else cat.displayNameEn) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaroonPrimary,
                            selectedLabelColor = OnPrimary
                        )
                    )
                }
            }

            // Reminders List
            if (filteredReminders.isEmpty()) {
                com.notification.app.ui.components.EmptyState(
                    icon = Icons.Default.EventAvailable,
                    title = if (isArabic) "لا توجد تذكيرات حالياً" else "No Reminders Found",
                    subtitle = if (isArabic) "أضف مهمة أو تذكيرًا وسيصلك تنبيه في موعده" else "Add a task or reminder and you'll be alerted on time"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReminders, key = { it.id }) { reminder ->
                        DetailedReminderCard(
                            reminder = reminder,
                            isArabic = isArabic,
                            dateFormat = dateFormat,
                            onToggle = { onToggleReminder(reminder) },
                            onDelete = { onDeleteReminder(reminder) },
                            onShare = {
                                shareReminderText(context, reminder, isArabic, dateFormat)
                            },
                            onEdit = onEditReminder?.let { edit -> { edit(reminder) } },
                            onTogglePin = onTogglePin?.let { pin -> { pin(reminder) } },
                            onArchiveToggle = onSetArchived?.let { arch ->
                                { arch(reminder, !reminder.isArchived) }
                            },
                            onDuplicate = onDuplicate?.let { dup -> { dup(reminder) } }
                        )
                    }
                }
            }
        }
        }
    }

}

@Composable
fun DetailedReminderCard(
    reminder: ReminderEntity,
    isArabic: Boolean,
    dateFormat: SimpleDateFormat,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onEdit: (() -> Unit)? = null,
    // Phase D — CRUD extras.
    onTogglePin: (() -> Unit)? = null,
    onArchiveToggle: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null
) {
    val category = ReminderCategory.fromString(reminder.category)

    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        com.notification.app.ui.components.ConfirmDeleteDialog(
            isArabic = isArabic,
            itemLabel = reminder.title,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = reminder.isCompleted,
                        onCheckedChange = { onToggle() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Phase D — pin toggle; pinned items always sort first.
                    if (onTogglePin != null) {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(44.dp)) {
                            Icon(
                                imageVector = if (reminder.isPinned) Icons.Filled.PushPin
                                else Icons.Outlined.PushPin,
                                contentDescription = if (isArabic) "تثبيت" else "Pin",
                                tint = if (reminder.isPinned) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) category.displayNameAr else category.displayNameEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (reminder.note.isNotBlank()) {
                Text(
                    text = reminder.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 8.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormat.format(Date(reminder.dueDate)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    // Phase D — duplicate: same schedule pipeline, fresh copy.
                    if (onDuplicate != null) {
                        IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Phase D — archive/unarchive (soft delete).
                    if (onArchiveToggle != null) {
                        IconButton(onClick = onArchiveToggle, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (reminder.isArchived) Icons.Default.Unarchive
                                else Icons.Default.Archive,
                                contentDescription = if (reminder.isArchived) "Unarchive" else "Archive",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Phase D — sort options for the tasks list (pinned always first). */
enum class ReminderSortMode(val labelEn: String, val labelAr: String) {
    DUE_DATE("Due date", "الموعد"),
    TITLE("Title", "الاسم"),
    NEWEST("Newest", "الأحدث")
}

private fun shareReminderText(
    context: Context,
    reminder: ReminderEntity,
    isArabic: Boolean,
    dateFormat: SimpleDateFormat
) {
    val text = if (isArabic) {
        "تذكير: ${reminder.title}\nالموعد: ${dateFormat.format(Date(reminder.dueDate))}\nتفاصيل: ${reminder.note}\n— تطبيق رفيق"
    } else {
        "Reminder: ${reminder.title}\nDue: ${dateFormat.format(Date(reminder.dueDate))}\nNote: ${reminder.note}\n— Rafeeq"
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Reminder"))
}
