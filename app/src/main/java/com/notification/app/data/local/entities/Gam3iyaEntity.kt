package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sprint 4 — professional Gam3iya (rotating savings) management.
 *
 * A single row models EITHER:
 *   • MANAGER mode  — I organize the gam3iya (members live in
 *     `gam3iya_members`, payments in `gam3iya_payments`).
 *   • PARTICIPANT mode — I only take part in someone else's gam3iya; the
 *     organizer + my own turn/installments are stored on this row and my
 *     payments are logged in `gam3iya_payments` with memberId = 0.
 *
 * Every column added after the original v1 baseline is nullable-free with a
 * default that reproduces prior behaviour, so migration 7→8 is purely
 * additive and existing rows keep working.
 */
@Entity(tableName = "gam3iyas")
data class Gam3iyaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val monthlyInstallment: Double,
    val membersCount: Int,
    val startDate: Long,
    val payoutDayOfMonth: Int = 1,
    val note: String = "",
    // ── Sprint 4 additions ────────────────────────────────────────────
    val mode: String = "MANAGER",            // MANAGER | PARTICIPANT
    val description: String = "",
    val currency: String = "EGP",
    val collectionTime: String = "",         // "HH:mm", empty = none
    val durationMonths: Int = 0,             // 0 = auto-calc from amount
    val status: String = "ACTIVE",           // ACTIVE | COMPLETED | ARCHIVED
    val photoUri: String = "",
    val reminderEnabled: Boolean = true,
    val reminderDaysBefore: Int = 1,
    val alarmEnabled: Boolean = false,
    val createdAt: Long = 0,
    // ── PARTICIPANT-mode fields (unused in MANAGER mode) ──────────────
    val organizerName: String = "",
    val organizerPhone: String = "",
    val organizerWhatsapp: String = "",
    val organizerEmail: String = "",
    val myInstallmentAmount: Double = 0.0,
    val myTurnNumber: Int = 0,
    val myCollectionDate: Long = 0,
    val myPaidInstallments: Int = 0
)

@Entity(tableName = "gam3iya_members")
data class Gam3iyaMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gam3iyaId: Long,
    val memberName: String,
    val turnMonth: Int, // 1st month, 2nd month...
    val payoutDate: Long,               // this member's collection date
    val isPayoutReceived: Boolean = false,      // collected?
    val isInstallmentPaidThisMonth: Boolean = false, // paid this month?
    // ── Sprint 4 additions ────────────────────────────────────────────
    val photoUri: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val address: String = "",
    val nationalId: String = "",
    val installmentAmount: Double = 0.0,   // 0 = use gam3iya monthlyInstallment
    val isLate: Boolean = false,
    val notes: String = ""
)

/**
 * One logged payment or collection event — the audit trail behind
 * "Total Paid", "Payment History" and the late calculations.
 */
@Entity(tableName = "gam3iya_payments")
data class Gam3iyaPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gam3iyaId: Long,
    val memberId: Long = 0,          // 0 = participant-self (PARTICIPANT mode)
    val monthIndex: Int = 0,         // 1-based month this payment covers
    val amount: Double = 0.0,
    val date: Long = 0,
    val type: String = "INSTALLMENT", // INSTALLMENT | COLLECTION
    val note: String = ""
)

/**
 * A file the user attached — a receipt image/PDF, a member photo, or any
 * document. memberId = 0 means it belongs to the gam3iya itself.
 */
@Entity(tableName = "gam3iya_attachments")
data class Gam3iyaAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gam3iyaId: Long,
    val memberId: Long = 0,
    val uri: String,
    val kind: String = "RECEIPT",    // RECEIPT | PHOTO | DOC
    val label: String = "",
    val createdAt: Long = 0
)
