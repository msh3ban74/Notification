package com.notification.app.ui.screens

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.notification.app.ui.permissions.canScheduleExactAlarms
import com.notification.app.ui.permissions.openExactAlarmSettings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumButton
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.designsystem.PremiumOutlinedButton
import com.notification.app.ui.theme.Spacing
import java.util.Calendar

/**
 * Stability sprint — Smart Alarm.
 *
 * The real "add an alarm that rings" flow that was missing. Reuses the
 * EXISTING AlarmEntity + MainViewModel.addAlarm + AlarmManagerScheduler
 * (exact-alarm scheduling → AlarmReceiver → AlarmRingingActivity), so no
 * new alarm logic is introduced. Lets the user pick a time, a label, and
 * a ringtone from the system picker.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CreateAlarmScreen(
    isArabic: Boolean,
    onSave: (AlarmEntity) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("") }

    // Alarm engine sprint — per-alarm options.
    var vibrate by remember { mutableStateOf(true) }
    var flashlight by remember { mutableStateOf(false) }
    var volumePercent by remember { mutableFloatStateOf(100f) }
    var snoozeMinutes by remember { mutableIntStateOf(10) }
    var autoStopMinutes by remember { mutableIntStateOf(5) }
    // Weekly repeat — Calendar.DAY_OF_WEEK values (1=Sun … 7=Sat). Empty = once.
    val repeatDays = remember { mutableStateListOf<Int>() }

    val now = remember { Calendar.getInstance() }
    val timeState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = false
    )

    // Default to the system alarm/notification sound; let the user change it.
    var ringtoneUri by remember {
        mutableStateOf(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString()
                ?: ""
        )
    }
    var ringtoneName by remember { mutableStateOf(currentRingtoneTitle(context, ringtoneUri, isArabic)) }

    val ringtonePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val picked: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (picked != null) {
            ringtoneUri = picked.toString()
            ringtoneName = currentRingtoneTitle(context, ringtoneUri, isArabic)
        }
    }

    // Any audio file from the device — music, downloads, recordings.
    // OpenDocument grants a URI we persist so the alarm can read it later,
    // even weeks after it was picked.
    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked: Uri? ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            ringtoneUri = picked.toString()
            ringtoneName = displayNameForUri(context, picked)
                ?: (if (isArabic) "ملف صوتي" else "Audio file")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "منبه جديد" else "New Alarm",
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
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) "وقت المنبه" else "Alarm time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TimePicker(state = timeState)
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(if (isArabic) "الاسم (اختياري)" else "Label (optional)") },
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Ringtone chooser — opens the system ringtone picker.
            PremiumCard(
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,
                            if (isArabic) "اختر نغمة المنبه" else "Pick an alarm sound")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            ringtoneUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                        )
                    }
                    ringtonePicker.launch(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "نغمة المنبه" else "Alarm sound",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ringtoneName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = if (isArabic) "نغمات النظام" else "System",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Second source — pick ANY audio file (music, downloads…).
            PremiumOutlinedButton(
                text = if (isArabic) "اختر ملفًا صوتيًا من الجهاز" else "Choose audio from device",
                onClick = {
                    audioFilePicker.launch(
                        arrayOf("audio/*")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Exact-alarm permission guidance — shown only if the OS is
            // withholding it. setAlarmClock works regardless, but on some
            // OEMs granting this improves reliability, so we guide the user.
            if (!canScheduleExactAlarms(context)) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isArabic) "لضمان دقة المنبه"
                            else "For the most reliable alarms",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isArabic)
                                "إذن \"المنبهات والتذكيرات الدقيقة\" غير مفعّل. فعّله ليرن المنبه في موعده بالثانية حتى في وضع توفير الطاقة."
                            else "The exact-alarms permission is off. Enable it so alarms fire precisely on time, even in battery-saver.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PremiumOutlinedButton(
                            text = if (isArabic) "فتح الإعدادات" else "Open settings",
                            onClick = { openExactAlarmSettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Options card — vibration, flashlight, volume, snooze, auto-stop.
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OptionSwitch(
                        label = if (isArabic) "اهتزاز" else "Vibration",
                        checked = vibrate, onCheckedChange = { vibrate = it }
                    )
                    OptionSwitch(
                        label = if (isArabic) "الفلاش" else "Flashlight",
                        checked = flashlight, onCheckedChange = { flashlight = it }
                    )
                    Text(
                        text = (if (isArabic) "مستوى الصوت: " else "Volume: ") + "${volumePercent.toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                    androidx.compose.material3.Slider(
                        value = volumePercent,
                        onValueChange = { volumePercent = it },
                        valueRange = 10f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StepperRow(
                        label = if (isArabic) "مدة الغفوة (دقيقة)" else "Snooze (minutes)",
                        value = snoozeMinutes,
                        onDecrement = { if (snoozeMinutes > 1) snoozeMinutes-- },
                        onIncrement = { if (snoozeMinutes < 60) snoozeMinutes++ }
                    )
                    StepperRow(
                        label = if (isArabic) "إيقاف تلقائي بعد (دقيقة)" else "Auto-stop after (minutes)",
                        value = autoStopMinutes,
                        onDecrement = { if (autoStopMinutes > 0) autoStopMinutes-- },
                        onIncrement = { if (autoStopMinutes < 30) autoStopMinutes++ },
                        zeroLabel = if (isArabic) "بدون" else "Off"
                    )

                    // Weekly repeat — tap days; none selected = one-time alarm.
                    Text(
                        text = if (isArabic) "التكرار الأسبوعي" else "Repeat weekly",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val dayLabels = if (isArabic)
                        listOf("الأحد" to 1, "الاثنين" to 2, "الثلاثاء" to 3, "الأربعاء" to 4, "الخميس" to 5, "الجمعة" to 6, "السبت" to 7)
                    else
                        listOf("Su" to 1, "Mo" to 2, "Tu" to 3, "We" to 4, "Th" to 5, "Fr" to 6, "Sa" to 7)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dayLabels.forEach { (lbl, dow) ->
                            androidx.compose.material3.FilterChip(
                                selected = dow in repeatDays,
                                onClick = {
                                    if (dow in repeatDays) repeatDays.remove(dow) else repeatDays.add(dow)
                                },
                                label = { Text(lbl, maxLines = 1) }
                            )
                        }
                    }
                    Text(
                        text = if (repeatDays.isEmpty()) {
                            if (isArabic) "مرة واحدة" else "One-time"
                        } else {
                            if (isArabic) "يتكرر أسبوعيًا في الأيام المحددة" else "Repeats weekly on the selected days"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PremiumButton(
                text = if (isArabic) "احفظ المنبه" else "Save Alarm",
                onClick = {
                    onSave(
                        AlarmEntity(
                            title = label.trim().ifBlank {
                                if (isArabic) "منبه" else "Alarm"
                            },
                            timeInMillis = nextTriggerFor(timeState.hour, timeState.minute),
                            ringtoneUri = ringtoneUri,
                            isEnabled = true,
                            label = label.trim(),
                            vibrate = vibrate,
                            flashlight = flashlight,
                            volumePercent = volumePercent.toInt(),
                            snoozeMinutes = snoozeMinutes,
                            autoStopMinutes = autoStopMinutes,
                            repeatDays = repeatDays.sorted().joinToString(",")
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
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    zeroLabel: String? = null
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDecrement) {
            Icon(Icons.Default.Remove, contentDescription = "-")
        }
        Text(
            text = if (value == 0 && zeroLabel != null) zeroLabel else value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.Add, contentDescription = "+")
        }
    }
}

/** Next occurrence of hour:minute — today if still ahead, else tomorrow. */
private fun nextTriggerFor(hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (cal.timeInMillis <= System.currentTimeMillis()) {
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return cal.timeInMillis
}

private fun currentRingtoneTitle(
    context: android.content.Context,
    uri: String,
    isArabic: Boolean
): String {
    if (uri.isBlank()) return if (isArabic) "الافتراضي" else "Default"
    return runCatching {
        RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context)
    }.getOrNull() ?: (if (isArabic) "الافتراضي" else "Default")
}

/** Human-readable file name for a picked content:// audio document. */
private fun displayNameForUri(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }.getOrNull()
}
