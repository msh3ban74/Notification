package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding

/**
 * Sprint 1 — Application Foundation.
 *
 * Notifications: one of the four primary Bottom Navigation destinations.
 *
 * IMPORTANT: This is UI SHELL ONLY for Sprint 1. Placeholder content only
 *  - not connected to Room, Firestore, or Gemini. A real notifications
 * feed will be implemented in a later sprint.
 */
@Composable
fun NotificationsScreen(isArabic: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppPadding.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AppDimens.iconSizeLarge)
        )
        Text(
            text = if (isArabic) "لا توجد إشعارات بعد" else "No notifications yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isArabic)
                "ستظهر إشعاراتك هنا قريبًا."
            else
                "Your notifications will appear here soon.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
