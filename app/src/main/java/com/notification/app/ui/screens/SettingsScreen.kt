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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    onSignOut: () -> Unit,
    // "syncing" | "success:<msg>" | "error: <reason>" | null — silent
    // failures made the sync button look dead; every outcome is visible.
    backupState: String? = null,
    onTriggerRestore: (() -> Unit)? = null,
    restoreState: String? = null,
    // Sprint 3 — local backup file (SAF).
    suggestedBackupFileName: String = "Rafeeq_Backup.rafeeq",
    onExportBackupFile: ((android.net.Uri) -> Unit)? = null,
    onPickRestoreFile: ((android.net.Uri) -> Unit)? = null,
    fileBackupState: String? = null,
    fileRestoreState: String? = null,
    restorePreview: com.notification.app.data.repository.LocalBackupManager.RestorePreview? = null,
    onConfirmRestore: () -> Unit = {},
    onCancelRestore: () -> Unit = {},
    // Sprint 3 — automatic scheduled backup.
    autoCloudBackup: Boolean = false,
    onSetAutoCloudBackup: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    // SAF pickers — the user chooses where to save / which file to open.
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) onExportBackupFile?.invoke(uri) }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onPickRestoreFile?.invoke(uri) }
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

                        // Backup + Restore actions. Backup uploads; restore
                        // pulls the cloud copy back. Each shows its own spinner.
                        if (backupState == "syncing") {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            FilledTonalIconButton(onClick = onTriggerBackup) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = if (isArabic) "نسخ احتياطي الآن" else "Back up now"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (restoreState == "syncing") {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else if (onTriggerRestore != null) {
                            FilledTonalIconButton(onClick = onTriggerRestore) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = if (isArabic) "استعادة" else "Restore"
                                )
                            }
                        }
                    }

                    // Visible outcome for backup — success message or failure reason.
                    StatusLine(state = backupState, isArabic = isArabic)
                    StatusLine(state = restoreState, isArabic = isArabic)
                }
            }
        }

        // Local backup file (export / import via SAF) — a portable,
        // compressed, checksummed, encrypted-when-possible package.
        if (onExportBackupFile != null || onPickRestoreFile != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (isArabic) "نسخة احتياطية كملف" else "Backup as a file",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic)
                                "ملف واحد مضغوط ومُتحقَّق منه، تختار مكان حفظه وتستعيده وقت ما تحب"
                            else "One compressed, verified file — save it anywhere and restore anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (onExportBackupFile != null) {
                                FilledTonalButton(
                                    onClick = {
                                        if (fileBackupState != "syncing") exportLauncher.launch(suggestedBackupFileName)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (fileBackupState == "syncing") {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isArabic) "تصدير" else "Export")
                                    }
                                }
                            }
                            if (onPickRestoreFile != null) {
                                FilledTonalButton(
                                    onClick = {
                                        if (fileRestoreState != "syncing") importLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (fileRestoreState == "syncing") {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (isArabic) "استيراد" else "Import")
                                    }
                                }
                            }
                        }
                        StatusLine(state = fileBackupState, isArabic = isArabic)
                        StatusLine(state = fileRestoreState, isArabic = isArabic)
                    }
                }
            }
        }

        // ── Automatic scheduled backup ───────────────────────────────
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "نسخ احتياطي تلقائي" else "Automatic backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic)
                                "يرفع بياناتك للسحابة تلقائيًا مع كل تعديل"
                            else "Backs up to the cloud automatically on every change",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        Switch(
                            checked = autoCloudBackup,
                            onCheckedChange = onSetAutoCloudBackup
                        )
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
                        text = if (isArabic) "الإصدار 1.0 • مساعدك الشخصي الذكي" else "Version 1.0 • Your intelligent personal assistant",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Restore confirmation — preview counts, then apply only on confirm.
    val preview = restorePreview
    if (preview != null) {
        val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text(if (isArabic) "تأكيد الاستعادة" else "Confirm restore") },
            text = {
                Column {
                    Text(
                        text = if (isArabic)
                            "نسخة بتاريخ ${dateFmt.format(Date(preview.createdAt))}"
                        else "Backup from ${dateFmt.format(Date(preview.createdAt))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "سيتم استعادة ${preview.totalCount} عنصر ودمجها مع بياناتك الحالية:"
                        else "${preview.totalCount} items will be restored and merged with your data:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    preview.counts.forEach { (key, n) ->
                        Text("• ${labelForKey(key, isArabic)}: $n", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = MaroonPrimary)
                ) { Text(if (isArabic) "استعادة" else "Restore") }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}

private fun labelForKey(key: String, isArabic: Boolean): String = when (key) {
    "reminders" -> if (isArabic) "المهام والتذكيرات" else "Tasks & reminders"
    "persons" -> if (isArabic) "جهات الاتصال" else "Contacts"
    "ledger_transactions" -> if (isArabic) "معاملات الديون" else "Ledger entries"
    "gam3iyas" -> if (isArabic) "الجمعيات" else "Gam3iyas"
    "gam3iya_members" -> if (isArabic) "أعضاء الجمعيات" else "Gam3iya members"
    "alarms" -> if (isArabic) "المنبهات" else "Alarms"
    "work_notes" -> if (isArabic) "الملاحظات" else "Notes"
    "financial_items" -> if (isArabic) "الفواتير والأقساط" else "Bills & installments"
    "habits" -> if (isArabic) "العادات" else "Habits"
    "habit_logs" -> if (isArabic) "سجل العادات" else "Habit logs"
    else -> key
}

/** Renders a backup/restore outcome line: green on success, red on error. */
@Composable
private fun StatusLine(state: String?, isArabic: Boolean) {
    when {
        state == null || state == "syncing" -> {}
        state.startsWith("success") -> Text(
            text = "✓ " + state.substringAfter("success:", "").ifBlank {
                if (isArabic) "تم بنجاح" else "Done"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
        state.startsWith("error:") -> Text(
            text = (if (isArabic) "تعذّرت العملية — " else "Failed — ") +
                state.removePrefix("error:").trim(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
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
