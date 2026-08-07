package com.notification.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notification.app.data.local.entities.HabitEntity
import com.notification.app.data.local.entities.HabitLogEntity
import com.notification.app.domain.calculator.HabitCalculator
import com.notification.app.ui.components.EmptyState
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Final Product sprint (Phase C) — the professional habit engine UI.
 * Everything shown here (streaks, calendar, percentages) is DERIVED from
 * the log rows via HabitCalculator — no stored counters to drift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    isArabic: Boolean,
    habits: List<HabitEntity>,
    logs: List<HabitLogEntity>,
    onBack: () -> Unit,
    onAddHabit: (HabitEntity) -> Unit,
    onDeleteHabit: (HabitEntity) -> Unit,
    onArchiveHabit: (HabitEntity) -> Unit,
    onToggleToday: (habitId: Long, dayStart: Long, done: Boolean) -> Unit
) {
    val today = remember { HabitCalculator.dayStartOf() }
    // habitId -> set of completed dayStarts.
    val daysByHabit = remember(logs) {
        logs.groupBy({ it.habitId }, { it.dayStart }).mapValues { it.value.toSet() }
    }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var expandedHabitId by rememberSaveable { mutableStateOf(-1L) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val visibleHabits = remember(habits, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) habits
        else habits.filter {
            it.title.contains(query, ignoreCase = true) || it.note.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "عاداتي" else "My Habits",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isArabic) "عادة جديدة" else "New habit")
            }
        }
    ) { innerPadding ->
        if (habits.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                EmptyState(
                    icon = Icons.Default.SelfImprovement,
                    title = if (isArabic) "ابدأ عادة جديدة" else "Start a new habit",
                    subtitle = if (isArabic) "تابع عاداتك اليومية وسلاسل إنجازك" else "Track your daily habits and streaks",
                    actionLabel = if (isArabic) "إضافة عادة" else "Add habit",
                    onAction = { showAddDialog = true }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = AppPadding.screen)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isArabic) "ابحث في عاداتك…" else "Search your habits…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm)
                )
                if (visibleHabits.isEmpty()) {
                    Text(
                        text = if (isArabic) "لا توجد عادة بهذا الاسم" else "No habit matches your search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.sm)
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(top = Spacing.sm, bottom = 96.dp)
                ) {
                items(visibleHabits, key = { it.id }) { habit ->
                    val days = daysByHabit[habit.id] ?: emptySet()
                    HabitCard(
                        isArabic = isArabic,
                        habit = habit,
                        days = days,
                        today = today,
                        expanded = expandedHabitId == habit.id,
                        onExpandToggle = {
                            expandedHabitId = if (expandedHabitId == habit.id) -1L else habit.id
                        },
                        onToggleToday = { done -> onToggleToday(habit.id, today, done) },
                        onArchive = { onArchiveHabit(habit.copy(isArchived = true)) },
                        onDelete = { onDeleteHabit(habit) }
                    )
                }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            isArabic = isArabic,
            onDismiss = { showAddDialog = false },
            onConfirm = { habit ->
                onAddHabit(habit)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HabitCard(
    isArabic: Boolean,
    habit: HabitEntity,
    days: Set<Long>,
    today: Long,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onToggleToday: (Boolean) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val doneToday = today in days
    val currentStreak = remember(days, today) { HabitCalculator.currentStreak(days, today) }
    val longestStreak = remember(days) { HabitCalculator.longestStreak(days) }
    val percent30 = remember(days, today) { HabitCalculator.completionPercent(days, 30, today) }

    PremiumCard(onClick = onExpandToggle, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = habit.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic)
                            "🔥 $currentStreak ${if (currentStreak == 1) "يوم" else "أيام"} متتالية"
                        else
                            "🔥 $currentStreak day${if (currentStreak == 1) "" else "s"} streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentStreak > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Today's completion toggle — the heart of the engine.
                IconButton(onClick = { onToggleToday(!doneToday) }) {
                    Icon(
                        imageVector = if (doneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isArabic) "إنجاز اليوم" else "Done today",
                        tint = if (doneToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(Spacing.sm))
                    MonthCalendar(days = days, today = today)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HabitStat(
                            value = "$longestStreak",
                            label = if (isArabic) "أطول سلسلة" else "Best streak"
                        )
                        HabitStat(
                            value = "$percent30%",
                            label = if (isArabic) "آخر ٣٠ يومًا" else "Last 30 days"
                        )
                        HabitStat(
                            value = "${days.size}",
                            label = if (isArabic) "إجمالي الإنجاز" else "Total done"
                        )
                    }
                    if (habit.note.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = habit.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onArchive) {
                            Text(if (isArabic) "أرشفة" else "Archive")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (isArabic) "حذف" else "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Mini month calendar: one dot per day of the current month — gold when
 * completed, outlined for today, muted otherwise. Rows of 7.
 */
@Composable
private fun MonthCalendar(days: Set<Long>, today: Long) {
    val monthDays = remember(today) { HabitCalculator.daysOfMonth(today) }
    val monthLabel = remember(today) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(today))
    }
    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

    Column {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.xs))
        monthDays.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val done = day in days
                    val isToday = day == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(30.dp)
                            .background(
                                color = if (done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = CircleShape
                            )
                            .then(
                                if (isToday) Modifier.border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayFormat.format(Date(day)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (done) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // Pad the last row so cells keep the same width.
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f).padding(2.dp))
                }
            }
        }
    }
}

private val HABIT_EMOJIS = listOf("✅", "💧", "🏃", "📖", "🕌", "🧘", "💪", "😴", "🥗", "✍️")

@Composable
private fun AddHabitDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (HabitEntity) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(HABIT_EMOJIS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "عادة جديدة" else "New habit") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isArabic) "اسم العادة" else "Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HABIT_EMOJIS.take(5).forEach { e ->
                        EmojiChoice(e, selected = emoji == e) { emoji = e }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HABIT_EMOJIS.drop(5).forEach { e ->
                        EmojiChoice(e, selected = emoji == e) { emoji = e }
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isArabic) "ملاحظة (اختياري)" else "Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(HabitEntity(title = title.trim(), emoji = emoji, note = note.trim()))
                }
            ) { Text(if (isArabic) "إضافة" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}

@Composable
private fun EmojiChoice(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape
            )
            .then(
                if (selected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
    }
}
