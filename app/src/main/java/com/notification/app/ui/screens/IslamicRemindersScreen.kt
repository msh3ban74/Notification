package com.notification.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notification.app.domain.calculator.PrayerTime
import com.notification.app.domain.calculator.PrayerTimesCalculator

@Composable
fun IslamicRemindersScreen(
    prayerTimes: List<PrayerTime>,
    isArabic: Boolean
) {
    val nextPrayer = remember(prayerTimes) {
        if (prayerTimes.isNotEmpty()) PrayerTimesCalculator.getNextPrayer(prayerTimes) else null
    }

    var morningAdhkarEnabled by remember { mutableStateOf(true) }
    var eveningAdhkarEnabled by remember { mutableStateOf(true) }
    var qiyamEnabled by remember { mutableStateOf(true) }
    var duhaEnabled by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = if (isArabic) "التذكيرات الإسلامية وأوقات الصلاة" else "Islamic Reminders & Prayer Times",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Hero Card
        if (nextPrayer != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isArabic) "الصلاة القادمة" else "Next Prayer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = if (isArabic) nextPrayer.nameAr else nextPrayer.nameEn,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = nextPrayer.timeFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Prayer Schedule List
        item {
            Text(
                text = if (isArabic) "مواقيت الصلاة اليومية" else "Daily Prayer Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(prayerTimes) { prayer ->
            val isNext = nextPrayer?.nameEn == prayer.nameEn
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isArabic) prayer.nameAr else prayer.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium
                    )

                    Text(
                        text = prayer.timeFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Sunnah & Adhkar Toggles
        item {
            Text(
                text = if (isArabic) "الأذكار والنوافل" else "Adhkar & Voluntary Prayers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        item {
            AdhkarToggleCard(
                title = if (isArabic) "أذكار الصباح" else "Morning Adhkar",
                icon = Icons.Default.WbSunny,
                checked = morningAdhkarEnabled,
                onCheckedChange = { morningAdhkarEnabled = it }
            )
        }

        item {
            AdhkarToggleCard(
                title = if (isArabic) "أذكار المساء" else "Evening Adhkar",
                icon = Icons.Default.NightsStay,
                checked = eveningAdhkarEnabled,
                onCheckedChange = { eveningAdhkarEnabled = it }
            )
        }

        item {
            AdhkarToggleCard(
                title = if (isArabic) "صلاة الضحى" else "Duha Prayer",
                icon = Icons.Default.WbSunny,
                checked = duhaEnabled,
                onCheckedChange = { duhaEnabled = it }
            )
        }

        item {
            AdhkarToggleCard(
                title = if (isArabic) "قيام الليل" else "Qiyam al-Layl",
                icon = Icons.Default.NightsStay,
                checked = qiyamEnabled,
                onCheckedChange = { qiyamEnabled = it }
            )
        }
    }
}

@Composable
private fun AdhkarToggleCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
