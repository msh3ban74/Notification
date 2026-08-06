package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.domain.model.FinancialType
import com.notification.app.ui.components.EmptyState
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Final Product sprint (Phase B) — the money list: bills, installments
 * and subscriptions with mark-paid, tap-to-edit and delete. Reads the
 * EXISTING financial flow (no new logic).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialListScreen(
    isArabic: Boolean,
    items: List<FinancialItemEntity>,
    onBack: () -> Unit,
    onEdit: (FinancialItemEntity) -> Unit,
    onDelete: (FinancialItemEntity) -> Unit,
    onTogglePaid: (FinancialItemEntity) -> Unit,
    onAdd: () -> Unit,
    // Phase D — duplicate an item as a fresh unpaid copy.
    onDuplicate: ((FinancialItemEntity) -> Unit)? = null,
    // Sprint 6 — installments open the payment-plan detail; other types edit.
    onOpenDetail: ((FinancialItemEntity) -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Phase D — search + type filter on top of the unpaid-first sort.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("") } // "" = all, else FinancialType name
    val sorted = remember(items, searchQuery, typeFilter) {
        val query = searchQuery.trim()
        items.asSequence()
            .filter { typeFilter.isEmpty() || it.type == typeFilter }
            .filter {
                query.isEmpty() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.seller.contains(query, ignoreCase = true) ||
                    it.note.contains(query, ignoreCase = true)
            }
            .sortedWith(compareBy({ it.isPaid }, { it.dueDate }))
            .toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الفواتير والأقساط والاشتراكات" else "Bills, Installments & Subscriptions",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isArabic) "رجوع" else "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                EmptyState(
                    icon = Icons.Default.ReceiptLong,
                    title = if (isArabic) "لا توجد التزامات مالية" else "Nothing to track yet",
                    subtitle = if (isArabic) "أضف فاتورة أو قسطًا أو اشتراكًا لمتابعته" else "Add a bill, installment or subscription to track it",
                    actionLabel = if (isArabic) "إضافة" else "Add",
                    onAction = onAdd
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppPadding.screen)
        ) {
            // Phase D — search + type filter controls.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isArabic) "ابحث…" else "Search…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.padding(vertical = Spacing.xs)
            ) {
                FilterChip(
                    selected = typeFilter.isEmpty(),
                    onClick = { typeFilter = "" },
                    label = { Text(if (isArabic) "الكل" else "All", maxLines = 1, softWrap = false) }
                )
                FinancialType.entries.forEach { t ->
                    FilterChip(
                        selected = typeFilter == t.name,
                        onClick = { typeFilter = if (typeFilter == t.name) "" else t.name },
                        label = {
                            Text(
                                if (isArabic) t.displayAr else t.displayEn,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            if (sorted.isEmpty()) {
                Text(
                    text = if (isArabic) "لا توجد نتائج مطابقة" else "No matching results",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.sm)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = Spacing.sm, bottom = 96.dp
                )
            ) {
            items(sorted, key = { it.id }) { item ->
                val type = FinancialType.fromString(item.type)
                val icon = when (type) {
                    FinancialType.INSTALLMENT -> Icons.Default.CreditCard
                    FinancialType.SUBSCRIPTION -> Icons.Default.Subscriptions
                    FinancialType.BILL -> Icons.Default.ReceiptLong
                }
                val amountText = when (type) {
                    FinancialType.INSTALLMENT ->
                        "${item.monthlyAmount.toLong()} / ${if (isArabic) "متبقٍ" else "left"} ${item.remaining.toLong()}"
                    else -> "${item.amount.toLong()} ${if (isArabic) "ج.م" else "EGP"}"
                }

                PremiumCard(
                    onClick = {
                        if (item.type == "INSTALLMENT" && onOpenDetail != null) onOpenDetail(item)
                        else onEdit(item)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { onTogglePaid(item) }) {
                            Icon(
                                imageVector = if (item.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (isArabic) "مدفوع" else "Paid",
                                tint = if (item.isPaid) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.iconSizeMedium)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$amountText • ${dateFormat.format(Date(item.dueDate))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (onDuplicate != null) {
                            IconButton(onClick = { onDuplicate(item) }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = if (isArabic) "تكرار" else "Duplicate",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (isArabic) "حذف" else "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
