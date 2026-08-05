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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onAdd: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val sorted = remember(items) { items.sortedWith(compareBy({ it.isPaid }, { it.dueDate })) }

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
        if (sorted.isEmpty()) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppPadding.screen),
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

                PremiumCard(onClick = { onEdit(item) }, modifier = Modifier.fillMaxWidth()) {
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
