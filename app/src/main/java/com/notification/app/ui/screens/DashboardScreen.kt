package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.notification.app.ui.designsystem.AppDimens
import com.notification.app.ui.designsystem.AppPadding
import com.notification.app.ui.designsystem.PremiumCard
import com.notification.app.ui.theme.Spacing

/**
 * Sprint 1 — Application Foundation.
 * Sprint 2 — UI/UX Polish: reworked spacing, hierarchy, alignment and
 * card proportions. No sections were added or removed, and none of the
 * placeholder copy or navigation targets changed — only how the same
 * content is laid out.
 *
 * Dashboard: the new default landing screen after Splash.
 *
 * IMPORTANT: This is UI SHELL ONLY for Sprint 1.
 *  - NOT connected to Room, Firestore, or Gemini.
 *  - All content below is placeholder/sample data.
 *  - No business logic lives in this file.
 *
 * The "Quick Actions" section links out to the app's EXISTING, already
 * built feature screens (Ledger, Gam3iya, Islamic, Health/Notes) so those
 * features remain reachable now that they are no longer in the Bottom
 * Navigation. Those destinations themselves are untouched.
 */
@Composable
fun DashboardScreen(
    isArabic: Boolean = false,
    onNavigateToLedger: () -> Unit = {},
    onNavigateToGam3iya: () -> Unit = {},
    onNavigateToIslamic: () -> Unit = {},
    onNavigateToHealthNotes: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppPadding.screen)
            .padding(top = Spacing.sm, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        GreetingSection(isArabic = isArabic)

        TodaysSummarySection(isArabic = isArabic)

        QuickActionsSection(
            isArabic = isArabic,
            onNavigateToLedger = onNavigateToLedger,
            onNavigateToGam3iya = onNavigateToGam3iya,
            onNavigateToIslamic = onNavigateToIslamic,
            onNavigateToHealthNotes = onNavigateToHealthNotes
        )

        RecentActivitySection(isArabic = isArabic)

        AiSuggestionsSection(isArabic = isArabic)
    }
}

@Composable
private fun GreetingSection(isArabic: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = if (isArabic) "أهلاً بيك 👋" else "Welcome back 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (isArabic) "نظرة سريعة على يومك" else "Here's a quick look at your day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shared section header style, used above every card group on this screen. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun TodaysSummarySection(isArabic: Boolean) {
    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "ملخص اليوم" else "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text(
                text = if (isArabic)
                    "سيتم عرض ملخص حقيقي هنا في مرحلة لاحقة."
                else
                    "A real summary will appear here in a later sprint.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class QuickAction(
    val labelEn: String,
    val labelAr: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsSection(
    isArabic: Boolean,
    onNavigateToLedger: () -> Unit,
    onNavigateToGam3iya: () -> Unit,
    onNavigateToIslamic: () -> Unit,
    onNavigateToHealthNotes: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionTitle(if (isArabic) "إجراءات سريعة" else "Quick Actions")

        val actions = listOf(
            QuickAction(
                "Ledger", "دفتر الديون",
                Icons.Default.AccountBalanceWallet, onNavigateToLedger
            ),
            QuickAction(
                "Gam3iya", "الجمعيات",
                Icons.Default.Group, onNavigateToGam3iya
            ),
            QuickAction(
                "Islamic", "إسلاميات",
                Icons.Default.Mosque, onNavigateToIslamic
            ),
            QuickAction(
                "Health", "الصحة",
                Icons.Default.WaterDrop, onNavigateToHealthNotes
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(actions) { action ->
                PremiumCard(
                    onClick = action.onClick,
                    contentPadding = AppPadding.cardCompact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.95f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = if (isArabic) action.labelAr else action.labelEn,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.iconSizeMedium)
                        )
                        Text(
                            text = if (isArabic) action.labelAr else action.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.padding(top = Spacing.xs)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivitySection(isArabic: Boolean) {
    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "النشاط الأخير" else "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text(
                text = if (isArabic)
                    "لا يوجد نشاط لعرضه بعد."
                else
                    "No recent activity to show yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiSuggestionsSection(isArabic: Boolean) {
    PremiumCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = if (isArabic) "اقتراحات الذكاء الاصطناعي" else "AI Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider()
            Text(
                text = if (isArabic)
                    "سيقترح المساعد الذكي أفكار مخصصة لك هنا قريبًا."
                else
                    "Your AI Assistant will suggest personalized ideas here soon.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
