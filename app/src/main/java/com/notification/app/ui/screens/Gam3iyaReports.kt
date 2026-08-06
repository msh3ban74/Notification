package com.notification.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.Gam3iyaPaymentEntity
import com.notification.app.domain.calculator.Gam3iyaStatus
import java.text.SimpleDateFormat
import java.util.*

private fun sameDay(a: Long, b: Long): Boolean {
    if (a <= 0 || b <= 0) return false
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

/** Monthly calendar highlighting collection dates, payments and late turns. */
@Composable
fun Gam3iyaCalendarCard(
    gam3iya: Gam3iyaEntity,
    members: List<Gam3iyaMemberEntity>,
    payments: List<Gam3iyaPaymentEntity>,
    isArabic: Boolean
) {
    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH) }
    // Displayed month anchored to the gam3iya start month by default.
    var anchor by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = if (gam3iya.startDate > 0) gam3iya.startDate else System.currentTimeMillis()
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis)
    }
    val cal = Calendar.getInstance().apply { timeInMillis = anchor; set(Calendar.DAY_OF_MONTH, 1) }
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val now = System.currentTimeMillis()

    // Collection dates in this month → member (manager); or my collection (participant).
    val collectionByDay = remember(anchor, members, gam3iya) {
        val map = HashMap<Int, Gam3iyaMemberEntity?>()
        if (gam3iya.mode == "PARTICIPANT") {
            val d = gam3iya.myCollectionDate
            if (d > 0) {
                val c = Calendar.getInstance().apply { timeInMillis = d }
                if (c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.MONTH) == cal.get(Calendar.MONTH))
                    map[c.get(Calendar.DAY_OF_MONTH)] = null
            }
        } else members.forEach { m ->
            val c = Calendar.getInstance().apply { timeInMillis = m.payoutDate }
            if (m.payoutDate > 0 && c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.MONTH) == cal.get(Calendar.MONTH))
                map[c.get(Calendar.DAY_OF_MONTH)] = m
        }
        map
    }
    val paymentDays = remember(anchor, payments) {
        payments.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            it.date > 0 && c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        }.map { Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH) }.toSet()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { anchor = Calendar.getInstance().apply { timeInMillis = anchor; add(Calendar.MONTH, -1) }.timeInMillis }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null)
                }
                Text(monthFmt.format(Date(anchor)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { anchor = Calendar.getInstance().apply { timeInMillis = anchor; add(Calendar.MONTH, 1) }.timeInMillis }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
            // Weekday header
            val dows = if (isArabic) listOf("ح", "ن", "ث", "ر", "خ", "ج", "س") else listOf("S", "M", "T", "W", "T", "F", "S")
            Row(Modifier.fillMaxWidth()) {
                dows.forEach { d -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            Spacer(Modifier.height(4.dp))
            // Grid
            val cells = firstDow + daysInMonth
            val rows = (cells + 6) / 7
            var day = 1
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val idx = r * 7 + c
                        if (idx < firstDow || day > daysInMonth) {
                            Box(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val thisDay = day
                            val dayCal = Calendar.getInstance().apply { timeInMillis = anchor; set(Calendar.DAY_OF_MONTH, thisDay) }
                            val isCollection = collectionByDay.containsKey(thisDay)
                            val member = collectionByDay[thisDay]
                            val isLate = member?.let { !it.isPayoutReceived && it.payoutDate in 1 until now } ?: false
                            val hasPayment = paymentDays.contains(thisDay)
                            val isToday = sameDay(dayCal.timeInMillis, now)
                            DayCell(
                                day = thisDay,
                                highlight = when {
                                    isLate -> MaterialTheme.colorScheme.error
                                    isCollection -> MaterialTheme.colorScheme.primary
                                    else -> null
                                },
                                dot = hasPayment,
                                today = isToday,
                                modifier = Modifier.weight(1f)
                            )
                            day++
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(MaterialTheme.colorScheme.primary, if (isArabic) "قبض" else "Collection")
                LegendDot(MaterialTheme.colorScheme.error, if (isArabic) "متأخر" else "Late")
                LegendDot(MaterialTheme.colorScheme.tertiary, if (isArabic) "دفعة" else "Payment")
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, highlight: Color?, dot: Boolean, today: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
        val bg = highlight?.copy(alpha = 0.18f) ?: Color.Transparent
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(bg)
                .then(if (today) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$day", style = MaterialTheme.typography.bodySmall, fontWeight = if (highlight != null) FontWeight.Bold else FontWeight.Normal,
                    color = highlight ?: MaterialTheme.colorScheme.onSurface)
                if (dot) Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Statistics + simple charts. */
@Composable
fun Gam3iyaReportsCard(
    gam3iya: Gam3iyaEntity,
    members: List<Gam3iyaMemberEntity>,
    payments: List<Gam3iyaPaymentEntity>,
    status: Gam3iyaStatus,
    isArabic: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tertiary = MaterialTheme.colorScheme.tertiary

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isArabic) "التقارير والإحصائيات" else "Reports & statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Stat tiles
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(if (isArabic) "قبضوا" else "Collected", "${status.collectedCount}/${if (gam3iya.mode == "PARTICIPANT") 1 else members.size}", Modifier.weight(1f))
                StatBox(if (isArabic) "دفعوا" else "Paid", "${status.paidThisMonthCount}", Modifier.weight(1f))
                StatBox(if (isArabic) "متأخرون" else "Late", "${status.lateMembers.size}", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(if (isArabic) "المدفوع" else "Paid amount", fmtMoney(status.totalPaid, gam3iya.currency), Modifier.weight(1f))
                StatBox(if (isArabic) "المتبقي" else "Remaining", fmtMoney(status.remainingAmount, gam3iya.currency), Modifier.weight(1f))
            }

            // Progress bar (collected fraction)
            Text(if (isArabic) "تقدّم الجمعية" else "Overall progress", style = MaterialTheme.typography.labelLarge)
            Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                val w = size.width
                drawRoundRect(color = track, size = Size(w, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
                drawRoundRect(color = primary, size = Size(w * status.progressFraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
            }
            Text("${(status.progressFraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

            // Monthly installments collected — simple bar chart from payments
            if (payments.isNotEmpty()) {
                Text(if (isArabic) "الدفعات الشهرية" else "Monthly payments", style = MaterialTheme.typography.labelLarge)
                val byMonth = remember(payments) {
                    val m = sortedMapOf<Int, Double>()
                    payments.forEach { p ->
                        val key = p.monthIndex.coerceAtLeast(0)
                        m[key] = (m[key] ?: 0.0) + p.amount
                    }
                    m
                }
                val maxVal = (byMonth.values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                    val n = byMonth.size.coerceAtLeast(1)
                    val gap = 6f
                    val barW = (size.width - gap * (n - 1)) / n
                    var x = 0f
                    byMonth.values.forEach { v ->
                        val h = (v / maxVal * size.height).toFloat()
                        drawRoundRect(
                            color = tertiary,
                            topLeft = Offset(x, size.height - h),
                            size = Size(barW, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                        x += barW + gap
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
