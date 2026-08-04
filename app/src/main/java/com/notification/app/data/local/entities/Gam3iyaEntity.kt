package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gam3iyas")
data class Gam3iyaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val monthlyInstallment: Double,
    val membersCount: Int,
    val startDate: Long,
    val payoutDayOfMonth: Int = 1,
    val note: String = ""
)

@Entity(tableName = "gam3iya_members")
data class Gam3iyaMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gam3iyaId: Long,
    val memberName: String,
    val turnMonth: Int, // 1st month, 2nd month...
    val payoutDate: Long,
    val isPayoutReceived: Boolean = false,
    val isInstallmentPaidThisMonth: Boolean = false
)
