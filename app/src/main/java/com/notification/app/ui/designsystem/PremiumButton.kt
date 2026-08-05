package com.notification.app.ui.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Sprint 1 — Application Foundation / Design System.
 *
 * Reusable primary/secondary buttons that wrap the app's EXISTING theme
 * (colors and typography come from MaterialTheme, i.e. NotificationTheme
 * — nothing is redefined here). Use these instead of a raw [Button] on
 * new screens so height, shape, and padding stay consistent.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(AppDimens.buttonHeight),
        enabled = enabled,
        shape = AppRadius.small,
        contentPadding = PaddingValues(
            horizontal = AppPadding.buttonHorizontal,
            vertical = AppPadding.buttonVertical
        ),
        colors = ButtonDefaults.buttonColors()
    ) {
        Text(text)
    }
}

@Composable
fun PremiumOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(AppDimens.buttonHeight),
        enabled = enabled,
        shape = AppRadius.small,
        contentPadding = PaddingValues(
            horizontal = AppPadding.buttonHorizontal,
            vertical = AppPadding.buttonVertical
        )
    ) {
        Text(text)
    }
}
