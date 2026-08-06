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
        LedgerAttachmentEntity::class,
        Gam3iyaEntity::class,
        Gam3iyaMemberEntity::class,
        Gam3iyaPaymentEntity::class,
        Gam3iyaAttachmentEntity::class,
        AlarmEntity::class,
        WorkNoteEntity::class,
        FinancialItemEntity::class,
        HabitEntity::class,
        HabitLogEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun personLedgerDao(): PersonLedgerDao
    abstract fun gam3iyaDao(): Gam3iyaDao
    abstract fun alarmDao(): AlarmDao
    abstract fun workNoteDao(): WorkNoteDao
    abstract fun financialDao(): FinancialDao
    abstract fun habitDao(): HabitDao

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

        /**
         * Migration 3→4 — Phase C: the habit engine tables. CREATE TABLE
         * only (plus the unique per-day log index Room expects), so no
         * existing data is touched. Mirrors HabitEntity / HabitLogEntity.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '✅',
                        note TEXT NOT NULL DEFAULT '',
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habit_logs (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        habitId INTEGER NOT NULL,
                        dayStart INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_habit_logs_habitId_dayStart ON habit_logs(habitId, dayStart)"
                )
            }
        }

        /**
         * Migration 4→5 — Phase D: CRUD extras on reminders (pin +
         * archive). Plain additive ALTER TABLEs, defaults mirror the
         * entity, no data touched.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration 5→6 — Product Completion: full contact profile on
         * ledger persons. Plain additive ALTER TABLEs, defaults mirror
         * the entity, no data touched.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE persons ADD COLUMN email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration 6→7 — Alarm engine: per-alarm behaviour columns.
         * Additive ALTER TABLEs with defaults that reproduce the old
         * always-vibrate / no-flash / 80% / 10-min-snooze behaviour.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN vibrate INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE alarms ADD COLUMN flashlight INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alarms ADD COLUMN volumePercent INTEGER NOT NULL DEFAULT 80")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeMinutes INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE alarms ADD COLUMN autoStopMinutes INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE alarms ADD COLUMN repeatDays TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration 7→8 — Sprint 4: professional Gam3iya. Additive ALTER
         * TABLEs on the two existing gam3iya tables (defaults reproduce the
         * old MANAGER-only, no-photo behaviour) plus two new CREATE TABLEs
         * for payment history and attachments. No existing data touched.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // gam3iyas — new columns
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN mode TEXT NOT NULL DEFAULT 'MANAGER'")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN currency TEXT NOT NULL DEFAULT 'EGP'")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN collectionTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN durationMonths INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN reminderDaysBefore INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN alarmEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN organizerName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN organizerPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN organizerWhatsapp TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN organizerEmail TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN myInstallmentAmount REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN myTurnNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN myCollectionDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iyas ADD COLUMN myPaidInstallments INTEGER NOT NULL DEFAULT 0")
                // gam3iya_members — new columns
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN whatsapp TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN email TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN nationalId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN installmentAmount REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN isLate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE gam3iya_members ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                // new tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gam3iya_payments (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        gam3iyaId INTEGER NOT NULL,
                        memberId INTEGER NOT NULL DEFAULT 0,
                        monthIndex INTEGER NOT NULL DEFAULT 0,
                        amount REAL NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL DEFAULT 0,
                        type TEXT NOT NULL DEFAULT 'INSTALLMENT',
                        note TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS gam3iya_attachments (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        gam3iyaId INTEGER NOT NULL,
                        memberId INTEGER NOT NULL DEFAULT 0,
                        uri TEXT NOT NULL,
                        kind TEXT NOT NULL DEFAULT 'RECEIPT',
                        label TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration 8→9 — Sprint 5: professional Debts & Ledger. Additive
         * ALTER TABLEs on persons + ledger_transactions (defaults reproduce
         * the previous behaviour) plus a new ledger_attachments table. No
         * existing data touched.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE persons ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN company TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN nationalId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE persons ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'EGP'")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN reason TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN dueDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN paymentType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN recurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ledger_attachments (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        personId INTEGER NOT NULL,
                        transactionId INTEGER NOT NULL DEFAULT 0,
                        uri TEXT NOT NULL,
                        kind TEXT NOT NULL DEFAULT 'IMAGE',
                        label TEXT NOT NULL DEFAULT '',
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
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9
                )
                // Safety net only — real migrations above preserve data.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
