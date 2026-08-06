package com.notification.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.local.entities.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Real cloud backup/restore for the local Room database, using Firestore
 * under the signed-in user's UID: users/{uid}/reminders, users/{uid}/persons…
 *
 * Serialization is MANUAL (entity -> Map and DocumentSnapshot -> entity):
 * Firestore's reflective mapper needs no-argument constructors that Kotlin
 * data classes with required fields don't have, which silently broke
 * restore. Manual maps also survive schema evolution (missing fields fall
 * back to entity defaults). Batches are chunked under Firestore's 500-op
 * limit.
 */
class BackupRepository(
    private val db: AppDatabase,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    sealed class BackupResult {
        /** [count] = number of records uploaded (backup) or restored. */
        data class Success(val count: Int = 0) : BackupResult()
        data class Failure(val message: String) : BackupResult()
    }

    private fun userDoc() = auth.currentUser?.uid?.let { uid -> firestore.collection("users").document(uid) }

    /** Uploads the full local database to Firestore for the current signed-in user. */
    suspend fun backupNow(): BackupResult {
        val userDocRef = userDoc() ?: return BackupResult.Failure("Not signed in.")
        return try {
            data class Op(val collection: String, val docId: String, val data: Map<String, Any?>)

            val ops = mutableListOf<Op>()
            db.reminderDao().getAllReminders().first().forEach {
                ops += Op("reminders", it.id.toString(), reminderToMap(it))
            }
            db.personLedgerDao().getAllPersons().first().forEach {
                ops += Op("persons", it.id.toString(), personToMap(it))
            }
            db.personLedgerDao().getAllTransactions().first().forEach {
                ops += Op("ledger_transactions", it.id.toString(), transactionToMap(it))
            }
            db.gam3iyaDao().getAllGam3iyas().first().forEach {
                ops += Op("gam3iyas", it.id.toString(), gam3iyaToMap(it))
            }
            db.gam3iyaDao().getAllMembers().first().forEach {
                ops += Op("gam3iya_members", it.id.toString(), gam3iyaMemberToMap(it))
            }
            db.alarmDao().getAllAlarms().first().forEach {
                ops += Op("alarms", it.id.toString(), alarmToMap(it))
            }
            db.workNoteDao().getAllNotes().first().forEach {
                ops += Op("work_notes", it.id.toString(), workNoteToMap(it))
            }
            db.financialDao().getAll().first().forEach {
                ops += Op("financial_items", it.id.toString(), financialToMap(it))
            }
            db.habitDao().getAllHabitsRaw().first().forEach {
                ops += Op("habits", it.id.toString(), habitToMap(it))
            }
            db.habitDao().getAllLogs().first().forEach {
                ops += Op("habit_logs", it.id.toString(), habitLogToMap(it))
            }

            // Firestore allows max 500 writes per batch — chunk well below it.
            ops.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { op ->
                    batch.set(userDocRef.collection(op.collection).document(op.docId), op.data)
                }
                batch.commit().await()
            }
            firestore.batch().apply {
                set(userDocRef, mapOf(
                    "lastBackupAt" to System.currentTimeMillis(),
                    "recordCount" to ops.size
                ))
            }.commit().await()

            BackupResult.Success(ops.size)
        } catch (e: Exception) {
            BackupResult.Failure(readableError(e))
        }
    }

    /**
     * Downloads the user's Firestore backup and repopulates the local Room
     * database. Uses upsert (REPLACE) so restoring MERGES with local data
     * by id instead of duplicating. Returns how many records were restored;
     * an empty cloud backup is a Failure so the UI can say "nothing to
     * restore" rather than a misleading success.
     */
    suspend fun restoreLatest(): BackupResult {
        val userDocRef = userDoc() ?: return BackupResult.Failure("Not signed in.")
        return try {
            var count = 0
            userDocRef.collection("reminders").get().await().documents
                .mapNotNull { it.toReminder() }
                .forEach { db.reminderDao().insertReminder(it); count++ }
            userDocRef.collection("persons").get().await().documents
                .mapNotNull { it.toPerson() }
                .forEach { db.personLedgerDao().insertPerson(it); count++ }
            userDocRef.collection("ledger_transactions").get().await().documents
                .mapNotNull { it.toTransaction() }
                .forEach { db.personLedgerDao().insertTransaction(it); count++ }
            userDocRef.collection("gam3iyas").get().await().documents
                .mapNotNull { it.toGam3iya() }
                .forEach { db.gam3iyaDao().insertGam3iya(it); count++ }
            val members = userDocRef.collection("gam3iya_members").get().await().documents
                .mapNotNull { it.toGam3iyaMember() }
            if (members.isNotEmpty()) { db.gam3iyaDao().insertMembers(members); count += members.size }
            userDocRef.collection("alarms").get().await().documents
                .mapNotNull { it.toAlarm() }
                .forEach { db.alarmDao().insertAlarm(it); count++ }
            userDocRef.collection("work_notes").get().await().documents
                .mapNotNull { it.toWorkNote() }
                .forEach { db.workNoteDao().insertNote(it); count++ }
            userDocRef.collection("financial_items").get().await().documents
                .mapNotNull { it.toFinancialItem() }
                .forEach { db.financialDao().insert(it); count++ }
            userDocRef.collection("habits").get().await().documents
                .mapNotNull { it.toHabit() }
                .forEach { db.habitDao().insertHabit(it); count++ }
            userDocRef.collection("habit_logs").get().await().documents
                .mapNotNull { it.toHabitLog() }
                .forEach { db.habitDao().insertLog(it); count++ }

            if (count == 0) BackupResult.Failure(
                "EMPTY — no cloud backup found for this account yet."
            ) else BackupResult.Success(count)
        } catch (e: Exception) {
            BackupResult.Failure(readableError(e))
        }
    }

    /** Firestore failures mapped to something the Settings screen can show. */
    private fun readableError(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "PERMISSION_DENIED — Firestore security rules block this account."
            msg.contains("UNAVAILABLE", ignoreCase = true) ->
                "UNAVAILABLE — no connection to the backup server."
            msg.contains("NOT_FOUND", ignoreCase = true) ->
                "NOT_FOUND — Firestore database is not set up for this project."
            else -> msg.ifBlank { "Backup failed." }
        }
    }

    // ── Entity -> Map (backup) ───────────────────────────────────────────

    private fun reminderToMap(r: ReminderEntity) = mapOf(
        "id" to r.id, "title" to r.title, "note" to r.note, "dueDate" to r.dueDate,
        "category" to r.category, "recurrence" to r.recurrence, "preAlerts" to r.preAlerts,
        "isCompleted" to r.isCompleted, "createdAt" to r.createdAt, "checklist" to r.checklist,
        "tags" to r.tags, "progress" to r.progress, "location" to r.location,
        "isPinned" to r.isPinned, "isArchived" to r.isArchived
    )

    private fun personToMap(p: PersonEntity) = mapOf(
        "id" to p.id, "name" to p.name, "phoneNumber" to p.phoneNumber,
        "whatsapp" to p.whatsapp, "email" to p.email, "address" to p.address,
        "category" to p.category, "createdAt" to p.createdAt
    )

    private fun transactionToMap(t: LedgerTransactionEntity) = mapOf(
        "id" to t.id, "personId" to t.personId, "type" to t.type, "amount" to t.amount,
        "date" to t.date, "note" to t.note, "linkedReminderId" to t.linkedReminderId
    )

    private fun gam3iyaToMap(g: Gam3iyaEntity) = mapOf(
        "id" to g.id, "title" to g.title, "totalAmount" to g.totalAmount,
        "monthlyInstallment" to g.monthlyInstallment, "membersCount" to g.membersCount,
        "startDate" to g.startDate, "payoutDayOfMonth" to g.payoutDayOfMonth, "note" to g.note
    )

    private fun gam3iyaMemberToMap(m: Gam3iyaMemberEntity) = mapOf(
        "id" to m.id, "gam3iyaId" to m.gam3iyaId, "memberName" to m.memberName,
        "turnMonth" to m.turnMonth, "payoutDate" to m.payoutDate,
        "isPayoutReceived" to m.isPayoutReceived,
        "isInstallmentPaidThisMonth" to m.isInstallmentPaidThisMonth
    )

    private fun alarmToMap(a: AlarmEntity) = mapOf(
        "id" to a.id, "title" to a.title, "timeInMillis" to a.timeInMillis,
        "ringtoneUri" to a.ringtoneUri, "isEnabled" to a.isEnabled, "label" to a.label,
        "createdViaAi" to a.createdViaAi, "createdAt" to a.createdAt
    )

    private fun workNoteToMap(w: WorkNoteEntity) = mapOf(
        "id" to w.id, "title" to w.title, "content" to w.content,
        "reminderTime" to w.reminderTime, "isDone" to w.isDone, "updatedAt" to w.updatedAt
    )

    private fun financialToMap(f: FinancialItemEntity) = mapOf(
        "id" to f.id, "type" to f.type, "title" to f.title, "amount" to f.amount,
        "totalPrice" to f.totalPrice, "downPayment" to f.downPayment,
        "monthlyAmount" to f.monthlyAmount, "remaining" to f.remaining, "dueDate" to f.dueDate,
        "accountNumber" to f.accountNumber, "billNumber" to f.billNumber, "seller" to f.seller,
        "paymentMethod" to f.paymentMethod, "recurring" to f.recurring, "isPaid" to f.isPaid,
        "note" to f.note, "linkedReminderId" to f.linkedReminderId, "createdAt" to f.createdAt
    )

    private fun habitToMap(h: HabitEntity) = mapOf(
        "id" to h.id, "title" to h.title, "emoji" to h.emoji, "note" to h.note,
        "isArchived" to h.isArchived, "createdAt" to h.createdAt
    )

    private fun habitLogToMap(l: HabitLogEntity) = mapOf(
        "id" to l.id, "habitId" to l.habitId, "dayStart" to l.dayStart,
        "completedAt" to l.completedAt
    )

    // ── DocumentSnapshot -> entity (restore) ─────────────────────────────

    private fun DocumentSnapshot.str(field: String, default: String = "") =
        getString(field) ?: default

    private fun DocumentSnapshot.lng(field: String, default: Long = 0L) =
        getLong(field) ?: default

    private fun DocumentSnapshot.dbl(field: String, default: Double = 0.0) =
        getDouble(field) ?: default

    private fun DocumentSnapshot.bool(field: String, default: Boolean = false) =
        getBoolean(field) ?: default

    private fun DocumentSnapshot.toReminder(): ReminderEntity? {
        val title = getString("title") ?: return null
        return ReminderEntity(
            id = lng("id"), title = title, note = str("note"), dueDate = lng("dueDate"),
            category = str("category", "CUSTOM"), recurrence = str("recurrence", "NONE"),
            preAlerts = str("preAlerts", "ONE_DAY,ONE_HOUR"), isCompleted = bool("isCompleted"),
            createdAt = lng("createdAt"), checklist = str("checklist"), tags = str("tags"),
            progress = lng("progress").toInt(), location = str("location"),
            isPinned = bool("isPinned"), isArchived = bool("isArchived")
        )
    }

    private fun DocumentSnapshot.toPerson(): PersonEntity? {
        val name = getString("name") ?: return null
        return PersonEntity(
            id = lng("id"), name = name, phoneNumber = str("phoneNumber"),
            whatsapp = str("whatsapp"), email = str("email"), address = str("address"),
            category = str("category"), createdAt = lng("createdAt")
        )
    }

    private fun DocumentSnapshot.toTransaction(): LedgerTransactionEntity? {
        val type = getString("type") ?: return null
        return LedgerTransactionEntity(
            id = lng("id"), personId = lng("personId"), type = type, amount = dbl("amount"),
            date = lng("date"), note = str("note"), linkedReminderId = getLong("linkedReminderId")
        )
    }

    private fun DocumentSnapshot.toGam3iya(): Gam3iyaEntity? {
        val title = getString("title") ?: return null
        return Gam3iyaEntity(
            id = lng("id"), title = title, totalAmount = dbl("totalAmount"),
            monthlyInstallment = dbl("monthlyInstallment"),
            membersCount = lng("membersCount").toInt(), startDate = lng("startDate"),
            payoutDayOfMonth = lng("payoutDayOfMonth", 1L).toInt(), note = str("note")
        )
    }

    private fun DocumentSnapshot.toGam3iyaMember(): Gam3iyaMemberEntity? {
        val memberName = getString("memberName") ?: return null
        return Gam3iyaMemberEntity(
            id = lng("id"), gam3iyaId = lng("gam3iyaId"), memberName = memberName,
            turnMonth = lng("turnMonth").toInt(), payoutDate = lng("payoutDate"),
            isPayoutReceived = bool("isPayoutReceived"),
            isInstallmentPaidThisMonth = bool("isInstallmentPaidThisMonth")
        )
    }

    private fun DocumentSnapshot.toAlarm(): AlarmEntity? {
        val title = getString("title") ?: return null
        return AlarmEntity(
            id = lng("id"), title = title, timeInMillis = lng("timeInMillis"),
            ringtoneUri = str("ringtoneUri"), isEnabled = bool("isEnabled", true),
            label = str("label"), createdViaAi = bool("createdViaAi"), createdAt = lng("createdAt")
        )
    }

    private fun DocumentSnapshot.toWorkNote(): WorkNoteEntity? {
        val title = getString("title") ?: return null
        return WorkNoteEntity(
            id = lng("id"), title = title, content = str("content"),
            reminderTime = getLong("reminderTime"), isDone = bool("isDone"),
            updatedAt = lng("updatedAt")
        )
    }

    private fun DocumentSnapshot.toFinancialItem(): FinancialItemEntity? {
        val title = getString("title") ?: return null
        return FinancialItemEntity(
            id = lng("id"), type = str("type", "BILL"), title = title, amount = dbl("amount"),
            totalPrice = dbl("totalPrice"), downPayment = dbl("downPayment"),
            monthlyAmount = dbl("monthlyAmount"), remaining = dbl("remaining"),
            dueDate = lng("dueDate"), accountNumber = str("accountNumber"),
            billNumber = str("billNumber"), seller = str("seller"),
            paymentMethod = str("paymentMethod"), recurring = bool("recurring"),
            isPaid = bool("isPaid"), note = str("note"),
            linkedReminderId = getLong("linkedReminderId"), createdAt = lng("createdAt")
        )
    }

    private fun DocumentSnapshot.toHabit(): HabitEntity? {
        val title = getString("title") ?: return null
        return HabitEntity(
            id = lng("id"), title = title, emoji = str("emoji", "✅"), note = str("note"),
            isArchived = bool("isArchived"), createdAt = lng("createdAt")
        )
    }

    private fun DocumentSnapshot.toHabitLog(): HabitLogEntity? {
        val habitId = getLong("habitId") ?: return null
        return HabitLogEntity(
            id = lng("id"), habitId = habitId, dayStart = lng("dayStart"),
            completedAt = lng("completedAt")
        )
    }
}
