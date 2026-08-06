package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Final Product sprint (Phase B) — one flexible entity covering the three
 * money-tracking Smart Items so no duplicate logic or tables are needed:
 *
 *  - BILL         → company / amount / account / bill no / due / paid
 *  - INSTALLMENT  → item / seller / total / down / monthly / remaining / next due
 *  - SUBSCRIPTION → service / monthly amount / renewal date / recurring
 *
 * `type` selects which fields matter; unused fields stay at their
 * defaults. Added via Room Migration 2→3 (CREATE TABLE — no data loss).
 */
@Entity(tableName = "financial_items")
data class FinancialItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                 // FinancialType name
    val title: String,                // company / item / service
    val amount: Double = 0.0,         // bill amount / monthly amount
    val totalPrice: Double = 0.0,     // installments
    val downPayment: Double = 0.0,    // installments
    val monthlyAmount: Double = 0.0,  // installments
    val remaining: Double = 0.0,      // installments (auto or manual)
    val dueDate: Long = 0L,           // next due / renewal
    val accountNumber: String = "",
    val billNumber: String = "",
    val seller: String = "",
    val paymentMethod: String = "",
    val recurring: Boolean = false,
    val isPaid: Boolean = false,
    val note: String = "",
    val linkedReminderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Sprint 6 (Installments) — full installment profile. Added via
    // Migration 9→10 (additive; defaults reproduce the prior blank data).
    val brand: String = "",
    val store: String = "",
    val storePhone: String = "",
    val storeWhatsapp: String = "",
    val category: String = "",
    val currency: String = "EGP",
    val interestAmount: Double = 0.0,   // total interest included in totalPrice
    val purchaseDate: Long = 0L,
    val warranty: String = "",          // warranty note / expiry text
    val tags: String = "",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val paidInstallments: Int = 0       // count of installments paid so far
)

/**
 * Sprint 6 — one recorded payment against a financial item (installment),
 * the audit trail behind "Total Paid", "Paid installments", "Interest Paid"
 * and the payment-history list.
 */
@Entity(tableName = "financial_payments")
data class FinancialPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val financialItemId: Long,
    val amount: Double = 0.0,
    val date: Long = 0,
    val type: String = "PARTIAL",       // PARTIAL | FULL | ADVANCE | EXTRA | RECURRING
    val note: String = ""
)

/**
 * Sprint 6 — an attachment on a financial item: invoice, warranty card,
 * receipt image or any document.
 */
@Entity(tableName = "financial_attachments")
data class FinancialAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val financialItemId: Long,
    val uri: String,
    val kind: String = "RECEIPT",       // INVOICE | WARRANTY | RECEIPT | DOC | IMAGE
    val label: String = "",
    val createdAt: Long = 0
)
