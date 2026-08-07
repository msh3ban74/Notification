package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.ui.components.RafeeqAtmosphere
import com.notification.app.ui.components.RafeeqWeather
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumButton
import com.notification.app.ui.designsystem.PremiumOutlinedButton
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * «مناسباتك» — the dedicated Occasions form.
 *
 * A wedding, a birthday, an engagement, a gathering: pick the occasion
 * type, say whose it is, pick the day and time — Rafeeq saves it as a
 * real scheduled reminder (with the standard day-before + hour-before
 * pre-alerts) so the user never misses it, and the assistant can answer
 * questions about it like everything else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    isArabic: Boolean,
    onSave: (ReminderEntity) -> Unit,
    onCancel: () -> Unit
) {
    data class EventKind(val id: String, val ar: String, val en: String, val category: String)
    val kinds = listOf(
        EventKind("wedding", "فرح", "Wedding", ReminderCategory.EVENT.name),
        EventKind("birthday", "عيد ميلاد", "Birthday", ReminderCategory.BIRTHDAY.name),
        EventKind("engagement", "خطوبة", "Engagement", ReminderCategory.EVENT.name),
        EventKind("gathering", "عزومة", "Gathering", ReminderCategory.EVENT.name),
        EventKind("other", "مناسبة أخرى", "Other", ReminderCategory.EVENT.name)
    )
    var kind by remember { mutableStateOf(kinds.first()) }
    var host by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val defaultTime = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 19); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var eventMillis by remember { mutableStateOf(defaultTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFmt = remember(isArabic) {
        SimpleDateFormat("EEEE، d MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)
    }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "مناسبة جديدة" else "New Occasion",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isArabic) "رجوع" else "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            RafeeqAtmosphere(
                palette = RafeeqWeather.Alerts,
                modifier = Modifier.fillMaxWidth().height(240.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppPadding.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = if (isArabic) "نوع المناسبة" else "Occasion type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    kinds.take(3).forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(if (isArabic) k.ar else k.en) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    kinds.drop(3).forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(if (isArabic) k.ar else k.en) }
                        )
                    }
                }

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(if (isArabic) "صاحب المناسبة" else "Host") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateFmt.format(Date(eventMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isArabic) "يوم المناسبة" else "Day") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeFmt.format(Date(eventMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isArabic) "الوقت" else "Time") },
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isArabic) "ملاحظات (اختياري)" else "Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                PremiumButton(
                    text = if (isArabic) "حفظ المناسبة" else "Save occasion",
                    onClick = {
                        val kindLabel = if (isArabic) kind.ar else kind.en
                        val title = if (host.isBlank()) kindLabel
                        else if (isArabic) "$kindLabel ${host.trim()}"
                        else "${host.trim()}'s $kindLabel"
                        onSave(
                            ReminderEntity(
                                title = title,
                                note = buildString {
                                    append(if (isArabic) "المناسبة: $kindLabel" else "Occasion: $kindLabel")
                                    if (host.isNotBlank()) {
                                        append(if (isArabic) " • صاحبها: ${host.trim()}" else " • Host: ${host.trim()}")
                                    }
                                    if (note.isNotBlank()) append(" • ${note.trim()}")
                                },
                                dueDate = eventMillis,
                                category = kind.category
                            )
                        )
                    },
                    enabled = eventMillis > System.currentTimeMillis(),
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)
                )
                PremiumOutlinedButton(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl)
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = eventMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val cur = Calendar.getInstance().apply { timeInMillis = eventMillis }
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = picked
                            set(Calendar.HOUR_OF_DAY, cur.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, cur.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        eventMillis = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text(if (isArabic) "تم" else "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val cur = Calendar.getInstance().apply { timeInMillis = eventMillis }
        val state = rememberTimePickerState(
            initialHour = cur.get(Calendar.HOUR_OF_DAY),
            initialMinute = cur.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (isArabic) "اختر الوقت" else "Select time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = eventMillis
                        set(Calendar.HOUR_OF_DAY, state.hour)
                        set(Calendar.MINUTE, state.minute)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    eventMillis = cal.timeInMillis
                    showTimePicker = false
                }) { Text(if (isArabic) "تم" else "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
