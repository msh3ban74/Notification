package com.notification.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notification.app.domain.model.SmartItemType
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumCard

/**
 * Sprint 2 — Smart Item Engine Foundation.
 *
 * Bottom sheet opened from the Dashboard's "+" FAB. UI/navigation only —
 * selecting a type just routes to a placeholder screen (Sprint 3 builds
 * the real forms). Uses ONLY the existing Sprint 1 Design System
 * (PremiumCard, AppRadius, AppPadding, AppDimens) and the existing
 * MaterialTheme — no new colors or typography are introduced here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartItemBottomSheet(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onItemSelected: (SmartItemType) -> Unit
) {
    // sheetState is created internally (rather than exposed as a default
    // parameter) so this function's public signature stays free of
    // experimental Material3 types — that way callers like MainActivity
    // don't need their own @OptIn just to invoke this composable.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppPadding.screen)
                .padding(bottom = AppPadding.screen)
        ) {
            Text(
                text = if (isArabic) "عايز تنظم إيه؟" else "What would you like to organize?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = AppPadding.card)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(AppPadding.cardCompact),
                verticalArrangement = Arrangement.spacedBy(AppPadding.cardCompact),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SmartItemType.all) { item ->
                    SmartItemGridCard(
                        item = item,
                        isArabic = isArabic,
                        onClick = { onItemSelected(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartItemGridCard(
    item: SmartItemType,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    PremiumCard(
        onClick = onClick,
        contentPadding = AppPadding.cardCompact,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    imageVector = item.icon,
                    contentDescription = if (isArabic) item.titleAr else item.titleEn,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(AppDimens.iconSizeMedium)
                )
            }

            Text(
                text = if (isArabic) item.titleAr else item.titleEn,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = if (isArabic) item.subtitleAr else item.subtitleEn,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
