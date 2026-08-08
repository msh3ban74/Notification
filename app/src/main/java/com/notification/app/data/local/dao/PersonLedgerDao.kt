package com.notification.app.data.local.dao

import androidx.room.*
import com.notification.app.data.local.entities.LedgerAttachmentEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonLedgerDao {
    @Query("SELECT * FROM persons ORDER BY name ASC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("SELECT * FROM ledger_transactions WHERE personId = :personId ORDER BY date ASC")
    fun getTransactionsForPerson(personId: Long): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<LedgerTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LedgerTransactionEntity): Long

    // Sprint 5 — edit flow for debts. Plain Room @Update on the EXISTING
    // entity (mirrors ReminderDao.updateReminder); no schema change.
    @Update
    suspend fun updateTransaction(transaction: LedgerTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: LedgerTransactionEntity)

    // ── Sprint 5 — person edit + cascade delete ───────────────────────
    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("DELETE FROM ledger_transactions WHERE personId = :personId")
    suspend fun deleteTransactionsForPerson(personId: Long)

    // ── Sprint 5 — attachments (receipts / docs / audio) ──────────────
    @Query("SELECT * FROM ledger_attachments WHERE personId = :personId ORDER BY createdAt DESC")
    fun getAttachmentsForPerson(personId: Long): Flow<List<LedgerAttachmentEntity>>

    @Query("SELECT * FROM ledger_attachments ORDER BY createdAt DESC")
    fun getAllLedgerAttachments(): Flow<List<LedgerAttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerAttachment(a: LedgerAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerAttachments(list: List<LedgerAttachmentEntity>)

    @Delete
    suspend fun deleteLedgerAttachment(a: LedgerAttachmentEntity)

    @Query("DELETE FROM ledger_attachments WHERE personId = :personId")
    suspend fun deleteLedgerAttachmentsForPerson(personId: Long)
}
