package com.notification.app.data.local.dao

import androidx.room.*
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.WorkNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY timeInMillis ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getActiveAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)
}

@Dao
interface WorkNoteDao {
    @Query("SELECT * FROM work_notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<WorkNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: WorkNoteEntity): Long

    @Update
    suspend fun updateNote(note: WorkNoteEntity)

    @Delete
    suspend fun deleteNote(note: WorkNoteEntity)
}
