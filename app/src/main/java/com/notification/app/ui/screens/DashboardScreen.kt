package com.notification.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.ui.components.SmartWidget
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumButton
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.designsystem.PremiumCardStyle
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
                        timeLabel = if (isArabic) "اليوم" else "Today",
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

        // «رفيق شايف إن…» — LOCAL pattern insights only: computed from the
        // user's own data at zero API cost (the Gemini-powered suggestions
        // were removed on purpose — they drained the chat's free quota).
        RafeeqNotices(
            isArabic = isArabic,
            reminders = reminders,
            persons = persons,
            transactions = transactions,
            financialItems = financialItems,
            habits = habits,
            habitLogs = habitLogs
        )
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
    val greetingLine = if (displayName != null) "$greeting، $displayName" else greeting

    val dateLine = remember(isArabic) {
        SimpleDateFormat(
            if (isArabic) "EEEE، d MMMM yyyy" else "EEEE, d MMMM yyyy",
            if (isArabic) Locale("ar") else Locale.ENGLISH
        ).format(Date())
    }

    val summary = when {
        pendingCount == 0 && doneToday > 0 ->
            if (isArabic) "اكتمل كل شيء لليوم" else "All done for today"
        pendingCount == 0 ->
            if (isArabic) "لا توجد مهام اليوم" else "Nothing scheduled today"
        overdueCount > 0 ->
            if (isArabic) "لديك $pendingCount ${if (pendingCount == 1) "مهمة" else "مهام"} — منها $overdueCount متأخرة"
            else "$pendingCount task${if (pendingCount == 1) "" else "s"} — $overdueCount overdue"
        else ->
            if (isArabic) "لديك $pendingCount ${if (pendingCount == 1) "مهمة" else "مهام"} اليوم"
            else "$pendingCount task${if (pendingCount == 1) "" else "s"} today"
    }

    // Quick capture — the fastest path into the smart memory: type it and
    // Rafeeq files it (reminder, debt, gam3iya…) through the assistant.
    var quickText by rememberSaveable { mutableStateOf("") }
    fun sendQuick() {
        val t = quickText.trim()
        if (t.isNotEmpty()) { onAskRafeeq(t); quickText = "" }
    }

    // Hero — the billion-dollar first impression: an electric indigo →
    // violet gradient card with white type, a frosted quick-capture pill
    // and a white CTA. Everything else on the page stays calm white cards,
    // so this ONE hero carries the identity.
    androidx.compose.material3.Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            com.notification.app.ui.theme.PrimaryDark,
                            com.notification.app.ui.theme.Primary,
                            com.notification.app.ui.theme.AccentViolet
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Text(
                greetingLine,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White
            )
            Text(
                dateLine,
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
            )
            Text(
                summary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (overdueCount > 0) androidx.compose.ui.graphics.Color(0xFFFFD5DE)
                else androidx.compose.ui.graphics.Color.White
            )
            androidx.compose.material3.OutlinedTextField(
                value = quickText,
                onValueChange = { quickText = it },
                placeholder = {
                    Text(
                        if (isArabic) "أضف تذكيرًا أو مهمة…" else "Add a reminder or task…",
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { sendQuick() }, enabled = quickText.isNotBlank()) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = if (isArabic) "إرسال" else "Send",
                            tint = androidx.compose.ui.graphics.Color.White.copy(
                                alpha = if (quickText.isNotBlank()) 1f else 0.5f
                            )
                        )
                    }
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                    cursorColor = androidx.compose.ui.graphics.Color.White,
                    focusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f),
                    focusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f),
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { sendQuick() }),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs)
            )
            androidx.compose.material3.Button(
                onClick = {
                    onAskRafeeq(if (isArabic) "لخص يومي ونظمه لي" else "Summarize and organize my day")
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = com.notification.app.ui.theme.PrimaryDark
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isArabic) "ملخص اليوم" else "Today's summary",
                    fontWeight = FontWeight.Bold
                )
            }
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
                text = if (isArabic) "مهام اليوم" else "Today's tasks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            if (items.isEmpty()) {
                Text(
                    text = if (isArabic) "لا توجد مهام مستحقة اليوم"
                    else "Nothing due today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item -> TodayRow(item, onToggleReminderDone) }
            }
            if (doneToday > 0) {
                Text(
                    text = if (isArabic) "اكتمل اليوم: $doneToday" else "Completed today: $doneToday",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (tomorrowCount > 0) {
                HorizontalDivider()
                Text(
                    text = if (isArabic) "غدًا: $tomorrowCount ${if (tomorrowCount == 1) "مهمة" else "مهام"}"
                    else "Tomorrow: $tomorrowCount task${if (tomorrowCount == 1) "" else "s"}",
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
                text = if (isArabic) "أموالك" else "Your money",
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
            // RTL-safe: plain "0 / 8" flips visually in Arabic, so spell it out.
            primaryLine = if (isArabic) "$waterCount من $waterGoal أكواب" else "$waterCount of $waterGoal glasses",
            secondaryLine = if (isArabic) "اضغط لتسجيل كوب" else "Tap to log a glass",
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
                    if (isArabic) "اكتمل هدف اليوم" else "Daily goal complete"
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

// ── «رفيق شايف إن…» — local pattern insights, zero API cost ────────────────

/**
 * رفيق بيلاحظ أنماطك من بياناتك المحلية فقط: عادة وقفت سلسلتها، وعد دين
 * عدّى معاده، قسط فات، دوا النهاردة… محسوبة كلها على الجهاز — مفيش أي
 * استهلاك من حصة الذكاء الاصطناعي، والكارت بيختفي لما مفيش حاجة تتقال.
 */
@Composable
private fun RafeeqNotices(
    isArabic: Boolean,
    reminders: List<ReminderEntity>,
    persons: List<PersonEntity>,
    transactions: List<LedgerTransactionEntity>,
    financialItems: List<FinancialItemEntity>,
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>
) {
    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000
    val weekAhead = now + 7 * dayMs

    val notices = remember(reminders, persons, transactions, financialItems, habits, habitLogs, isArabic) {
        buildList {
            // نمط: وعد دين عدّى معاده — بالاسم وعدد الأيام.
            val personNames = persons.associateBy({ it.id }, { it.name })
            transactions
                .filter { it.dueDate in 1 until now }
                .maxByOrNull { it.dueDate }
                ?.let { tx ->
                    val name = personNames[tx.personId] ?: return@let
                    val days = ((now - tx.dueDate) / dayMs).toInt().coerceAtLeast(1)
                    add(
                        if (isArabic) "الدين مع $name تجاوز موعده بـ$days ${if (days == 1) "يوم" else "أيام"}"
                        else "The debt with $name is $days day${if (days == 1) "" else "s"} past due"
                    )
                }

            // نمط: عادة كانت لها سلسلة قوية ووقفت. (DST-safe: كل يوم
            // بيتحسب بـ dayStartOf بدل طرح ٢٤ ساعة صريحة.)
            val today = HabitCalculator.dayStartOf(now)
            val yesterday = HabitCalculator.dayStartOf(now - dayMs)
            val twoDaysAgo = HabitCalculator.dayStartOf(now - 2 * dayMs)
            val daysByHabit = habitLogs.groupBy({ it.habitId }, { it.dayStart }).mapValues { it.value.toSet() }
            habits.firstOrNull { habit ->
                val days = daysByHabit[habit.id] ?: emptySet()
                today !in days && yesterday !in days &&
                    HabitCalculator.currentStreak(days, twoDaysAgo) >= 3
            }?.let { habit ->
                add(
                    if (isArabic) "عادة \"${habit.title}\" متوقفة منذ يومين"
                    else "\"${habit.title}\" has been paused for 2 days"
                )
            }

            // نمط: قسط/فاتورة فاتت من غير ما تتعلم "مدفوع".
            financialItems
                .filter { !it.isPaid && !it.isArchived && it.dueDate in 1 until now }
                .maxByOrNull { it.dueDate }
                ?.let { f ->
                    val days = ((now - f.dueDate) / dayMs).toInt().coerceAtLeast(1)
                    add(
                        if (isArabic) "\"${f.title}\" تجاوزت موعد استحقاقها بـ$days ${if (days == 1) "يوم" else "أيام"}"
                        else "\"${f.title}\" is $days day${if (days == 1) "" else "s"} past due"
                    )
                }

            val medicineToday = reminders.count {
                !it.isCompleted && it.category == ReminderCategory.MEDICINE.name &&
                    it.dueDate in now..(now + dayMs)
            }
            if (medicineToday > 0) add(
                if (isArabic) "لديك $medicineToday ${if (medicineToday == 1) "جرعة دواء" else "جرعات دواء"} خلال ٢٤ ساعة"
                else "$medicineToday medicine dose${if (medicineToday == 1) "" else "s"} in the next 24 hours"
            )

            val billsThisWeek = reminders.count {
                !it.isCompleted && it.category == ReminderCategory.BILL.name &&
                    it.dueDate in now..weekAhead
            }
            if (billsThisWeek > 0) add(
                if (isArabic) "لديك $billsThisWeek ${if (billsThisWeek == 1) "فاتورة" else "فواتير"} خلال هذا الأسبوع"
                else "$billsThisWeek bill${if (billsThisWeek == 1) "" else "s"} due this week"
            )

            val iOweCount = persons.count { p ->
                LedgerCalculator.calculateNetBalance(
                    transactions.filter { it.personId == p.id }
                ).status == LedgerStatus.I_OWE_THEM
            }
            if (iOweCount > 0) add(
                if (isArabic) "لديك مبالغ مستحقة لـ${if (iOweCount == 1) "شخص واحد" else "$iOweCount أشخاص"} في الديون"
                else "You have amounts due to $iOweCount ${if (iOweCount == 1) "person" else "people"} in Debts"
            )
        }.take(4)
    }

    if (notices.isEmpty()) return

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
                    text = if (isArabic) "تنبيهات ذكية" else "Smart insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider()
            notices.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
