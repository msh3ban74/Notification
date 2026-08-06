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
    val createdAt: Long = System.currentTimeMillis(),
    // Alarm engine sprint — per-alarm behaviour, all added via Migration
    // 6→7 (additive, safe defaults matching the pre-existing behaviour).
    val vibrate: Boolean = true,
    val flashlight: Boolean = false,
    val volumePercent: Int = 100,         // 0-100, applied to the alarm stream (loudest by default)
    val snoozeMinutes: Int = 10,
    val autoStopMinutes: Int = 5,         // stop ringing if untouched; 0 = never
    /** Comma-separated weekday numbers (Calendar.DAY_OF_WEEK 1=Sun…7=Sat)
     *  for a repeating alarm; empty = fires once. */
    val repeatDays: String = ""
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
