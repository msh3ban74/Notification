package com.notification.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notification.app.ui.designsystem.RafeeqCard
import com.notification.app.ui.designsystem.RafeeqElevatedCard
import com.notification.app.ui.designsystem.RafeeqEmptyState
import com.notification.app.ui.designsystem.RafeeqPlaceholderItem
import com.notification.app.ui.designsystem.RafeeqQuickActionButton
import com.notification.app.ui.designsystem.RafeeqSectionHeader
import com.notification.app.ui.designsystem.RafeeqSpacing

/**
 * Dashboard Screen - The central landing screen of Rafeeq
 * Contains: Greeting, Today's Summary, Quick Actions, Recent Activity, AI Suggestions
 * NOTE: This screen uses placeholder data only. No business logic connections yet.
 */
@Composable
fun DashboardScreen(
    onNavigateToLedger: () -> Unit = {},
    onNavigateToGam3iya: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToIslamic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = RafeeqSpacing.medium),
            horizontalAlignment = Alignment.Start
        ) {
            // 1. Greeting Section
            GreetingSection()
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.large))
            
            // 2. Today's Summary
            TodaysSummarySection()
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.large))
            
            // 3. Quick Actions
            QuickActionsSection(
                onLedgerClick = onNavigateToLedger,
                onGam3iyaClick = onNavigateToGam3iya,
                onRemindersClick = onNavigateToReminders,
                onIslamicClick = onNavigateToIslamic
            )
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.large))
            
            // 4. Recent Activity (Placeholder)
            RecentActivitySection()
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.large))
            
            // 5. AI Suggestions (Placeholder)
            AiSuggestionsSection()
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.extraLarge))
        }
    }
}

@Composable
private fun GreetingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RafeeqSpacing.medium),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Good Morning",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Welcome to Rafeeq",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodaysSummarySection() {
    RafeeqCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RafeeqSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(RafeeqSpacing.small)
        ) {
            RafeeqSectionHeader(title = "Today's Summary")
            
            RafeeqPlaceholderItem(text = "• 3 reminders pending")
            RafeeqPlaceholderItem(text = "• 2 transactions recorded")
            RafeeqPlaceholderItem(text = "• Next prayer: Asr at 3:45 PM")
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.small))
        }
    }
}

@Composable
private fun QuickActionsSection(
    onLedgerClick: () -> Unit,
    onGam3iyaClick: () -> Unit,
    onRemindersClick: () -> Unit,
    onIslamicClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        RafeeqSectionHeader(title = "Quick Actions")
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RafeeqSpacing.medium),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RafeeqQuickActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Ledger",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = "Ledger",
                onClick = onLedgerClick
            )
            
            RafeeqQuickActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Gam3iya",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = "Gam3iya",
                onClick = onGam3iyaClick
            )
            
            RafeeqQuickActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Reminders",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = "Tasks",
                onClick = onRemindersClick
            )
            
            RafeeqQuickActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Islamic",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = "Prayer",
                onClick = onIslamicClick
            )
        }
    }
}

@Composable
private fun RecentActivitySection() {
    RafeeqCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RafeeqSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(RafeeqSpacing.small)
        ) {
            RafeeqSectionHeader(title = "Recent Activity")
            
            RafeeqPlaceholderItem(text = "No recent activity")
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.small))
        }
    }
}

@Composable
private fun AiSuggestionsSection() {
    RafeeqElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RafeeqSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(RafeeqSpacing.small)
        ) {
            RafeeqSectionHeader(title = "AI Suggestions")
            
            RafeeqEmptyState(message = "Ask Rafeeq AI for personalized suggestions")
            
            Spacer(modifier = Modifier.height(RafeeqSpacing.small))
        }
    }
}
