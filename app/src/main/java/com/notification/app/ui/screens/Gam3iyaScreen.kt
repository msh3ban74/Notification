package com.notification.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.StatementExporter
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.OnPrimary
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gam3iyaScreen(
    gam3iyas: List<Gam3iyaEntity>,
    isArabic: Boolean,
    getMembersForGam3iya: (Long) -> Flow<List<Gam3iyaMemberEntity>>,
    onCreateGam3iya: (
        title: String,
        totalAmount: Double,
        monthlyInstallment: Double,
        memberTurns: List<Pair<String, Int>>,
        startDate: Long
    ) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGam3iyaForDetail by remember { mutableStateOf<Gam3iyaEntity?>(null) }
    val context = LocalContext.current

    if (selectedGam3iyaForDetail != null) {
        val membersFlow = getMembersForGam3iya(selectedGam3iyaForDetail!!.id)
        val members by membersFlow.collectAsState(initial = emptyList())

        Gam3iyaDetailScreen(
            gam3iya = selectedGam3iyaForDetail!!,
            members = members,
            isArabic = isArabic,
            onBack = { selectedGam3iyaForDetail = null },
            onExportStatement = {
                val statement = StatementExporter.generateGam3iyaStatement(selectedGam3iyaForDetail!!, members)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, statement)
                }
                context.startActivity(Intent.createChooser(intent, "Share Gam3iya Schedule"))
            }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaroonPrimary,
                contentColor = OnPrimary,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Gam3iya")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = if (isArabic) "إدارة الجمعيات (Gam3iya)" else "Gam3iya Savings Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            if (gam3iyas.isEmpty()) {
                com.notification.app.ui.components.EmptyState(
                    icon = Icons.Default.Group,
                    title = if (isArabic) "لا توجد جمعيات قائمة" else "No Active Savings Groups",
                    subtitle = if (isArabic) "اضغط على زر الإضافة (+) لإنشاء جمعية جديدة وتحديد أدوار الأعضاء والمواعيد" else "Tap (+) below to set up a new rotating savings group with automated turn tracking"
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(gam3iyas, key = { it.id }) { gam3iya ->
                        val membersFlow = getMembersForGam3iya(gam3iya.id)
                        val members by membersFlow.collectAsState(initial = emptyList())
                        val summary = remember(gam3iya, members) {
                            Gam3iyaCalculator.calculateSummary(gam3iya, members)
                        }

                        Gam3iyaSummaryCard(
                            gam3iya = gam3iya,
                            summary = summary,
                            isArabic = isArabic,
                            onClick = { selectedGam3iyaForDetail = gam3iya }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGam3iyaDialog(
            isArabic = isArabic,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, total, installment, members ->
                onCreateGam3iya(title, total, installment, members, System.currentTimeMillis())
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun Gam3iyaSummaryCard(
    gam3iya: Gam3iyaEntity,
    summary: com.notification.app.domain.calculator.Gam3iyaSummary,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = gam3iya.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${gam3iya.totalAmount} EGP",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isArabic) "القسط الشهري" else "Monthly Installment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${gam3iya.monthlyInstallment} EGP",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = if (isArabic) "المدة والأعضاء" else "Duration & Members",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${summary.durationMonths} ${if (isArabic) "أشهر" else "months"} (${gam3iya.membersCount})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = if (isArabic) "نهاية الجمعية" else "End Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(summary.projectedEndDate)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gam3iyaDetailScreen(
    gam3iya: Gam3iyaEntity,
    members: List<Gam3iyaMemberEntity>,
    isArabic: Boolean,
    onBack: () -> Unit,
    onExportStatement: () -> Unit
) {
    val summary = remember(gam3iya, members) { Gam3iyaCalculator.calculateSummary(gam3iya, members) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gam3iya.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onExportStatement) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Schedule")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // Overview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isArabic) "ملخص إحصائيات الجمعية" else "Gam3iya Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isArabic) "المبلغ الإجمالي:" else "Total Pool:",
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${gam3iya.totalAmount} EGP",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isArabic) "الأقساط المسددة:" else "Paid Months:",
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${summary.installmentsPaidCount} / ${summary.durationMonths}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Member Rotation Timeline
            item {
                Text(
                    text = if (isArabic) "جدول الأدوار وصرف المبالغ" else "Member Payout Rotation Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(members) { m ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${m.turnMonth}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = m.memberName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(Date(m.payoutDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${gam3iya.totalAmount} EGP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGam3iyaDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        total: Double,
        installment: Double,
        members: List<Pair<String, Int>>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var installmentText by remember { mutableStateOf("") }
    var memberNamesInput by remember { mutableStateOf("") } // Comma separated

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "إنشاء جمعية جديدة" else "Create New Gam3iya") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isArabic) "اسم الجمعية" else "Gam3iya Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalText,
                    onValueChange = { totalText = it },
                    label = { Text(if (isArabic) "المبلغ الكلي للقبض (ج.م)" else "Total Payout Pool (EGP)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = installmentText,
                    onValueChange = { installmentText = it },
                    label = { Text(if (isArabic) "القسط الشهري للشخص" else "Monthly Installment (EGP)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memberNamesInput,
                    onValueChange = { memberNamesInput = it },
                    label = { Text(if (isArabic) "أسماء المشاركين (مفصولة بفاصلة)" else "Member Names (Comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = totalText.toDoubleOrNull() ?: 0.0
                    val installment = installmentText.toDoubleOrNull() ?: 0.0
                    val names = memberNamesInput.split(",").map { it.trim() }.filter { it.isNotBlank() }

                    if (title.isNotBlank() && total > 0 && names.isNotEmpty()) {
                        val memberTurns = names.mapIndexed { idx, name -> Pair(name, idx + 1) }
                        onConfirm(title, total, installment, memberTurns)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
            ) {
                Text(if (isArabic) "إنشاء" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}
