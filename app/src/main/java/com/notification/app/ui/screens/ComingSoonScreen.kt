package com.notification.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notification.app.ui.components.EmptyState

/**
 * Sprint 2 — Smart Item Engine Foundation.
 * Sprint 2 (UI/UX Polish): now reuses the shared [EmptyState] component
 * instead of a hand-rolled Column, so this placeholder matches the same
 * premium look used by Reminders, Ledger, and Gam3iya's empty states.
 * Same copy, same back navigation — visual only.
 *
 * Reusable placeholder shown after picking a type from the Smart Item
 * bottom sheet that doesn't have a real form yet (Gam3iya, Bill, etc).
 * Sprint 3/4: Task and Debt now have real forms (CreateTaskScreen /
 * CreateDebtScreen) and no longer land here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComingSoonScreen(
    titleEn: String,
    titleAr: String,
    isArabic: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        EmptyState(
            icon = Icons.Default.Construction,
            title = if (isArabic) "نموذج $titleAr" else "$titleEn Form",
            subtitle = if (isArabic) "قريبًا في مرحلة قادمة" else "Coming in a future sprint",
            modifier = Modifier.padding(innerPadding),
            bottomInset = 0.dp
        )
    }
}
