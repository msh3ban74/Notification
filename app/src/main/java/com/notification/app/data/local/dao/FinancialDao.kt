package com.notification.app.data.local.dao

import androidx.room.*
import com.notification.app.data.local.entities.FinancialAttachmentEntity
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.FinancialPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financial_items ORDER BY dueDate ASC")
    fun getAll(): Flow<List<FinancialItemEntity>>

    @Query("SELECT * FROM financial_items WHERE id = :id")
    suspend fun getById(id: Long): FinancialItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FinancialItemEntity): Long

    @Update
    suspend fun update(item: FinancialItemEntity)

    @Delete
    suspend fun delete(item: FinancialItemEntity)

    // ── Sprint 6 — payment history ────────────────────────────────────
    @Query("SELECT * FROM financial_payments WHERE financialItemId = :itemId ORDER BY date DESC")
    fun getPaymentsForItem(itemId: Long): Flow<List<FinancialPaymentEntity>>

    @Query("SELECT * FROM financial_payments ORDER BY date DESC")
    fun getAllFinancialPayments(): Flow<List<FinancialPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(p: FinancialPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(list: List<FinancialPaymentEntity>)

    @Delete
    suspend fun deletePayment(p: FinancialPaymentEntity)

    @Query("DELETE FROM financial_payments WHERE financialItemId = :itemId")
    suspend fun deletePaymentsForItem(itemId: Long)

    // ── Sprint 6 — attachments (invoice / warranty / receipt) ─────────
    @Query("SELECT * FROM financial_attachments WHERE financialItemId = :itemId ORDER BY createdAt DESC")
    fun getAttachmentsForItem(itemId: Long): Flow<List<FinancialAttachmentEntity>>

    @Query("SELECT * FROM financial_attachments ORDER BY createdAt DESC")
    fun getAllFinancialAttachments(): Flow<List<FinancialAttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(a: FinancialAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(list: List<FinancialAttachmentEntity>)

    @Delete
    suspend fun deleteAttachment(a: FinancialAttachmentEntity)

    @Query("DELETE FROM financial_attachments WHERE financialItemId = :itemId")
    suspend fun deleteAttachmentsForItem(itemId: Long)
}
