package com.notification.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.notification.app.data.local.entities.Gam3iyaAttachmentEntity
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.Gam3iyaPaymentEntity
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.Gam3iyaStatus
import com.notification.app.domain.calculator.StatementExporter
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.OnPrimary
import com.notification.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────
// Sprint 4 — Professional Gam3iya management. Two modes (I manage / I take
// part), full member CRUD, payment marking with history, attachments, live
// automation (next collector, late members, totals, progress), search + a
// status filter. This screen owns its own create / edit / detail navigation
// via local state so MainActivity only needs the ViewModel.
// ─────────────────────────────────────────────────────────────────────────

private enum class G3View { LIST, CREATE_MANAGER, CREATE_PARTICIPANT, EDIT, DETAIL }

internal val CURRENCIES = listOf("EGP", "SAR", "AED", "USD", "EUR", "KWD", "QAR")

internal fun fmtMoney(amount: Double, currency: String): String {
    val n = if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else String.format(Locale.US, "%.2f", amount)
    return "$n $currency"
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun Gam3iyaScreen(
    viewModel: MainViewModel,
    isArabic: Boolean
) {
    val gam3iyas by viewModel.allGam3iyas.collectAsState()
    val context = LocalContext.current

    var view by remember { mutableStateOf(G3View.LIST) }
    var current by remember { mutableStateOf<Gam3iyaEntity?>(null) } // for EDIT / DETAIL
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") } // ALL/ACTIVE/LATE/FINISHED/ARCHIVED
    var showModePicker by remember { mutableStateOf(false) }

    // Detail view ---------------------------------------------------------
    if (view == G3View.DETAIL && current != null) {
        val live = gam3iyas.firstOrNull { it.id == current!!.id } ?: current!!
        Gam3iyaDetailScreen(
            viewModel = viewModel,
            gam3iya = live,
            isArabic = isArabic,
            onBack = { view = G3View.LIST },
            onEdit = { current = live; view = G3View.EDIT },
            onDelete = {
                viewModel.deleteGam3iya(live)
                view = G3View.LIST
            }
        )
        return
    }

    // Create / edit forms -------------------------------------------------
    if (view == G3View.CREATE_MANAGER || (view == G3View.EDIT && current?.mode != "PARTICIPANT")) {
        ManagerGam3iyaForm(
            viewModel = viewModel,
            isArabic = isArabic,
            existing = if (view == G3View.EDIT) current else null,
            onDone = { view = G3View.LIST },
            onCancel = { view = G3View.LIST }
        )
        return
    }
    if (view == G3View.CREATE_PARTICIPANT || (view == G3View.EDIT && current?.mode == "PARTICIPANT")) {
        ParticipantGam3iyaForm(
            viewModel = viewModel,
            isArabic = isArabic,
            existing = if (view == G3View.EDIT) current else null,
            onDone = { view = G3View.LIST },
            onCancel = { view = G3View.LIST }
        )
        return
    }

    // We need members to evaluate the LATE filter; observe all members once.
    val allMembers by viewModel.allGam3iyaMembers.collectAsState()

    val visible = remember(gam3iyas, search, filter, allMembers) {
        val q = search.trim()
        gam3iyas.filter { g ->
            val matchesSearch = q.isEmpty() ||
                g.title.contains(q, true) || g.note.contains(q, true) ||
                g.organizerName.contains(q, true) || g.description.contains(q, true)
            val members = allMembers.filter { it.gam3iyaId == g.id }
            val status = Gam3iyaCalculator.computeStatus(g, members)
            val matchesFilter = when (filter) {
                "ACTIVE" -> g.status == "ACTIVE" && !status.isFinished
                "LATE" -> status.lateMembers.isNotEmpty()
                "FINISHED" -> status.isFinished || g.status == "COMPLETED"
                "ARCHIVED" -> g.status == "ARCHIVED"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showModePicker = true },
                containerColor = MaroonPrimary,
                contentColor = OnPrimary,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = if (isArabic) "جمعية جديدة" else "New Gam3iya") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = if (isArabic) "الجمعيات" else "Gam3iya",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            if (gam3iyas.isEmpty()) {
                com.notification.app.ui.components.EmptyState(
                    icon = Icons.Default.Group,
                    title = if (isArabic) "لا توجد جمعيات بعد" else "No gam3iyas yet",
                    subtitle = if (isArabic)
                        "أنشئ جمعية تديرها بنفسك أو سجّل جمعية تشارك فيها — كل الحسابات والمواعيد تلقائية"
                    else "Create one you manage, or track one you take part in — all math and dates are automatic"
                )
            } else {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(if (isArabic) "ابحث بالاسم أو المنظّم…" else "Search name or organizer…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to (if (isArabic) "الكل" else "All"),
                        "ACTIVE" to (if (isArabic) "نشطة" else "Active"),
                        "LATE" to (if (isArabic) "متأخرة" else "Late"),
                        "FINISHED" to (if (isArabic) "منتهية" else "Finished"),
                        "ARCHIVED" to (if (isArabic) "مؤرشفة" else "Archived")
                    )
                    filters.forEach { (key, label) ->
                        FilterChip(
                            selected = filter == key,
                            onClick = { filter = key },
                            label = { Text(label, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (visible.isEmpty()) {
                    Text(
                        text = if (isArabic) "لا نتائج مطابقة" else "No matches",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(visible, key = { it.id }) { g ->
                        val members = allMembers.filter { it.gam3iyaId == g.id }
                        val status = remember(g, members) { Gam3iyaCalculator.computeStatus(g, members) }
                        Gam3iyaCard(
                            gam3iya = g,
                            status = status,
                            isArabic = isArabic,
                            onClick = { current = g; view = G3View.DETAIL }
                        )
                    }
                }
            }
        }
    }

    if (showModePicker) {
        ModePickerDialog(
            isArabic = isArabic,
            onDismiss = { showModePicker = false },
            onManager = { showModePicker = false; current = null; view = G3View.CREATE_MANAGER },
            onParticipant = { showModePicker = false; current = null; view = G3View.CREATE_PARTICIPANT }
        )
    }
}

@Composable
private fun ModePickerDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onManager: () -> Unit,
    onParticipant: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "نوع الجمعية" else "Gam3iya type") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeOption(
                    icon = Icons.Default.ManageAccounts,
                    title = if (isArabic) "أنا أدير الجمعية" else "I manage the gam3iya",
                    subtitle = if (isArabic) "أضف الأعضاء وتابع الدفعات والأدوار" else "Add members, track payments & turns",
                    onClick = onManager
                )
                ModeOption(
                    icon = Icons.Default.Groups,
                    title = if (isArabic) "أنا مشارك في جمعية" else "I take part in a gam3iya",
                    subtitle = if (isArabic) "تابع دورك وأقساطك مع المنظّم" else "Track your turn & installments",
                    onClick = onParticipant
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") } }
    )
}

@Composable
private fun ModeOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Gam3iyaCard(
    gam3iya: Gam3iyaEntity,
    status: Gam3iyaStatus,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (gam3iya.photoUri.isNotBlank()) {
                        AsyncImage(
                            model = gam3iya.photoUri, contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(gam3iya.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val modeLabel = if (gam3iya.mode == "PARTICIPANT") (if (isArabic) "مشارك" else "Participant") else (if (isArabic) "مدير" else "Manager")
                        Text(modeLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                StatusBadge(gam3iya = gam3iya, status = status, isArabic = isArabic)
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { status.progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat(if (isArabic) "القسط" else "Installment", fmtMoney(gam3iya.monthlyInstallment, gam3iya.currency))
                MiniStat(if (isArabic) "الشهر" else "Month", "${status.currentMonthIndex}/${status.durationMonths}")
                MiniStat(if (isArabic) "متبقٍ" else "Remaining", "${status.remainingMonths} ${if (isArabic) "شهر" else "mo"}")
            }
            if (status.nextCollectionDate > 0 && gam3iya.mode != "PARTICIPANT" && status.nextCollector != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (if (isArabic) "التالي في القبض: " else "Next to collect: ") +
                        "${status.nextCollector.memberName} • ${df.format(Date(status.nextCollectionDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (status.lateMembers.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = (if (isArabic) "متأخرون: " else "Late: ") + "${status.lateMembers.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(gam3iya: Gam3iyaEntity, status: Gam3iyaStatus, isArabic: Boolean) {
    val (label, color) = when {
        gam3iya.status == "ARCHIVED" -> (if (isArabic) "مؤرشفة" else "Archived") to MaterialTheme.colorScheme.outline
        status.isFinished || gam3iya.status == "COMPLETED" -> (if (isArabic) "منتهية" else "Done") to MaterialTheme.colorScheme.tertiary
        status.lateMembers.isNotEmpty() -> (if (isArabic) "متأخرة" else "Late") to MaterialTheme.colorScheme.error
        else -> (if (isArabic) "نشطة" else "Active") to MaterialTheme.colorScheme.primary
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
