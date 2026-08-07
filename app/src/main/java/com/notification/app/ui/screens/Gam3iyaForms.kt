package com.notification.app.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

internal fun takePersistable(context: android.content.Context, uri: android.net.Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) { }
}

/**
 * رفيق — تسجيل الجمعية اللي انت مشترك فيها. أربع إجابات وخلاص:
 * اسمها إيه، قسطك كام، كام شهر، ودورك رقم كام — ورفيق يفكرك بالباقي.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantGam3iyaForm(
    viewModel: MainViewModel,
    isArabic: Boolean,
    existing: Gam3iyaEntity?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val editing = existing != null

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var myInstallment by remember { mutableStateOf(existing?.myInstallmentAmount?.takeIf { it > 0 }?.let { trimNum(it) } ?: "") }
    var duration by remember { mutableStateOf(existing?.durationMonths?.takeIf { it > 0 }?.toString() ?: "") }
    var myTurn by remember { mutableStateOf(existing?.myTurnNumber?.takeIf { it > 0 }?.toString() ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate?.takeIf { it > 0 } ?: System.currentTimeMillis()) }
    var orgName by remember { mutableStateOf(existing?.organizerName ?: "") }
    var orgPhone by remember { mutableStateOf(existing?.organizerPhone ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editing) (if (isArabic) "تعديل جمعيتي" else "Edit my gam3iya")
                        else (if (isArabic) "جمعية أنا فيها" else "My gam3iya"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Field(title, { title = it }, if (isArabic) "اسم الجمعية *" else "Gam3iya name *")
            Field(myInstallment, { myInstallment = it }, if (isArabic) "قسطي الشهري (ج.م) *" else "My monthly installment *", KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Field(duration, { duration = it }, if (isArabic) "كام شهر؟" else "How many months?", KeyboardType.Number) }
                Box(Modifier.weight(1f)) { Field(myTurn, { myTurn = it }, if (isArabic) "دوري رقم" else "My turn #", KeyboardType.Number) }
            }
            DateField(startDate, { startDate = it }, if (isArabic) "بدأت إمتى؟" else "Started on", context, isArabic)
            Field(orgName, { orgName = it }, if (isArabic) "المنظّم (اختياري)" else "Organizer (optional)")
            Field(orgPhone, { orgPhone = it }, if (isArabic) "رقم المنظّم (اختياري)" else "Organizer phone (optional)", KeyboardType.Phone)
            Field(note, { note = it }, if (isArabic) "ملاحظة (اختياري)" else "Note (optional)")
            SwitchRow(if (isArabic) "فكرني بالقسط كل شهر" else "Remind me every month", reminderEnabled) { reminderEnabled = it }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val inst = myInstallment.toDoubleOrNull() ?: 0.0
                    if (title.isBlank() || inst <= 0) return@Button
                    val base = (existing ?: Gam3iyaEntity(
                        title = "", totalAmount = 0.0, monthlyInstallment = 0.0,
                        membersCount = 0, startDate = startDate
                    )).copy(
                        title = title.trim(), monthlyInstallment = inst,
                        startDate = startDate, durationMonths = duration.toIntOrNull() ?: 0,
                        note = note.trim(), reminderEnabled = reminderEnabled, mode = "PARTICIPANT",
                        organizerName = orgName.trim(), organizerPhone = orgPhone.trim(),
                        myInstallmentAmount = inst, myTurnNumber = myTurn.toIntOrNull() ?: 0
                    )
                    if (editing) viewModel.updateGam3iya(base) else viewModel.createParticipantGam3iya(base)
                    onDone()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (editing) (if (isArabic) "حفظ" else "Save") else (if (isArabic) "إضافة" else "Add")) }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Shared small form widgets ──────────────────────────────────────────────

internal fun trimNum(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

@Composable
internal fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
internal fun DateField(
    value: Long,
    onPick: (Long) -> Unit,
    label: String,
    context: android.content.Context,
    isArabic: Boolean,
    allowEmpty: Boolean = false
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val shown = if (value > 0) df.format(Date(value)) else (if (isArabic) "— اختر —" else "— pick —")
    OutlinedButton(
        onClick = {
            val cal = Calendar.getInstance().apply { if (value > 0) timeInMillis = value }
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                    onPick(c.timeInMillis)
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("$label: $shown")
    }
}
