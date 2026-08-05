package com.notification.app.data.repository

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

    // Gam3iya
    val allGam3iyas: Flow<List<Gam3iyaEntity>> = db.gam3iyaDao().getAllGam3iyas()

    fun getMembersForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaMemberEntity>> =
        db.gam3iyaDao().getMembersForGam3iya(gam3iyaId)

    suspend fun getGam3iyaById(id: Long): Gam3iyaEntity? = db.gam3iyaDao().getGam3iyaById(id)

    suspend fun createGam3iyaWithMembers(
        gam3iya: Gam3iyaEntity,
        memberNamesWithTurns: List<Pair<String, Int>>
    ): Long {
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
        return gam3iyaId
    }

    suspend fun updateGam3iyaMember(member: Gam3iyaMemberEntity) =
        db.gam3iyaDao().updateMember(member)

    suspend fun deleteGam3iya(gam3iya: Gam3iyaEntity) = db.gam3iyaDao().deleteGam3iya(gam3iya)

    // Alarms
    val allAlarms: Flow<List<AlarmEntity>> = db.alarmDao().getAllAlarms()

    suspend fun getActiveAlarms(): List<AlarmEntity> = db.alarmDao().getActiveAlarms()
    suspend fun getAlarmById(id: Long): AlarmEntity? = db.alarmDao().getAlarmById(id)
    suspend fun insertAlarm(alarm: AlarmEntity): Long = db.alarmDao().insertAlarm(alarm)
    suspend fun updateAlarm(alarm: AlarmEntity) = db.alarmDao().updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: AlarmEntity) = db.alarmDao().deleteAlarm(alarm)

    // Work Notes
    val allWorkNotes: Flow<List<WorkNoteEntity>> = db.workNoteDao().getAllNotes()

    suspend fun insertWorkNote(note: WorkNoteEntity): Long = db.workNoteDao().insertNote(note)
    suspend fun updateWorkNote(note: WorkNoteEntity) = db.workNoteDao().updateNote(note)
    suspend fun deleteWorkNote(note: WorkNoteEntity) = db.workNoteDao().deleteNote(note)
}
