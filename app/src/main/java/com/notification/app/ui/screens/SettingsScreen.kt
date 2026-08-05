package com.notification.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notification.app.ui.theme.MaroonPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    currentLanguage: String,
    isLoggedIn: Boolean,
    userName: String,
    userEmail: String,
    lastSyncTime: Long,
    isArabic: Boolean,
    onSetLanguage: (String) -> Unit,
    onTriggerBackup: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = if (isArabic) "الإعدادات" else "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── Section: Account ─────────────────────────────────────────
        item {
            Text(
                text = if (isArabic) "الحساب والنسخ الاحتياطي" else "Account & Backup",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Account & Cloud Backup Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isLoggedIn) userName else if (isArabic) "حساب زائر" else "Guest Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isLoggedIn) userEmail else if (isArabic) "لم يتم تسجيل الدخول" else "Not logged in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isLoggedIn) {
                            TextButton(onClick = onSignOut) {
                                Text(
                                    text = if (isArabic) "خروج" else "Sign Out",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "النسخ الاحتياطي السحابي" else "Google Cloud Backup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (lastSyncTime > 0) "${if (isArabic) "آخر مزامنة:" else "Last sync:"} ${dateFormat.format(Date(lastSyncTime))}"
                                else if (isArabic) "لم تتم المزامنة بعد" else "Never synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Compact icon-only sync (the old full-width button
                        // dominated the card). Tonal, fixed size.
                        FilledTonalIconButton(onClick = onTriggerBackup) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = if (isArabic) "مزامنة الآن" else "Sync now"
                            )
                        }
                    }
                }
            }
        }

        // Language & Appearance Section
        item {
            Text(
                text = if (isArabic) "اللغة والمظهر" else "Language & Appearance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isArabic) "اللغة / Language" else "App Language",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row {
                            // QA fix: labels wrapped to two lines on device
                            // ("Engl/ish") — force a single unwrapped line.
                            FilterChip(
                                selected = currentLanguage == "ar",
                                onClick = { onSetLanguage("ar") },
                                label = { Text("العربية", maxLines = 1, softWrap = false) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = currentLanguage == "en",
                                onClick = { onSetLanguage("en") },
                                label = { Text("English", maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                }
            }
        }

        // ── Section: Notifications ───────────────────────────────────
        item {
            Text(
                text = if (isArabic) "الإشعارات والتنبيهات" else "Notifications",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Battery Optimization Exemption Prompt
        item {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isArabic) "استثناء توفير البطارية" else "Battery Optimization",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isArabic) "ضمان وصول التنبيهات والمنبهات في موعدها الدقيق" else "Ensure exact alarms ring on schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(onClick = { requestBatteryOptimizationExemption(context) }) {
                        Text(if (isArabic) "تفعيل" else "Enable")
                    }
                }
            }
        }

        // Permissions: notification access + exact-alarm special access.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isArabic) "السماح بالإشعارات" else "Notification access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(onClick = {
                            com.notification.app.ui.permissions.openNotificationSettings(context)
                        }) {
                            Text(if (isArabic) "فتح" else "Open")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isArabic) "المنبهات الدقيقة" else "Exact alarms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(onClick = {
                            com.notification.app.ui.permissions.openExactAlarmSettings(context)
                        }) {
                            Text(if (isArabic) "فتح" else "Open")
                        }
                    }
                }
            }
        }

        // ── Section: About ───────────────────────────────────────────
        item {
            Text(
                text = if (isArabic) "حول" else "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isArabic) "رفيق — Rafeeq" else "Rafeeq — رفيق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isArabic) "الإصدار 1.0 • رفيقك الذكي في كل تفاصيل حياتك" else "Version 1.0 • Your intelligent life companion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun requestBatteryOptimizationExemption(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent().apply {
            val packageName = context.packageName
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            } else {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
