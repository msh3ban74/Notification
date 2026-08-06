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
    val createdAt: Long = System.currentTimeMillis()
)
