package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.HabitEntity
import com.notification.app.data.local.entities.HabitLogEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.data.local.entities.WorkNoteEntity
import com.notification.app.domain.calculator.HabitCalculator
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.calculator.LedgerStatus
import com.notification.app.domain.calculator.PrayerTime
import com.notification.app.domain.model.AiSuggestion
import com.notification.app.domain.model.AiSuggestionAction
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.ui.components.SmartWidget
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumButton
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.designsystem.PremiumCardStyle
import com.notification.app.ui.designsystem.SkeletonLine
import com.notification.app.ui.theme.AccentAmber
import com.notification.app.ui.theme.AccentCoral
import com.notification.app.ui.theme.AccentPink
import com.notification.app.ui.theme.AccentSky
import com.notification.app.ui.theme.AccentTeal
import com.notification.app.ui.theme.AccentViolet
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * رفيق — شاشة «يومك».
 *
 * The whole app answers one question: «إيه اللي عليّا؟». This screen is
 * that answer: one chronological list of everything the user needs today —
 * medicine, tasks, bills, debts promised back, gam3iya installments,
 * alarms — each checkable in place, followed by a simple money snapshot
 * and the assistant's suggestions. Every row comes straight from the
 * existing Room flows; nothing here is fake or duplicated logic.
 */

/** One row in the «عليك النهاردة» timeline. */
private data class TodayItem(
    val time: Long,
    val icon: ImageVector,
    val title: String,
    val timeLabel: String,
    val accent: androidx.compose.ui.graphics.Color,
    val isOverdue: Boolean = false,
    val reminder: ReminderEntity? = null,   // checkable when present
    val onOpen: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isArabic: Boolean = false,
    userName: String = "",
    gam3iyas: List<Gam3iyaEntity> = emptyList(),
    reminders: List<ReminderEntity> = emptyList(),
    persons: List<PersonEntity> = emptyList(),
    transactions: List<LedgerTransactionEntity> = emptyList(),
    alarms: List<AlarmEntity> = emptyList(),
    prayerTimes: List<PrayerTime> = emptyList(),
    workNotes: List<WorkNoteEntity> = emptyList(),
    waterCount: Int = 0,
    financialItems: List<FinancialItemEntity> = emptyList(),
    habits: List<HabitEntity> = emptyList(),
    habitLogs: List<HabitLogEntity> = emptyList(),
    aiSuggestions: List<AiSuggestion> = emptyList(),
    aiSuggestionsLoading: Boolean = false,
    onRefreshSuggestions: () -> Unit = {},
    onPullRefresh: () -> Unit = {},
    onAskRafeeq: (String) -> Unit = {},
    onWaterClick: () -> Unit = {},
    onToggleReminderDone: (ReminderEntity) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLedger: () -> Unit = {},
    onNavigateToGam3iya: () -> Unit = {},
    onNavigateToIslamic: () -> Unit = {},
    onNavigateToHealthNotes: () -> Unit = {},
    onNavigateToFinancial: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {}
) {
    LaunchedEffect(Unit) { onRefreshSuggestions() }

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val now = System.currentTimeMillis()
    val (dayStart, dayEnd) = remember {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, 1)
        start to c.timeInMillis
    }
    val tomorrowEnd = dayEnd + 24L * 60 * 60 * 1000
    val personNames = remember(persons) { persons.associateBy({ it.id }, { it.name }) }

    // ── «عليك النهاردة» — one merged, chronological timeline ────────────
    val todayItems = remember(reminders, alarms, transactions, personNames, isArabic) {
        buildList {
            // Overdue first: anything the user let slip is what the
            // companion should surface loudest.
            reminders.filter { !it.isCompleted && it.dueDate in 1 until dayStart }
                .sortedBy { it.dueDate }
                .forEach { r ->
                    add(TodayItem(
                        time = r.dueDate,
                        icon = categoryIcon(r.category),
                        title = r.title,
                        timeLabel = if (isArabic) "متأخر" else "Overdue",
                        accent = categoryAccent(r.category),
                        isOverdue = true,
                        reminder = r,
                        onOpen = onNavigateToTasks
                    ))
                }
            // Everything due today.
            reminders.filter { !it.isCompleted && it.dueDate in dayStart until dayEnd }
                .forEach { r ->
                    add(TodayItem(
                        time = r.dueDate,
                        icon = categoryIcon(r.category),
                        title = r.title,
                        timeLabel = timeFormat.format(Date(r.dueDate)),
                        accent = categoryAccent(r.category),
                        reminder = r,
                        onOpen = onNavigateToTasks
                    ))
                }
            alarms.filter { it.isEnabled && it.timeInMillis in dayStart until dayEnd }
                .forEach { a ->
                    add(TodayItem(
                        time = a.timeInMillis,
                        icon = Icons.Default.Alarm,
                        title = a.title.ifBlank { if (isArabic) "منبّه" else "Alarm" },
                        timeLabel = timeFormat.format(Date(a.timeInMillis)),
                        accent = AccentSky,
                        onOpen = onNavigateToNotifications
                    ))
                }
            // Debts promised today that have no linked reminder of their own
            // (those with one already appear as reminders above).
            transactions.filter { it.linkedReminderId == null && it.dueDate in dayStart until dayEnd }
                .forEach { tx ->
                    val name = personNames[tx.personId] ?: ""
                    add(TodayItem(
                        time = tx.dueDate,
                        icon = Icons.Default.AccountBalanceWallet,
                        title = if (isArabic) "دين مع $name — ${tx.amount.toLong()} ج.م"
                        else "Debt with $name — ${tx.amount.toLong()} EGP",
                        timeLabel = if (isArabic) "النهاردة" else "Today",
                        accent = com.notification.app.ui.theme.Primary,
                        onOpen = onNavigateToLedger
                    ))
                }
        }.sortedWith(compareBy({ !it.isOverdue }, { it.time }))
    }
    val doneToday = remember(reminders) {
        reminders.count { it.isCompleted && it.dueDate in dayStart until dayEnd }
    }
    val tomorrowCount = remember(reminders, alarms) {
        reminders.count { !it.isCompleted && it.dueDate in dayEnd until tomorrowEnd } +
            alarms.count { it.isEnabled && it.timeInMillis in dayEnd until tomorrowEnd }
    }

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
                .padding(top = Spacing.sm, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            TodayHero(
                isArabic = isArabic,
                userName = userName,
                pendingCount = todayItems.size,
                overdueCount = todayItems.count { it.isOverdue },
                doneToday = doneToday,
                onAskRafeeq = onAskRafeeq
            )

            TodayTimelineCard(
                isArabic = isArabic,
                items = todayItems,
                doneToday = doneToday,
                tomorrowCount = tomorrowCount,
                onToggleReminderDone = onToggleReminderDone
            )

            MoneySnapshotCard(
                isArabic = isArabic,
                persons = persons,
                transactions = transactions,
                financialItems = financialItems,
                gam3iyas = gam3iyas,
                onOpenLedger = onNavigateToLedger,
                onOpenFinancial = onNavigateToFinancial,
                onOpenGam3iya = onNavigateToGam3iya
            )

            CompanionWidgets(
                isArabic = isArabic,
                prayerTimes = prayerTimes,
                workNotes = workNotes,
                waterCount = waterCount,
                habits = habits,
                habitLogs = habitLogs,
                onWaterClick = onWaterClick,
                onNavigateToIslamic = onNavigateToIslamic,
                onNavigateToHealthNotes = onNavigateToHealthNotes,
                onNavigateToHabits = onNavigateToHabits
            )

            RafeeqSuggestionsSection(
                isArabic = isArabic,
                reminders = reminders,
                persons = persons,
                transactions = transactions,
                financialItems = financialItems,
                aiSuggestions = aiSuggestions,
                aiLoading = aiSuggestionsLoading,
                onRefresh = onRefreshSuggestions,
                onAskRafeeq = onAskRafeeq,
                onNavigateToTasks = onNavigateToTasks,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToLedger = onNavigateToLedger
            )
        }
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    ReminderCategory.MEDICINE.name -> Icons.Default.Medication
    ReminderCategory.BILL.name -> Icons.Default.ReceiptLong
    ReminderCategory.MONEY.name -> Icons.Default.AccountBalanceWallet
    ReminderCategory.APPOINTMENT.name -> Icons.Default.Event
    ReminderCategory.BIRTHDAY.name -> Icons.Default.Cake
    ReminderCategory.WORK.name -> Icons.Default.Work
    else -> Icons.Default.NotificationsActive
}

/** Rafeeq Vivid — one accent per life-category, same saturation family. */
private fun categoryAccent(category: String): androidx.compose.ui.graphics.Color = when (category) {
    ReminderCategory.MEDICINE.name -> AccentCoral
    ReminderCategory.BILL.name -> AccentAmber
    ReminderCategory.APPOINTMENT.name -> AccentTeal
    ReminderCategory.BIRTHDAY.name -> AccentPink
    ReminderCategory.WORK.name -> AccentViolet
    else -> com.notification.app.ui.theme.Primary // MONEY + default = indigo
}

// ── Hero: greeting + honest one-line summary of the day ────────────────────

@Composable
private fun TodayHero(
    isArabic: Boolean,
    userName: String,
    pendingCount: Int,
    overdueCount: Int,
    doneToday: Int,
    onAskRafeeq: (String) -> Unit
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        isArabic && hour < 12 -> "صباح الخير"
        isArabic -> "مساء الخير"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val displayName = userName.takeIf { it.isNotBlank() && it != "User" && it != "Guest User" }
    val greetingLine = if (displayName != null) "$greeting، $displayName 👋" else "$greeting 👋"

    val dateLine = remember(isArabic) {
        SimpleDateFormat(
            if (isArabic) "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy",
            if (isArabic) Locale("ar") else Locale.ENGLISH
        ).format(Date())
    }

    val summary = when {
        pendingCount == 0 && doneToday > 0 ->
            if (isArabic) "خلّصت كل حاجة النهاردة 🎉" else "Everything done for today 🎉"
        pendingCount == 0 ->
            if (isArabic) "مفيش حاجة عليك النهاردة — يوم هادي ✨" else "Nothing on you today — enjoy ✨"
        overdueCount > 0 ->
            if (isArabic) "عليك $pendingCount ${if (pendingCount == 1) "حاجة" else "حاجات"} — منها $overdueCount متأخرة"
            else "$pendingCount thing${if (pendingCount == 1) "" else "s"} on you — $overdueCount overdue"
        else ->
            if (isArabic) "عليك $pendingCount ${if (pendingCount == 1) "حاجة" else "حاجات"} النهاردة"
            else "$pendingCount thing${if (pendingCount == 1) "" else "s"} on you today"
    }

    // Quick capture — the fastest path into the smart memory: type it and
    // Rafeeq files it (reminder, debt, gam3iya…) through the assistant.
    var quickText by rememberSaveable { mutableStateOf("") }
    fun sendQuick() {
        val t = quickText.trim()
        if (t.isNotEmpty()) { onAskRafeeq(t); quickText = "" }
    }

    PremiumCard(style = PremiumCardStyle.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(greetingLine, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(dateLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                summary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (overdueCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            androidx.compose.material3.OutlinedTextField(
                value = quickText,
                onValueChange = { quickText = it },
                placeholder = {
                    Text(
                        if (isArabic) "قول لرفيق يفتكرلك حاجة…" else "Tell Rafeeq to remember something…",
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { sendQuick() }, enabled = quickText.isNotBlank()) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = if (isArabic) "إرسال" else "Send",
                            tint = if (quickText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { sendQuick() }),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)
            )
            PremiumButton(
                text = if (isArabic) "لخصلي يومي ✨" else "Summarize my day ✨",
                onClick = {
                    onAskRafeeq(if (isArabic) "لخص يومي ونظمه لي" else "Summarize and organize my day")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── «عليك النهاردة» — the timeline itself ──────────────────────────────────

@Composable
private fun TodayTimelineCard(
    isArabic: Boolean,
    items: List<TodayItem>,
    doneToday: Int,
    tomorrowCount: Int,
    onToggleReminderDone: (ReminderEntity) -> Unit
) {
    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "عليك النهاردة" else "On you today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            if (items.isEmpty()) {
                Text(
                    text = if (isArabic) "مفيش حاجة مستحقة — استمتع بيومك ✨"
                    else "Nothing due — enjoy your day ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item -> TodayRow(item, onToggleReminderDone) }
            }
            if (doneToday > 0) {
                Text(
                    text = if (isArabic) "أنجزت $doneToday النهاردة ✓" else "$doneToday done today ✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (tomorrowCount > 0) {
                HorizontalDivider()
                Text(
                    text = if (isArabic) "بكرة عليك $tomorrowCount ${if (tomorrowCount == 1) "حاجة" else "حاجات"}"
                    else "Tomorrow: $tomorrowCount thing${if (tomorrowCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayRow(item: TodayItem, onToggleReminderDone: (ReminderEntity) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Checkable in place when it's a reminder — done means done,
        // without leaving the home screen.
        if (item.reminder != null) {
            IconButton(
                onClick = { onToggleReminderDone(item.reminder) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (item.reminder.isCompleted) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    // Done is ALWAYS green; pending carries its category color.
                    tint = if (item.reminder.isCompleted) MaterialTheme.colorScheme.tertiary
                    else item.accent
                )
            }
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.accent,
                modifier = Modifier.size(AppDimens.iconSizeSmall)
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.reminder?.isCompleted == true) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp)
        )
        TextButton(onClick = item.onOpen, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.xs)) {
            Text(
                text = item.timeLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (item.isOverdue) MaterialTheme.colorScheme.error else item.accent
            )
        }
    }
}

// ── «فلوسك» — three honest lines, no dashboards-inside-dashboards ──────────

@Composable
private fun MoneySnapshotCard(
    isArabic: Boolean,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    financialItems: List<FinancialItemEntity>,
    gam3iyas: List<Gam3iyaEntity>,
    onOpenLedger: () -> Unit,
    onOpenFinancial: () -> Unit,
    onOpenGam3iya: () -> Unit
) {
    val now = System.currentTimeMillis()
    val summaries = remember(persons, transactions) {
        persons.map { p ->
            LedgerCalculator.calculateNetBalance(transactions.filter { it.personId == p.id })
        }
    }
    val owedToMe = summaries.filter { it.status == LedgerStatus.THEY_OWE_ME }.sumOf { it.netAmount }
    val iOwe = summaries.filter { it.status == LedgerStatus.I_OWE_THEM }.sumOf { it.netAmount }
    val nextPayment = remember(financialItems) {
        financialItems.filter { !it.isPaid && !it.isArchived && it.dueDate > now }
            .minByOrNull { it.dueDate }
    }
    val hasAnyMoney = owedToMe > 0 || iOwe > 0 || nextPayment != null || gam3iyas.isNotEmpty()
    if (!hasAnyMoney) return

    val df = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "فلوسك" else "Your money",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            if (owedToMe > 0) {
                MoneyLine(
                    icon = Icons.Default.AccountBalanceWallet,
                    text = if (isArabic) "ليك عند الناس" else "People owe you",
                    value = "${owedToMe.toLong()} ${if (isArabic) "ج.م" else "EGP"}",
                    valueColor = MaterialTheme.colorScheme.tertiary
                )
            }
            if (iOwe > 0) {
                MoneyLine(
                    icon = Icons.Default.AccountBalanceWallet,
                    text = if (isArabic) "عليك للناس" else "You owe people",
                    value = "${iOwe.toLong()} ${if (isArabic) "ج.م" else "EGP"}",
                    valueColor = MaterialTheme.colorScheme.error
                )
            }
            if (nextPayment != null) {
                MoneyLine(
                    icon = Icons.Default.ReceiptLong,
                    text = if (isArabic) "أقرب استحقاق: ${nextPayment.title}"
                    else "Next due: ${nextPayment.title}",
                    value = df.format(Date(nextPayment.dueDate)),
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextButton(onClick = onOpenLedger) { Text(if (isArabic) "الديون" else "Debts") }
                TextButton(onClick = onOpenFinancial) { Text(if (isArabic) "الأقساط والفواتير" else "Bills & installments") }
                if (gam3iyas.isNotEmpty()) {
                    TextButton(onClick = onOpenGam3iya) { Text(if (isArabic) "جمعيتي" else "My gam3iya") }
                }
            }
        }
    }
}

@Composable
private fun MoneyLine(icon: ImageVector, text: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimens.iconSizeSmall))
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// ── Companion widgets: prayer, water, habits, open notes ────────────────────

@Composable
private fun CompanionWidgets(
    isArabic: Boolean,
    prayerTimes: List<PrayerTime>,
    workNotes: List<WorkNoteEntity>,
    waterCount: Int,
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>,
    onWaterClick: () -> Unit,
    onNavigateToIslamic: () -> Unit,
    onNavigateToHealthNotes: () -> Unit,
    onNavigateToHabits: () -> Unit
) {
    val now = System.currentTimeMillis()
    val nextPrayer = remember(prayerTimes) {
        prayerTimes.filter { it.timestamp > now }.minByOrNull { it.timestamp }
    }
    val openNotes = remember(workNotes) { workNotes.count { !it.isDone } }
    val waterGoal = 8

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SmartWidget(
            visible = nextPrayer != null,
            icon = Icons.Default.Mosque,
            title = if (isArabic) "الصلاة القادمة" else "Next prayer",
            primaryLine = nextPrayer?.let { if (isArabic) it.nameAr else it.nameEn } ?: "",
            secondaryLine = nextPrayer?.timeFormatted,
            accent = AccentTeal,
            onClick = onNavigateToIslamic
        )

        SmartWidget(
            visible = waterCount < waterGoal,
            icon = Icons.Default.WaterDrop,
            title = if (isArabic) "شرب المياه" else "Water",
            primaryLine = "$waterCount / $waterGoal",
            secondaryLine = if (isArabic) "اضغط لتسجيل كوب 💧" else "Tap to log a glass 💧",
            accent = AccentSky,
            onClick = onWaterClick
        )

        val today = remember { HabitCalculator.dayStartOf() }
        val doneToday = remember(habitLogs) { habitLogs.count { it.dayStart == today } }
        SmartWidget(
            visible = habits.isNotEmpty(),
            icon = Icons.Default.Repeat,
            title = if (isArabic) "عاداتك اليوم" else "Today's habits",
            primaryLine = if (isArabic) "أنجزت $doneToday من ${habits.size}"
            else "$doneToday of ${habits.size} completed",
            secondaryLine = when {
                doneToday >= habits.size ->
                    if (isArabic) "يوم مكتمل — واصل السلسلة 🔥" else "Perfect day — keep the streak 🔥"
                else -> if (isArabic) "اضغط لتسجيل إنجازك" else "Tap to check in"
            },
            accent = AccentAmber,
            onClick = onNavigateToHabits
        )

        SmartWidget(
            visible = openNotes > 0,
            icon = Icons.Default.CheckCircle,
            title = if (isArabic) "ملاحظات الشغل" else "Work notes",
            primaryLine = if (isArabic) "$openNotes قيد التنفيذ" else "$openNotes open",
            accent = AccentViolet,
            onClick = onNavigateToHealthNotes
        )
    }
}

// ── اقتراحات رفيق (الذكاء) — unchanged pipeline, companion tone ────────────

@Composable
private fun RafeeqSuggestionsSection(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    financialItems: List<FinancialItemEntity>,
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
                        text = if (isArabic) "رفيق شايف إن…" else "Rafeeq noticed…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (aiLoading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
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
                    SkeletonLine(widthFraction = 0.9f)
                    SkeletonLine(widthFraction = 0.7f)
                    SkeletonLine(widthFraction = 0.8f)
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
        transactions = transactions,
        financialItems = financialItems
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
        AiSuggestionAction.OPEN_LEDGER -> if (isArabic) "افتح الديون" else "Open debts"
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.sm, vertical = 0.dp)
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
 * Offline fallback: the same honest, rule-based lines computed from the
 * real repositories — hides itself entirely when there is nothing to say.
 */
@Composable
private fun LocalRuleSuggestions(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    financialItems: List<FinancialItemEntity> = emptyList()
) {
    val now = System.currentTimeMillis()
    val weekAhead = now + 7L * 24 * 60 * 60 * 1000

    val suggestions = remember(reminders, persons, transactions, financialItems, isArabic) {
        buildList {
            val overdue = reminders.count { !it.isCompleted && it.dueDate < now }
            if (overdue > 0) add(
                if (isArabic) "عندك $overdue ${if (overdue == 1) "حاجة متأخرة" else "حاجات متأخرة"} — راجعها"
                else "You have $overdue overdue item${if (overdue == 1) "" else "s"} — review them"
            )

            val billsThisWeek = reminders.count {
                !it.isCompleted && it.category == ReminderCategory.BILL.name &&
                    it.dueDate in now..weekAhead
            }
            if (billsThisWeek > 0) add(
                if (isArabic) "عندك $billsThisWeek ${if (billsThisWeek == 1) "فاتورة" else "فواتير"} الأسبوع ده — جهّز حسابك"
                else "$billsThisWeek bill${if (billsThisWeek == 1) "" else "s"} due this week — plan ahead"
            )

            val iOweCount = persons.count { p ->
                LedgerCalculator.calculateNetBalance(
                    transactions.filter { it.personId == p.id }
                ).status == LedgerStatus.I_OWE_THEM
            }
            if (iOweCount > 0) add(
                if (isArabic) "فيه ${if (iOweCount == 1) "شخص مستني" else "$iOweCount أشخاص مستنيين"} فلوس منك — شوف الديون"
                else "You owe $iOweCount ${if (iOweCount == 1) "person" else "people"} — check your debts"
            )

            val medicineToday = reminders.count {
                !it.isCompleted && it.category == ReminderCategory.MEDICINE.name &&
                    it.dueDate in now..(now + 24L * 60 * 60 * 1000)
            }
            if (medicineToday > 0) add(
                if (isArabic) "متنساش دواك — $medicineToday ${if (medicineToday == 1) "جرعة" else "جرعات"} خلال ٢٤ ساعة"
                else "Don't forget your medicine — $medicineToday dose${if (medicineToday == 1) "" else "s"} in the next 24h"
            )

            val paymentsDueSoon = financialItems.count { !it.isPaid && it.dueDate in now..weekAhead }
            if (paymentsDueSoon > 0) add(
                if (isArabic) "عندك $paymentsDueSoon ${if (paymentsDueSoon == 1) "التزام مالي" else "التزامات مالية"} خلال أسبوع — جهّز المبلغ"
                else "$paymentsDueSoon payment${if (paymentsDueSoon == 1) "" else "s"} due within a week — plan ahead"
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
                    text = if (isArabic) "رفيق شايف إن…" else "Rafeeq noticed…",
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
