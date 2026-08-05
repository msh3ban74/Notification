package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.data.local.entities.WorkNoteEntity
import com.notification.app.domain.calculator.PrayerTime
import com.notification.app.ui.components.SmartWidget
import com.notification.app.ui.components.SummaryCountRow
import androidx.compose.ui.unit.dp
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.calculator.LedgerStatus
import com.notification.app.domain.model.AiSuggestion
import com.notification.app.domain.model.AiSuggestionAction
import com.notification.app.domain.model.ReminderCategory
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isArabic: Boolean = false,
    reminders: List<ReminderEntity> = emptyList(),
    persons: List<PersonEntity> = emptyList(),
    transactions: List<LedgerTransactionEntity> = emptyList(),
    alarms: List<AlarmEntity> = emptyList(),
    gam3iyaMembers: List<Gam3iyaMemberEntity> = emptyList(),
    prayerTimes: List<PrayerTime> = emptyList(),
    workNotes: List<WorkNoteEntity> = emptyList(),
    waterCount: Int = 0,
    aiSuggestions: List<AiSuggestion> = emptyList(),
    aiSuggestionsLoading: Boolean = false,
    onRefreshSuggestions: () -> Unit = {},
    onPullRefresh: () -> Unit = {},
    onAskRafeeq: (String) -> Unit = {},
    onWaterClick: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {},
    onNavigateToGam3iya: () -> Unit = {},
    onNavigateToIslamic: () -> Unit = {},
    onNavigateToHealthNotes: () -> Unit = {}
) {
    // Ask the existing Gemini pipeline for fresh suggestions once per
    // dashboard entry (the ViewModel guards with a TTL cache, so this is
    // free while the cache is warm).
    LaunchedEffect(Unit) { onRefreshSuggestions() }

    // Sprint 6 — pull-to-refresh: forces a fresh AI-suggestion fetch; the
    // Room-backed sections refresh themselves reactively (hot StateFlows),
    // so nothing is recreated and the UI never blocks.
    PullToRefreshBox(
        isRefreshing = aiSuggestionsLoading,
        onRefresh = onPullRefresh,
        modifier = Modifier.fillMaxSize()
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

        // Sprint 6 — premium first-run welcome: shown only when the user
        // has no data at all anywhere; disappears forever after the first
        // Smart Item is created.
        val hasAnyData = reminders.isNotEmpty() || persons.isNotEmpty() ||
            alarms.isNotEmpty() || gam3iyaMembers.isNotEmpty() || workNotes.isNotEmpty()
        if (!hasAnyData) {
            PremiumCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.iconSizeLarge)
                    )
                    Text(
                        text = if (isArabic) "أهلاً بك في رفيق" else "Welcome to Rafeeq",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isArabic)
                            "رفيقك الذكي في كل تفاصيل حياتك.\nاضغط زر (+) لإضافة أول عنصر ذكي"
                        else
                            "Your intelligent life companion.\nTap (+) to add your first Smart Item",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Sprint 6 — Executive: today's full summary + contextual widgets.
        TodaysSummarySection(
            isArabic = isArabic,
            reminders = reminders,
            persons = persons,
            transactions = transactions,
            alarms = alarms,
            gam3iyaMembers = gam3iyaMembers,
            workNotes = workNotes
        )

        SmartWidgetsSection(
            isArabic = isArabic,
            reminders = reminders,
            gam3iyaMembers = gam3iyaMembers,
            prayerTimes = prayerTimes,
            workNotes = workNotes,
            waterCount = waterCount,
            onWaterClick = onWaterClick,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToGam3iya = onNavigateToGam3iya,
            onNavigateToIslamic = onNavigateToIslamic,
            onNavigateToHealthNotes = onNavigateToHealthNotes
        )

        // Rafeeq Design Language — executive overview: the day at a
        // glance in four large calm tiles, all real data.
        ExecutiveStatsSection(
            isArabic = isArabic,
            reminders = reminders,
            persons = persons,
            transactions = transactions,
            alarms = alarms,
            onTasksClick = onNavigateToTasks,
            onNotificationsClick = onNavigateToNotifications,
            onLedgerClick = onNavigateToLedger
        )

        RafeeqSuggestionsSection(
            isArabic = isArabic,
            reminders = reminders,
            persons = persons,
            transactions = transactions,
            aiSuggestions = aiSuggestions,
            aiLoading = aiSuggestionsLoading,
            onRefresh = onRefreshSuggestions,
            onAskRafeeq = onAskRafeeq,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToLedger = onNavigateToLedger
        )

        TodaysTasksSection(
            isArabic = isArabic,
            reminders = reminders,
            onClick = onNavigateToTasks
        )

        // Sprint 6 — smart sections: these cards appear only when they
        // actually have something to show.
        val now = System.currentTimeMillis()
        val hasOpenDebts = remember(persons, transactions) {
            persons.any { p ->
                LedgerCalculator.calculateNetBalance(
                    transactions.filter { it.personId == p.id }
                ).status != LedgerStatus.SETTLED
            }
        }
        val hasUpcomingNotifications = remember(reminders, alarms) {
            reminders.any { !it.isCompleted && it.dueDate >= now } ||
                alarms.any { it.isEnabled && it.timeInMillis >= now }
        }

        if (hasOpenDebts) {
            UpcomingDebtsSection(
                isArabic = isArabic,
                persons = persons,
                transactions = transactions,
                onClick = onNavigateToLedger
            )
        }

        if (hasUpcomingNotifications) {
            UpcomingNotificationsSection(
                isArabic = isArabic,
                reminders = reminders,
                alarms = alarms,
                onClick = onNavigateToNotifications
            )
        }

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
}

@Composable
private fun GreetingSection(isArabic: Boolean) {
    val dateLine = remember(isArabic) {
        SimpleDateFormat(
            "EEEE، d MMMM yyyy",
            if (isArabic) Locale("ar") else Locale.ENGLISH
        ).format(Date())
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = if (isArabic) "أهلاً بيك 👋" else "Welcome back 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = dateLine,
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
 * Sprint 6 — Today's Summary: the full executive breakdown of the day
 * (pending/overdue tasks, upcoming reminders and alarms, outstanding
 * debts, upcoming gam3iya payouts, health reminders). Only non-zero
 * rows are shown, and the whole card hides itself when the day is
 * clear. All figures come from the EXISTING repositories/calculators.
 */
@Composable
private fun TodaysSummarySection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    alarms: List<AlarmEntity>,
    gam3iyaMembers: List<Gam3iyaMemberEntity>,
    workNotes: List<WorkNoteEntity>
) {
    val now = System.currentTimeMillis()
    val monthAhead = now + 35L * 24 * 60 * 60 * 1000

    val pendingTasks = remember(reminders) { reminders.count { !it.isCompleted } }
    val overdueTasks = remember(reminders) {
        reminders.count { !it.isCompleted && it.dueDate < now }
    }
    val upcomingReminders = remember(reminders) {
        reminders.count { !it.isCompleted && it.dueDate >= now }
    }
    val upcomingAlarms = remember(alarms) {
        alarms.count { it.isEnabled && it.timeInMillis >= now }
    }
    val outstandingDebts = remember(persons, transactions) {
        persons.count { p ->
            LedgerCalculator.calculateNetBalance(
                transactions.filter { it.personId == p.id }
            ).status != LedgerStatus.SETTLED
        }
    }
    val gam3iyaPayments = remember(gam3iyaMembers) {
        gam3iyaMembers.count { !it.isPayoutReceived && it.payoutDate in now..monthAhead }
    }
    val healthReminders = remember(reminders, workNotes) {
        reminders.count {
            !it.isCompleted && it.category == ReminderCategory.MEDICINE.name
        } + workNotes.count { !it.isDone && (it.reminderTime ?: 0L) >= now }
    }

    val total = pendingTasks + upcomingAlarms + outstandingDebts + gam3iyaPayments + healthReminders
    if (total == 0) return

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "ملخص اليوم" else "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            if (pendingTasks > 0) SummaryCountRow(
                icon = Icons.Default.CheckCircle,
                label = if (isArabic) "مهام معلقة" else "Pending tasks",
                count = pendingTasks
            )
            if (overdueTasks > 0) SummaryCountRow(
                icon = Icons.Default.Warning,
                label = if (isArabic) "مهام متأخرة" else "Overdue tasks",
                count = overdueTasks,
                highlight = true
            )
            if (upcomingReminders > 0) SummaryCountRow(
                icon = Icons.Default.NotificationsActive,
                label = if (isArabic) "تذكيرات قادمة" else "Upcoming reminders",
                count = upcomingReminders
            )
            if (upcomingAlarms > 0) SummaryCountRow(
                icon = Icons.Default.Alarm,
                label = if (isArabic) "منبهات قادمة" else "Upcoming alarms",
                count = upcomingAlarms
            )
            if (outstandingDebts > 0) SummaryCountRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = if (isArabic) "ديون قائمة" else "Outstanding debts",
                count = outstandingDebts
            )
            if (gam3iyaPayments > 0) SummaryCountRow(
                icon = Icons.Default.Group,
                label = if (isArabic) "أقساط جمعية قادمة" else "Upcoming gam3iya payments",
                count = gam3iyaPayments
            )
            if (healthReminders > 0) SummaryCountRow(
                icon = Icons.Default.Medication,
                label = if (isArabic) "تذكيرات صحية" else "Health reminders",
                count = healthReminders
            )
        }
    }
}

/**
 * Sprint 6 — Smart Widgets: contextual cards that animate in only when
 * relevant (medicine due, next prayer, upcoming meeting, gam3iya payout,
 * water goal, open work notes) and collapse away otherwise. Each one is
 * a [SmartWidget] reading EXISTING data — no new logic.
 */
@Composable
private fun SmartWidgetsSection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    gam3iyaMembers: List<Gam3iyaMemberEntity>,
    prayerTimes: List<PrayerTime>,
    workNotes: List<WorkNoteEntity>,
    waterCount: Int,
    onWaterClick: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToGam3iya: () -> Unit,
    onNavigateToIslamic: () -> Unit,
    onNavigateToHealthNotes: () -> Unit
) {
    val now = System.currentTimeMillis()
    val dayAhead = now + 24L * 60 * 60 * 1000
    val twoDaysAhead = now + 48L * 60 * 60 * 1000
    val monthAhead = now + 35L * 24 * 60 * 60 * 1000
    val timeFormat = remember { SimpleDateFormat("EEE dd MMM, hh:mm a", Locale.getDefault()) }

    val medicineDue = remember(reminders) {
        reminders.filter {
            !it.isCompleted && it.category == ReminderCategory.MEDICINE.name &&
                it.dueDate in now..dayAhead
        }.sortedBy { it.dueDate }
    }
    val nextPrayer = remember(prayerTimes) {
        prayerTimes.filter { it.timestamp > now }.minByOrNull { it.timestamp }
    }
    val nextAppointment = remember(reminders) {
        reminders.filter {
            !it.isCompleted && it.category == ReminderCategory.APPOINTMENT.name &&
                it.dueDate in now..twoDaysAhead
        }.minByOrNull { it.dueDate }
    }
    val nextPayout = remember(gam3iyaMembers) {
        gam3iyaMembers.filter { !it.isPayoutReceived && it.payoutDate in now..monthAhead }
            .minByOrNull { it.payoutDate }
    }
    val openNotes = remember(workNotes) { workNotes.count { !it.isDone } }
    val waterGoal = 8

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SmartWidget(
            visible = medicineDue.isNotEmpty(),
            icon = Icons.Default.Medication,
            title = if (isArabic) "دواء اليوم" else "Medicine today",
            primaryLine = medicineDue.firstOrNull()?.title ?: "",
            secondaryLine = medicineDue.firstOrNull()?.let { timeFormat.format(Date(it.dueDate)) },
            onClick = onNavigateToTasks
        )

        SmartWidget(
            visible = nextPrayer != null,
            icon = Icons.Default.Mosque,
            title = if (isArabic) "الصلاة القادمة" else "Next prayer",
            primaryLine = nextPrayer?.let { if (isArabic) it.nameAr else it.nameEn } ?: "",
            secondaryLine = nextPrayer?.timeFormatted,
            onClick = onNavigateToIslamic
        )

        SmartWidget(
            visible = nextAppointment != null,
            icon = Icons.Default.Event,
            title = if (isArabic) "موعد قادم" else "Upcoming meeting",
            primaryLine = nextAppointment?.title ?: "",
            secondaryLine = nextAppointment?.let { timeFormat.format(Date(it.dueDate)) },
            onClick = onNavigateToTasks
        )

        SmartWidget(
            visible = nextPayout != null,
            icon = Icons.Default.Group,
            title = if (isArabic) "قبض جمعية قادم" else "Gam3iya payout",
            primaryLine = nextPayout?.memberName ?: "",
            secondaryLine = nextPayout?.let { timeFormat.format(Date(it.payoutDate)) },
            onClick = onNavigateToGam3iya
        )

        SmartWidget(
            visible = waterCount in 1 until waterGoal,
            icon = Icons.Default.WaterDrop,
            title = if (isArabic) "هدف الماء" else "Water goal",
            primaryLine = "$waterCount / $waterGoal",
            secondaryLine = if (isArabic) "اضغط لتسجيل كوب" else "Tap to log a glass",
            onClick = onWaterClick
        )

        SmartWidget(
            visible = openNotes > 0,
            icon = Icons.Default.CheckCircle,
            title = if (isArabic) "ملاحظات العمل" else "Work notes",
            primaryLine = if (isArabic) "$openNotes قيد التنفيذ" else "$openNotes open",
            onClick = onNavigateToHealthNotes
        )
    }
}

/**
 * Rafeeq Design Language — executive stat tiles (2×2): today's tasks,
 * upcoming notifications, open debts, and due bills as large calm
 * numbers. Every figure is computed from the EXISTING repositories.
 */
@Composable
private fun ExecutiveStatsSection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    alarms: List<AlarmEntity>,
    onTasksClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLedgerClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val (dayStart, dayEnd) = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        start to cal.timeInMillis
    }

    val todayTasks = remember(reminders) {
        reminders.count { !it.isCompleted && it.dueDate in dayStart until dayEnd }
    }
    val upcomingNotifications = remember(reminders, alarms) {
        reminders.count { !it.isCompleted && it.dueDate >= now } +
            alarms.count { it.isEnabled && it.timeInMillis >= now }
    }
    val openDebts = remember(persons, transactions) {
        persons.count { p ->
            val summary = LedgerCalculator.calculateNetBalance(
                transactions.filter { it.personId == p.id }
            )
            summary.status != LedgerStatus.SETTLED
        }
    }
    val dueBills = remember(reminders) {
        reminders.count {
            !it.isCompleted && it.category == ReminderCategory.BILL.name
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatTile(
                value = todayTasks,
                label = if (isArabic) "مهام اليوم" else "Today's tasks",
                onClick = onTasksClick,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = upcomingNotifications,
                label = if (isArabic) "التنبيهات القادمة" else "Upcoming alerts",
                onClick = onNotificationsClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatTile(
                value = openDebts,
                label = if (isArabic) "ديون مفتوحة" else "Open debts",
                onClick = onLedgerClick,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = dueBills,
                label = if (isArabic) "فواتير مستحقة" else "Bills due",
                onClick = onTasksClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    value: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * "Rafeeq Suggestions" — the dashboard's intelligent voice.
 *
 * Primary source: the EXISTING Gemini pipeline
 * (GeminiRepository.generateDashboardSuggestions), which reads the
 * user's REAL data and returns up to three actionable suggestions.
 * Each suggestion carries an [AiSuggestionAction] that maps to an
 * EXISTING flow (open Tasks / Notifications / Ledger, or hand the
 * question to the assistant) — no new business logic.
 *
 * Fallback: when the AI list is empty (offline, no key, no data), the
 * card falls back to local rule-based insights computed from the same
 * repositories; it hides entirely when there is nothing worth saying.
 */
@Composable
private fun RafeeqSuggestionsSection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    aiSuggestions: List<AiSuggestion>,
    aiLoading: Boolean,
    onRefresh: () -> Unit,
    onAskRafeeq: (String) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLedger: () -> Unit
) {
    if (aiSuggestions.isNotEmpty() || aiLoading) {
        PremiumCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.iconSizeMedium)
                    )
                    Text(
                        text = if (isArabic) "اقتراحات رفيق" else "Rafeeq Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (aiLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isArabic) "تحديث" else "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                HorizontalDivider()
                if (aiSuggestions.isEmpty()) {
                    SectionEmptyText(
                        if (isArabic) "رفيق يحضّر اقتراحاتك..." else "Rafeeq is preparing your suggestions..."
                    )
                } else {
                    aiSuggestions.forEach { suggestion ->
                        AiSuggestionRow(
                            suggestion = suggestion,
                            isArabic = isArabic,
                            onAskRafeeq = onAskRafeeq,
                            onNavigateToTasks = onNavigateToTasks,
                            onNavigateToNotifications = onNavigateToNotifications,
                            onNavigateToLedger = onNavigateToLedger
                        )
                    }
                }
            }
        }
        return
    }

    LocalRuleSuggestions(
        isArabic = isArabic,
        reminders = reminders,
        persons = persons,
        transactions = transactions
    )
}

@Composable
private fun AiSuggestionRow(
    suggestion: AiSuggestion,
    isArabic: Boolean,
    onAskRafeeq: (String) -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToLedger: () -> Unit
) {
    val actionLabel = when (suggestion.action) {
        AiSuggestionAction.OPEN_TASKS -> if (isArabic) "افتح المهام" else "Open tasks"
        AiSuggestionAction.OPEN_NOTIFICATIONS -> if (isArabic) "افتح التنبيهات" else "Open alerts"
        AiSuggestionAction.OPEN_LEDGER -> if (isArabic) "افتح الدفتر" else "Open ledger"
        AiSuggestionAction.ASK_RAFEEQ -> if (isArabic) "اسأل رفيق" else "Ask Rafeeq"
    }
    val onAction: () -> Unit = when (suggestion.action) {
        AiSuggestionAction.OPEN_TASKS -> onNavigateToTasks
        AiSuggestionAction.OPEN_NOTIFICATIONS -> onNavigateToNotifications
        AiSuggestionAction.OPEN_LEDGER -> onNavigateToLedger
        AiSuggestionAction.ASK_RAFEEQ -> ({ onAskRafeeq(suggestion.text) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = suggestion.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Local rule-based fallback insights (pre-AI behavior, unchanged):
 * computed from the same repositories, hides itself when empty.
 */
@Composable
private fun LocalRuleSuggestions(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>
) {
    val now = System.currentTimeMillis()
    val weekAhead = now + 7L * 24 * 60 * 60 * 1000

    val suggestions = remember(reminders, persons, transactions, isArabic) {
        buildList {
            val overdue = reminders.count { !it.isCompleted && it.dueDate < now }
            if (overdue > 0) add(
                if (isArabic) "لديك $overdue ${if (overdue == 1) "مهمة متأخرة" else "مهام متأخرة"} — راجعها الآن"
                else "You have $overdue overdue task${if (overdue == 1) "" else "s"} — review them now"
            )

            val billsThisWeek = reminders.count {
                !it.isCompleted && it.category == ReminderCategory.BILL.name &&
                    it.dueDate in now..weekAhead
            }
            if (billsThisWeek > 0) add(
                if (isArabic) "لديك $billsThisWeek ${if (billsThisWeek == 1) "فاتورة" else "فواتير"} خلال الأسبوع القادم"
                else "You have $billsThisWeek bill${if (billsThisWeek == 1) "" else "s"} due within a week"
            )

            val iOweCount = persons.count { p ->
                LedgerCalculator.calculateNetBalance(
                    transactions.filter { it.personId == p.id }
                ).status == LedgerStatus.I_OWE_THEM
            }
            if (iOweCount > 0) add(
                if (isArabic) "عليك مستحقات لـ $iOweCount ${if (iOweCount == 1) "شخص" else "أشخاص"} — راجع الدفتر"
                else "You owe $iOweCount ${if (iOweCount == 1) "person" else "people"} — check the ledger"
            )

            val medicineToday = reminders.count {
                !it.isCompleted && it.category == ReminderCategory.MEDICINE.name &&
                    it.dueDate in now..(now + 24L * 60 * 60 * 1000)
            }
            if (medicineToday > 0) add(
                if (isArabic) "لا تنسَ دواءك — $medicineToday ${if (medicineToday == 1) "جرعة" else "جرعات"} خلال ٢٤ ساعة"
                else "Don't forget your medicine — $medicineToday dose${if (medicineToday == 1) "" else "s"} in the next 24h"
            )
        }.take(3)
    }

    if (suggestions.isEmpty()) return

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.iconSizeMedium)
                )
                Text(
                    text = if (isArabic) "اقتراحات رفيق" else "Rafeeq Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider()
            suggestions.forEach { suggestion ->
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
