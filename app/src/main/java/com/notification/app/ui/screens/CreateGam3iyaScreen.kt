package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumButton
import com.notification.app.ui.designsystem.PremiumOutlinedButton
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Sprint 5 — Smart Gam3iya.
 *
 * Replaces the "Coming Soon" placeholder for the "Gam3iya" type picked
 * from the Dashboard's "+" bottom sheet.
 *
 * UI ONLY — the exact same fields as the existing CreateGam3iyaDialog on
 * the Gam3iya screen (title, total pool, monthly installment, members),
 * plus an explicit start date. Saving hands the values to [onSave]
 * (MainViewModel.createGam3iya), which is the EXISTING creation pipeline
 * (Gam3iyaEntity + members with payout dates via Gam3iyaCalculator).
 * No new entities, no duplicated Gam3iya logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGam3iyaScreen(
    isArabic: Boolean,
    onSave: (
        title: String,
        total: Double,
        installment: Double,
        members: List<Pair<String, Int>>,
        startDate: Long
    ) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var installmentText by remember { mutableStateOf("") }
    var memberNamesInput by remember { mutableStateOf("") } // Comma separated

    // Default start date: today (same as the existing dialog, which uses
    // System.currentTimeMillis() at confirm time) — but visible/editable.
    var startDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    val total = totalText.toDoubleOrNull() ?: 0.0
    val installment = installmentText.toDoubleOrNull() ?: 0.0
    val memberNames = memberNamesInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val canSave = title.isNotBlank() && total > 0 && memberNames.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "جمعية جديدة" else "New Gam3iya",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppPadding.screen)
                .padding(top = Spacing.sm, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (isArabic) "اسم الجمعية" else "Gam3iya Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = totalText,
                onValueChange = { totalText = it },
                label = { Text(if (isArabic) "المبلغ الكلي للقبض (ج.م)" else "Total Payout Pool (EGP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = installmentText,
                onValueChange = { installmentText = it },
                label = { Text(if (isArabic) "القسط الشهري للشخص" else "Monthly Installment (EGP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = memberNamesInput,
                onValueChange = { memberNamesInput = it },
                label = { Text(if (isArabic) "أسماء المشاركين (مفصولة بفاصلة)" else "Member Names (Comma separated)") },
                supportingText = {
                    Text(
                        text = if (memberNames.isEmpty()) {
                            if (isArabic) "ترتيب الأسماء يحدد ترتيب القبض" else "Name order = payout turn order"
                        } else {
                            if (isArabic) "${memberNames.size} مشارك" else "${memberNames.size} member(s)"
                        }
                    )
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            PickerField(
                value = dateFormat.format(Date(startDateMillis)),
                label = if (isArabic) "تاريخ البداية" else "Start date",
                icon = Icons.Default.CalendarMonth,
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            // Live turn preview: makes the "order = turn" rule tangible.
            if (memberNames.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                    Text(
                        text = memberNames.mapIndexed { idx, name ->
                            "${idx + 1}. $name"
                        }.joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PremiumButton(
                text = if (isArabic) "إنشاء" else "Create",
                enabled = canSave,
                onClick = {
                    onSave(
                        title.trim(),
                        total,
                        installment,
                        memberNames.mapIndexed { idx, name -> Pair(name, idx + 1) },
                        startDateMillis
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            PremiumOutlinedButton(
                text = if (isArabic) "إلغاء" else "Cancel",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            startDateMillis = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) { Text(if (isArabic) "تم" else "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
