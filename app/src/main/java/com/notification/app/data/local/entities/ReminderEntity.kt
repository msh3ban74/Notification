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
    val createdAt: Long = System.currentTimeMillis(),
    // Final Product sprint (Phase A) — rich Smart Task fields. All added
    // via Room Migration 1→2 (see AppDatabase) so no data is lost.
    // checklist: items joined by "||", each "1:text" (done) or "0:text".
    val checklist: String = "",
    val tags: String = "",          // comma-separated
    val progress: Int = 0,          // 0..100
    val location: String = "",
    // Final Product sprint (Phase D) — CRUD extras, added via Migration
    // 4→5. Pinned items always sort first; archived items are hidden
    // from the main list (soft delete).
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)
