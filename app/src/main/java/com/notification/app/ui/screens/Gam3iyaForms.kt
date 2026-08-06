package com.notification.app.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/** Editable draft for one member row while creating a gam3iya. */
private data class MemberDraft(
    var name: String = "",
    var phone: String = "",
    var whatsapp: String = "",
    var installment: String = ""
)

internal fun takePersistable(context: android.content.Context, uri: android.net.Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) { }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerGam3iyaForm(
    viewModel: MainViewModel,
    isArabic: Boolean,
    existing: Gam3iyaEntity?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val editing = existing != null

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: "EGP") }
    var total by remember { mutableStateOf(existing?.totalAmount?.takeIf { it > 0 }?.let { trimNum(it) } ?: "") }
    var installment by remember { mutableStateOf(existing?.monthlyInstallment?.takeIf { it > 0 }?.let { trimNum(it) } ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate?.takeIf { it > 0 } ?: System.currentTimeMillis()) }
    var payoutDay by remember { mutableStateOf((existing?.payoutDayOfMonth ?: 1).toString()) }
    var collectionTime by remember { mutableStateOf(existing?.collectionTime ?: "") }
    var duration by remember { mutableStateOf(existing?.durationMonths?.takeIf { it > 0 }?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var photoUri by remember { mutableStateOf(existing?.photoUri ?: "") }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }
    var reminderDays by remember { mutableStateOf((existing?.reminderDaysBefore ?: 1).toString()) }
    var alarmEnabled by remember { mutableStateOf(existing?.alarmEnabled ?: false) }

    // Member drafts only used when creating a NEW gam3iya.
    val members = remember { mutableStateListOf(MemberDraft(), MemberDraft(), MemberDraft()) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { takePersistable(context, uri); photoUri = uri.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) (if (isArabic) "تعديل الجمعية" else "Edit gam3iya") else (if (isArabic) "جمعية جديدة (أديرها)" else "New gam3iya (I manage)"), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            // Photo
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (photoUri.isNotBlank()) {
                    AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape))
                } else {
                    Box(Modifier.size(56.dp).clip(CircleShape).then(Modifier), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) {
                    Text(if (isArabic) "صورة الجمعية" else "Gam3iya photo")
                }
                if (photoUri.isNotBlank()) TextButton(onClick = { photoUri = "" }) { Text(if (isArabic) "إزالة" else "Remove") }
            }

            Field(title, { title = it }, if (isArabic) "اسم الجمعية *" else "Gam3iya name *")
            Field(description, { description = it }, if (isArabic) "الوصف" else "Description")
            CurrencyDropdown(currency, { currency = it }, isArabic)
            Field(total, { total = it }, if (isArabic) "المبلغ الكلي" else "Total amount", KeyboardType.Number)
            Field(installment, { installment = it }, if (isArabic) "القسط الشهري *" else "Monthly installment *", KeyboardType.Number)
            DateField(startDate, { startDate = it }, if (isArabic) "تاريخ البداية" else "Start date", context, isArabic)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Field(payoutDay, { payoutDay = it }, if (isArabic) "يوم القبض" else "Collection day", KeyboardType.Number) }
                Box(Modifier.weight(1f)) { Field(collectionTime, { collectionTime = it }, if (isArabic) "الوقت (HH:mm)" else "Time (HH:mm)") }
            }
            Field(duration, { duration = it }, if (isArabic) "المدة بالأشهر (اختياري)" else "Duration months (optional)", KeyboardType.Number)
            Field(note, { note = it }, if (isArabic) "ملاحظات" else "Notes")

            // Reminder / alarm
            SwitchRow(if (isArabic) "تذكير قبل القبض" else "Reminder before collection", reminderEnabled) { reminderEnabled = it }
            if (reminderEnabled) Field(reminderDays, { reminderDays = it }, if (isArabic) "قبل بكم يوم" else "Days before", KeyboardType.Number)
            SwitchRow(if (isArabic) "منبّه صوتي" else "Alarm", alarmEnabled) { alarmEnabled = it }

            if (!editing) {
                Divider()
                Text(if (isArabic) "الأعضاء" else "Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                members.forEachIndexed { idx, m ->
                    MemberDraftCard(idx + 1, m, isArabic,
                        onChange = { members[idx] = it },
                        onRemove = { if (members.size > 1) members.removeAt(idx) }
                    )
                }
                OutlinedButton(onClick = { members.add(MemberDraft()) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null); Spacer(Modifier.width(6.dp))
                    Text(if (isArabic) "إضافة عضو" else "Add member")
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val inst = installment.toDoubleOrNull() ?: 0.0
                    if (title.isBlank() || inst <= 0) return@Button
                    val base = (existing ?: Gam3iyaEntity(
                        title = "", totalAmount = 0.0, monthlyInstallment = 0.0,
                        membersCount = 0, startDate = startDate
                    )).copy(
                        title = title.trim(), description = description.trim(), currency = currency,
                        totalAmount = total.toDoubleOrNull() ?: 0.0, monthlyInstallment = inst,
                        startDate = startDate, payoutDayOfMonth = payoutDay.toIntOrNull() ?: 1,
                        collectionTime = collectionTime.trim(), durationMonths = duration.toIntOrNull() ?: 0,
                        note = note.trim(), photoUri = photoUri, reminderEnabled = reminderEnabled,
                        reminderDaysBefore = reminderDays.toIntOrNull() ?: 1, alarmEnabled = alarmEnabled,
                        mode = "MANAGER"
                    )
                    if (editing) {
                        viewModel.updateGam3iya(base)
                    } else {
                        val list = members.filter { it.name.isNotBlank() }.mapIndexed { i, d ->
                            Gam3iyaMemberEntity(
                                gam3iyaId = 0, memberName = d.name.trim(), turnMonth = i + 1, payoutDate = 0,
                                phone = d.phone.trim(), whatsapp = d.whatsapp.trim(),
                                installmentAmount = d.installment.toDoubleOrNull() ?: 0.0
                            )
                        }
                        viewModel.createManagerGam3iya(base.copy(membersCount = list.size), list)
                    }
                    onDone()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (editing) (if (isArabic) "حفظ" else "Save") else (if (isArabic) "إنشاء" else "Create")) }
            Spacer(Modifier.height(40.dp))
        }
    }
}

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
    var currency by remember { mutableStateOf(existing?.currency ?: "EGP") }
    var orgName by remember { mutableStateOf(existing?.organizerName ?: "") }
    var orgPhone by remember { mutableStateOf(existing?.organizerPhone ?: "") }
    var orgWhats by remember { mutableStateOf(existing?.organizerWhatsapp ?: "") }
    var orgEmail by remember { mutableStateOf(existing?.organizerEmail ?: "") }
    var myInstallment by remember { mutableStateOf(existing?.myInstallmentAmount?.takeIf { it > 0 }?.let { trimNum(it) } ?: "") }
    var myTurn by remember { mutableStateOf(existing?.myTurnNumber?.takeIf { it > 0 }?.toString() ?: "") }
    var myCollectionDate by remember { mutableStateOf(existing?.myCollectionDate ?: 0L) }
    var startDate by remember { mutableStateOf(existing?.startDate?.takeIf { it > 0 } ?: System.currentTimeMillis()) }
    var duration by remember { mutableStateOf(existing?.durationMonths?.takeIf { it > 0 }?.toString() ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }
    var alarmEnabled by remember { mutableStateOf(existing?.alarmEnabled ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) (if (isArabic) "تعديل مشاركتي" else "Edit my gam3iya") else (if (isArabic) "جمعية أشارك فيها" else "Gam3iya I take part in"), fontWeight = FontWeight.Bold) },
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
            CurrencyDropdown(currency, { currency = it }, isArabic)
            Divider()
            Text(if (isArabic) "المنظّم" else "Organizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Field(orgName, { orgName = it }, if (isArabic) "اسم المنظّم" else "Organizer name")
            Field(orgPhone, { orgPhone = it }, if (isArabic) "هاتف المنظّم" else "Organizer phone", KeyboardType.Phone)
            Field(orgWhats, { orgWhats = it }, if (isArabic) "واتساب المنظّم" else "Organizer WhatsApp", KeyboardType.Phone)
            Field(orgEmail, { orgEmail = it }, if (isArabic) "بريد المنظّم" else "Organizer email", KeyboardType.Email)
            Divider()
            Text(if (isArabic) "مشاركتي" else "My participation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Field(myInstallment, { myInstallment = it }, if (isArabic) "قسطي الشهري *" else "My monthly installment *", KeyboardType.Number)
            Field(myTurn, { myTurn = it }, if (isArabic) "رقم دوري" else "My turn number", KeyboardType.Number)
            DateField(startDate, { startDate = it }, if (isArabic) "بداية الجمعية" else "Start date", context, isArabic)
            DateField(myCollectionDate, { myCollectionDate = it }, if (isArabic) "تاريخ قبضي" else "My collection date", context, isArabic, allowEmpty = true)
            Field(duration, { duration = it }, if (isArabic) "المدة بالأشهر" else "Duration months", KeyboardType.Number)
            Field(note, { note = it }, if (isArabic) "ملاحظات" else "Notes")
            SwitchRow(if (isArabic) "تذكير قبل القسط" else "Reminder before installment", reminderEnabled) { reminderEnabled = it }
            SwitchRow(if (isArabic) "منبّه صوتي" else "Alarm", alarmEnabled) { alarmEnabled = it }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val inst = myInstallment.toDoubleOrNull() ?: 0.0
                    if (title.isBlank() || inst <= 0) return@Button
                    val base = (existing ?: Gam3iyaEntity(
                        title = "", totalAmount = 0.0, monthlyInstallment = 0.0,
                        membersCount = 0, startDate = startDate
                    )).copy(
                        title = title.trim(), currency = currency, monthlyInstallment = inst,
                        startDate = startDate, durationMonths = duration.toIntOrNull() ?: 0, note = note.trim(),
                        reminderEnabled = reminderEnabled, alarmEnabled = alarmEnabled, mode = "PARTICIPANT",
                        organizerName = orgName.trim(), organizerPhone = orgPhone.trim(),
                        organizerWhatsapp = orgWhats.trim(), organizerEmail = orgEmail.trim(),
                        myInstallmentAmount = inst, myTurnNumber = myTurn.toIntOrNull() ?: 0,
                        myCollectionDate = myCollectionDate
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyDropdown(currency: String, onSelect: (String) -> Unit, isArabic: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = currency, onValueChange = {}, readOnly = true,
            label = { Text(if (isArabic) "العملة" else "Currency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CURRENCIES.forEach { c ->
                DropdownMenuItem(text = { Text(c) }, onClick = { onSelect(c); expanded = false })
            }
        }
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

@Composable
private fun MemberDraftCard(
    turn: Int,
    draft: MemberDraft,
    isArabic: Boolean,
    onChange: (MemberDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text((if (isArabic) "الدور " else "Turn ") + "$turn", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            }
            OutlinedTextField(draft.name, { onChange(draft.copy(name = it)) }, label = { Text(if (isArabic) "الاسم" else "Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.phone, { onChange(draft.copy(phone = it)) }, label = { Text(if (isArabic) "هاتف" else "Phone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
                OutlinedTextField(draft.whatsapp, { onChange(draft.copy(whatsapp = it)) }, label = { Text("WhatsApp") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
            }
        }
    }
}
