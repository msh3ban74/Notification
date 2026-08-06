package com.notification.app.domain.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.preferences.UserPreferencesRepository
import com.notification.app.data.repository.LocalBackupManager
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sprint 3 — automatic scheduled backup.
 *
 * Runs off the main thread (WorkManager background thread + Dispatchers.IO
 * inside LocalBackupManager). It writes a fresh timestamped `.rafeeq` file
 * into the folder the user granted via [ActivityResultContracts.OpenDocumentTree],
 * then records the run timestamp so Settings can show "last backup".
 *
 * It never throws to the caller: any failure returns Result.retry() (for
 * transient issues WorkManager will retry with backoff) or Result.failure()
 * (for permanent misconfiguration such as a missing/revoked folder grant),
 * so it can never crash the app or fail silently in a way the user can't
 * diagnose from the Settings status line.
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(applicationContext)

        // OFF means the schedule shouldn't exist at all; treat as a no-op success.
        val freq = prefs.autoBackupFreqFlow.first()
        if (freq == "OFF") return Result.success()

        val treeUriString = prefs.autoBackupTreeUriFlow.first()
        if (treeUriString.isBlank()) {
            // No destination folder chosen — nothing we can do until the user
            // picks one. Permanent failure (not a retry) so WorkManager stops.
            return Result.failure()
        }

        val treeUri = try {
            Uri.parse(treeUriString)
        } catch (e: Exception) {
            return Result.failure()
        }

        return try {
            val fileUri = createBackupFile(treeUri)
                ?: return Result.retry() // folder grant may be temporarily unavailable

            val db = AppDatabase.getDatabase(applicationContext)
            val manager = LocalBackupManager(applicationContext, db, prefs)

            when (val res = manager.exportToUri(fileUri)) {
                is LocalBackupManager.Result.Success -> {
                    prefs.setAutoBackupLast(System.currentTimeMillis())
                    Result.success()
                }
                is LocalBackupManager.Result.Failure -> {
                    // A PERMISSION failure means the grant was revoked — permanent.
                    if (res.reason.startsWith("PERMISSION")) Result.failure()
                    else Result.retry()
                }
            }
        } catch (e: SecurityException) {
            // Persisted permission to the tree was revoked.
            Result.failure()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Creates a new document inside the granted tree and returns its Uri.
     * Uses DocumentsContract directly (no extra androidx.documentfile dep).
     */
    private fun createBackupFile(treeUri: Uri): Uri? {
        val resolver = applicationContext.contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val displayName = "Rafeeq_AutoBackup_$stamp.rafeeq"
        return DocumentsContract.createDocument(
            resolver,
            parentDocUri,
            "application/octet-stream",
            displayName
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "rafeeq_auto_backup"
    }
}
