package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.domain.model.RecurrenceType
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.domain.model.TaskPriority
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumButton
import com.notification.app.ui.designsystem.PremiumOutlinedButton
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Sprint 5 — Smart Items rollout.
 *
 * Per-type configuration for the shared smart reminder form below. Every
 * type here rides the SAME existing reminder pipeline — only the screen
 * title, the stored [ReminderCategory], and the pre-selected repeat differ.
 * This is what lets Bill / Appointment / Medicine become real flows without
 * duplicating the Task form or inventing new business logic.
 */
data class SmartReminderFormConfig(
    val newTitleEn: String,
    val newTitleAr: String,
    val editTitleEn: String,
    val editTitleAr: String,
    val category: ReminderCategory,
    val defaultRecurrence: RecurrenceType = RecurrenceType.NONE,
    val savedMessageEn: String,
    val savedMessageAr: String
) {
    companion object {
        val Task = SmartReminderFormConfig(
            newTitleEn = "New Task", newTitleAr = "مهمة جديدة",
            editTitleEn = "Edit Task", editTitleAr = "تعديل المهمة",
            category = ReminderCategory.CUSTOM,
            savedMessageEn = "Task created successfully.",
            savedMessageAr = "تم إنشاء المهمة بنجاح"
        )
        val Bill = SmartReminderFormConfig(
            newTitleEn = "New Bill", newTitleAr = "فاتورة جديدة",
            editTitleEn = "Edit Bill", editTitleAr = "تعديل الفاتورة",
            category = ReminderCategory.BILL,
            defaultRecurrence = RecurrenceType.MONTHLY,
            savedMessageEn = "Bill saved successfully.",
            savedMessageAr = "تم حفظ الفاتورة بنجاح"
        )
        val Appointment = SmartReminderFormConfig(
            newTitleEn = "New Appointment", newTitleAr = "موعد جديد",
            editTitleEn = "Edit Appointment", editTitleAr = "تعديل الموعد",
            category = ReminderCategory.APPOINTMENT,
            savedMessageEn = "Appointment saved successfully.",
            savedMessageAr = "تم حفظ الموعد بنجاح"
        )
        val Medicine = SmartReminderFormConfig(
            newTitleEn = "New Medicine", newTitleAr = "دواء جديد",
            editTitleEn = "Edit Medicine", editTitleAr = "تعديل الدواء",
            category = ReminderCategory.MEDICINE,
            defaultRecurrence = RecurrenceType.DAILY,
            savedMessageEn = "Medicine reminder saved successfully.",
            savedMessageAr = "تم حفظ تذكير الدواء بنجاح"
        )

        val Study = SmartReminderFormConfig(
            newTitleEn = "New Study Plan", newTitleAr = "خطة مذاكرة جديدة",
            editTitleEn = "Edit Study Plan", editTitleAr = "تعديل خطة المذاكرة",
            category = ReminderCategory.TUTORING,
            savedMessageEn = "Study plan saved.",
            savedMessageAr = "تم حفظ خطة المذاكرة"
        )
        val Work = SmartReminderFormConfig(
            newTitleEn = "New Work Item", newTitleAr = "مهمة شغل جديدة",
            editTitleEn = "Edit Work Item", editTitleAr = "تعديل مهمة الشغل",
            category = ReminderCategory.WORK,
            savedMessageEn = "Work item saved.",
            savedMessageAr = "تم حفظ مهمة الشغل"
        )
        val Event = SmartReminderFormConfig(
            newTitleEn = "New Event", newTitleAr = "مناسبة جديدة",
            editTitleEn = "Edit Event", editTitleAr = "تعديل المناسبة",
            category = ReminderCategory.EVENT,
            savedMessageEn = "Event saved.",
            savedMessageAr = "تم حفظ المناسبة"
        )
        val Personal = SmartReminderFormConfig(
            newTitleEn = "New Personal Item", newTitleAr = "أمر شخصي جديد",
            editTitleEn = "Edit Personal Item", editTitleAr = "تعديل الأمر الشخصي",
            category = ReminderCategory.PERSONAL,
            savedMessageEn = "Saved.",
            savedMessageAr = "تم الحفظ"
        )

        /** Maps a SmartItemType id from the "+" bottom sheet to its form config. */
        fun forItemId(itemId: String?): SmartReminderFormConfig = when (itemId) {
            "bill" -> Bill
            "appointment" -> Appointment
            "medicine" -> Medicine
            "study" -> Study
            "work" -> Work
            "event" -> Event
            "personal" -> Personal
            else -> Task
        }
    }
}

/**
 * Sprint 3 — Smart Task (extended in Sprint 5 to power Bill, Appointment
 * and Medicine through [SmartReminderFormConfig], plus an EDIT mode).
 *
 * The REAL Smart Item flow replacing the "Coming Soon" placeholders for
 * reminder-shaped types picked from the Dashboard's "+" bottom sheet.
 *
 * This screen is UI ONLY — it builds a [ReminderEntity] and hands it to
 * [onSave]. Persistence and alarm scheduling are the EXISTING pipeline
 * (MainViewModel.addReminder / updateTaskReminder → NotificationRepository
 * → AlarmManagerScheduler). No second reminder implementation exists.
 *
 *  - Priority   → maps onto the existing PreAlertOption system (see
 *                 [TaskPriority]); the schema is untouched.
 *  - Repeat     → the existing, fully supported [RecurrenceType].
 *  - Edit mode  → pass [initial]; the form pre-fills and Save keeps the
 *                 same row id (and original category) so the existing
 *                 update pipeline replaces the scheduled alarm.
 */
/**
 * Encodes/decodes a task checklist to the reminders.checklist column.
 * Format: items joined by "||", each "1:text" (done) or "0:text".
 * Round-trip safe: newlines and the "||" separator are stripped from
 * item text on encode.
 */
object ChecklistCodec {
    fun decode(raw: String): List<Pair<Boolean, String>> {
        if (raw.isBlank()) return emptyList()
        return raw.split("||").mapNotNull { part ->
            val idx = part.indexOf(':')
            if (idx <= 0) null
            else (part.substring(0, idx) == "1") to part.substring(idx + 1)
        }
    }

    fun encode(items: List<Pair<Boolean, String>>): String =
        items.filter { it.second.isNotBlank() }.joinToString("||") { (done, text) ->
            val clean = text.replace("||", "/").replace("\n", " ")
            (if (done) "1:" else "0:") + clean
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    isArabic: Boolean,
    config: SmartReminderFormConfig = SmartReminderFormConfig.Task,
    initial: ReminderEntity? = null,
    onSave: (ReminderEntity) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.note ?: "") }
    var priority by remember {
        mutableStateOf(
            initial?.let { TaskPriority.fromPreAlerts(it.preAlerts) } ?: TaskPriority.MEDIUM
        )
    }
    var recurrence by remember {
        mutableStateOf(
            initial?.let { RecurrenceType.fromString(it.recurrence) } ?: config.defaultRecurrence
        )
    }
    // Phase A — rich fields.
    var tags by remember { mutableStateOf(initial?.tags ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var progress by remember { mutableFloatStateOf((initial?.progress ?: 0).toFloat()) }
    val checklist = remember {
        mutableStateListOf<Pair<Boolean, String>>().apply {
            addAll(ChecklistCodec.decode(initial?.checklist ?: ""))
        }
    }
    var newChecklistItem by remember { mutableStateOf("") }

    // Default due moment: tomorrow at 09:00 — same "tomorrow" default the
    // existing AddReminderDialog uses, just with a friendlier fixed hour.
    // In edit mode the stored due date wins.
    val defaultCal = remember {
        Calendar.getInstance().apply {
            if (initial != null) {
                timeInMillis = initial.dueDate
            } else {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var dateMillis by remember { mutableLongStateOf(defaultCal.timeInMillis) }
    var hour by remember { mutableStateOf(defaultCal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(defaultCal.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val displayedTimeCal = remember(hour, minute) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            initial != null && isArabic -> config.editTitleAr
                            initial != null -> config.editTitleEn
                            isArabic -> config.newTitleAr
                            else -> config.newTitleEn
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppPadding.screen)
                .padding(top = Spacing.sm, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (isArabic) "العنوان" else "Title") },
                placeholder = { Text(if (isArabic) "ماذا تريد أن تتذكر؟" else "What do you want to remember?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isArabic) "الوصف" else "Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Date + Time — read-only fields that open the Material 3 pickers.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PickerField(
                    value = dateFormat.format(Date(dateMillis)),
                    label = if (isArabic) "التاريخ" else "Date",
                    icon = Icons.Default.CalendarMonth,
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                )
                PickerField(
                    value = timeFormat.format(displayedTimeCal.time),
                    label = if (isArabic) "الوقت" else "Time",
                    icon = Icons.Default.AccessTime,
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Priority — maps to the EXISTING pre-alert system (no schema change).
            Text(
                text = if (isArabic) "الأولوية" else "Priority",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TaskPriority.entries.forEachIndexed { index, p ->
                    SegmentedButton(
                        selected = priority == p,
                        onClick = { priority = p },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TaskPriority.entries.size
                        )
                    ) {
                        Text(if (isArabic) p.displayNameAr else p.displayNameEn)
                    }
                }
            }

            // Repeat — the EXISTING RecurrenceType (fully supported).
            Text(
                text = if (isArabic) "التكرار" else "Repeat",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(RecurrenceType.entries) { r ->
                    FilterChip(
                        selected = recurrence == r,
                        onClick = { recurrence = r },
                        leadingIcon = if (recurrence == r) {
                            { Icon(Icons.Default.Repeat, contentDescription = null) }
                        } else null,
                        label = { Text(if (isArabic) r.displayNameAr else r.displayNameEn) }
                    )
                }
            }

            // Rich power-user fields (tags / location / progress / checklist)
            // were removed to keep the task form calm and companion-simple.
            // Their backing state stays at its empty defaults on save.

            PremiumButton(
                text = if (isArabic) "حفظ" else "Save",
                enabled = title.isNotBlank(),
                onClick = {
                    // Merge the picked (UTC) date with the picked local time.
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = dateMillis
                    }
                    val dueCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSave(
                        // Edit mode keeps the same row id (and createdAt/
                        // completion state) so the existing update pipeline
                        // replaces the reminder in place; create mode stores
                        // this form's category so each Smart Item type keeps
                        // its identity on the Tasks tab and in backups.
                        (initial ?: ReminderEntity(
                            title = "",
                            dueDate = 0L,
                            category = config.category.name
                        )).copy(
                            title = title.trim(),
                            note = description.trim(),
                            dueDate = dueCal.timeInMillis,
                            recurrence = recurrence.name,
                            preAlerts = priority.toPreAlertsString(),
                            tags = tags.trim(),
                            location = location.trim(),
                            progress = progress.toInt(),
                            checklist = ChecklistCodec.encode(checklist)
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            PremiumOutlinedButton(
                text = if (isArabic) "إلغاء" else "Cancel",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) { Text(if (isArabic) "تم" else "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (isArabic) "اختر الوقت" else "Select time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        hour = timePickerState.hour
                        minute = timePickerState.minute
                        showTimePicker = false
                    }
                ) { Text(if (isArabic) "تم" else "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

/**
 * A read-only OutlinedTextField that opens a picker when tapped.
 * Shared by the Create Task and Create Debt forms (date/time fields).
 */
@Composable
internal fun PickerField(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(imageVector = icon, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay so the whole field is tappable even though the
        // underlying text field is read-only.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        )
    }
}
