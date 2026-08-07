package com.notification.app.data.repository

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.local.entities.*
import com.notification.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Sprint 3 — production local Backup & Restore.
 *
 * A backup is a SINGLE portable file: a small JSON envelope whose payload
 * is GZIP-compressed and (when the Keystore is available) AES/GCM
 * encrypted. The envelope carries a version, a timestamp and a SHA-256
 * checksum of the uncompressed data, so restore can VERIFY integrity and
 * compatibility before touching the database.
 *
 * Envelope (plain-text JSON, safe to store anywhere via SAF):
 * {
 *   "magic": "RAFEEQ_BACKUP",
 *   "formatVersion": 1,
 *   "dbVersion": 7,
 *   "createdAt": <epoch millis>,
 *   "checksum": "<sha256 hex of inner JSON bytes>",
 *   "encrypted": true|false,
 *   "iv": "<base64 or null>",
 *   "payload": "<base64( [enc?] gzip(inner JSON bytes) )>"
 * }
 *
 * Everything runs off the main thread (callers use Dispatchers.IO); no
 * whole-file duplication beyond the in-memory JSON, which is tiny for a
 * personal database.
 */
class LocalBackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val prefs: UserPreferencesRepository
) {

    sealed class Result {
        data class Success(val count: Int) : Result()
        data class Failure(val reason: String) : Result()
    }

    /** A validated, not-yet-applied restore — lets the UI preview + confirm. */
    data class RestorePreview(
        val inner: JSONObject,
        val createdAt: Long,
        val counts: Map<String, Int>,
        val totalCount: Int
    )

    companion object {
        const val MAGIC = "RAFEEQ_BACKUP"
        const val FORMAT_VERSION = 1
        // A personal backup is kilobytes; 32 MB is a generous ceiling that
        // still stops a decompression-bomb / huge-file OOM on import.
        private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
        const val CURRENT_DB_VERSION = 7
        private const val KEY_ALIAS = "rafeeq_backup_key"
        private const val GCM_TAG_BITS = 128
    }

    // ── EXPORT ───────────────────────────────────────────────────────────

    suspend fun exportToUri(uri: Uri): Result {
        return try {
            val inner = buildInnerJson()
            val innerBytes = inner.toString().toByteArray(Charsets.UTF_8)
            val checksum = sha256Hex(innerBytes)
            val gz = gzip(innerBytes)

            val envelope = JSONObject()
                .put("magic", MAGIC)
                .put("formatVersion", FORMAT_VERSION)
                .put("dbVersion", CURRENT_DB_VERSION)
                .put("createdAt", inner.optLong("createdAt"))
                .put("checksum", checksum)

            val enc = tryEncrypt(gz)
            if (enc != null) {
                envelope.put("encrypted", true)
                envelope.put("iv", base64(enc.first))
                envelope.put("payload", base64(enc.second))
            } else {
                envelope.put("encrypted", false)
                envelope.put("iv", JSONObject.NULL)
                envelope.put("payload", base64(gz))
            }

            context.contentResolver.openOutputStream(uri, "w")?.use { out ->
                out.write(envelope.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return Result.Failure("Could not open the chosen file for writing.")

            Result.Success(inner.optInt("totalCount"))
        } catch (e: SecurityException) {
            Result.Failure("PERMISSION — storage access was denied.")
        } catch (e: java.io.IOException) {
            Result.Failure("IO — couldn't write the file (storage full or unavailable).")
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Backup failed.")
        }
    }

    // ── READ + VALIDATE (no DB writes yet) ────────────────────────────────

    suspend fun readAndValidate(uri: Uri): Result {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                // Cap the read so a crafted huge/looping file can't OOM the
                // app (bounded manual read — readNBytes needs API 33+).
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(64 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BACKUP_BYTES) {
                        return Result.Failure("TOO LARGE — this file exceeds the backup size limit.")
                    }
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            } ?: return Result.Failure("Could not open the selected file.")
            val envelope = try {
                JSONObject(String(raw, Charsets.UTF_8))
            } catch (e: Exception) {
                return Result.Failure("CORRUPT — this is not a valid Rafeeq backup file.")
            }
            if (envelope.optString("magic") != MAGIC) {
                return Result.Failure("INVALID — this file isn't a Rafeeq backup.")
            }
            if (envelope.optInt("formatVersion") > FORMAT_VERSION) {
                return Result.Failure("VERSION — this backup is from a newer app version. Update Rafeeq first.")
            }

            val payload = unbase64(envelope.getString("payload"))
            val gz = if (envelope.optBoolean("encrypted")) {
                val iv = unbase64(envelope.getString("iv"))
                tryDecrypt(iv, payload) ?: return Result.Failure(
                    "ENCRYPTED — this backup was encrypted on another device and can't be opened here."
                )
            } else payload

            val innerBytes = gunzip(gz)
            val checksum = sha256Hex(innerBytes)
            if (checksum != envelope.optString("checksum")) {
                return Result.Failure("CHECKSUM — the backup is corrupted (integrity check failed).")
            }

            val inner = JSONObject(String(innerBytes, Charsets.UTF_8))
            // Success here just means the file is VALID; the caller previews
            // and confirms before applyRestore().
            Result.Success(inner.optInt("totalCount"))
        } catch (e: SecurityException) {
            Result.Failure("PERMISSION — storage access was denied.")
        } catch (e: Exception) {
            Result.Failure("CORRUPT — couldn't read this backup (${e.message ?: "unknown"}).")
        }
    }

    /** Full validated preview (counts per type) for a confirmation dialog. */
    suspend fun preview(uri: Uri): RestorePreview? {
        val inner = decodeInner(uri) ?: return null
        val counts = linkedMapOf<String, Int>()
        listOf(
            "reminders", "persons", "ledger_transactions", "ledger_attachments",
            "gam3iyas", "gam3iya_members", "gam3iya_payments", "gam3iya_attachments",
            "alarms", "work_notes", "financial_items",
            "financial_payments", "financial_attachments",
            "habits", "habit_logs"
        ).forEach { key -> inner.optJSONArray(key)?.let { if (it.length() > 0) counts[key] = it.length() } }
        return RestorePreview(
            inner = inner,
            createdAt = inner.optLong("createdAt"),
            counts = counts,
            totalCount = inner.optInt("totalCount")
        )
    }

    // ── APPLY RESTORE (writes to DB; merge by id) ─────────────────────────

    suspend fun applyRestore(inner: JSONObject): Result {
        return try {
            var count = 0
            inner.optJSONArray("reminders")?.forEachObj { db.reminderDao().insertReminder(it.toReminder()); count++ }
            inner.optJSONArray("persons")?.forEachObj { db.personLedgerDao().insertPerson(it.toPerson()); count++ }
            inner.optJSONArray("ledger_transactions")?.forEachObj { db.personLedgerDao().insertTransaction(it.toTransaction()); count++ }
            inner.optJSONArray("ledger_attachments")?.forEachObj { db.personLedgerDao().insertLedgerAttachment(it.toLedgerAttachment()); count++ }
            inner.optJSONArray("gam3iyas")?.forEachObj { db.gam3iyaDao().insertGam3iya(it.toGam3iya()); count++ }
            inner.optJSONArray("gam3iya_members")?.forEachObj { db.gam3iyaDao().insertMember(it.toGam3iyaMember()); count++ }
            inner.optJSONArray("gam3iya_payments")?.forEachObj { db.gam3iyaDao().insertPayment(it.toGam3iyaPayment()); count++ }
            inner.optJSONArray("gam3iya_attachments")?.forEachObj { db.gam3iyaDao().insertAttachment(it.toGam3iyaAttachment()); count++ }
            inner.optJSONArray("alarms")?.forEachObj { db.alarmDao().insertAlarm(it.toAlarm()); count++ }
            inner.optJSONArray("work_notes")?.forEachObj { db.workNoteDao().insertNote(it.toWorkNote()); count++ }
            inner.optJSONArray("financial_items")?.forEachObj { db.financialDao().insert(it.toFinancialItem()); count++ }
            inner.optJSONArray("financial_payments")?.forEachObj { db.financialDao().insertPayment(it.toFinancialPayment()); count++ }
            inner.optJSONArray("financial_attachments")?.forEachObj { db.financialDao().insertAttachment(it.toFinancialAttachment()); count++ }
            inner.optJSONArray("habits")?.forEachObj { db.habitDao().insertHabit(it.toHabit()); count++ }
            inner.optJSONArray("habit_logs")?.forEachObj { db.habitDao().insertLog(it.toHabitLog()); count++ }
            // Preferences (best-effort; failures here never abort a data restore).
            inner.optJSONObject("preferences")?.let { p ->
                runCatching {
                    if (p.has("language")) prefs.setLanguage(p.getString("language"))
                    if (p.has("darkMode")) prefs.setDarkMode(p.getBoolean("darkMode"))
                    if (p.has("waterInterval")) prefs.setWaterInterval(p.getInt("waterInterval"))
                    if (p.has("alarmRingtoneUri")) prefs.setAlarmRingtoneUri(p.getString("alarmRingtoneUri"))
                }
            }
            Result.Success(count)
        } catch (e: Exception) {
            Result.Failure("Restore failed while writing data (${e.message ?: "unknown"}).")
        }
    }

    private suspend fun decodeInner(uri: Uri): JSONObject? {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val envelope = JSONObject(String(raw, Charsets.UTF_8))
            if (envelope.optString("magic") != MAGIC) return null
            val payload = unbase64(envelope.getString("payload"))
            val gz = if (envelope.optBoolean("encrypted")) {
                tryDecrypt(unbase64(envelope.getString("iv")), payload) ?: return null
            } else payload
            val innerBytes = gunzip(gz)
            if (sha256Hex(innerBytes) != envelope.optString("checksum")) return null
            JSONObject(String(innerBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    // ── Data collection ───────────────────────────────────────────────────

    private suspend fun buildInnerJson(): JSONObject {
        val o = JSONObject()
        o.put("createdAt", System.currentTimeMillis())
        var total = 0

        o.put("reminders", JSONArray().also { arr ->
            db.reminderDao().getAllReminders().first().forEach { arr.put(reminderMap(it)); total++ }
        })
        o.put("persons", JSONArray().also { arr ->
            db.personLedgerDao().getAllPersons().first().forEach { arr.put(personMap(it)); total++ }
        })
        o.put("ledger_transactions", JSONArray().also { arr ->
            db.personLedgerDao().getAllTransactions().first().forEach { arr.put(txMap(it)); total++ }
        })
        o.put("ledger_attachments", JSONArray().also { arr ->
            db.personLedgerDao().getAllLedgerAttachments().first().forEach { arr.put(ledgerAttachmentMap(it)); total++ }
        })
        o.put("gam3iyas", JSONArray().also { arr ->
            db.gam3iyaDao().getAllGam3iyas().first().forEach { arr.put(gam3iyaMap(it)); total++ }
        })
        o.put("gam3iya_members", JSONArray().also { arr ->
            db.gam3iyaDao().getAllMembers().first().forEach { arr.put(memberMap(it)); total++ }
        })
        o.put("gam3iya_payments", JSONArray().also { arr ->
            db.gam3iyaDao().getAllPayments().first().forEach { arr.put(gam3iyaPaymentMap(it)); total++ }
        })
        o.put("gam3iya_attachments", JSONArray().also { arr ->
            db.gam3iyaDao().getAllAttachments().first().forEach { arr.put(gam3iyaAttachmentMap(it)); total++ }
        })
        o.put("alarms", JSONArray().also { arr ->
            db.alarmDao().getAllAlarms().first().forEach { arr.put(alarmMap(it)); total++ }
        })
        o.put("work_notes", JSONArray().also { arr ->
            db.workNoteDao().getAllNotes().first().forEach { arr.put(noteMap(it)); total++ }
        })
        o.put("financial_items", JSONArray().also { arr ->
            db.financialDao().getAll().first().forEach { arr.put(financialMap(it)); total++ }
        })
        o.put("financial_payments", JSONArray().also { arr ->
            db.financialDao().getAllFinancialPayments().first().forEach { arr.put(financialPaymentMap(it)); total++ }
        })
        o.put("financial_attachments", JSONArray().also { arr ->
            db.financialDao().getAllFinancialAttachments().first().forEach { arr.put(financialAttachmentMap(it)); total++ }
        })
        o.put("habits", JSONArray().also { arr ->
            db.habitDao().getAllHabitsRaw().first().forEach { arr.put(habitMap(it)); total++ }
        })
        o.put("habit_logs", JSONArray().also { arr ->
            db.habitDao().getAllLogs().first().forEach { arr.put(habitLogMap(it)); total++ }
        })

        // User preferences snapshot.
        o.put("preferences", JSONObject()
            .put("language", prefs.languageFlow.first())
            .put("darkMode", prefs.isDarkModeFlow.first())
            .put("waterInterval", prefs.waterIntervalFlow.first())
            .put("alarmRingtoneUri", prefs.alarmRingtoneUriFlow.first())
        )

        o.put("totalCount", total)
        return o
    }

    // ── Entity → JSON ──────────────────────────────────────────────────────

    private fun reminderMap(r: ReminderEntity) = JSONObject()
        .put("id", r.id).put("title", r.title).put("note", r.note).put("dueDate", r.dueDate)
        .put("category", r.category).put("recurrence", r.recurrence).put("preAlerts", r.preAlerts)
        .put("isCompleted", r.isCompleted).put("createdAt", r.createdAt).put("checklist", r.checklist)
        .put("tags", r.tags).put("progress", r.progress).put("location", r.location)
        .put("isPinned", r.isPinned).put("isArchived", r.isArchived)

    private fun personMap(p: PersonEntity) = JSONObject()
        .put("id", p.id).put("name", p.name).put("phoneNumber", p.phoneNumber)
        .put("whatsapp", p.whatsapp).put("email", p.email).put("address", p.address)
        .put("category", p.category).put("createdAt", p.createdAt)
        .put("photoUri", p.photoUri).put("company", p.company).put("nationalId", p.nationalId)
        .put("notes", p.notes).put("tags", p.tags).put("isFavorite", p.isFavorite).put("isArchived", p.isArchived)

    private fun txMap(t: LedgerTransactionEntity) = JSONObject()
        .put("id", t.id).put("personId", t.personId).put("type", t.type).put("amount", t.amount)
        .put("date", t.date).put("note", t.note).put("linkedReminderId", t.linkedReminderId ?: JSONObject.NULL)
        .put("currency", t.currency).put("reason", t.reason).put("dueDate", t.dueDate)
        .put("paymentType", t.paymentType).put("recurring", t.recurring)
        .put("createdAt", t.createdAt).put("updatedAt", t.updatedAt)

    private fun ledgerAttachmentMap(a: LedgerAttachmentEntity) = JSONObject()
        .put("id", a.id).put("personId", a.personId).put("transactionId", a.transactionId)
        .put("uri", a.uri).put("kind", a.kind).put("label", a.label).put("createdAt", a.createdAt)

    private fun gam3iyaMap(g: Gam3iyaEntity) = JSONObject()
        .put("id", g.id).put("title", g.title).put("totalAmount", g.totalAmount)
        .put("monthlyInstallment", g.monthlyInstallment).put("membersCount", g.membersCount)
        .put("startDate", g.startDate).put("payoutDayOfMonth", g.payoutDayOfMonth).put("note", g.note)
        .put("mode", g.mode).put("description", g.description).put("currency", g.currency)
        .put("collectionTime", g.collectionTime).put("durationMonths", g.durationMonths)
        .put("status", g.status).put("photoUri", g.photoUri).put("reminderEnabled", g.reminderEnabled)
        .put("reminderDaysBefore", g.reminderDaysBefore).put("alarmEnabled", g.alarmEnabled)
        .put("createdAt", g.createdAt).put("organizerName", g.organizerName)
        .put("organizerPhone", g.organizerPhone).put("organizerWhatsapp", g.organizerWhatsapp)
        .put("organizerEmail", g.organizerEmail).put("myInstallmentAmount", g.myInstallmentAmount)
        .put("myTurnNumber", g.myTurnNumber).put("myCollectionDate", g.myCollectionDate)
        .put("myPaidInstallments", g.myPaidInstallments)

    private fun memberMap(m: Gam3iyaMemberEntity) = JSONObject()
        .put("id", m.id).put("gam3iyaId", m.gam3iyaId).put("memberName", m.memberName)
        .put("turnMonth", m.turnMonth).put("payoutDate", m.payoutDate)
        .put("isPayoutReceived", m.isPayoutReceived).put("isInstallmentPaidThisMonth", m.isInstallmentPaidThisMonth)
        .put("photoUri", m.photoUri).put("phone", m.phone).put("whatsapp", m.whatsapp)
        .put("email", m.email).put("address", m.address).put("nationalId", m.nationalId)
        .put("installmentAmount", m.installmentAmount).put("isLate", m.isLate).put("notes", m.notes)

    private fun gam3iyaPaymentMap(p: Gam3iyaPaymentEntity) = JSONObject()
        .put("id", p.id).put("gam3iyaId", p.gam3iyaId).put("memberId", p.memberId)
        .put("monthIndex", p.monthIndex).put("amount", p.amount).put("date", p.date)
        .put("type", p.type).put("note", p.note)

    private fun gam3iyaAttachmentMap(a: Gam3iyaAttachmentEntity) = JSONObject()
        .put("id", a.id).put("gam3iyaId", a.gam3iyaId).put("memberId", a.memberId)
        .put("uri", a.uri).put("kind", a.kind).put("label", a.label).put("createdAt", a.createdAt)

    private fun alarmMap(a: AlarmEntity) = JSONObject()
        .put("id", a.id).put("title", a.title).put("timeInMillis", a.timeInMillis)
        .put("ringtoneUri", a.ringtoneUri).put("isEnabled", a.isEnabled).put("label", a.label)
        .put("createdViaAi", a.createdViaAi).put("createdAt", a.createdAt)
        .put("vibrate", a.vibrate).put("flashlight", a.flashlight).put("volumePercent", a.volumePercent)
        .put("snoozeMinutes", a.snoozeMinutes).put("autoStopMinutes", a.autoStopMinutes).put("repeatDays", a.repeatDays)

    private fun noteMap(w: WorkNoteEntity) = JSONObject()
        .put("id", w.id).put("title", w.title).put("content", w.content)
        .put("reminderTime", w.reminderTime ?: JSONObject.NULL).put("isDone", w.isDone).put("updatedAt", w.updatedAt)

    private fun financialMap(f: FinancialItemEntity) = JSONObject()
        .put("id", f.id).put("type", f.type).put("title", f.title).put("amount", f.amount)
        .put("totalPrice", f.totalPrice).put("downPayment", f.downPayment).put("monthlyAmount", f.monthlyAmount)
        .put("remaining", f.remaining).put("dueDate", f.dueDate).put("accountNumber", f.accountNumber)
        .put("billNumber", f.billNumber).put("seller", f.seller).put("paymentMethod", f.paymentMethod)
        .put("recurring", f.recurring).put("isPaid", f.isPaid).put("note", f.note)
        .put("linkedReminderId", f.linkedReminderId ?: JSONObject.NULL).put("createdAt", f.createdAt)
        .put("brand", f.brand).put("store", f.store).put("storePhone", f.storePhone)
        .put("storeWhatsapp", f.storeWhatsapp).put("category", f.category).put("currency", f.currency)
        .put("interestAmount", f.interestAmount).put("purchaseDate", f.purchaseDate)
        .put("warranty", f.warranty).put("tags", f.tags).put("isFavorite", f.isFavorite)
        .put("isArchived", f.isArchived).put("paidInstallments", f.paidInstallments)

    private fun financialPaymentMap(p: FinancialPaymentEntity) = JSONObject()
        .put("id", p.id).put("financialItemId", p.financialItemId).put("amount", p.amount)
        .put("date", p.date).put("type", p.type).put("note", p.note)

    private fun financialAttachmentMap(a: FinancialAttachmentEntity) = JSONObject()
        .put("id", a.id).put("financialItemId", a.financialItemId).put("uri", a.uri)
        .put("kind", a.kind).put("label", a.label).put("createdAt", a.createdAt)

    private fun habitMap(h: HabitEntity) = JSONObject()
        .put("id", h.id).put("title", h.title).put("emoji", h.emoji).put("note", h.note)
        .put("isArchived", h.isArchived).put("createdAt", h.createdAt)

    private fun habitLogMap(l: HabitLogEntity) = JSONObject()
        .put("id", l.id).put("habitId", l.habitId).put("dayStart", l.dayStart).put("completedAt", l.completedAt)

    // ── JSON → Entity ──────────────────────────────────────────────────────

    private fun JSONObject.toReminder() = ReminderEntity(
        id = optLong("id"), title = optString("title"), note = optString("note"), dueDate = optLong("dueDate"),
        category = optString("category", "CUSTOM"), recurrence = optString("recurrence", "NONE"),
        preAlerts = optString("preAlerts", "ONE_DAY,ONE_HOUR"), isCompleted = optBoolean("isCompleted"),
        createdAt = optLong("createdAt"), checklist = optString("checklist"), tags = optString("tags"),
        progress = optInt("progress"), location = optString("location"),
        isPinned = optBoolean("isPinned"), isArchived = optBoolean("isArchived")
    )

    private fun JSONObject.toPerson() = PersonEntity(
        id = optLong("id"), name = optString("name"), phoneNumber = optString("phoneNumber"),
        whatsapp = optString("whatsapp"), email = optString("email"), address = optString("address"),
        category = optString("category"), createdAt = optLong("createdAt"),
        photoUri = optString("photoUri"), company = optString("company"), nationalId = optString("nationalId"),
        notes = optString("notes"), tags = optString("tags"),
        isFavorite = optBoolean("isFavorite"), isArchived = optBoolean("isArchived")
    )

    private fun JSONObject.toTransaction() = LedgerTransactionEntity(
        id = optLong("id"), personId = optLong("personId"), type = optString("type"),
        amount = optDouble("amount", 0.0), date = optLong("date"), note = optString("note"),
        linkedReminderId = if (isNull("linkedReminderId")) null else optLong("linkedReminderId"),
        currency = optString("currency", "EGP"), reason = optString("reason"), dueDate = optLong("dueDate"),
        paymentType = optString("paymentType"), recurring = optBoolean("recurring"),
        createdAt = optLong("createdAt"), updatedAt = optLong("updatedAt")
    )

    private fun JSONObject.toLedgerAttachment() = LedgerAttachmentEntity(
        id = optLong("id"), personId = optLong("personId"), transactionId = optLong("transactionId"),
        uri = optString("uri"), kind = optString("kind", "IMAGE"), label = optString("label"),
        createdAt = optLong("createdAt")
    )

    private fun JSONObject.toGam3iya() = Gam3iyaEntity(
        id = optLong("id"), title = optString("title"), totalAmount = optDouble("totalAmount", 0.0),
        monthlyInstallment = optDouble("monthlyInstallment", 0.0), membersCount = optInt("membersCount"),
        startDate = optLong("startDate"), payoutDayOfMonth = optInt("payoutDayOfMonth", 1), note = optString("note"),
        mode = optString("mode", "MANAGER"), description = optString("description"),
        currency = optString("currency", "EGP"), collectionTime = optString("collectionTime"),
        durationMonths = optInt("durationMonths"), status = optString("status", "ACTIVE"),
        photoUri = optString("photoUri"), reminderEnabled = optBoolean("reminderEnabled", true),
        reminderDaysBefore = optInt("reminderDaysBefore", 1), alarmEnabled = optBoolean("alarmEnabled"),
        createdAt = optLong("createdAt"), organizerName = optString("organizerName"),
        organizerPhone = optString("organizerPhone"), organizerWhatsapp = optString("organizerWhatsapp"),
        organizerEmail = optString("organizerEmail"), myInstallmentAmount = optDouble("myInstallmentAmount", 0.0),
        myTurnNumber = optInt("myTurnNumber"), myCollectionDate = optLong("myCollectionDate"),
        myPaidInstallments = optInt("myPaidInstallments")
    )

    private fun JSONObject.toGam3iyaMember() = Gam3iyaMemberEntity(
        id = optLong("id"), gam3iyaId = optLong("gam3iyaId"), memberName = optString("memberName"),
        turnMonth = optInt("turnMonth"), payoutDate = optLong("payoutDate"),
        isPayoutReceived = optBoolean("isPayoutReceived"),
        isInstallmentPaidThisMonth = optBoolean("isInstallmentPaidThisMonth"),
        photoUri = optString("photoUri"), phone = optString("phone"), whatsapp = optString("whatsapp"),
        email = optString("email"), address = optString("address"), nationalId = optString("nationalId"),
        installmentAmount = optDouble("installmentAmount", 0.0), isLate = optBoolean("isLate"),
        notes = optString("notes")
    )

    private fun JSONObject.toGam3iyaPayment() = Gam3iyaPaymentEntity(
        id = optLong("id"), gam3iyaId = optLong("gam3iyaId"), memberId = optLong("memberId"),
        monthIndex = optInt("monthIndex"), amount = optDouble("amount", 0.0), date = optLong("date"),
        type = optString("type", "INSTALLMENT"), note = optString("note")
    )

    private fun JSONObject.toGam3iyaAttachment() = Gam3iyaAttachmentEntity(
        id = optLong("id"), gam3iyaId = optLong("gam3iyaId"), memberId = optLong("memberId"),
        uri = optString("uri"), kind = optString("kind", "RECEIPT"), label = optString("label"),
        createdAt = optLong("createdAt")
    )

    private fun JSONObject.toAlarm() = AlarmEntity(
        id = optLong("id"), title = optString("title"), timeInMillis = optLong("timeInMillis"),
        ringtoneUri = optString("ringtoneUri"), isEnabled = optBoolean("isEnabled", true), label = optString("label"),
        createdViaAi = optBoolean("createdViaAi"), createdAt = optLong("createdAt"),
        vibrate = optBoolean("vibrate", true), flashlight = optBoolean("flashlight"),
        volumePercent = optInt("volumePercent", 100), snoozeMinutes = optInt("snoozeMinutes", 10),
        autoStopMinutes = optInt("autoStopMinutes", 5), repeatDays = optString("repeatDays")
    )

    private fun JSONObject.toWorkNote() = WorkNoteEntity(
        id = optLong("id"), title = optString("title"), content = optString("content"),
        reminderTime = if (isNull("reminderTime")) null else optLong("reminderTime"),
        isDone = optBoolean("isDone"), updatedAt = optLong("updatedAt")
    )

    private fun JSONObject.toFinancialItem() = FinancialItemEntity(
        id = optLong("id"), type = optString("type", "BILL"), title = optString("title"),
        amount = optDouble("amount", 0.0), totalPrice = optDouble("totalPrice", 0.0),
        downPayment = optDouble("downPayment", 0.0), monthlyAmount = optDouble("monthlyAmount", 0.0),
        remaining = optDouble("remaining", 0.0), dueDate = optLong("dueDate"),
        accountNumber = optString("accountNumber"), billNumber = optString("billNumber"),
        seller = optString("seller"), paymentMethod = optString("paymentMethod"),
        recurring = optBoolean("recurring"), isPaid = optBoolean("isPaid"), note = optString("note"),
        linkedReminderId = if (isNull("linkedReminderId")) null else optLong("linkedReminderId"),
        createdAt = optLong("createdAt"),
        brand = optString("brand"), store = optString("store"), storePhone = optString("storePhone"),
        storeWhatsapp = optString("storeWhatsapp"), category = optString("category"),
        currency = optString("currency", "EGP"), interestAmount = optDouble("interestAmount", 0.0),
        purchaseDate = optLong("purchaseDate"), warranty = optString("warranty"), tags = optString("tags"),
        isFavorite = optBoolean("isFavorite"), isArchived = optBoolean("isArchived"),
        paidInstallments = optInt("paidInstallments")
    )

    private fun JSONObject.toFinancialPayment() = FinancialPaymentEntity(
        id = optLong("id"), financialItemId = optLong("financialItemId"), amount = optDouble("amount", 0.0),
        date = optLong("date"), type = optString("type", "PARTIAL"), note = optString("note")
    )

    private fun JSONObject.toFinancialAttachment() = FinancialAttachmentEntity(
        id = optLong("id"), financialItemId = optLong("financialItemId"), uri = optString("uri"),
        kind = optString("kind", "RECEIPT"), label = optString("label"), createdAt = optLong("createdAt")
    )

    private fun JSONObject.toHabit() = HabitEntity(
        id = optLong("id"), title = optString("title"), emoji = optString("emoji", "✅"),
        note = optString("note"), isArchived = optBoolean("isArchived"), createdAt = optLong("createdAt")
    )

    private fun JSONObject.toHabitLog() = HabitLogEntity(
        id = optLong("id"), habitId = optLong("habitId"), dayStart = optLong("dayStart"),
        completedAt = optLong("completedAt")
    )

    // ── Crypto / compression / hashing ─────────────────────────────────────

    /** @return (iv, ciphertext) or null if Keystore isn't usable. */
    private fun tryEncrypt(data: ByteArray): Pair<ByteArray, ByteArray>? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ct = cipher.doFinal(data)
            Pair(cipher.iv, ct)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryDecrypt(iv: ByteArray, ct: ByteArray): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getKey() ?: return null, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(ct)
        } catch (e: Exception) {
            null
        }
    }

    private fun getKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        getKey()?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    private fun gzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray {
        return GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
    }

    private fun sha256Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }

    private fun base64(data: ByteArray): String =
        android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)

    private fun unbase64(s: String): ByteArray =
        android.util.Base64.decode(s, android.util.Base64.NO_WRAP)

    private inline fun JSONArray.forEachObj(action: (JSONObject) -> Unit) {
        for (i in 0 until length()) optJSONObject(i)?.let(action)
    }
}
