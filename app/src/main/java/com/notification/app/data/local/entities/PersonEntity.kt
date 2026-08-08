package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String = "",
    // Final Product sprint (Phase A) — WhatsApp number for the one-tap
    // reminder action. Added via Migration 1→2 (no data loss).
    val whatsapp: String = "",
    // Product Completion sprint — full contact profile, added via
    // Migration 5→6 (additive, no data loss). category groups people
    // (family / friends / work / clients...).
    val email: String = "",
    val address: String = "",
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Sprint 5 (Debts & Ledger) — full person profile. Added via Migration
    // 8→9 (additive, defaults reproduce the previous blank profile).
    val photoUri: String = "",
    val company: String = "",
    val nationalId: String = "",
    val notes: String = "",
    val tags: String = "",           // comma-separated
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "ledger_transactions")
data class LedgerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: String, // GAVE_THEM, THEY_GAVE_BACK, THEY_GAVE_ME, I_GAVE_BACK
    val amount: Double,
    val date: Long,
    val note: String = "",
    val linkedReminderId: Long? = null,
    // Sprint 5 additions (Migration 8→9).
    val currency: String = "EGP",
    val reason: String = "",
    val dueDate: Long = 0,
    val paymentType: String = "",    // PARTIAL | FULL | ADVANCE | SCHEDULED | RECURRING
    val recurring: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

/**
 * Sprint 5 — an attachment on a person or a specific ledger transaction:
 * a receipt image, a PDF/document, or a voice note. transactionId = 0 means
 * it belongs to the person (profile-level), not one transaction.
 */
@Entity(tableName = "ledger_attachments")
data class LedgerAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val transactionId: Long = 0,
    val uri: String,
    val kind: String = "IMAGE",      // IMAGE | PDF | DOC | AUDIO
    val label: String = "",
    val createdAt: Long = 0
)
