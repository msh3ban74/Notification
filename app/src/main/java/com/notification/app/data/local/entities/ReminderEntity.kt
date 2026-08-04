package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val dueDate: Long,
    val category: String,
    val recurrence: String = "NONE",
    val preAlerts: String = "ONE_DAY,ONE_HOUR", // Comma-separated PreAlertOption names
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
