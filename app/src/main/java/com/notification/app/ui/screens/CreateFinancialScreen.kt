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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.domain.model.FinancialType
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
 * Final Product sprint (Phase B) — the Bill / Installment / Subscription
 * form. One screen adapts its fields to [type]; saving builds a
 * [FinancialItemEntity] and hands it to [onSave] (MainViewModel.add/
 * updateFinancialItem). Edit mode pre-fills from [initial].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFinancialScreen(
    isArabic: Boolean,
    type: FinancialType,
    initial: FinancialItemEntity? = null,
    onSave: (FinancialItemEntity) -> Unit,
    onCancel: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var totalPrice by remember { mutableStateOf(initial?.totalPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var downPayment by remember { mutableStateOf(initial?.downPayment?.takeIf { it > 0 }?.toString() ?: "") }
    var monthlyAmount by remember { mutableStateOf(initial?.monthlyAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var accountNumber by remember { mutableStateOf(initial?.accountNumber ?: "") }
    var billNumber by remember { mutableStateOf(initial?.billNumber ?: "") }
    var seller by remember { mutableStateOf(initial?.seller ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var recurring by remember { mutableStateOf(initial?.recurring ?: (type != FinancialType.INSTALLMENT)) }

    val defaultDue = remember {
        Calendar.getInstance().apply {
            if (initial != null && initial.dueDate > 0) timeInMillis = initial.dueDate
            else add(Calendar.MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }
    var dueMillis by remember { mutableLongStateOf(defaultDue.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val titleLabel = when (type) {
        FinancialType.BILL -> if (isArabic) "الجهة / الشركة" else "Company"
        FinancialType.INSTALLMENT -> if (isArabic) "اسم المنتج" else "Item name"
        FinancialType.SUBSCRIPTION -> if (isArabic) "اسم الخدمة" else "Service name"
    }
    val screenTitle = when (type) {
        FinancialType.BILL -> if (isArabic) "فاتورة جديدة" else "New Bill"
        FinancialType.INSTALLMENT -> if (isArabic) "قسط جديد" else "New Installment"
        FinancialType.SUBSCRIPTION -> if (isArabic) "اشتراك جديد" else "New Subscription"
    }
    val dueLabel = if (type == FinancialType.SUBSCRIPTION) {
        if (isArabic) "تاريخ التجديد" else "Renewal date"
    } else {
        if (isArabic) "تاريخ الاستحقاق" else "Due date"
    }

    val canSave = title.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isArabic) "رجوع" else "Back")
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
                label = { Text(titleLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (type == FinancialType.INSTALLMENT) {
                OutlinedTextField(
                    value = seller,
                    onValueChange = { seller = it },
                    label = { Text(if (isArabic) "البائع / المتجر" else "Seller / Store") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                MoneyField(totalPrice, { totalPrice = it }, if (isArabic) "السعر الكلي (ج.م)" else "Total price (EGP)")
                MoneyField(downPayment, { downPayment = it }, if (isArabic) "المقدم (ج.م)" else "Down payment (EGP)")
                MoneyField(monthlyAmount, { monthlyAmount = it }, if (isArabic) "القسط الشهري (ج.م)" else "Monthly amount (EGP)")
            } else {
                MoneyField(
                    amount, { amount = it },
                    if (type == FinancialType.SUBSCRIPTION) {
                        if (isArabic) "قيمة الاشتراك (ج.م)" else "Subscription amount (EGP)"
                    } else {
                        if (isArabic) "قيمة الفاتورة (ج.م)" else "Bill amount (EGP)"
                    }
                )
            }

            if (type == FinancialType.BILL) {
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text(if (isArabic) "رقم الحساب (اختياري)" else "Account number (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text(if (isArabic) "رقم الفاتورة (اختياري)" else "Bill number (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PickerField(
                value = dateFormat.format(Date(dueMillis)),
                label = dueLabel,
                icon = Icons.Default.CalendarMonth,
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (isArabic) "ملاحظات (اختياري)" else "Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (type != FinancialType.INSTALLMENT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isArabic) "متكرر شهريًا" else "Recurring monthly",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = recurring, onCheckedChange = { recurring = it })
                }
            }

            PremiumButton(
                text = if (isArabic) "حفظ" else "Save",
                enabled = canSave,
                onClick = {
                    val total = totalPrice.toDoubleOrNull() ?: 0.0
                    val down = downPayment.toDoubleOrNull() ?: 0.0
                    onSave(
                        (initial ?: FinancialItemEntity(type = type.name, title = "")).copy(
                            type = type.name,
                            title = title.trim(),
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            totalPrice = total,
                            downPayment = down,
                            monthlyAmount = monthlyAmount.toDoubleOrNull() ?: 0.0,
                            remaining = (total - down).coerceAtLeast(0.0),
                            dueDate = dueMillis,
                            accountNumber = accountNumber.trim(),
                            billNumber = billNumber.trim(),
                            seller = seller.trim(),
                            note = note.trim(),
                            recurring = recurring
                        )
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
        val state = rememberDatePickerState(initialSelectedDateMillis = dueMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
                        dueMillis = Calendar.getInstance().apply {
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    showDatePicker = false
                }) { Text(if (isArabic) "تم" else "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun MoneyField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
