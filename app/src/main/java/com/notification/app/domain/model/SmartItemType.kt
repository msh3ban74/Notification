package com.notification.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Smart Item Engine — the picker metadata (id / bilingual title / icon)
 * for the "+" sheet. Each id routes to its OWN dedicated create form in
 * MainActivity: debt → the debt form, installment → the installment form
 * with down payment and live remaining, event → the occasions form, and
 * so on. No generic catch-all types — every entry earns its place.
 */
data class SmartItemType(
    val id: String,
    val titleEn: String, val titleAr: String,
    val subtitleEn: String, val subtitleAr: String,
    val icon: ImageVector
)

/** One titled group in the "+" sheet. */
data class SmartItemSection(
    val titleEn: String,
    val titleAr: String,
    val items: List<SmartItemType>
)

object SmartItemCatalog {
    val sections: List<SmartItemSection> = listOf(
        SmartItemSection(
            titleEn = "Money", titleAr = "التزاماتك المالية",
            items = listOf(
                SmartItemType(
                    id = "debt",
                    titleEn = "Debt", titleAr = "دين",
                    subtitleEn = "For you or on you", subtitleAr = "لك أو عليك",
                    icon = Icons.Default.AccountBalanceWallet
                ),
                SmartItemType(
                    id = "installment",
                    titleEn = "Installment", titleAr = "قسط",
                    subtitleEn = "Down payment & remaining", subtitleAr = "مقدّم ومتبقّي",
                    icon = Icons.Default.CreditCard
                ),
                SmartItemType(
                    id = "bill",
                    titleEn = "Bill", titleAr = "فاتورة",
                    subtitleEn = "Payments due", subtitleAr = "مدفوعات مستحقة",
                    icon = Icons.Default.Receipt
                ),
                SmartItemType(
                    id = "subscription",
                    titleEn = "Subscription", titleAr = "اشتراك",
                    subtitleEn = "Monthly renewals", subtitleAr = "تجديد شهري",
                    icon = Icons.Default.Subscriptions
                ),
                SmartItemType(
                    id = "gam3iya",
                    titleEn = "Gam3iya", titleAr = "جمعية",
                    subtitleEn = "Your circle & turn", subtitleAr = "قسطك ودورك",
                    icon = Icons.Default.Group
                )
            )
        ),
        SmartItemSection(
            titleEn = "Your day", titleAr = "يومك",
            items = listOf(
                SmartItemType(
                    id = "task",
                    titleEn = "Task / Appointment", titleAr = "مهمة أو موعد",
                    subtitleEn = "With a timely alert", subtitleAr = "بتنبيه في موعدها",
                    icon = Icons.Default.CheckCircle
                ),
                SmartItemType(
                    id = "medicine",
                    titleEn = "Medicine", titleAr = "دواء",
                    subtitleEn = "Doses on time", subtitleAr = "جرعات في مواعيدها",
                    icon = Icons.Default.Medication
                ),
                SmartItemType(
                    id = "alarm",
                    titleEn = "Alarm", titleAr = "منبه",
                    subtitleEn = "Rings exactly on time", subtitleAr = "يرن في الموعد بالضبط",
                    icon = Icons.Default.Alarm
                ),
                SmartItemType(
                    id = "habit",
                    titleEn = "Habit", titleAr = "عادة",
                    subtitleEn = "Daily routines", subtitleAr = "روتين يومي",
                    icon = Icons.Default.Repeat
                )
            )
        ),
        SmartItemSection(
            titleEn = "Occasions", titleAr = "مناسباتك",
            items = listOf(
                SmartItemType(
                    id = "event",
                    titleEn = "Occasion", titleAr = "مناسبة",
                    subtitleEn = "Weddings, birthdays…", subtitleAr = "أفراح، أعياد ميلاد…",
                    icon = Icons.Default.Celebration
                )
            )
        )
    )
}
