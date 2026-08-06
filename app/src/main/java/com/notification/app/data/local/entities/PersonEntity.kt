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
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ledger_transactions")
data class LedgerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: String, // GAVE_THEM, THEY_GAVE_BACK, THEY_GAVE_ME, I_GAVE_BACK
    val amount: Double,
    val date: Long,
    val note: String = "",
    val linkedReminderId: Long? = null
)
