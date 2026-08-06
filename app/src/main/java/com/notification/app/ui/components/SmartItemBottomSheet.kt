package com.notification.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notification.app.domain.model.SmartItemType
import com.notification.app.ui.designsystem.AppAnimationDuration
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.AppRadius
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.theme.Spacing

/**
 * Sprint 2 — Smart Item Engine Foundation.
 * Sprint 2 (UI/UX Polish) — spacing, grid alignment, icon sizing,
 * typography and a staggered entrance animation for the grid were
 * refined. The Smart Item list and selection behavior are unchanged.
 *
 * Bottom sheet opened from the Dashboard's "+" FAB. Selecting a type
 * routes to its real create screen (handled in MainActivity). Uses ONLY
 * the existing Sprint 1 Design System
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
        sheetState = sheetState,
        // Rafeeq Design Language: bottom sheets are very rounded.
        shape = AppRadius.sheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppPadding.screen)
                .padding(bottom = AppPadding.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = if (isArabic) "عايز تنظم إيه؟" else "What would you like to organize?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(items = SmartItemType.all) { index, item ->
                    SmartItemGridCard(
                        item = item,
                        isArabic = isArabic,
                        entranceDelayMillis = index * 25,
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
    entranceDelayMillis: Int,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(item) {
        kotlinx.coroutines.delay(entranceDelayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(AppAnimationDuration.normal)) +
            scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(AppAnimationDuration.normal)
            )
    ) {
        PremiumCard(
            onClick = onClick,
            contentPadding = AppPadding.cardCompact,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
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
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = Spacing.xs)
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
}
