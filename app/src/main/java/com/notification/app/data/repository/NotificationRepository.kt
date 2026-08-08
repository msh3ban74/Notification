package com.notification.app.data.repository

import androidx.room.withTransaction
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.local.entities.*
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.model.LedgerTransactionType
import com.notification.app.domain.model.ReminderCategory
import kotlinx.coroutines.flow.Flow
import java.util.*

class NotificationRepository(private val db: AppDatabase) {

    // Reminders
    val allReminders: Flow<List<ReminderEntity>> = db.reminderDao().getAllReminders()
    val pendingReminders: Flow<List<ReminderEntity>> = db.reminderDao().getPendingReminders()

    suspend fun insertReminder(reminder: ReminderEntity): Long = db.reminderDao().insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = db.reminderDao().updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = db.reminderDao().deleteReminder(reminder)
    suspend fun deleteReminderById(id: Long) = db.reminderDao().deleteReminderById(id)
    suspend fun getReminderById(id: Long): ReminderEntity? = db.reminderDao().getReminderById(id)

    // Ledger & Person
    val allPersons: Flow<List<PersonEntity>> = db.personLedgerDao().getAllPersons()
    val allTransactions: Flow<List<LedgerTransactionEntity>> = db.personLedgerDao().getAllTransactions()

    fun getTransactionsForPerson(personId: Long): Flow<List<LedgerTransactionEntity>> =
        db.personLedgerDao().getTransactionsForPerson(personId)

    suspend fun insertPerson(person: PersonEntity): Long = db.personLedgerDao().insertPerson(person)
    suspend fun deletePerson(person: PersonEntity) = db.personLedgerDao().deletePerson(person)
    suspend fun getPersonById(id: Long): PersonEntity? = db.personLedgerDao().getPersonById(id)

    suspend fun insertLedgerTransaction(tx: LedgerTransactionEntity): Long =
        db.personLedgerDao().insertTransaction(tx)

    suspend fun updateLedgerTransaction(tx: LedgerTransactionEntity) =
        db.personLedgerDao().updateTransaction(tx)

    suspend fun deleteLedgerTransaction(tx: LedgerTransactionEntity) =
        db.personLedgerDao().deleteTransaction(tx)

    // ── Sprint 5 — person edit + cascade delete + attachments ─────────
    suspend fun updatePerson(person: PersonEntity) = db.personLedgerDao().updatePerson(person)
    suspend fun deletePersonCascade(person: PersonEntity) = db.withTransaction {
        // Atomic: a crash mid-delete can never leave a person's transactions
        // or attachments orphaned (or vice-versa).
        db.personLedgerDao().deleteTransactionsForPerson(person.id)
        db.personLedgerDao().deleteLedgerAttachmentsForPerson(person.id)
        db.personLedgerDao().deletePerson(person)
    }
    val allLedgerAttachments: Flow<List<LedgerAttachmentEntity>> = db.personLedgerDao().getAllLedgerAttachments()
    fun getAttachmentsForPerson(personId: Long): Flow<List<LedgerAttachmentEntity>> =
        db.personLedgerDao().getAttachmentsForPerson(personId)
    suspend fun insertLedgerAttachment(a: LedgerAttachmentEntity): Long =
        db.personLedgerDao().insertLedgerAttachment(a)
    suspend fun deleteLedgerAttachment(a: LedgerAttachmentEntity) =
        db.personLedgerDao().deleteLedgerAttachment(a)

    // Gam3iya
    val allGam3iyas: Flow<List<Gam3iyaEntity>> = db.gam3iyaDao().getAllGam3iyas()

    fun getMembersForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaMemberEntity>> =
        db.gam3iyaDao().getMembersForGam3iya(gam3iyaId)

    // Sprint 6 — Executive Dashboard (read-only observation).
    val allGam3iyaMembers: Flow<List<Gam3iyaMemberEntity>> = db.gam3iyaDao().getAllMembers()

    suspend fun getGam3iyaById(id: Long): Gam3iyaEntity? = db.gam3iyaDao().getGam3iyaById(id)

    suspend fun createGam3iyaWithMembers(
        gam3iya: Gam3iyaEntity,
        memberNamesWithTurns: List<Pair<String, Int>>
    ): Long = db.withTransaction {
        // Atomic: the gam3iya and its members are inserted together, so a
        // crash can't leave a memberless gam3iya.
        val gam3iyaId = db.gam3iyaDao().insertGam3iya(gam3iya)
        val members = memberNamesWithTurns.map { (name, turn) ->
            val payoutDate = Gam3iyaCalculator.calculateMemberPayoutDate(gam3iya.startDate, turn)
            Gam3iyaMemberEntity(
                gam3iyaId = gam3iyaId,
                memberName = name,
                turnMonth = turn,
                payoutDate = payoutDate
            )
        }
        db.gam3iyaDao().insertMembers(members)
        gam3iyaId
    }

    suspend fun updateGam3iyaMember(member: Gam3iyaMemberEntity) =
        db.gam3iyaDao().updateMember(member)

    suspend fun deleteGam3iya(gam3iya: Gam3iyaEntity) = db.withTransaction {
        // Cascade — the tables have no FK, so clear children explicitly, and
        // atomically so a crash can't half-delete a gam3iya.
        db.gam3iyaDao().deleteMembersForGam3iya(gam3iya.id)
        db.gam3iyaDao().deletePaymentsForGam3iya(gam3iya.id)
        db.gam3iyaDao().deleteAttachmentsForGam3iya(gam3iya.id)
        db.gam3iyaDao().deleteGam3iya(gam3iya)
    }

    // ── Sprint 4 — professional Gam3iya management ─────────────────────
    suspend fun insertGam3iya(gam3iya: Gam3iyaEntity): Long = db.gam3iyaDao().insertGam3iya(gam3iya)
    suspend fun updateGam3iya(gam3iya: Gam3iyaEntity) = db.gam3iyaDao().updateGam3iya(gam3iya)
    suspend fun insertGam3iyaMember(member: Gam3iyaMemberEntity): Long =
        db.gam3iyaDao().insertMember(member)
    suspend fun deleteGam3iyaMember(member: Gam3iyaMemberEntity) = db.gam3iyaDao().deleteMember(member)
    suspend fun getGam3iyaMemberById(id: Long): Gam3iyaMemberEntity? = db.gam3iyaDao().getMemberById(id)

    val allGam3iyaPayments: Flow<List<Gam3iyaPaymentEntity>> = db.gam3iyaDao().getAllPayments()
    fun getPaymentsForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaPaymentEntity>> =
        db.gam3iyaDao().getPaymentsForGam3iya(gam3iyaId)
    suspend fun insertGam3iyaPayment(payment: Gam3iyaPaymentEntity): Long =
        db.gam3iyaDao().insertPayment(payment)
    suspend fun deleteGam3iyaPayment(payment: Gam3iyaPaymentEntity) = db.gam3iyaDao().deletePayment(payment)

    val allGam3iyaAttachments: Flow<List<Gam3iyaAttachmentEntity>> = db.gam3iyaDao().getAllAttachments()
    fun getAttachmentsForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaAttachmentEntity>> =
        db.gam3iyaDao().getAttachmentsForGam3iya(gam3iyaId)
    suspend fun insertGam3iyaAttachment(a: Gam3iyaAttachmentEntity): Long =
        db.gam3iyaDao().insertAttachment(a)
    suspend fun deleteGam3iyaAttachment(a: Gam3iyaAttachmentEntity) = db.gam3iyaDao().deleteAttachment(a)

    // Alarms
    val allAlarms: Flow<List<AlarmEntity>> = db.alarmDao().getAllAlarms()

    suspend fun getActiveAlarms(): List<AlarmEntity> = db.alarmDao().getActiveAlarms()
    suspend fun getAlarmById(id: Long): AlarmEntity? = db.alarmDao().getAlarmById(id)
    suspend fun insertAlarm(alarm: AlarmEntity): Long = db.alarmDao().insertAlarm(alarm)
    suspend fun updateAlarm(alarm: AlarmEntity) = db.alarmDao().updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: AlarmEntity) = db.alarmDao().deleteAlarm(alarm)

    // Phase B — financial items (bills / installments / subscriptions).
    val allFinancialItems: Flow<List<FinancialItemEntity>> = db.financialDao().getAll()
    suspend fun insertFinancialItem(item: FinancialItemEntity): Long = db.financialDao().insert(item)
    suspend fun updateFinancialItem(item: FinancialItemEntity) = db.financialDao().update(item)
    suspend fun deleteFinancialItem(item: FinancialItemEntity) {
        db.financialDao().deletePaymentsForItem(item.id)
        db.financialDao().deleteAttachmentsForItem(item.id)
        db.financialDao().delete(item)
    }
    suspend fun getFinancialItemById(id: Long): FinancialItemEntity? = db.financialDao().getById(id)

    // ── Sprint 6 — installment payment history + attachments ──────────
    val allFinancialPayments: Flow<List<FinancialPaymentEntity>> = db.financialDao().getAllFinancialPayments()
    fun getPaymentsForFinancialItem(itemId: Long): Flow<List<FinancialPaymentEntity>> =
        db.financialDao().getPaymentsForItem(itemId)
    suspend fun insertFinancialPayment(p: FinancialPaymentEntity): Long = db.financialDao().insertPayment(p)
    suspend fun deleteFinancialPayment(p: FinancialPaymentEntity) = db.financialDao().deletePayment(p)

    val allFinancialAttachments: Flow<List<FinancialAttachmentEntity>> = db.financialDao().getAllFinancialAttachments()
    fun getAttachmentsForFinancialItem(itemId: Long): Flow<List<FinancialAttachmentEntity>> =
        db.financialDao().getAttachmentsForItem(itemId)
    suspend fun insertFinancialAttachment(a: FinancialAttachmentEntity): Long = db.financialDao().insertAttachment(a)
    suspend fun deleteFinancialAttachment(a: FinancialAttachmentEntity) = db.financialDao().deleteAttachment(a)

    // Phase C — habit engine. A habit is "done today" when a log row
    // exists for today's local-midnight dayStart; toggling deletes or
    // inserts that row (unique index keeps it one-per-day).
    val allHabits: Flow<List<HabitEntity>> = db.habitDao().getAllHabits()
    val allHabitLogs: Flow<List<HabitLogEntity>> = db.habitDao().getAllLogs()

    suspend fun insertHabit(habit: HabitEntity): Long = db.habitDao().insertHabit(habit)
    suspend fun updateHabit(habit: HabitEntity) = db.habitDao().updateHabit(habit)

    suspend fun deleteHabit(habit: HabitEntity) {
        db.habitDao().deleteLogsForHabit(habit.id)
        db.habitDao().deleteHabit(habit)
    }

    suspend fun setHabitDone(habitId: Long, dayStart: Long, done: Boolean) {
        if (done) {
            db.habitDao().insertLog(HabitLogEntity(habitId = habitId, dayStart = dayStart))
        } else {
            db.habitDao().deleteLog(habitId, dayStart)
        }
    }

    // Work Notes
    val allWorkNotes: Flow<List<WorkNoteEntity>> = db.workNoteDao().getAllNotes()

    suspend fun insertWorkNote(note: WorkNoteEntity): Long = db.workNoteDao().insertNote(note)
    suspend fun updateWorkNote(note: WorkNoteEntity) = db.workNoteDao().updateNote(note)
    suspend fun deleteWorkNote(note: WorkNoteEntity) = db.workNoteDao().deleteNote(note)

    // General memory — free-form facts Rafeeq keeps for the user.
    val allMemories: Flow<List<MemoryEntity>> = db.memoryDao().getAllMemories()
    suspend fun insertMemory(memory: MemoryEntity): Long = db.memoryDao().insertMemory(memory)
    suspend fun searchMemories(query: String): List<MemoryEntity> = db.memoryDao().searchMemories(query)
    suspend fun getAllMemoriesOnce(): List<MemoryEntity> = db.memoryDao().getAllMemoriesOnce()
    suspend fun deleteMemory(memory: MemoryEntity) = db.memoryDao().deleteMemory(memory)
    suspend fun deleteMemoryById(id: Long) = db.memoryDao().deleteMemoryById(id)
}
