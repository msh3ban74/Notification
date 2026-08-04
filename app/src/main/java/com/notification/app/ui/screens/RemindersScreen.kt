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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    isArabic: Boolean,
    onAddReminder: (ReminderEntity) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf<ReminderCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredReminders = remember(reminders, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) reminders
        else reminders.filter { it.category == selectedCategoryFilter!!.name }
    }

    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaroonPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // Header Title
            Text(
                text = if (isArabic) "التذكيرات والتنبيهات" else "Reminders & Alerts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

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
                            selectedLabelColor = Color.White
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
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Reminders List
            if (filteredReminders.isEmpty()) {
                com.notification.app.ui.components.EmptyState(
                    icon = Icons.Default.EventAvailable,
                    title = if (isArabic) "لا توجد تذكيرات حالياً" else "No Reminders Found",
                    subtitle = if (isArabic) "اضغط على زر الإضافة (+) لإنشاء تذكير جديد وتنبيهاتك القادمة" else "Tap the (+) button below to schedule your upcoming alerts and tasks"
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
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            isArabic = isArabic,
            onDismiss = { showAddDialog = false },
            onConfirm = { reminder ->
                onAddReminder(reminder)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DetailedReminderCard(
    reminder: ReminderEntity,
    isArabic: Boolean,
    dateFormat: SimpleDateFormat,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val category = ReminderCategory.fromString(reminder.category)

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
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddReminderDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ReminderEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReminderCategory.MONEY) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceType.NONE) }

    val cal = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إضافة تذكير جديد" else "Add New Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isArabic) "العنوان" else "Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isArabic) "تفاصيل/ملاحظة" else "Note/Details") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isArabic) "الفئة:" else "Category:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ReminderCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(if (isArabic) cat.displayNameAr else cat.displayNameEn) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            ReminderEntity(
                                title = title,
                                note = note,
                                dueDate = cal.timeInMillis,
                                category = selectedCategory.name,
                                recurrence = selectedRecurrence.name
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Text(if (isArabic) "حفظ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

private fun shareReminderText(
    context: Context,
    reminder: ReminderEntity,
    isArabic: Boolean,
    dateFormat: SimpleDateFormat
) {
    val text = if (isArabic) {
        "📌 تذكير: ${reminder.title}\n📅 الموعد: ${dateFormat.format(Date(reminder.dueDate))}\n📝 تفاصيل: ${reminder.note}\nأُرسل عبر تطبيق إشعار"
    } else {
        "📌 Reminder: ${reminder.title}\n📅 Due Date: ${dateFormat.format(Date(reminder.dueDate))}\n📝 Note: ${reminder.note}\nSent via Notification App"
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Reminder"))
}
