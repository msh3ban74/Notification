package com.notification.app.data.local.dao

import androidx.room.*
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

    @Delete
    suspend fun deleteTransaction(transaction: LedgerTransactionEntity)
}
