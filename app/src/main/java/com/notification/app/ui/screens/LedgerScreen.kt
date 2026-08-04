package com.notification.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.calculator.LedgerStatus
import com.notification.app.domain.calculator.StatementExporter
import com.notification.app.domain.model.LedgerTransactionType
import com.notification.app.ui.theme.MaroonPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LedgerScreen(
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    isArabic: Boolean,
    onAddPerson: (name: String, phone: String) -> Unit,
    onAddTransaction: (LedgerTransactionEntity) -> Unit,
    onDeleteTransaction: (LedgerTransactionEntity) -> Unit
) {
    var selectedPersonForDetail by remember { mutableStateOf<PersonEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Owed to me (لهم), 1: I owe (لي)
    var showAddPersonDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (selectedPersonForDetail != null) {
        val personTxs = transactions.filter { it.personId == selectedPersonForDetail!!.id }
        PersonDetailScreen(
            person = selectedPersonForDetail!!,
            transactions = personTxs,
            isArabic = isArabic,
            onBack = { selectedPersonForDetail = null },
            onAddTransaction = onAddTransaction,
            onDeleteTransaction = onDeleteTransaction,
            onExportStatement = {
                val statement = StatementExporter.generatePersonLedgerStatement(selectedPersonForDetail!!, personTxs)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, statement)
                }
                context.startActivity(Intent.createChooser(intent, "Share Ledger Statement"))
            }
        )
        return
    }

    // Compute net balance per person
    val personSummaries = remember(persons, transactions) {
        persons.map { p ->
            val pTxs = transactions.filter { it.personId == p.id }
            val summary = LedgerCalculator.calculateNetBalance(pTxs)
            Triple(p, summary, pTxs)
        }
    }

    val owedToMeList = remember(personSummaries) {
        personSummaries.filter { it.second.status == LedgerStatus.THEY_OWE_ME }
    }

    val iOweList = remember(personSummaries) {
        personSummaries.filter { it.second.status == LedgerStatus.I_OWE_THEM }
    }

    val settledList = remember(personSummaries) {
        personSummaries.filter { it.second.status == LedgerStatus.SETTLED }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPersonDialog = true },
                containerColor = MaroonPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Person")
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
                text = if (isArabic) "دفتر الديون والمعاملات" else "Debt & Loans Ledger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            // Tabs for "Owed to Me" vs "I Owe"
            TabRow(
                selectedTabIndex = selectedTab,
                contentColor = MaroonPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaroonPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = MaroonPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            if (isArabic) "يطلبوني / استلمت (لهم)" else "Owed to Me",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = MaroonPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            if (isArabic) "أطلبهم / أعطيتهم (لي)" else "I Owe",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentList = if (selectedTab == 0) owedToMeList else iOweList

            if (currentList.isEmpty() && settledList.isEmpty()) {
                com.notification.app.ui.components.EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = if (isArabic) "لا توجد سجلات ديون" else "No Debt Records Found",
                    subtitle = if (isArabic) "اضغط على زر (+ شخص) لإضافة أول حساب شخص ومتابعة المستحقات" else "Tap (+ Person) above to add your first account and track balance dues"
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList) { (person, summary, txs) ->
                        PersonLedgerSummaryCard(
                            person = person,
                            summary = summary,
                            isArabic = isArabic,
                            onClick = { selectedPersonForDetail = person }
                        )
                    }

                    if (settledList.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isArabic) "معاملات مسددة بالكامل" else "Settled Accounts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(settledList) { (person, summary, txs) ->
                            PersonLedgerSummaryCard(
                                person = person,
                                summary = summary,
                                isArabic = isArabic,
                                onClick = { selectedPersonForDetail = person }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPersonDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text(if (isArabic) "إضافة شخص جديد" else "Add New Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isArabic) "الاسم" else "Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isArabic) "رقم الهاتف (اختياري)" else "Phone Number (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddPerson(name, phone)
                            showAddPersonDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isArabic) "إضافة" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun PersonLedgerSummaryCard(
    person: PersonEntity,
    summary: com.notification.app.domain.calculator.LedgerSummary,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val statusText = when (summary.status) {
        LedgerStatus.THEY_OWE_ME -> if (isArabic) "يطالبك بـ ${summary.netAmount} ج.م" else "They owe you ${summary.netAmount} EGP"
        LedgerStatus.I_OWE_THEM -> if (isArabic) "تطالبه بـ ${summary.netAmount} ج.م" else "You owe them ${summary.netAmount} EGP"
        LedgerStatus.SETTLED -> if (isArabic) "مسدد بالكامل (خالص)" else "Settled / Even"
    }

    val statusColor = when (summary.status) {
        LedgerStatus.THEY_OWE_ME -> Color(0xFF27AE60)
        LedgerStatus.I_OWE_THEM -> Color(0xFFC0392B)
        LedgerStatus.SETTLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
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
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    person: PersonEntity,
    transactions: List<LedgerTransactionEntity>,
    isArabic: Boolean,
    onBack: () -> Unit,
    onAddTransaction: (LedgerTransactionEntity) -> Unit,
    onDeleteTransaction: (LedgerTransactionEntity) -> Unit,
    onExportStatement: () -> Unit
) {
    val summary = remember(transactions) { LedgerCalculator.calculateNetBalance(transactions) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onExportStatement) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Statement")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTxDialog = true },
                containerColor = MaroonPrimary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // Pinned Net Balance Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) "صافي الحساب الحالى" else "Current Net Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    val netString = when (summary.status) {
                        LedgerStatus.THEY_OWE_ME -> if (isArabic) "يطالبك بـ ${summary.netAmount} ج.م" else "They owe you ${summary.netAmount} EGP"
                        LedgerStatus.I_OWE_THEM -> if (isArabic) "تطالبه بـ ${summary.netAmount} ج.م" else "You owe them ${summary.netAmount} EGP"
                        LedgerStatus.SETTLED -> if (isArabic) "الحساب مسدد بالكامل (خالص)" else "Settled (0 EGP)"
                    }

                    Text(
                        text = netString,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Text(
                text = if (isArabic) "سجل المعاملات والعمليات" else "Transaction Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد معاملات مسجلة حتى الآن" else "No transactions recorded yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(transactions) { tx ->
                        val txType = LedgerTransactionType.fromString(tx.type)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (isArabic) txType.displayNameAr else txType.displayNameEn,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (tx.note.isNotBlank()) {
                                        Text(
                                            text = tx.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = dateFormat.format(Date(tx.date)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${tx.amount} EGP",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (txType.isGivingToThem) Color(0xFF27AE60) else Color(0xFFC0392B)
                                    )

                                    IconButton(onClick = { onDeleteTransaction(tx) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTxDialog) {
        var amountText by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(LedgerTransactionType.GAVE_THEM) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTxDialog = false },
            title = { Text(if (isArabic) "تسجيل معاملة مالية" else "Add Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (isArabic) "المبلغ (ج.م)" else "Amount (EGP)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = if (isArabic) "نوع المعاملة:" else "Transaction Type:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Column {
                        LedgerTransactionType.entries.forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedType = type }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isArabic) type.displayNameAr else type.displayNameEn)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (isArabic) "ملاحظات" else "Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            onAddTransaction(
                                LedgerTransactionEntity(
                                    personId = person.id,
                                    type = selectedType.name,
                                    amount = amount,
                                    date = System.currentTimeMillis(),
                                    note = note
                                )
                            )
                            showAddTxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
