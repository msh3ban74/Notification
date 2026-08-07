package com.notification.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.HabitEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.data.local.entities.ReminderEntity

/**
 * Global search across every module — tasks/reminders, debts (people),
 * gam3iyas, money items and habits. Reads the same lists the rest of the
 * app already collects; tapping a result routes to its screen. Introduces
 * no new data or product scope.
 */
private data class SearchHit(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onOpen: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    gam3iyas: List<Gam3iyaEntity>,
    financialItems: List<FinancialItemEntity>,
    habits: List<HabitEntity>,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenLedger: () -> Unit,
    onOpenGam3iya: () -> Unit,
    onOpenInstallment: (Long) -> Unit,
    onOpenFinancial: (Long) -> Unit,
    onOpenHabits: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val q = query.trim()
    fun String.hit() = q.isNotEmpty() && contains(q, ignoreCase = true)

    // Section builders — each returns hits only when the query matches.
    val taskHits = remember(q, reminders) {
        if (q.isEmpty()) emptyList() else reminders
            .filter { !it.isArchived && (it.title.hit() || it.note.hit()) }
            .map { r ->
                SearchHit(Icons.Default.Assignment, r.title,
                    r.note.ifBlank { if (isArabic) "مهمة / تذكير" else "Task / reminder" }) { onOpenTask(r.id) }
            }
    }
    val personHits = remember(q, persons) {
        if (q.isEmpty()) emptyList() else persons
            .filter { !it.isArchived && (it.name.hit() || it.phoneNumber.hit() || it.notes.hit()) }
            .map { p ->
                SearchHit(Icons.Default.AccountBalanceWallet, p.name,
                    if (isArabic) "الديون" else "Debts") { onOpenLedger() }
            }
    }
    val gam3iyaHits = remember(q, gam3iyas) {
        if (q.isEmpty()) emptyList() else gam3iyas
            .filter { it.title.hit() || it.organizerName.hit() || it.note.hit() }
            .map { g ->
                SearchHit(Icons.Default.Groups, g.title,
                    if (isArabic) "جمعيتي" else "My gam3iya") { onOpenGam3iya() }
            }
    }
    val financialHits = remember(q, financialItems) {
        if (q.isEmpty()) emptyList() else financialItems
            .filter { !it.isArchived && (it.title.hit() || it.seller.hit() || it.note.hit()) }
            .map { f ->
                val icon = when (f.type) {
                    "INSTALLMENT" -> Icons.Default.CreditCard
                    "SUBSCRIPTION" -> Icons.Default.Subscriptions
                    else -> Icons.Default.ReceiptLong
                }
                val sub = when (f.type) {
                    "INSTALLMENT" -> if (isArabic) "قسط" else "Installment"
                    "SUBSCRIPTION" -> if (isArabic) "اشتراك" else "Subscription"
                    else -> if (isArabic) "فاتورة" else "Bill"
                }
                SearchHit(icon, f.title, sub) {
                    if (f.type == "INSTALLMENT") onOpenInstallment(f.id) else onOpenFinancial(f.id)
                }
            }
    }
    val habitHits = remember(q, habits) {
        if (q.isEmpty()) emptyList() else habits
            .filter { it.title.hit() }
            .map { h -> SearchHit(Icons.Default.Loop, "${h.emoji} ${h.title}", if (isArabic) "عادة" else "Habit") { onOpenHabits() } }
    }

    val sections = listOf(
        (if (isArabic) "المهام والتذكيرات" else "Tasks & reminders") to taskHits,
        (if (isArabic) "الديون" else "Debts") to personHits,
        (if (isArabic) "الجمعيات" else "Gam3iyas") to gam3iyaHits,
        (if (isArabic) "المالية" else "Money") to financialHits,
        (if (isArabic) "العادات" else "Habits") to habitHits
    ).filter { it.second.isNotEmpty() }
    val totalHits = sections.sumOf { it.second.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "بحث" else "Search", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(if (isArabic) "ابحث في كل شيء…" else "Search everything…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = null) }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(focusRequester)
            )

            when {
                q.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (isArabic) "البحث يشمل المهام والديون والجمعيات والمالية والعادات"
                            else "Search covers tasks, debts, gam3iyas, money and habits",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                totalHits == 0 -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (isArabic) "لا نتائج لـ \"$q\"" else "No results for \"$q\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        sections.forEach { (label, hits) ->
                            item(key = "h_$label") {
                                Text(
                                    "$label (${hits.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                            items(hits) { hit -> SearchRow(hit) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchRow(hit: SearchHit) {
    Card(
        onClick = hit.onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(hit.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(hit.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    hit.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
