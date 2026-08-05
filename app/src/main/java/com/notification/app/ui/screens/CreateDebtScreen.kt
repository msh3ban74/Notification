package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumButton
import com.notification.app.ui.designsystem.PremiumOutlinedButton
import com.notification.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

/**
 * Sprint 4 — Smart Debt.
 *
 * Replaces the "Coming Soon" placeholder for the "Debt" type picked from
 * the Dashboard's "+" bottom sheet.
 *
 * This screen is UI ONLY and reuses the EXISTING Ledger implementation:
 * no new entities and no duplicated debt logic. Saving hands the values
 * to [onSave] (MainViewModel.addDebt), which composes the existing
 * repository methods — insertPerson / insertLedgerTransaction — and, when
 * a due date is set, the existing reminder pipeline via linkedReminderId.
 *
 *  - Person          → picks an EXISTING PersonEntity or types a new name.
 *  - Borrowed / Lent → maps to the EXISTING LedgerTransactionType
 *                      (THEY_GAVE_ME / GAVE_THEM).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDebtScreen(
    persons: List<PersonEntity>,
    isArabic: Boolean,
    onSave: (
        existingPersonId: Long?,
        personName: String,
        amount: Double,
        isLent: Boolean,
        dueDate: Long?,
        note: String
    ) -> Unit,
    onCancel: () -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var selectedPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var personDropdownExpanded by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("") }
    var isLent by remember { mutableStateOf(true) } // true = I lent them money
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val canSave = personName.isNotBlank() && amount > 0

    val filteredPersons = remember(personName, persons) {
        if (personName.isBlank()) persons
        else persons.filter { it.name.contains(personName, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "دين جديد" else "New Debt",
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
            // Person — an existing ledger contact, or a new name typed in.
            ExposedDropdownMenuBox(
                expanded = personDropdownExpanded && filteredPersons.isNotEmpty(),
                onExpandedChange = { personDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        selectedPerson = null
                        personDropdownExpanded = true
                    },
                    label = { Text(if (isArabic) "الشخص" else "Person") },
                    supportingText = {
                        Text(
                            text = if (selectedPerson != null) {
                                if (isArabic) "شخص موجود في دفتر الديون" else "Existing ledger contact"
                            } else {
                                if (isArabic) "اكتب اسمًا جديدًا أو اختر من القائمة" else "Type a new name or pick from the list"
                            }
                        )
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = personDropdownExpanded)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = personDropdownExpanded && filteredPersons.isNotEmpty(),
                    onDismissRequest = { personDropdownExpanded = false }
                ) {
                    filteredPersons.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person.name) },
                            onClick = {
                                selectedPerson = person
                                personName = person.name
                                personDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(if (isArabic) "المبلغ (ج.م)" else "Amount (EGP)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Borrowed / Lent — maps onto the EXISTING LedgerTransactionType.
            Text(
                text = if (isArabic) "نوع الدين" else "Debt direction",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isLent,
                    onClick = { isLent = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(if (isArabic) "أقرضتهم (لهم)" else "Lent")
                }
                SegmentedButton(
                    selected = !isLent,
                    onClick = { isLent = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(if (isArabic) "اقترضت (لي)" else "Borrowed")
                }
            }

            PickerField(
                value = dueDateMillis?.let { dateFormat.format(Date(it)) }
                    ?: if (isArabic) "بدون تاريخ استحقاق" else "No due date",
                label = if (isArabic) "تاريخ الاستحقاق (اختياري)" else "Due date (optional)",
                icon = Icons.Default.CalendarMonth,
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (isArabic) "ملاحظات" else "Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            PremiumButton(
                text = if (isArabic) "حفظ" else "Save",
                enabled = canSave,
                onClick = {
                    onSave(
                        selectedPerson?.id,
                        personName.trim(),
                        amount,
                        isLent,
                        dueDateMillis,
                        note.trim()
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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // The Material 3 date picker returns midnight UTC —
                        // normalize to 09:00 local so the linked reminder
                        // (existing pipeline) fires at a sensible hour.
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            dueDateMillis = Calendar.getInstance().apply {
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
                TextButton(
                    onClick = {
                        dueDateMillis = null
                        showDatePicker = false
                    }
                ) { Text(if (isArabic) "مسح التاريخ" else "Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
