package com.notification.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sprint 2 — Smart Item Engine Foundation.
 *
 * Pure UI/navigation data — NOT wired to Room, Firestore, or any
 * ViewModel yet. Selecting a type only routes to a placeholder screen.
 * Real forms and persistence land in Sprint 3.
 */
data class SmartItemType(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val icon: ImageVector
) {
    companion object {
        val all: List<SmartItemType> = listOf(
            SmartItemType(
                id = "task",
                titleEn = "Task", titleAr = "مهمة",
                subtitleEn = "To-dos & checklists", subtitleAr = "مهام وقوائم",
                icon = Icons.Default.CheckCircle
            ),
            SmartItemType(
                id = "appointment",
                titleEn = "Appointment", titleAr = "موعد",
                subtitleEn = "Meetings & visits", subtitleAr = "اجتماعات وزيارات",
                icon = Icons.Default.CalendarMonth
            ),
            SmartItemType(
                id = "debt",
                titleEn = "Debt", titleAr = "دين",
                subtitleEn = "Money owed", subtitleAr = "مبالغ مستحقة",
                icon = Icons.Default.AccountBalanceWallet
            ),
            SmartItemType(
                id = "gam3iya",
                titleEn = "Gam3iya", titleAr = "جمعية",
                subtitleEn = "Group savings", subtitleAr = "ادخار جماعي",
                icon = Icons.Default.Group
            ),
            SmartItemType(
                id = "installment",
                titleEn = "Installment", titleAr = "قسط",
                subtitleEn = "Buy-now-pay-later", subtitleAr = "تقسيط ومتابعة",
                icon = Icons.Default.CreditCard
            ),
            SmartItemType(
                id = "subscription",
                titleEn = "Subscription", titleAr = "اشتراك",
                subtitleEn = "Netflix, Spotify…", subtitleAr = "نتفليكس، سبوتيفاي…",
                icon = Icons.Default.Subscriptions
            ),
            SmartItemType(
                id = "bill",
                titleEn = "Bill", titleAr = "فاتورة",
                subtitleEn = "Payments due", subtitleAr = "مدفوعات مستحقة",
                icon = Icons.Default.Receipt
            ),
            SmartItemType(
                id = "alarm",
                titleEn = "Alarm", titleAr = "منبه",
                subtitleEn = "Wake-up & ring", subtitleAr = "صحيني ورن",
                icon = Icons.Default.Alarm
            ),
            SmartItemType(
                id = "medicine",
                titleEn = "Medicine", titleAr = "دواء",
                subtitleEn = "Doses & refills", subtitleAr = "جرعات وتجديد",
                icon = Icons.Default.Medication
            ),
            SmartItemType(
                id = "habit",
                titleEn = "Habit", titleAr = "عادة",
                subtitleEn = "Daily routines", subtitleAr = "روتين يومي",
                icon = Icons.Default.Repeat
            ),
            SmartItemType(
                id = "study",
                titleEn = "Study", titleAr = "دراسة",
                subtitleEn = "Courses & exams", subtitleAr = "مذاكرة وامتحانات",
                icon = Icons.Default.School
            ),
            SmartItemType(
                id = "work",
                titleEn = "Work", titleAr = "شغل",
                subtitleEn = "Projects & deadlines", subtitleAr = "مشاريع ومواعيد نهائية",
                icon = Icons.Default.Work
            ),
            SmartItemType(
                id = "event",
                titleEn = "Event", titleAr = "مناسبة",
                subtitleEn = "Celebrations & plans", subtitleAr = "احتفالات وخطط",
                icon = Icons.Default.Celebration
            ),
            SmartItemType(
                id = "personal",
                titleEn = "Personal", titleAr = "شخصي",
                subtitleEn = "Just for you", subtitleAr = "خاص بيك",
                icon = Icons.Default.Person
            ),
            SmartItemType(
                id = "more",
                titleEn = "More", titleAr = "المزيد",
                subtitleEn = "Other categories", subtitleAr = "تصنيفات تانية",
                icon = Icons.Default.MoreHoriz
            )
        )
    }
}
