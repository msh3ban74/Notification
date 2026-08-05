package com.notification.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notification.app.ui.designsystem.AppDimens

/**
 * Sprint 1 — Application Foundation.
 *
 * Reusable top bar used across all four primary destinations
 * (Dashboard, AI Assistant, Tasks, Notifications).
 *
 * Contains ONLY:
 *  - Application logo
 *  - Application title
 *  - Profile / Settings button (the ONLY way to reach Settings)
 *
 * No search field, per spec.
 *
 * @param onProfileClick navigates to the Settings screen. Settings is
 * intentionally NOT part of Bottom Navigation — this button is its only
 * entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopAppBar(
    title: String,
    onProfileClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "App logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(AppDimens.iconSizeLarge)
                    .padding(start = 12.dp)
            )
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.avatarSizeMedium)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile / Settings",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(AppDimens.iconSizeMedium)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
    )
}
