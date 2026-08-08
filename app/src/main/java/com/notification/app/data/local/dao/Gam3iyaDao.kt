package com.notification.app.data.local.dao

import androidx.room.*
import com.notification.app.data.local.entities.Gam3iyaAttachmentEntity
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.Gam3iyaPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface Gam3iyaDao {
    @Query("SELECT * FROM gam3iyas ORDER BY startDate DESC")
    fun getAllGam3iyas(): Flow<List<Gam3iyaEntity>>

    @Query("SELECT * FROM gam3iyas WHERE id = :id")
    suspend fun getGam3iyaById(id: Long): Gam3iyaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGam3iya(gam3iya: Gam3iyaEntity): Long

    @Delete
    suspend fun deleteGam3iya(gam3iya: Gam3iyaEntity)

    @Query("SELECT * FROM gam3iya_members WHERE gam3iyaId = :gam3iyaId ORDER BY turnMonth ASC")
    fun getMembersForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaMemberEntity>>

    // Sprint 6 — Executive Dashboard. Read-only query so the dashboard's
    // Gam3iya payment widget can observe upcoming payouts; no logic change.
    @Query("SELECT * FROM gam3iya_members ORDER BY payoutDate ASC")
    fun getAllMembers(): Flow<List<Gam3iyaMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Gam3iyaMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Gam3iyaMemberEntity>)

    @Update
    suspend fun updateMember(member: Gam3iyaMemberEntity)

    @Delete
    suspend fun deleteMember(member: Gam3iyaMemberEntity)

    // ── Sprint 4 — the gam3iya row itself is now editable ─────────────
    @Update
    suspend fun updateGam3iya(gam3iya: Gam3iyaEntity)

    @Query("SELECT * FROM gam3iya_members WHERE id = :id")
    suspend fun getMemberById(id: Long): Gam3iyaMemberEntity?

    @Query("DELETE FROM gam3iya_members WHERE gam3iyaId = :gam3iyaId")
    suspend fun deleteMembersForGam3iya(gam3iyaId: Long)

    // ── Sprint 4 — payment history ────────────────────────────────────
    @Query("SELECT * FROM gam3iya_payments WHERE gam3iyaId = :gam3iyaId ORDER BY date DESC")
    fun getPaymentsForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaPaymentEntity>>

    @Query("SELECT * FROM gam3iya_payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Gam3iyaPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Gam3iyaPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Gam3iyaPaymentEntity>)

    @Delete
    suspend fun deletePayment(payment: Gam3iyaPaymentEntity)

    @Query("DELETE FROM gam3iya_payments WHERE gam3iyaId = :gam3iyaId")
    suspend fun deletePaymentsForGam3iya(gam3iyaId: Long)

    // ── Sprint 4 — attachments / receipts ─────────────────────────────
    @Query("SELECT * FROM gam3iya_attachments WHERE gam3iyaId = :gam3iyaId ORDER BY createdAt DESC")
    fun getAttachmentsForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaAttachmentEntity>>

    @Query("SELECT * FROM gam3iya_attachments ORDER BY createdAt DESC")
    fun getAllAttachments(): Flow<List<Gam3iyaAttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Gam3iyaAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<Gam3iyaAttachmentEntity>)

    @Delete
    suspend fun deleteAttachment(attachment: Gam3iyaAttachmentEntity)

    @Query("DELETE FROM gam3iya_attachments WHERE gam3iyaId = :gam3iyaId")
    suspend fun deleteAttachmentsForGam3iya(gam3iyaId: Long)
}
