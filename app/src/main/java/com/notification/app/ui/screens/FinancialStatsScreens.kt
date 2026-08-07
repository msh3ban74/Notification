package com.notification.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.domain.calculator.FinancialCalculator
import com.notification.app.domain.calculator.LedgerCalculator

/**
 * Sprint 5/6 — statistics + charts for Debts and Installments. Surfaces the
 * already-built LedgerCalculator.computeGlobalStats and
 * FinancialCalculator.computeStatsFromItems; no new data.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerStatsScreen(
    transactions: List<LedgerTransactionEntity>,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val stats = remember(transactions) { LedgerCalculator.computeGlobalStats(transactions) }
    val cur = "EGP"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "إحصائيات الديون" else "Debt statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            item {
                KpiRow(listOf(
                    (if (isArabic) "لِيَ (مستحق)" else "Receivable") to fmtMoney(stats.totalReceivable, cur),
                    (if (isArabic) "عليّ" else "Payable") to fmtMoney(stats.totalPayable, cur)
                ))
            }
            item {
                KpiRow(listOf(
                    (if (isArabic) "استُلم" else "Collected") to fmtMoney(stats.totalRepaidToMe, cur),
                    (if (isArabic) "هذا الشهر" else "This month") to fmtMoney(stats.thisMonthVolume, cur)
                ))
            }
            item {
                KpiRow(listOf(
                    (if (isArabic) "متأخرات" else "Overdue") to "${stats.overdueCount}",
                    (if (isArabic) "متوسط الدفعة" else "Avg payment") to fmtMoney(stats.averagePayment, cur)
                ))
            }
            item {
                StatCard(if (isArabic) "نسبة التحصيل" else "Collection rate") {
                    ProgressBarWithLabel(stats.collectionRate)
                }
            }
            item {
                StatCard(if (isArabic) "النشاط الشهري (١٢ شهر)" else "Monthly activity (12 mo)") {
                    BarChart(stats.monthlyVolume.values.toList())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentStatsScreen(
    items: List<FinancialItemEntity>,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    val stats = remember(items) { FinancialCalculator.computeStatsFromItems(items) }
    val cur = items.firstOrNull { it.type == "INSTALLMENT" }?.currency ?: "EGP"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "إحصائيات الأقساط" else "Installment statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            item {
                KpiRow(listOf(
                    (if (isArabic) "إجمالي الأقساط" else "Total") to "${stats.totalCount}",
                    (if (isArabic) "مكتملة" else "Paid off") to "${stats.paidCount}"
                ))
            }
            item {
                KpiRow(listOf(
                    (if (isArabic) "المدفوع" else "Paid") to fmtMoney(stats.totalPaid, cur),
                    (if (isArabic) "المتبقّي" else "Remaining") to fmtMoney(stats.totalRemaining, cur)
                ))
            }
            item {
                KpiRow(listOf(
                    (if (isArabic) "مستحق اليوم" else "Due today") to "${stats.dueToday}",
                    (if (isArabic) "هذا الأسبوع" else "This week") to "${stats.dueThisWeek}"
                ))
            }
            item {
                KpiRow(listOf(
                    (if (isArabic) "هذا الشهر" else "This month") to "${stats.dueThisMonth}",
                    (if (isArabic) "متأخرة" else "Overdue") to "${stats.overdue}"
                ))
            }
            item {
                StatCard(if (isArabic) "نسبة الإنجاز" else "Completion rate") {
                    ProgressBarWithLabel(stats.completionRate)
                }
            }
            item {
                StatCard(if (isArabic) "الاستحقاقات الشهرية القادمة" else "Upcoming monthly due") {
                    BarChart(stats.monthlyPaid.values.toList())
                }
            }
        }
    }
}

// ── Shared stat widgets ────────────────────────────────────────────────────

@Composable
private fun KpiRow(pairs: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        pairs.forEach { (label, value) ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)
            ) {
                Column {
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ProgressBarWithLabel(fraction: Float) {
    val f = fraction.coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        drawRoundRect(color = track, size = Size(size.width, size.height), cornerRadius = CornerRadius(8f, 8f))
        drawRoundRect(color = primary, size = Size(size.width * f, size.height), cornerRadius = CornerRadius(8f, 8f))
    }
    Text("${(f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun BarChart(values: List<Double>) {
    if (values.isEmpty() || values.all { it <= 0.0 }) {
        Text(
            "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val bar = MaterialTheme.colorScheme.primary
    val maxVal = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val n = values.size
        val gap = 6f
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        var x = 0f
        values.forEach { v ->
            val h = (v / maxVal * size.height).toFloat().coerceAtLeast(0f)
            drawRoundRect(
                color = bar,
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(3f, 3f)
            )
            x += barW + gap
        }
    }
}
