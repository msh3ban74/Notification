package com.notification.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.FinancialAttachmentEntity
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.FinancialPaymentEntity
import com.notification.app.domain.calculator.FinancialCalculator
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sprint 6 — installment detail: the live payment plan, payment recording
 * with history, and invoice/warranty/receipt attachments. Surfaces the
 * already-built FinancialCalculator + MainViewModel payment/attachment
 * functions; introduces no new data or product scope.
 */
private val PAYMENT_TYPES = listOf("PARTIAL", "FULL", "ADVANCE", "EXTRA", "RECURRING")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentDetailScreen(
    viewModel: MainViewModel,
    item: FinancialItemEntity,
    isArabic: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val payments by viewModel.getFinancialPaymentsForItem(item.id).collectAsState(initial = emptyList())
    val attachments by viewModel.getFinancialAttachmentsForItem(item.id).collectAsState(initial = emptyList())
    val plan = remember(item, payments) { FinancialCalculator.computePlan(item, payments) }

    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showPay by remember { mutableStateOf(false) }

    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            takePersistable(context, uri)
            viewModel.addFinancialAttachment(item.id, uri.toString(), "RECEIPT", "")
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                actions = {
                    IconButton(onClick = { viewModel.updateFinancialItem(item.copy(isFavorite = !item.isFavorite)) }) {
                        Icon(if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null,
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(if (isArabic) "تعديل" else "Edit") },
                            onClick = { menuOpen = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                        DropdownMenuItem(
                            text = { Text(if (item.isArchived) (if (isArabic) "إلغاء الأرشفة" else "Unarchive") else (if (isArabic) "أرشفة" else "Archive")) },
                            onClick = { menuOpen = false; viewModel.updateFinancialItem(item.copy(isArchived = !item.isArchived)) },
                            leadingIcon = { Icon(if (item.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) })
                        DropdownMenuItem(text = { Text(if (isArabic) "حذف" else "Delete") },
                            onClick = { menuOpen = false; confirmDelete = true }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) })
                    }
                }
            )
        },
        floatingActionButton = {
            if (!plan.isFinished) {
                ExtendedFloatingActionButton(
                    onClick = { showPay = true },
                    containerColor = MaroonPrimary,
                    icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                    text = { Text(if (isArabic) "تسجيل دفعة" else "Record payment") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
        ) {
            item { PlanCard(item, plan, isArabic, df) }

            item {
                Text(if (isArabic) "سجل الدفعات" else "Payment history",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp))
            }
            if (payments.isEmpty()) {
                item {
                    Text(if (isArabic) "لا دفعات مسجّلة بعد" else "No payments logged yet",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(payments, key = { it.id }) { p ->
                PaymentRow(item, p, isArabic, df, onDelete = { viewModel.deleteFinancialPayment(item, p) })
            }

            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isArabic) "المرفقات" else "Attachments",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { attachmentPicker.launch(arrayOf("image/*", "application/pdf")) }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null); Spacer(Modifier.width(4.dp))
                        Text(if (isArabic) "إضافة" else "Add")
                    }
                }
            }
            items(attachments, key = { it.id }) { a ->
                AttachmentRow(a, isArabic, onOpen = {
                    // Only open our own SAF content URIs (restore-safety).
                    val parsed = Uri.parse(a.uri)
                    if (parsed.scheme == "content") {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, parsed).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) {}
                    }
                }, onDelete = { viewModel.deleteFinancialAttachment(a) })
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isArabic) "حذف القسط؟" else "Delete installment?") },
            text = { Text(if (isArabic) "سيتم حذف القسط وكل دفعاته ومرفقاته." else "This deletes the installment with all its payments and attachments.") },
            confirmButton = {
                Button(onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(if (isArabic) "إلغاء" else "Cancel") } }
        )
    }

    if (showPay) {
        RecordPaymentDialog(
            isArabic = isArabic,
            currency = item.currency,
            suggested = if (item.monthlyAmount > 0) item.monthlyAmount else plan.remainingAmount,
            onDismiss = { showPay = false },
            onConfirm = { amount, type, note ->
                viewModel.recordFinancialPayment(item, amount, type, note)
                showPay = false
            }
        )
    }
}

@Composable
private fun PlanCard(item: FinancialItemEntity, plan: com.notification.app.domain.calculator.InstallmentPlan, isArabic: Boolean, df: SimpleDateFormat) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isArabic) "خطة السداد" else "Payment plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            LinearProgressIndicator(progress = { plan.completionFraction }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            Text("${(plan.completionFraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            InfoRow(if (isArabic) "الأقساط المدفوعة" else "Paid installments", "${plan.paidInstallments} / ${plan.totalInstallments}")
            InfoRow(if (isArabic) "المتبقّي" else "Remaining installments", plan.remainingInstallments.toString())
            InfoRow(if (isArabic) "إجمالي المدفوع" else "Total paid", fmtMoney(plan.totalPaid, item.currency))
            InfoRow(if (isArabic) "المبلغ المتبقّي" else "Remaining amount", fmtMoney(plan.remainingAmount, item.currency))
            if (!plan.isFinished && plan.nextPaymentDate > 0) InfoRow(if (isArabic) "الدفعة القادمة" else "Next payment", df.format(Date(plan.nextPaymentDate)))
            if (plan.lastPaymentDate > 0) InfoRow(if (isArabic) "آخر دفعة" else "Last payment", df.format(Date(plan.lastPaymentDate)))
            InfoRow(if (isArabic) "الانتهاء المتوقّع" else "Expected finish", df.format(Date(plan.expectedFinishDate)))
            if (plan.isFinished) {
                Text(if (isArabic) "تم السداد بالكامل" else "Fully paid", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun PaymentRow(item: FinancialItemEntity, p: FinancialPaymentEntity, isArabic: Boolean, df: SimpleDateFormat, onDelete: () -> Unit) {
    val typeLabel = when (p.type) {
        "FULL" -> if (isArabic) "كامل" else "Full"
        "ADVANCE" -> if (isArabic) "مقدم" else "Advance"
        "EXTRA" -> if (isArabic) "إضافي" else "Extra"
        "RECURRING" -> if (isArabic) "شهري" else "Monthly"
        else -> if (isArabic) "جزئي" else "Partial"
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(typeLabel + (if (p.note.isNotBlank()) " • ${p.note}" else ""), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(df.format(Date(p.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(fmtMoney(p.amount, item.currency), fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun AttachmentRow(a: FinancialAttachmentEntity, isArabic: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(a.label.ifBlank { if (isArabic) "إيصال" else "Receipt" }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onOpen) { Text(if (isArabic) "فتح" else "Open") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun RecordPaymentDialog(
    isArabic: Boolean,
    currency: String,
    suggested: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    var amount by remember { mutableStateOf(if (suggested > 0) trimNum(suggested) else "") }
    var type by remember { mutableStateOf("PARTIAL") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "تسجيل دفعة ($currency)" else "Record payment ($currency)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text(if (isArabic) "المبلغ" else "Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(if (isArabic) "نوع الدفعة" else "Payment type", style = MaterialTheme.typography.labelLarge)
                PAYMENT_TYPES.forEach { t ->
                    val label = when (t) {
                        "FULL" -> if (isArabic) "سداد كامل" else "Full"
                        "ADVANCE" -> if (isArabic) "دفعة مقدمة" else "Advance"
                        "EXTRA" -> if (isArabic) "دفعة إضافية" else "Extra"
                        "RECURRING" -> if (isArabic) "قسط شهري" else "Monthly"
                        else -> if (isArabic) "دفعة جزئية" else "Partial"
                    }
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = type == t, onClick = { type = t }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(label)
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(if (isArabic) "ملاحظة (اختياري)" else "Note (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amount.toDoubleOrNull() ?: 0.0
                if (a > 0) onConfirm(a, type, note.trim())
            }, colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)) { Text(if (isArabic) "تسجيل" else "Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") } }
    )
}
