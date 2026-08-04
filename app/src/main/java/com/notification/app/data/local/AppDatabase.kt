package com.notification.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.notification.app.data.local.dao.*
import com.notification.app.data.local.entities.*

@Database(
    entities = [
        ReminderEntity::class,
        PersonEntity::class,
        LedgerTransactionEntity::class,
        Gam3iyaEntity::class,
        Gam3iyaMemberEntity::class,
        AlarmEntity::class,
        WorkNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun personLedgerDao(): PersonLedgerDao
    abstract fun gam3iyaDao(): Gam3iyaDao
    abstract fun alarmDao(): AlarmDao
    abstract fun workNoteDao(): WorkNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
