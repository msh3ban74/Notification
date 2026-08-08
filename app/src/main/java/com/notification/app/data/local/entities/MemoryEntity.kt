package com.notification.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A free-form memory — the heart of Rafeeq as a real personal assistant.
 * Unlike reminders/debts/alarms (which are typed and time-bound), a memory is
 * ANY fact the user wants Rafeeq to keep: a car plate, a clothing size, a
 * passport expiry, a router password, a doctor's name — anything. The user
 * tells Rafeeq in chat, and asks for it back whenever they need it.
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val label: String = "",
    val createdAt: Long = 0L
)
