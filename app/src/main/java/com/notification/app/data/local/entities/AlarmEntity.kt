package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeInMillis: Long,
    val ringtoneUri: String = "",
    val isEnabled: Boolean = true,
    val label: String = "",
    val createdViaAi: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "work_notes")
data class WorkNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val reminderTime: Long? = null,
    val isDone: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
