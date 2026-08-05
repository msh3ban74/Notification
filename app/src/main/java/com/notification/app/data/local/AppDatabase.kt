package com.notification.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        WorkNoteEntity::class,
        FinancialItemEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun personLedgerDao(): PersonLedgerDao
    abstract fun gam3iyaDao(): Gam3iyaDao
    abstract fun alarmDao(): AlarmDao
    abstract fun workNoteDao(): WorkNoteDao
    abstract fun financialDao(): FinancialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Final Product sprint — Migration 1→2 adds the rich Smart Task
         * columns and the person WhatsApp column WITHOUT losing data
         * (plain additive ALTER TABLEs with matching defaults). The
         * column types/defaults mirror the entities exactly so Room's
         * post-migration schema validation passes.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN checklist TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN progress INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN location TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN whatsapp TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration 2→3 — Phase B: the financial_items table (bills /
         * installments / subscriptions). CREATE TABLE only, so no existing
         * data is touched. Column types/defaults mirror FinancialItemEntity.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS financial_items (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL DEFAULT 0,
                        totalPrice REAL NOT NULL DEFAULT 0,
                        downPayment REAL NOT NULL DEFAULT 0,
                        monthlyAmount REAL NOT NULL DEFAULT 0,
                        remaining REAL NOT NULL DEFAULT 0,
                        dueDate INTEGER NOT NULL DEFAULT 0,
                        accountNumber TEXT NOT NULL DEFAULT '',
                        billNumber TEXT NOT NULL DEFAULT '',
                        seller TEXT NOT NULL DEFAULT '',
                        paymentMethod TEXT NOT NULL DEFAULT '',
                        recurring INTEGER NOT NULL DEFAULT 0,
                        isPaid INTEGER NOT NULL DEFAULT 0,
                        note TEXT NOT NULL DEFAULT '',
                        linkedReminderId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notification_app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // Safety net only — real migrations above preserve data.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
