package com.notification.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Real cloud backup/restore for the local Room database, using Firestore under
 * the signed-in user's UID: users/{uid}/reminders, users/{uid}/persons, etc.
 *
 * This replaces the previous fake "triggerBackupSync()" which only updated a
 * local timestamp without uploading anything.
 */
class BackupRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    sealed class BackupResult {
        object Success : BackupResult()
        data class Failure(val message: String) : BackupResult()
    }

    private fun userDoc() = auth.currentUser?.uid?.let { uid -> firestore.collection("users").document(uid) }

    /** Uploads the full local database to Firestore for the current signed-in user. */
    suspend fun backupNow(): BackupResult {
        val userDocRef = userDoc() ?: return BackupResult.Failure("Not signed in.")
        return try {
            val reminders = db.reminderDao().getAllReminders().first()
            val persons = db.personLedgerDao().getAllPersons().first()
            val transactions = db.personLedgerDao().getAllTransactions().first()
            val gam3iyas = db.gam3iyaDao().getAllGam3iyas().first()
            val alarms = db.alarmDao().getAllAlarms().first()
            val workNotes = db.workNoteDao().getAllNotes().first()

            val batch = firestore.batch()

            batch.set(userDocRef, mapOf("lastBackupAt" to System.currentTimeMillis()))

            reminders.forEach { item ->
                batch.set(userDocRef.collection("reminders").document(item.id.toString()), item)
            }
            persons.forEach { item ->
                batch.set(userDocRef.collection("persons").document(item.id.toString()), item)
            }
            transactions.forEach { item ->
                batch.set(userDocRef.collection("ledger_transactions").document(item.id.toString()), item)
            }
            gam3iyas.forEach { item ->
                batch.set(userDocRef.collection("gam3iyas").document(item.id.toString()), item)
            }
            alarms.forEach { item ->
                batch.set(userDocRef.collection("alarms").document(item.id.toString()), item)
            }
            workNotes.forEach { item ->
                batch.set(userDocRef.collection("work_notes").document(item.id.toString()), item)
            }

            batch.commit().await()
            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Backup failed.")
        }
    }

    /** Downloads the user's Firestore backup and repopulates the local Room database. */
    suspend fun restoreLatest(): BackupResult {
        val userDocRef = userDoc() ?: return BackupResult.Failure("Not signed in.")
        return try {
            val reminders = userDocRef.collection("reminders").get().await()
                .toObjects(ReminderEntity::class.java)
            val persons = userDocRef.collection("persons").get().await()
                .toObjects(PersonEntity::class.java)
            val transactions = userDocRef.collection("ledger_transactions").get().await()
                .toObjects(LedgerTransactionEntity::class.java)
            val gam3iyas = userDocRef.collection("gam3iyas").get().await()
                .toObjects(Gam3iyaEntity::class.java)
            val alarms = userDocRef.collection("alarms").get().await()
                .toObjects(AlarmEntity::class.java)
            val workNotes = userDocRef.collection("work_notes").get().await()
                .toObjects(WorkNoteEntity::class.java)

            reminders.forEach { db.reminderDao().insertReminder(it) }
            persons.forEach { db.personLedgerDao().insertPerson(it) }
            transactions.forEach { db.personLedgerDao().insertTransaction(it) }
            gam3iyas.forEach { db.gam3iyaDao().insertGam3iya(it) }
            alarms.forEach { db.alarmDao().insertAlarm(it) }
            workNotes.forEach { db.workNoteDao().insertNote(it) }

            BackupResult.Success
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "Restore failed.")
        }
    }
}
