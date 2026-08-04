package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String = "",
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
