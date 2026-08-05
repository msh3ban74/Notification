package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.calculator.LedgerStatus
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Sprint 1 — Application Foundation (UI shell).
 * Sprint 2 — UI/UX Polish (spacing, hierarchy, proportions).
 * Sprint 3/4 — Smart Task & Smart Debt foundation: the placeholder cards
 * are now REAL summaries read from the EXISTING data layer.
 *
 *  - Today's Tasks    ← the existing reminders flow (Room, via
 *                       MainViewModel.allReminders).
 *  - Upcoming Debts   ← the existing ledger (persons + transactions) using
 *                       the existing LedgerCalculator — no debt logic is
 *                       duplicated here.
 *  - Recent Activity  ← latest reminders + ledger transactions merged.
 *
 * No fake data: every row shown comes straight from the repositories, and
 * each section shows an honest empty message when there is nothing yet.
 */
@Composable
fun DashboardScreen(
    isArabic: Boolean = false,
    reminders: List<ReminderEntity> = emptyList(),
    persons: List<PersonEntity> = emptyList(),
    transactions: List<LedgerTransactionEntity> = emptyList(),
    alarms: List<AlarmEntity> = emptyList(),
    onNavigateToTasks: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {},
    onNavigateToGam3iya: () -> Unit = {},
    onNavigateToIslamic: () -> Unit = {},
    onNavigateToHealthNotes: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppPadding.screen)
            .padding(top = Spacing.sm, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        GreetingSection(isArabic = isArabic)

        TodaysTasksSection(
            isArabic = isArabic,
            reminders = reminders,
            onClick = onNavigateToTasks
        )

        UpcomingDebtsSection(
            isArabic = isArabic,
            persons = persons,
            transactions = transactions,
            onClick = onNavigateToLedger
        )

        UpcomingNotificationsSection(
            isArabic = isArabic,
            reminders = reminders,
            alarms = alarms,
            onClick = onNavigateToNotifications
        )

        QuickActionsSection(
            isArabic = isArabic,
            onNavigateToLedger = onNavigateToLedger,
            onNavigateToGam3iya = onNavigateToGam3iya,
            onNavigateToIslamic = onNavigateToIslamic,
            onNavigateToHealthNotes = onNavigateToHealthNotes
        )

        RecentActivitySection(
            isArabic = isArabic,
            reminders = reminders,
            persons = persons,
            transactions = transactions
        )
    }
}

@Composable
private fun GreetingSection(isArabic: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = if (isArabic) "أهلاً بيك 👋" else "Welcome back 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (isArabic) "نظرة سريعة على يومك" else "Here's a quick look at your day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shared section header style, used above every card group on this screen. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

/** One compact icon+text line inside a summary card. */
@Composable
private fun SummaryRow(
    icon: ImageVector,
    text: String,
    trailing: String? = null,
    trailingColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppDimens.iconSizeSmall)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = trailingColor
            )
        }
    }
}

/** Muted single-line message used when a section has no real data yet. */
@Composable
private fun SectionEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TodaysTasksSection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // "Today" = [start of today, start of tomorrow) in local time.
    val todaysTasks = remember(reminders) {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 24L * 60 * 60 * 1000
        reminders
            .filter { it.dueDate in startOfDay until endOfDay }
            .sortedBy { it.dueDate }
    }
    val pendingCount = todaysTasks.count { !it.isCompleted }

    PremiumCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isArabic) "مهام اليوم" else "Today's Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (todaysTasks.isNotEmpty()) {
                    Text(
                        text = if (isArabic) "$pendingCount متبقية" else "$pendingCount pending",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            HorizontalDivider()
            if (todaysTasks.isEmpty()) {
                SectionEmptyText(
                    if (isArabic) "لا توجد مهام مستحقة اليوم." else "No tasks due today."
                )
            } else {
                todaysTasks.take(3).forEach { task ->
                    SummaryRow(
                        icon = if (task.isCompleted) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },
                        text = task.title,
                        trailing = timeFormat.format(Date(task.dueDate))
                    )
                }
                if (todaysTasks.size > 3) {
                    Text(
                        text = if (isArabic) {
                            "و${todaysTasks.size - 3} مهام أخرى…"
                        } else {
                            "and ${todaysTasks.size - 3} more…"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingDebtsSection(
    isArabic: Boolean,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    onClick: () -> Unit
) {
    // Reuses the EXISTING LedgerCalculator — the same net-balance logic the
    // Ledger screen uses. Nothing is recomputed differently here.
    val openBalances = remember(persons, transactions) {
        persons.mapNotNull { person ->
            val summary = LedgerCalculator.calculateNetBalance(
                transactions.filter { it.personId == person.id }
            )
            if (summary.status == LedgerStatus.SETTLED) null else person to summary
        }.sortedByDescending { it.second.netAmount }
    }

    PremiumCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "الديون القادمة" else "Upcoming Debts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            if (openBalances.isEmpty()) {
                SectionEmptyText(
                    if (isArabic) "لا توجد ديون مفتوحة." else "No open debts."
                )
            } else {
                openBalances.take(3).forEach { (person, summary) ->
                    val owedToMe = summary.status == LedgerStatus.THEY_OWE_ME
                    SummaryRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        text = person.name,
                        trailing = if (isArabic) {
                            if (owedToMe) "له ${summary.netAmount} ج.م" else "عليك ${summary.netAmount} ج.م"
                        } else {
                            if (owedToMe) "+${summary.netAmount} EGP" else "-${summary.netAmount} EGP"
                        },
                        trailingColor = if (owedToMe) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                if (openBalances.size > 3) {
                    Text(
                        text = if (isArabic) {
                            "و${openBalances.size - 3} حسابات أخرى…"
                        } else {
                            "and ${openBalances.size - 3} more…"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Sprint 5 — Upcoming Notifications: the next scheduled notifications from
 * the EXISTING data — pending future reminders plus enabled future alarms
 * (both are what the AlarmManagerScheduler pipeline will actually fire).
 */
@Composable
private fun UpcomingNotificationsSection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    alarms: List<AlarmEntity>,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE dd MMM, hh:mm a", Locale.getDefault()) }

    // (timestamp, isAlarm, title) triples merged and sorted — soonest first.
    val upcoming = remember(reminders, alarms) {
        val now = System.currentTimeMillis()
        val reminderItems = reminders
            .filter { !it.isCompleted && it.dueDate >= now }
            .map { Triple(it.dueDate, false, it.title) }
        val alarmItems = alarms
            .filter { it.isEnabled && it.timeInMillis >= now }
            .map { Triple(it.timeInMillis, true, it.title) }
        (reminderItems + alarmItems).sortedBy { it.first }
    }

    PremiumCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "الإشعارات القادمة" else "Upcoming Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            if (upcoming.isEmpty()) {
                SectionEmptyText(
                    if (isArabic) "لا توجد إشعارات مجدولة." else "No scheduled notifications."
                )
            } else {
                upcoming.take(3).forEach { (timestamp, isAlarm, title) ->
                    SummaryRow(
                        icon = if (isAlarm) Icons.Default.Alarm else Icons.Default.NotificationsActive,
                        text = title,
                        trailing = dateFormat.format(Date(timestamp))
                    )
                }
                if (upcoming.size > 3) {
                    Text(
                        text = if (isArabic) {
                            "و${upcoming.size - 3} إشعارات أخرى…"
                        } else {
                            "and ${upcoming.size - 3} more…"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class QuickAction(
    val labelEn: String,
    val labelAr: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsSection(
    isArabic: Boolean,
    onNavigateToLedger: () -> Unit,
    onNavigateToGam3iya: () -> Unit,
    onNavigateToIslamic: () -> Unit,
    onNavigateToHealthNotes: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionTitle(if (isArabic) "إجراءات سريعة" else "Quick Actions")

        val actions = listOf(
            QuickAction(
                "Ledger", "دفتر الديون",
                Icons.Default.AccountBalanceWallet, onNavigateToLedger
            ),
            QuickAction(
                "Gam3iya", "الجمعيات",
                Icons.Default.Group, onNavigateToGam3iya
            ),
            QuickAction(
                "Islamic", "إسلاميات",
                Icons.Default.Mosque, onNavigateToIslamic
            ),
            QuickAction(
                "Health", "الصحة",
                Icons.Default.WaterDrop, onNavigateToHealthNotes
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(actions) { action ->
                PremiumCard(
                    onClick = action.onClick,
                    contentPadding = AppPadding.cardCompact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.95f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = if (isArabic) action.labelAr else action.labelEn,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.iconSizeMedium)
                        )
                        Text(
                            text = if (isArabic) action.labelAr else action.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
                }
            }
        }
    }
}

/** A single, already-formatted feed line for the Recent Activity card. */
private data class ActivityEntry(
    val timestamp: Long,
    val icon: ImageVector,
    val text: String,
    val trailing: String
)

@Composable
private fun RecentActivitySection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    // Latest reminders (by creation) merged with latest ledger transactions
    // (by date) — both straight from the existing Room flows.
    val entries = remember(reminders, persons, transactions, isArabic) {
        val personNames = persons.associateBy({ it.id }, { it.name })
        val reminderEntries = reminders.map {
            ActivityEntry(
                timestamp = it.createdAt,
                icon = Icons.Default.NotificationsActive,
                text = it.title,
                trailing = dateFormat.format(Date(it.createdAt))
            )
        }
        val txEntries = transactions.map {
            val name = personNames[it.personId]
                ?: if (isArabic) "غير معروف" else "Unknown"
            ActivityEntry(
                timestamp = it.date,
                icon = Icons.Default.AccountBalanceWallet,
                text = if (isArabic) {
                    "معاملة مع $name — ${it.amount} ج.م"
                } else {
                    "Transaction with $name — ${it.amount} EGP"
                },
                trailing = dateFormat.format(Date(it.date))
            )
        }
        (reminderEntries + txEntries).sortedByDescending { it.timestamp }.take(4)
    }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "النشاط الأخير" else "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            if (entries.isEmpty()) {
                SectionEmptyText(
                    if (isArabic) "لا يوجد نشاط لعرضه بعد." else "No recent activity to show yet."
                )
            } else {
                entries.forEach { entry ->
                    SummaryRow(
                        icon = entry.icon,
                        text = entry.text,
                        trailing = entry.trailing,
                        trailingColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
