package com.notification.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import com.notification.app.data.local.entities.Gam3iyaAttachmentEntity
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.Gam3iyaPaymentEntity
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.StatementExporter
import com.notification.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gam3iyaDetailScreen(
    viewModel: MainViewModel,
    gam3iya: Gam3iyaEntity,
    isArabic: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isParticipant = gam3iya.mode == "PARTICIPANT"

    val members by viewModel.getMembersForGam3iya(gam3iya.id).collectAsState(initial = emptyList())
    val payments by viewModel.getPaymentsForGam3iya(gam3iya.id).collectAsState(initial = emptyList())
    val attachments by viewModel.getAttachmentsForGam3iya(gam3iya.id).collectAsState(initial = emptyList())
    val status = remember(gam3iya, members, payments) {
        Gam3iyaCalculator.computeStatus(gam3iya, members, payments)
    }

    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editMember by remember { mutableStateOf<Gam3iyaMemberEntity?>(null) }
    var addingMember by remember { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            takePersistable(context, uri)
            viewModel.addGam3iyaAttachment(gam3iya.id, 0, uri.toString(), "RECEIPT", "")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gam3iya.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = {
                        val statement = StatementExporter.generateGam3iyaStatement(gam3iya, members)
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, statement)
                        }, if (isArabic) "مشاركة الجمعية" else "Share gam3iya"))
                    }) { Icon(Icons.Default.Share, contentDescription = null) }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(if (isArabic) "تعديل" else "Edit") }, onClick = { menuOpen = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                        if (gam3iya.status != "ARCHIVED") {
                            DropdownMenuItem(text = { Text(if (isArabic) "أرشفة" else "Archive") }, onClick = { menuOpen = false; viewModel.setGam3iyaStatus(gam3iya, "ARCHIVED") },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) })
                        } else {
                            DropdownMenuItem(text = { Text(if (isArabic) "إلغاء الأرشفة" else "Unarchive") }, onClick = { menuOpen = false; viewModel.setGam3iyaStatus(gam3iya, "ACTIVE") },
                                leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) })
                        }
                        DropdownMenuItem(text = { Text(if (isArabic) "وضع كمنتهية" else "Mark completed") }, onClick = { menuOpen = false; viewModel.setGam3iyaStatus(gam3iya, "COMPLETED") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) })
                        DropdownMenuItem(text = { Text(if (isArabic) "حذف" else "Delete") }, onClick = { menuOpen = false; confirmDelete = true },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isParticipant && gam3iya.status != "ARCHIVED") {
                ExtendedFloatingActionButton(
                    onClick = { addingMember = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text(if (isArabic) "عضو" else "Member") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {
            item { StatusOverviewCard(gam3iya, status, isArabic, df) }
            item {
                Text(if (isArabic) "التقويم" else "Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item { Gam3iyaCalendarCard(gam3iya, members, payments, isArabic) }
            item { Gam3iyaReportsCard(gam3iya, members, payments, status, isArabic) }

            if (isParticipant) {
                item { OrganizerCard(gam3iya, isArabic, context) }
                item {
                    ParticipantProgressCard(gam3iya, status, isArabic, df,
                        onRecordPayment = { viewModel.participantRecordPayment(gam3iya) })
                }
            } else {
                item {
                    Text(if (isArabic) "الأعضاء (${members.size})" else "Members (${members.size})",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (members.isEmpty()) {
                    item {
                        Text(if (isArabic) "أضف الأعضاء من زر «عضو»" else "Add members with the “Member” button",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(members, key = { it.id }) { m ->
                    MemberCard(
                        gam3iya = gam3iya, member = m, isArabic = isArabic, df = df, context = context,
                        onTogglePaid = { viewModel.markMemberInstallmentPaid(gam3iya, m, it) },
                        onToggleCollected = { viewModel.markMemberCollected(gam3iya, m, it) },
                        onEdit = { editMember = m },
                        onDelete = { viewModel.deleteGam3iyaMember(m) }
                    )
                }
            }

            // Payment history
            item {
                Text(if (isArabic) "سجل الدفعات" else "Payment history",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp))
            }
            if (payments.isEmpty()) {
                item {
                    Text(if (isArabic) "لا دفعات مسجّلة بعد" else "No payments logged yet",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(payments, key = { it.id }) { p ->
                PaymentRow(gam3iya, p, members, isArabic, df, onDelete = { viewModel.deleteGam3iyaPayment(p) })
            }

            // Attachments
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isArabic) "المرفقات والإيصالات" else "Attachments & receipts",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { receiptPicker.launch(arrayOf("image/*", "application/pdf")) }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null); Spacer(Modifier.width(4.dp))
                        Text(if (isArabic) "إضافة" else "Add")
                    }
                }
            }
            items(attachments, key = { it.id }) { a ->
                AttachmentRow(a, isArabic, context, onDelete = { viewModel.deleteGam3iyaAttachment(a) })
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isArabic) "حذف الجمعية؟" else "Delete gam3iya?") },
            text = { Text(if (isArabic) "سيتم حذف الجمعية وكل أعضائها ودفعاتها ومرفقاتها نهائيًا." else "This permanently deletes the gam3iya with all its members, payments and attachments.") },
            confirmButton = {
                Button(onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(if (isArabic) "حذف" else "Delete")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(if (isArabic) "إلغاء" else "Cancel") } }
        )
    }

    if (addingMember) {
        MemberEditDialog(
            isArabic = isArabic,
            existing = null,
            defaultTurn = members.size + 1,
            onDismiss = { addingMember = false },
            onSave = { m -> viewModel.addGam3iyaMember(gam3iya.id, m); addingMember = false }
        )
    }
    editMember?.let { m ->
        MemberEditDialog(
            isArabic = isArabic,
            existing = m,
            defaultTurn = m.turnMonth,
            onDismiss = { editMember = null },
            onSave = { updated -> viewModel.updateGam3iyaMember(updated); editMember = null }
        )
    }
}

@Composable
private fun StatusOverviewCard(
    gam3iya: Gam3iyaEntity,
    status: com.notification.app.domain.calculator.Gam3iyaStatus,
    isArabic: Boolean,
    df: SimpleDateFormat
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isArabic) "الحالة الحالية" else "Live status",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            LinearProgressIndicator(progress = { status.progressFraction }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            InfoRow(if (isArabic) "الشهر الحالي" else "Current month", "${status.currentMonthIndex} / ${status.durationMonths}")
            InfoRow(if (isArabic) "الأشهر المتبقية" else "Remaining months", status.remainingMonths.toString())
            InfoRow(if (isArabic) "إجمالي المدفوع" else "Total paid", fmtMoney(status.totalPaid, gam3iya.currency))
            InfoRow(if (isArabic) "المبلغ المتبقي" else "Remaining amount", fmtMoney(status.remainingAmount, gam3iya.currency))
            if (gam3iya.mode != "PARTICIPANT" && status.nextCollector != null) {
                InfoRow(if (isArabic) "التالي في القبض" else "Next collector",
                    "${status.nextCollector.memberName} • ${df.format(Date(status.nextCollectionDate))}")
            }
            InfoRow(if (isArabic) "النهاية المتوقعة" else "Projected end", df.format(Date(status.projectedEndDate)))
            if (status.lateMembers.isNotEmpty()) {
                InfoRow(if (isArabic) "متأخرون" else "Late members", status.lateMembers.size.toString(), MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(value, fontWeight = FontWeight.Bold, color = valueColor ?: MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun OrganizerCard(gam3iya: Gam3iyaEntity, isArabic: Boolean, context: android.content.Context) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (isArabic) "المنظّم" else "Organizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (gam3iya.organizerName.isNotBlank()) Text(gam3iya.organizerName, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (gam3iya.organizerPhone.isNotBlank()) {
                    AssistChip(onClick = { dial(context, gam3iya.organizerPhone) },
                        label = { Text(if (isArabic) "اتصال" else "Call") },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp)) })
                }
                if (gam3iya.organizerWhatsapp.isNotBlank()) {
                    AssistChip(onClick = { whatsapp(context, gam3iya.organizerWhatsapp) },
                        label = { Text("WhatsApp") },
                        leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) })
                }
                if (gam3iya.organizerEmail.isNotBlank()) {
                    AssistChip(onClick = { email(context, gam3iya.organizerEmail) },
                        label = { Text(if (isArabic) "بريد" else "Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) })
                }
            }
        }
    }
}

@Composable
private fun ParticipantProgressCard(
    gam3iya: Gam3iyaEntity,
    status: com.notification.app.domain.calculator.Gam3iyaStatus,
    isArabic: Boolean,
    df: SimpleDateFormat,
    onRecordPayment: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isArabic) "مشاركتي" else "My participation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            InfoRowPlain(if (isArabic) "أقساط مدفوعة" else "Installments paid", "${gam3iya.myPaidInstallments} / ${status.durationMonths}")
            if (gam3iya.myTurnNumber > 0) InfoRowPlain(if (isArabic) "دوري" else "My turn", gam3iya.myTurnNumber.toString())
            if (status.nextCollectionDate > 0) InfoRowPlain(if (isArabic) "تاريخ قبضي" else "My collection", df.format(Date(status.nextCollectionDate)))
            Button(onClick = onRecordPayment, colors = ButtonDefaults.buttonColors(containerColor = com.notification.app.ui.theme.MaroonPrimary), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Payments, contentDescription = null); Spacer(Modifier.width(6.dp))
                Text(if (isArabic) "سجّل قسطًا" else "Record a payment")
            }
        }
    }
}

@Composable
private fun InfoRowPlain(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MemberCard(
    gam3iya: Gam3iyaEntity,
    member: Gam3iyaMemberEntity,
    isArabic: Boolean,
    df: SimpleDateFormat,
    context: android.content.Context,
    onTogglePaid: (Boolean) -> Unit,
    onToggleCollected: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (member.photoUri.isNotBlank()) {
                    AsyncImage(model = member.photoUri, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                } else {
                    Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${member.turnMonth}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.memberName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((if (isArabic) "الدور ${member.turnMonth} • " else "Turn ${member.turnMonth} • ") + df.format(Date(member.payoutDate)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (member.isLate) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = null) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            }
            // contact quick actions
            if (member.phone.isNotBlank() || member.whatsapp.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    if (member.phone.isNotBlank()) AssistChip(onClick = { dial(context, member.phone) }, label = { Text(if (isArabic) "اتصال" else "Call") }, leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp)) })
                    if (member.whatsapp.isNotBlank()) AssistChip(onClick = { whatsapp(context, member.whatsapp) }, label = { Text("WhatsApp") }, leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp)) })
                }
            }
            Divider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isArabic) "دفع القسط" else "Paid installment", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = member.isInstallmentPaidThisMonth, onCheckedChange = onTogglePaid)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isArabic) "استلم دوره (قبض)" else "Collected payout", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = member.isPayoutReceived, onCheckedChange = onToggleCollected)
            }
        }
    }
}

@Composable
private fun PaymentRow(
    gam3iya: Gam3iyaEntity,
    payment: Gam3iyaPaymentEntity,
    members: List<Gam3iyaMemberEntity>,
    isArabic: Boolean,
    df: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val who = when {
        payment.memberId == 0L -> if (isArabic) "أنا" else "Me"
        else -> members.firstOrNull { it.id == payment.memberId }?.memberName ?: (if (isArabic) "عضو" else "Member")
    }
    val typeLabel = if (payment.type == "COLLECTION") (if (isArabic) "قبض" else "Collection") else (if (isArabic) "قسط" else "Installment")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (payment.type == "COLLECTION") Icons.Default.CallReceived else Icons.Default.CallMade,
                contentDescription = null, tint = if (payment.type == "COLLECTION") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("$who • $typeLabel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(df.format(Date(payment.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(fmtMoney(payment.amount, gam3iya.currency), fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun AttachmentRow(a: Gam3iyaAttachmentEntity, isArabic: Boolean, context: android.content.Context, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(a.label.ifBlank { if (isArabic) "إيصال" else "Receipt" }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.uri)).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
                } catch (_: Exception) {}
            }) { Text(if (isArabic) "فتح" else "Open") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun MemberEditDialog(
    isArabic: Boolean,
    existing: Gam3iyaMemberEntity?,
    defaultTurn: Int,
    onDismiss: () -> Unit,
    onSave: (Gam3iyaMemberEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.memberName ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var whatsapp by remember { mutableStateOf(existing?.whatsapp ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var nationalId by remember { mutableStateOf(existing?.nationalId ?: "") }
    var installment by remember { mutableStateOf(existing?.installmentAmount?.takeIf { it > 0 }?.let { trimNum(it) } ?: "") }
    var turn by remember { mutableStateOf((existing?.turnMonth ?: defaultTurn).toString()) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var photoUri by remember { mutableStateOf(existing?.photoUri ?: "") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { takePersistable(context, uri); photoUri = uri.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) (if (isArabic) "عضو جديد" else "New member") else (if (isArabic) "تعديل العضو" else "Edit member")) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (photoUri.isNotBlank()) AsyncImage(model = photoUri, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape))
                    else Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) { Text(if (isArabic) "صورة" else "Photo") }
                }
                Field(name, { name = it }, if (isArabic) "الاسم *" else "Name *")
                Field(turn, { turn = it }, if (isArabic) "رقم الدور" else "Turn number", KeyboardType.Number)
                Field(phone, { phone = it }, if (isArabic) "الهاتف" else "Phone", KeyboardType.Phone)
                Field(whatsapp, { whatsapp = it }, "WhatsApp", KeyboardType.Phone)
                Field(email, { email = it }, if (isArabic) "البريد" else "Email", KeyboardType.Email)
                Field(address, { address = it }, if (isArabic) "العنوان" else "Address")
                Field(nationalId, { nationalId = it }, if (isArabic) "الرقم القومي" else "National ID")
                Field(installment, { installment = it }, if (isArabic) "قسط مخصص" else "Custom installment", KeyboardType.Number)
                Field(notes, { notes = it }, if (isArabic) "ملاحظات" else "Notes")
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) return@Button
                val base = existing ?: Gam3iyaMemberEntity(gam3iyaId = 0, memberName = "", turnMonth = defaultTurn, payoutDate = 0)
                onSave(base.copy(
                    memberName = name.trim(), turnMonth = turn.toIntOrNull() ?: defaultTurn,
                    phone = phone.trim(), whatsapp = whatsapp.trim(), email = email.trim(),
                    address = address.trim(), nationalId = nationalId.trim(),
                    installmentAmount = installment.toDoubleOrNull() ?: 0.0, notes = notes.trim(),
                    photoUri = photoUri
                ))
            }) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") } }
    )
}

// ── Contact intents ─────────────────────────────────────────────────────
private fun dial(context: android.content.Context, phone: String) {
    try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}"))) } catch (_: Exception) {}
}
private fun whatsapp(context: android.content.Context, number: String) {
    val n = number.filter { it.isDigit() || it == '+' }.removePrefix("+")
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$n"))) } catch (_: Exception) {}
}
private fun email(context: android.content.Context, addr: String) {
    try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${addr.trim()}"))) } catch (_: Exception) {}
}
