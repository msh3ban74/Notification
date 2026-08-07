package com.notification.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.Gam3iyaStatus
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.OnPrimary
import com.notification.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────
// رفيق — جمعيتي. Rafeeq is a personal companion: you take part in a
// gam3iya, you don't run one from an app. One simple journey:
//   سجّل جمعيتك → رفيق يفكرك بالقسط كل شهر → تضغط "دفعت" → يعدّ لك
//   ويقولك دورك إمتى وقبضك إمتى.
// ─────────────────────────────────────────────────────────────────────────

internal fun fmtMoney(amount: Double, currency: String): String {
    val n = if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else String.format(Locale.US, "%.2f", amount)
    return "$n $currency"
}

private enum class G3View { LIST, CREATE, EDIT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gam3iyaScreen(
    viewModel: MainViewModel,
    isArabic: Boolean
) {
    val gam3iyas by viewModel.allGam3iyas.collectAsState()

    var view by remember { mutableStateOf(G3View.LIST) }
    var current by remember { mutableStateOf<Gam3iyaEntity?>(null) }

    if (view == G3View.CREATE || view == G3View.EDIT) {
        ParticipantGam3iyaForm(
            viewModel = viewModel,
            isArabic = isArabic,
            existing = if (view == G3View.EDIT) current else null,
            onDone = { view = G3View.LIST },
            onCancel = { view = G3View.LIST }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { current = null; view = G3View.CREATE },
                containerColor = MaroonPrimary,
                contentColor = OnPrimary,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = if (isArabic) "جمعية جديدة" else "New gam3iya") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = if (isArabic) "جمعيتي" else "My Gam3iya",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            if (gam3iyas.isEmpty()) {
                com.notification.app.ui.components.EmptyState(
                    icon = Icons.Default.Group,
                    title = if (isArabic) "لا توجد جمعيات مسجّلة" else "No gam3iya yet",
                    subtitle = if (isArabic)
                        "سجّل جمعيتك لمتابعة الأقساط الشهرية وموعد دورك"
                    else "Add your gam3iya to track monthly installments and your turn",
                    actionLabel = if (isArabic) "إضافة جمعية" else "Add gam3iya",
                    onAction = { current = null; view = G3View.CREATE }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(gam3iyas, key = { it.id }) { g ->
                        val status = remember(g) { Gam3iyaCalculator.computeStatus(g, emptyList()) }
                        MyGam3iyaCard(
                            gam3iya = g,
                            status = status,
                            isArabic = isArabic,
                            onPaidThisMonth = { viewModel.participantRecordPayment(g) },
                            onEdit = { current = g; view = G3View.EDIT },
                            onDelete = { viewModel.deleteGam3iya(g) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyGam3iyaCard(
    gam3iya: Gam3iyaEntity,
    status: Gam3iyaStatus,
    isArabic: Boolean,
    onPaidThisMonth: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val installment = if (gam3iya.myInstallmentAmount > 0) gam3iya.myInstallmentAmount else gam3iya.monthlyInstallment
    val paid = gam3iya.myPaidInstallments
    val duration = status.durationMonths

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(gam3iya.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (gam3iya.organizerName.isNotBlank()) {
                        Text(
                            (if (isArabic) "المنظّم: " else "Organizer: ") + gam3iya.organizerName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (status.isFinished) {
                    Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                        Text(
                            if (isArabic) "مكتملة" else "Completed",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "تعديل" else "Edit") },
                        onClick = { menuOpen = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "حذف" else "Delete") },
                        onClick = { menuOpen = false; confirmDelete = true },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { status.progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isArabic) "دفعت $paid من $duration قسط" else "Paid $paid of $duration installments",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat(if (isArabic) "قسطي" else "My installment", fmtMoney(installment, gam3iya.currency))
                if (gam3iya.myTurnNumber > 0) {
                    MiniStat(if (isArabic) "دوري" else "My turn", "#${gam3iya.myTurnNumber}")
                }
                if (status.nextCollectionDate > 0) {
                    MiniStat(if (isArabic) "قبضي" else "My payout", df.format(Date(status.nextCollectionDate)))
                }
            }

            if (!status.isFinished) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onPaidThisMonth,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isArabic) "تم سداد قسط هذا الشهر" else "Paid this month")
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isArabic) "حذف الجمعية؟" else "Delete gam3iya?") },
            text = { Text(if (isArabic) "سيتم حذفها مع تذكيراتها." else "It will be removed along with its reminders.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(if (isArabic) "إلغاء" else "Cancel") } }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
