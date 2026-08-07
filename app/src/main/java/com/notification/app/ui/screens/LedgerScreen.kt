package com.notification.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.notification.app.data.local.entities.LedgerAttachmentEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import kotlinx.coroutines.flow.Flow
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.calculator.LedgerStatus
import com.notification.app.domain.calculator.LedgerSummary
import com.notification.app.domain.calculator.StatementExporter
import com.notification.app.domain.model.LedgerTransactionType
import com.notification.app.ui.theme.MaroonPrimary
import com.notification.app.ui.theme.OnPrimary
import com.notification.app.ui.theme.Success
import com.notification.app.ui.theme.Error
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LedgerScreen(
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    isArabic: Boolean,
    onAddPerson: (name: String, phone: String) -> Unit,
    // Product Completion — full contact profile capture.
    onAddPersonFull: (PersonEntity) -> Unit = { onAddPerson(it.name, it.phoneNumber) },
    onAddTransaction: (LedgerTransactionEntity) -> Unit,
    onDeleteTransaction: (LedgerTransactionEntity) -> Unit,
    // Sprint 5 — edit flow. Optional so existing call sites keep working.
    onUpdateTransaction: ((LedgerTransactionEntity) -> Unit)? = null,
    // Person profile edit / delete (optional).
    onUpdatePerson: ((PersonEntity) -> Unit)? = null,
    onDeletePerson: ((PersonEntity) -> Unit)? = null,
    // Person attachments (receipts / docs) — optional.
    getLedgerAttachments: ((Long) -> Flow<List<LedgerAttachmentEntity>>)? = null,
    onAddLedgerAttachment: ((Long, String) -> Unit)? = null,
    onDeleteLedgerAttachment: ((LedgerAttachmentEntity) -> Unit)? = null
) {
    var selectedPersonForDetail by remember { mutableStateOf<PersonEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Owed to me (لهم), 1: I owe (لي)
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var personSearch by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (showStats) {
        LedgerStatsScreen(transactions = transactions, isArabic = isArabic, onBack = { showStats = false })
        return
    }

    if (selectedPersonForDetail != null) {
        val personTxs = transactions.filter { it.personId == selectedPersonForDetail!!.id }
        PersonDetailScreen(
            person = selectedPersonForDetail!!,
            transactions = personTxs,
            isArabic = isArabic,
            onBack = { selectedPersonForDetail = null },
            onAddTransaction = onAddTransaction,
            onDeleteTransaction = onDeleteTransaction,
            onUpdateTransaction = onUpdateTransaction,
            onUpdatePerson = onUpdatePerson,
            onDeletePerson = onDeletePerson?.let { del -> { p: PersonEntity -> del(p); selectedPersonForDetail = null } },
            getLedgerAttachments = getLedgerAttachments,
            onAddLedgerAttachment = onAddLedgerAttachment,
            onDeleteLedgerAttachment = onDeleteLedgerAttachment,
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
                contentColor = OnPrimary,
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isArabic) "الديون" else "Debts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showStats = true }) {
                    Icon(Icons.Default.BarChart, contentDescription = if (isArabic) "الإحصائيات" else "Statistics")
                }
            }

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
                            if (isArabic) "مستحقات لي" else "Owed to me",
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
                            if (isArabic) "مستحقات عليّ" else "I owe",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search across everyone in the ledger by name or phone.
            OutlinedTextField(
                value = personSearch,
                onValueChange = { personSearch = it },
                placeholder = { Text(if (isArabic) "ابحث بالاسم أو الهاتف…" else "Search by name or phone…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            val searchFilter: (Triple<PersonEntity, LedgerSummary, List<LedgerTransactionEntity>>) -> Boolean = {
                val query = personSearch.trim()
                query.isEmpty() ||
                    it.first.name.contains(query, ignoreCase = true) ||
                    it.first.phoneNumber.contains(query, ignoreCase = true)
            }
            val currentList = (if (selectedTab == 0) owedToMeList else iOweList).filter(searchFilter)
            val visibleSettled = settledList.filter(searchFilter)

            if (currentList.isEmpty() && settledList.isEmpty()) {
                com.notification.app.ui.components.EmptyState(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = if (isArabic) "لا توجد سجلات ديون" else "No Debt Records Found",
                    subtitle = if (isArabic) "احفظ حقوقك وحقوق الآخرين — صافي المديونية لكل شخص يُحسب لحظيًا" else "Every balance accounted for — per-person net debt, calculated live",
                    actionLabel = if (isArabic) "أضف شخصًا" else "Add Person",
                    onAction = { showAddPersonDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.first.id }) { (person, summary, txs) ->
                        PersonLedgerSummaryCard(
                            person = person,
                            summary = summary,
                            isArabic = isArabic,
                            onClick = { selectedPersonForDetail = person }
                        )
                    }

                    if (visibleSettled.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isArabic) "معاملات مسددة بالكامل" else "Settled Accounts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(visibleSettled) { (person, summary, txs) ->
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
        // رحلة "دين جديد" تبدأ من هنا: مين الشخص؟ اسمه ورقمه وكفاية —
        // ده تطبيق أفراد، مش دفتر عملاء شركة.
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var whatsapp by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text(if (isArabic) "شخص جديد" else "New person") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text(if (isArabic) "الاسم" else "Name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text(if (isArabic) "رقم الهاتف (اختياري)" else "Phone (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = whatsapp, onValueChange = { whatsapp = it },
                        label = { Text(if (isArabic) "واتساب (اختياري)" else "WhatsApp (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        label = { Text(if (isArabic) "ملاحظة (اختياري)" else "Note (optional)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onAddPersonFull(
                            PersonEntity(
                                name = name.trim(), phoneNumber = phone.trim(),
                                whatsapp = whatsapp.trim(), notes = notes.trim()
                            )
                        )
                        showAddPersonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
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
    // QA fix: the Arabic copy here was reversed (showed "he demands from
    // you" for money owed TO you). Correct: THEY_OWE_ME = «لك عنده».
    val statusText = when (summary.status) {
        LedgerStatus.THEY_OWE_ME -> if (isArabic) "لك عنده ${summary.netAmount} ج.م" else "They owe you ${summary.netAmount} EGP"
        LedgerStatus.I_OWE_THEM -> if (isArabic) "عليك له ${summary.netAmount} ج.م" else "You owe them ${summary.netAmount} EGP"
        LedgerStatus.SETTLED -> if (isArabic) "مسدد بالكامل (خالص)" else "Settled / Even"
    }

    val statusColor = when (summary.status) {
        LedgerStatus.THEY_OWE_ME -> Success
        LedgerStatus.I_OWE_THEM -> Error
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
    onExportStatement: () -> Unit,
    // Sprint 5 — edit flow. When provided, each transaction row gets an
    // Edit action that reopens the same dialog pre-filled.
    onUpdateTransaction: ((LedgerTransactionEntity) -> Unit)? = null,
    onUpdatePerson: ((PersonEntity) -> Unit)? = null,
    onDeletePerson: ((PersonEntity) -> Unit)? = null,
    getLedgerAttachments: ((Long) -> Flow<List<LedgerAttachmentEntity>>)? = null,
    onAddLedgerAttachment: ((Long, String) -> Unit)? = null,
    onDeleteLedgerAttachment: ((LedgerAttachmentEntity) -> Unit)? = null
) {
    val summary = remember(transactions) { LedgerCalculator.calculateNetBalance(transactions) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<LedgerTransactionEntity?>(null) }
    var showEditPerson by remember { mutableStateOf(false) }
    var showDeletePerson by remember { mutableStateOf(false) }
    var personMenu by remember { mutableStateOf(false) }
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
                    if (onUpdatePerson != null) {
                        IconButton(onClick = { onUpdatePerson(person.copy(isFavorite = !person.isFavorite)) }) {
                            Icon(
                                imageVector = if (person.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isArabic) "مفضّلة" else "Favorite",
                                tint = if (person.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                    IconButton(onClick = onExportStatement) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = if (isArabic) "مشاركة كشف الحساب" else "Share statement")
                    }
                    if (onUpdatePerson != null || onDeletePerson != null) {
                        IconButton(onClick = { personMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = if (isArabic) "خيارات" else "Options")
                        }
                        DropdownMenu(expanded = personMenu, onDismissRequest = { personMenu = false }) {
                            if (onUpdatePerson != null) {
                                DropdownMenuItem(
                                    text = { Text(if (isArabic) "تعديل جهة الاتصال" else "Edit contact") },
                                    onClick = { personMenu = false; showEditPerson = true },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (person.isArchived) (if (isArabic) "إلغاء الأرشفة" else "Unarchive") else (if (isArabic) "أرشفة" else "Archive")) },
                                    onClick = { personMenu = false; onUpdatePerson(person.copy(isArchived = !person.isArchived)) },
                                    leadingIcon = { Icon(if (person.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = null) }
                                )
                            }
                            if (onDeletePerson != null) {
                                DropdownMenuItem(
                                    text = { Text(if (isArabic) "حذف جهة الاتصال" else "Delete contact") },
                                    onClick = { personMenu = false; showDeletePerson = true },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTxDialog = true },
                containerColor = MaroonPrimary,
                contentColor = OnPrimary
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
                        LedgerStatus.THEY_OWE_ME -> if (isArabic) "لك عنده ${summary.netAmount} ج.م" else "Owed to you: ${summary.netAmount} EGP"
                        LedgerStatus.I_OWE_THEM -> if (isArabic) "عليك له ${summary.netAmount} ج.م" else "You owe: ${summary.netAmount} EGP"
                        LedgerStatus.SETTLED -> if (isArabic) "الحساب مسدد بالكامل" else "Fully settled"
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

            if (getLedgerAttachments != null) {
                val attachments by getLedgerAttachments(person.id).collectAsState(initial = emptyList())
                val ctx = LocalContext.current
                val attachPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) { takePersistable(ctx, uri); onAddLedgerAttachment?.invoke(person.id, uri.toString()) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) "المرفقات والإيصالات" else "Attachments & receipts",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { attachPicker.launch(arrayOf("image/*", "application/pdf")) }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null); Spacer(Modifier.width(4.dp))
                        Text(if (isArabic) "إضافة" else "Add")
                    }
                }
                attachments.forEach { a ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(a.label.ifBlank { if (isArabic) "إيصال" else "Receipt" }, Modifier.weight(1f), maxLines = 1)
                            TextButton(onClick = {
                                try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.uri)).apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) } catch (_: Exception) {}
                            }) { Text(if (isArabic) "فتح" else "Open") }
                            IconButton(onClick = { onDeleteLedgerAttachment?.invoke(a) }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
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
                                        color = if (txType.isGivingToThem) Success else Error
                                    )

                                    if (onUpdateTransaction != null) {
                                        IconButton(onClick = { editingTx = tx }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

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

    if (showAddTxDialog || editingTx != null) {
        // One dialog for both flows: blank for "add", pre-filled for "edit"
        // (Sprint 5). Keyed on editingTx so switching between the two modes
        // resets the fields correctly.
        val editing = editingTx
        var amountText by remember(editing) { mutableStateOf(editing?.amount?.toString() ?: "") }
        var selectedType by remember(editing) {
            mutableStateOf(
                editing?.let { LedgerTransactionType.fromString(it.type) }
                    ?: LedgerTransactionType.GAVE_THEM
            )
        }
        var note by remember(editing) { mutableStateOf(editing?.note ?: "") }
        var dueDate by remember(editing) { mutableStateOf(editing?.dueDate ?: 0L) }
        val txContext = LocalContext.current
        val dismissDialog = {
            showAddTxDialog = false
            editingTx = null
        }

        AlertDialog(
            onDismissRequest = dismissDialog,
            title = {
                Text(
                    if (editing != null) {
                        if (isArabic) "تعديل معاملة مالية" else "Edit Transaction"
                    } else {
                        if (isArabic) "تسجيل معاملة مالية" else "Add Transaction"
                    }
                )
            },
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

                    // "قالي هيرجعهالك يوم الأحد" → حدد اليوم ورفيق يفكرك
                    // تلقائيًا (تذكير مجدول يتعمل مع الحفظ).
                    DateField(
                        value = dueDate, onPick = { dueDate = it },
                        label = if (isArabic) "هيتسدد إمتى؟ (يفكرك رفيق)" else "When is it due? (Rafeeq reminds you)",
                        context = txContext, isArabic = isArabic, allowEmpty = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (isArabic) "ملاحظة (السبب مثلًا)" else "Note (e.g. the reason)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            if (editing != null) {
                                onUpdateTransaction?.invoke(
                                    editing.copy(
                                        type = selectedType.name,
                                        amount = amount,
                                        note = note,
                                        dueDate = dueDate
                                    )
                                )
                            } else {
                                onAddTransaction(
                                    LedgerTransactionEntity(
                                        personId = person.id,
                                        type = selectedType.name,
                                        amount = amount,
                                        date = System.currentTimeMillis(),
                                        note = note,
                                        dueDate = dueDate
                                    )
                                )
                            }
                            dismissDialog()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = dismissDialog) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    if (showEditPerson && onUpdatePerson != null) {
        var eName by remember { mutableStateOf(person.name) }
        var ePhone by remember { mutableStateOf(person.phoneNumber) }
        var eWhatsapp by remember { mutableStateOf(person.whatsapp) }
        var eNotes by remember { mutableStateOf(person.notes) }
        AlertDialog(
            onDismissRequest = { showEditPerson = false },
            title = { Text(if (isArabic) "تعديل الشخص" else "Edit person") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(eName, { eName = it }, label = { Text(if (isArabic) "الاسم" else "Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ePhone, { ePhone = it }, label = { Text(if (isArabic) "رقم الهاتف" else "Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(eWhatsapp, { eWhatsapp = it }, label = { Text(if (isArabic) "واتساب" else "WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(eNotes, { eNotes = it }, label = { Text(if (isArabic) "ملاحظة" else "Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    enabled = eName.isNotBlank(),
                    onClick = {
                        onUpdatePerson(
                            person.copy(
                                name = eName.trim(), phoneNumber = ePhone.trim(),
                                whatsapp = eWhatsapp.trim(), notes = eNotes.trim()
                            )
                        )
                        showEditPerson = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) { Text(if (isArabic) "حفظ" else "Save") }
            },
            dismissButton = { TextButton(onClick = { showEditPerson = false }) { Text(if (isArabic) "إلغاء" else "Cancel") } }
        )
    }

    if (showDeletePerson && onDeletePerson != null) {
        AlertDialog(
            onDismissRequest = { showDeletePerson = false },
            title = { Text(if (isArabic) "حذف جهة الاتصال؟" else "Delete contact?") },
            text = { Text(if (isArabic) "سيتم حذف جهة الاتصال وكل معاملاتها ومرفقاتها نهائيًا." else "This permanently deletes the contact with all its transactions and attachments.") },
            confirmButton = {
                Button(onClick = { showDeletePerson = false; onDeletePerson(person) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeletePerson = false }) { Text(if (isArabic) "إلغاء" else "Cancel") } }
        )
    }
}

