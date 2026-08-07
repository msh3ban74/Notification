package com.notification.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notification.app.data.local.AppDatabase
import com.notification.app.data.local.entities.*
import com.notification.app.data.preferences.UserPreferencesRepository
import android.util.Log
import com.notification.app.BuildConfig
import com.notification.app.data.remote.GeminiContent
import com.notification.app.data.remote.GeminiPart
import com.notification.app.data.repository.GeminiRepository
import com.notification.app.data.repository.NotificationRepository
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.PrayerTime
import com.notification.app.domain.calculator.PrayerTimesCalculator
import com.notification.app.domain.model.LedgerTransactionType
import com.notification.app.domain.model.RecurrenceType
import com.notification.app.domain.model.ReminderCategory
import com.notification.app.domain.scheduler.AlarmManagerScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = NotificationRepository(db)
    val preferencesRepository = UserPreferencesRepository(application)
    private val geminiRepository = GeminiRepository(repository)

    // User Preferences State
    val language: StateFlow<String> = preferencesRepository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ar")

    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isLoggedIn: StateFlow<Boolean> = preferencesRepository.isLoggedInFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName: StateFlow<String> = preferencesRepository.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val userEmail: StateFlow<String> = preferencesRepository.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val lastSyncTime: StateFlow<Long> = preferencesRepository.lastSyncTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val alarmRingtoneUri: StateFlow<String> = preferencesRepository.alarmRingtoneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Database Flows.
    // Sprint 6 — performance: core dashboard flows use SharingStarted.Lazily
    // so Room keeps them HOT for the ViewModel's whole life. Switching tabs
    // never tears the subscription down, so returning to the Dashboard (or
    // any list screen) renders instantly from the cached StateFlow value —
    // Room pushes fresh values in the background when data changes.
    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingReminders: StateFlow<List<ReminderEntity>> = repository.pendingReminders
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allPersons: StateFlow<List<PersonEntity>> = repository.allPersons
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTransactions: StateFlow<List<LedgerTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allGam3iyas: StateFlow<List<Gam3iyaEntity>> = repository.allGam3iyas
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allAlarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allWorkNotes: StateFlow<List<WorkNoteEntity>> = repository.allWorkNotes
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Phase B — financial items (bills / installments / subscriptions).
    val allFinancialItems: StateFlow<List<FinancialItemEntity>> = repository.allFinancialItems
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Money items are never silent: every bill/installment/subscription
     * carries a linked due-date reminder that rides the SAME reminder +
     * scheduler pipeline as tasks (linkedReminderId ties them together).
     * Paying an item completes its reminder; unpaying reactivates it;
     * deleting the item removes it. Recurring items get MONTHLY recurrence.
     */
    private fun financialReminderFor(
        item: FinancialItemEntity,
        base: ReminderEntity? = null
    ): ReminderEntity {
        val isArabic = language.value == "ar"
        val typeLabel = when (item.type) {
            "INSTALLMENT" -> if (isArabic) "قسط مستحق" else "Installment due"
            "SUBSCRIPTION" -> if (isArabic) "تجديد اشتراك" else "Subscription renewal"
            else -> if (isArabic) "فاتورة مستحقة" else "Bill due"
        }
        val amount = if (item.monthlyAmount > 0) item.monthlyAmount else item.amount
        val amountLabel = if (amount > 0) " — ${amount.toLong()} ${if (isArabic) "ج.م" else "EGP"}" else ""
        val recurrence = if (item.recurring || item.type == "INSTALLMENT") {
            RecurrenceType.MONTHLY.name
        } else {
            RecurrenceType.NONE.name
        }
        return (base ?: ReminderEntity(title = "", dueDate = 0L, category = ReminderCategory.BILL.name)).copy(
            title = item.title,
            note = "$typeLabel$amountLabel",
            dueDate = item.dueDate,
            category = ReminderCategory.BILL.name,
            recurrence = recurrence,
            isCompleted = item.isPaid
        )
    }

    fun addFinancialItem(item: FinancialItemEntity) {
        viewModelScope.launch {
            val reminderId = insertAndScheduleReminder(financialReminderFor(item))
            repository.insertFinancialItem(item.copy(linkedReminderId = reminderId))
        }
    }

    fun updateFinancialItem(item: FinancialItemEntity) {
        viewModelScope.launch {
            val linked = item.linkedReminderId?.let { repository.getReminderById(it) }
            val toStore = when {
                // Paid: silence the alert and check off the linked reminder.
                item.isPaid -> {
                    linked?.let {
                        repository.updateReminder(financialReminderFor(item, base = it))
                        AlarmManagerScheduler.cancelReminderAlarm(getApplication(), it.id)
                    }
                    item
                }
                // Unpaid with a live link: refresh its fields and re-arm.
                linked != null -> {
                    val updated = financialReminderFor(item, base = linked)
                    repository.updateReminder(updated)
                    AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), updated)
                    item
                }
                // Unpaid but the link is gone (older item or reminder was
                // deleted from the Tasks list) — create a fresh one.
                else -> {
                    val reminderId = insertAndScheduleReminder(financialReminderFor(item))
                    item.copy(linkedReminderId = reminderId)
                }
            }
            repository.updateFinancialItem(toStore)
        }
    }

    fun deleteFinancialItem(item: FinancialItemEntity) {
        viewModelScope.launch {
            item.linkedReminderId?.let { linkedId ->
                repository.getReminderById(linkedId)?.let { reminder ->
                    AlarmManagerScheduler.cancelReminderAlarm(getApplication(), reminder.id)
                    repository.deleteReminder(reminder)
                }
            }
            repository.deleteFinancialItem(item)
        }
    }

    // ── Sprint 6 — installment payments + attachments ─────────────────
    fun getFinancialPaymentsForItem(itemId: Long): Flow<List<FinancialPaymentEntity>> =
        repository.getPaymentsForFinancialItem(itemId)
    fun getFinancialAttachmentsForItem(itemId: Long): Flow<List<FinancialAttachmentEntity>> =
        repository.getAttachmentsForFinancialItem(itemId)

    /**
     * Record a payment against an installment. Logs the payment, advances
     * paidInstallments, recomputes remaining and the next due date (or
     * marks the item paid when the balance reaches zero) and refreshes the
     * linked reminder via the existing [updateFinancialItem] pipeline.
     */
    fun recordFinancialPayment(item: FinancialItemEntity, amount: Double, type: String, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.insertFinancialPayment(
                FinancialPaymentEntity(
                    financialItemId = item.id, amount = amount, date = System.currentTimeMillis(),
                    type = type, note = note
                )
            )
            val payments = repository.getPaymentsForFinancialItem(item.id).first()
            val plan = com.notification.app.domain.calculator.FinancialCalculator.computePlan(item, payments)
            val nextDue = if (plan.isFinished) item.dueDate else {
                java.util.Calendar.getInstance().apply {
                    timeInMillis = if (item.dueDate > 0) item.dueDate else System.currentTimeMillis()
                    add(java.util.Calendar.MONTH, 1)
                }.timeInMillis
            }
            val updated = item.copy(
                remaining = plan.remainingAmount,
                paidInstallments = plan.paidInstallments,
                isPaid = plan.isFinished,
                dueDate = if (plan.isFinished) item.dueDate else nextDue
            )
            // Reuses the reminder-aware update pipeline (cancels the alarm
            // when finished, else reschedules for the new due date).
            updateFinancialItem(updated)
        }
    }

    fun deleteFinancialPayment(item: FinancialItemEntity, p: FinancialPaymentEntity) {
        viewModelScope.launch {
            repository.deleteFinancialPayment(p)
            val payments = repository.getPaymentsForFinancialItem(item.id).first()
            val plan = com.notification.app.domain.calculator.FinancialCalculator.computePlan(item, payments)
            updateFinancialItem(item.copy(
                remaining = plan.remainingAmount, paidInstallments = plan.paidInstallments,
                isPaid = plan.isFinished
            ))
        }
    }

    fun addFinancialAttachment(itemId: Long, uri: String, kind: String, label: String) {
        viewModelScope.launch {
            repository.insertFinancialAttachment(
                FinancialAttachmentEntity(
                    financialItemId = itemId, uri = uri, kind = kind, label = label,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    fun deleteFinancialAttachment(a: FinancialAttachmentEntity) {
        viewModelScope.launch { repository.deleteFinancialAttachment(a) }
    }

    // Phase C — habit engine. The log flow feeds HabitCalculator
    // (streaks / calendar / percentages) in the UI layer.
    val allHabits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allHabitLogs: StateFlow<List<HabitLogEntity>> = repository.allHabitLogs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.insertHabit(habit) }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.updateHabit(habit) }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { repository.deleteHabit(habit) }
    }

    fun setHabitDone(habitId: Long, dayStart: Long, done: Boolean) {
        viewModelScope.launch { repository.setHabitDone(habitId, dayStart, done) }
    }

    // Islamic Prayers State
    private val _prayerTimes = MutableStateFlow<List<PrayerTime>>(emptyList())
    val prayerTimes: StateFlow<List<PrayerTime>> = _prayerTimes.asStateFlow()

    // AI Chat Messages State
    private val _chatMessages = MutableStateFlow<List<GeminiContent>>(emptyList())
    val chatMessages: StateFlow<List<GeminiContent>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // AI sprint — single-flight guard + persistence.
    private var aiJob: kotlinx.coroutines.Job? = null
    private var lastUserMessage: String? = null

    /** Persist the visible conversation (role + text) so it survives
     *  process death / app restart, not just rotation. */
    private fun persistChat() {
        viewModelScope.launch {
            val arr = org.json.JSONArray()
            _chatMessages.value.forEach { c ->
                val text = c.parts.firstOrNull { !it.text.isNullOrBlank() }?.text ?: return@forEach
                arr.put(org.json.JSONObject().put("role", c.role ?: "model").put("text", text))
            }
            preferencesRepository.setChatHistory(arr.toString())
        }
    }

    private fun loadChat() {
        viewModelScope.launch {
            val json = preferencesRepository.chatHistoryFlow.first()
            if (json.isBlank()) return@launch
            runCatching {
                val arr = org.json.JSONArray(json)
                val restored = (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    val text = o.optString("text").ifBlank { return@mapNotNull null }
                    GeminiContent(role = o.optString("role", "model"), parts = listOf(GeminiPart(text = text)))
                }
                if (restored.isNotEmpty() && _chatMessages.value.isEmpty()) {
                    _chatMessages.value = restored
                }
            }
        }
    }

    // NOTE: AI dashboard suggestions were removed on purpose — they burned
    // the free-tier quota the CHAT needs. «رفيق شايف إن…» on the home
    // screen is computed locally from the user's own data at zero cost.

    /**
     * Stability sprint — full logout: Firebase sign-out happens at the
     * call site; here we clear the persisted session and every piece of
     * cached user-scoped state so the app is truly signed out (and stays
     * signed out after restart via the persisted isLoggedIn=false).
     */
    fun onLogout() {
        viewModelScope.launch {
            preferencesRepository.setUserAuth("Guest User", "", false)
        }
        aiJob?.cancel()
        _chatMessages.value = emptyList()
        _isAiLoading.value = false
        persistChat()
    }

    companion object {
        /** Hard ceiling for a single AI generation before a friendly cancel.
         *  30s covers a tool round-trip plus one transient retry; the OkHttp
         *  call timeout (25s) usually trips first, so this is the backstop. */
        const val AI_GENERATION_TIMEOUT_MS: Long = 30_000L
    }

    // Water counter — persisted per-day (survives restart, resets each day).
    val waterCount: StateFlow<Int> = preferencesRepository.waterCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)


    init {
        // Debug-only diagnostic of the key pipeline (GitHub Secret -> .env ->
        // BuildConfig). Logs a boolean availability flag ONLY, never the key,
        // and is stripped from release builds.
        if (BuildConfig.DEBUG) {
            Log.i("Rafeeq", "API Key Available = " + BuildConfig.GEMINI_API_KEY.isNotBlank())
        }
        updatePrayerTimes()
        loadChat()
        // رسائل رفيق اليومية — idempotent 8:00 morning + 21:00 evening briefs.
        AlarmManagerScheduler.scheduleMorningBrief(getApplication())
        AlarmManagerScheduler.scheduleEveningBrief(getApplication())
        // Home-screen widget: refresh with live data whenever the app opens.
        com.notification.app.receiver.TodayWidgetProvider.refreshAll(getApplication())
    }

    fun updatePrayerTimes() {
        _prayerTimes.value = PrayerTimesCalculator.getDailyPrayerTimes(Date())
    }

    // Reminders Actions
    fun addReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            insertAndScheduleReminder(reminder)
        }
    }

    /**
     * The single insert-and-schedule path for every reminder in the app.
     * Sprint 3/4 reuse: called by [addReminder] (Tasks) and by [addDebt]
     * (due-date reminder), so there is exactly ONE scheduling pipeline.
     */
    private suspend fun insertAndScheduleReminder(reminder: ReminderEntity): Long {
        val id = repository.insertReminder(reminder)
        AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), reminder.copy(id = id))
        return id
    }

    /**
     * Sprint 5 — edit flow for Tasks (and every other reminder-based Smart
     * Item). Persists through the existing repository and re-schedules via
     * the existing AlarmManagerScheduler: the reminder's PendingIntent
     * request code is derived from its id, so scheduling again simply
     * replaces the previously scheduled alarm.
     */
    fun updateTaskReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), reminder)
        }
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            // Cancel its scheduled alert too, so a deleted reminder can
            // never fire a ghost notification later.
            AlarmManagerScheduler.cancelReminderAlarm(getApplication(), reminder.id)
            repository.deleteReminder(reminder)
        }
    }

    // Phase D — CRUD extras (pin / archive / duplicate).
    fun toggleReminderPinned(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isPinned = !reminder.isPinned))
        }
    }

    fun setReminderArchived(reminder: ReminderEntity, archived: Boolean) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isArchived = archived))
        }
    }

    /**
     * Duplicate rides the SAME insert-and-schedule pipeline as a new
     * reminder, so the copy gets its own alarms. The copy starts fresh:
     * not completed, not pinned, id 0 (auto-generated).
     */
    fun duplicateReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            insertAndScheduleReminder(
                reminder.copy(
                    id = 0,
                    isCompleted = false,
                    isPinned = false,
                    isArchived = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Ledger Actions
    fun addPerson(name: String, phone: String = "") {
        viewModelScope.launch {
            repository.insertPerson(PersonEntity(name = name, phoneNumber = phone))
        }
    }

    fun addPersonFull(person: PersonEntity) {
        viewModelScope.launch { repository.insertPerson(person) }
    }

    fun updatePerson(person: PersonEntity) {
        viewModelScope.launch { repository.updatePerson(person) }
    }

    // ── Sprint 5 — person profile CRUD extras ─────────────────────────
    fun deletePerson(person: PersonEntity) {
        viewModelScope.launch { repository.deletePersonCascade(person) }
    }
    fun togglePersonFavorite(person: PersonEntity) {
        viewModelScope.launch { repository.updatePerson(person.copy(isFavorite = !person.isFavorite)) }
    }
    fun togglePersonArchived(person: PersonEntity) {
        viewModelScope.launch { repository.updatePerson(person.copy(isArchived = !person.isArchived)) }
    }

    // ── Sprint 5 — ledger attachments (receipts / docs / audio) ───────
    fun getLedgerAttachmentsForPerson(personId: Long): Flow<List<LedgerAttachmentEntity>> =
        repository.getAttachmentsForPerson(personId)
    fun addLedgerAttachment(personId: Long, transactionId: Long, uri: String, kind: String, label: String) {
        viewModelScope.launch {
            repository.insertLedgerAttachment(
                LedgerAttachmentEntity(
                    personId = personId, transactionId = transactionId, uri = uri,
                    kind = kind, label = label, createdAt = System.currentTimeMillis()
                )
            )
        }
    }
    fun deleteLedgerAttachment(a: LedgerAttachmentEntity) {
        viewModelScope.launch { repository.deleteLedgerAttachment(a) }
    }

    fun addLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val stamped = tx.copy(
                createdAt = if (tx.createdAt == 0L) now else tx.createdAt,
                updatedAt = now
            )
            val id = repository.insertLedgerTransaction(stamped)
            // Schedule a due-date reminder when one is set and in the future.
            if (stamped.dueDate > now) {
                val person = repository.getPersonById(stamped.personId)
                val title = if (ar()) "استحقاق دين: ${person?.name ?: ""}" else "Debt due: ${person?.name ?: ""}"
                val reminderId = insertAndScheduleReminder(
                    ReminderEntity(
                        title = title, note = stamped.note, dueDate = stamped.dueDate,
                        category = ReminderCategory.MONEY.name
                    )
                )
                repository.updateLedgerTransaction(stamped.copy(id = id, linkedReminderId = reminderId))
            }
        }
    }

    /**
     * Edit flow for debts. The promised-date reminder stays in sync:
     * changing the date reschedules it, clearing the date cancels it, and
     * adding a date to an old transaction creates one.
     */
    fun updateLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var toStore = tx.copy(updatedAt = now)
            val linked = tx.linkedReminderId?.let { repository.getReminderById(it) }
            when {
                linked != null && tx.dueDate > now -> {
                    val updated = linked.copy(dueDate = tx.dueDate, note = tx.note)
                    repository.updateReminder(updated)
                    AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), updated)
                }
                linked != null -> {
                    AlarmManagerScheduler.cancelReminderAlarm(getApplication(), linked.id)
                    repository.deleteReminder(linked)
                    toStore = toStore.copy(linkedReminderId = null)
                }
                tx.dueDate > now -> {
                    val person = repository.getPersonById(tx.personId)
                    val title = if (ar()) "استحقاق دين: ${person?.name ?: ""}" else "Debt due: ${person?.name ?: ""}"
                    val reminderId = insertAndScheduleReminder(
                        ReminderEntity(
                            title = title, note = tx.note, dueDate = tx.dueDate,
                            category = ReminderCategory.MONEY.name
                        )
                    )
                    toStore = toStore.copy(linkedReminderId = reminderId)
                }
            }
            repository.updateLedgerTransaction(toStore)
        }
    }

    fun deleteLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            // Never leave a ghost reminder behind a deleted debt.
            tx.linkedReminderId?.let { linkedId ->
                repository.getReminderById(linkedId)?.let { reminder ->
                    AlarmManagerScheduler.cancelReminderAlarm(getApplication(), reminder.id)
                    repository.deleteReminder(reminder)
                }
            }
            repository.deleteLedgerTransaction(tx)
        }
    }

    /**
     * Sprint 4 — Smart Debt.
     *
     * Creates a debt by COMPOSING the existing Ledger implementation:
     *  - person       → existing [PersonEntity] (or a new one via the
     *                   existing repository.insertPerson).
     *  - the debt     → the existing [LedgerTransactionEntity]
     *                   (GAVE_THEM = lent / THEY_GAVE_ME = borrowed).
     *  - due date     → the existing reminder pipeline
     *                   ([insertAndScheduleReminder]) linked through the
     *                   entity's existing linkedReminderId column.
     * No new entities, no duplicated debt logic, no new scheduler.
     */
    fun addDebt(
        existingPersonId: Long?,
        personName: String,
        amount: Double,
        isLent: Boolean,
        receivedDate: Long = System.currentTimeMillis(),
        dueDate: Long?,
        note: String
    ) {
        viewModelScope.launch {
            val personId = existingPersonId
                ?: repository.insertPerson(PersonEntity(name = personName))

            val linkedReminderId = if (dueDate != null && dueDate > System.currentTimeMillis()) {
                val isArabic = language.value == "ar"
                insertAndScheduleReminder(
                    ReminderEntity(
                        title = if (isArabic) "استحقاق دين: $personName" else "Debt due: $personName",
                        note = note,
                        dueDate = dueDate,
                        category = ReminderCategory.MONEY.name
                    )
                )
            } else null

            repository.insertLedgerTransaction(
                LedgerTransactionEntity(
                    personId = personId,
                    type = if (isLent) {
                        LedgerTransactionType.GAVE_THEM.name
                    } else {
                        LedgerTransactionType.THEY_GAVE_ME.name
                    },
                    amount = amount,
                    date = receivedDate,
                    note = note,
                    dueDate = dueDate ?: 0L,
                    linkedReminderId = linkedReminderId
                )
            )
        }
    }

    // ── رفيق — جمعيتي (مشترك فقط): تسجيل، تعديل، "دفعت قسط الشهر"،
    // وتذكير شهري تلقائي. وضع "مدير الجمعية" اتشال نهائيًا من التطبيق.

    /** Create a PARTICIPANT-mode gam3iya (I only take part). */
    fun createParticipantGam3iya(gam3iya: Gam3iyaEntity) {
        viewModelScope.launch {
            val p = gam3iya.copy(
                mode = "PARTICIPANT",
                membersCount = 0,
                createdAt = if (gam3iya.createdAt == 0L) System.currentTimeMillis() else gam3iya.createdAt
            )
            val id = repository.insertGam3iya(p)
            syncGam3iyaReminder(p.copy(id = id))
        }
    }

    fun updateGam3iya(gam3iya: Gam3iyaEntity) {
        viewModelScope.launch {
            repository.updateGam3iya(gam3iya)
            syncGam3iyaReminder(gam3iya)
        }
    }

    fun deleteGam3iya(gam3iya: Gam3iyaEntity) {
        viewModelScope.launch {
            AlarmManagerScheduler.cancelReminderAlarm(getApplication(), gam3iyaReminderId(gam3iya.id))
            repository.deleteReminderById(gam3iyaReminderId(gam3iya.id))
            repository.deleteGam3iya(gam3iya)
        }
    }

    /** "دفعت قسط الشهر ✓" — record that I paid one more installment. */
    fun participantRecordPayment(gam3iya: Gam3iyaEntity) {
        viewModelScope.launch {
            val amount = if (gam3iya.myInstallmentAmount > 0) gam3iya.myInstallmentAmount else gam3iya.monthlyInstallment
            val newCount = gam3iya.myPaidInstallments + 1
            val updated = gam3iya.copy(myPaidInstallments = newCount)
            repository.updateGam3iya(updated)
            repository.insertGam3iyaPayment(
                Gam3iyaPaymentEntity(
                    gam3iyaId = gam3iya.id, memberId = 0,
                    monthIndex = newCount, amount = amount,
                    date = System.currentTimeMillis(), type = "INSTALLMENT"
                )
            )
            syncGam3iyaReminder(updated)
        }
    }

    // A single rolling "next installment" reminder per gam3iya, upserted
    // with a stable id derived from the gam3iya id (no schema change, no
    // duplicates), riding the app's one reminder+alarm pipeline.
    private fun gam3iyaReminderId(gam3iyaId: Long): Long = 900_000_000L + gam3iyaId

    /** (Re)compute and (re)schedule the next-installment reminder. */
    private suspend fun syncGam3iyaReminder(gam3iya: Gam3iyaEntity) {
        val rid = gam3iyaReminderId(gam3iya.id)
        val app = getApplication<android.app.Application>()
        val now = System.currentTimeMillis()

        val status = Gam3iyaCalculator.computeStatus(gam3iya, emptyList())

        // Cancel path: reminders off, archived/completed, or fully paid.
        if (!gam3iya.reminderEnabled || gam3iya.status != "ACTIVE" || status.isFinished) {
            AlarmManagerScheduler.cancelReminderAlarm(app, rid)
            repository.deleteReminderById(rid)
            return
        }

        val date = if (gam3iya.myCollectionDate > now) gam3iya.myCollectionDate
        else Gam3iyaCalculator.calculateMemberPayoutDate(gam3iya.startDate, gam3iya.myPaidInstallments + 1)
        if (date <= now) {
            AlarmManagerScheduler.cancelReminderAlarm(app, rid)
            repository.deleteReminderById(rid)
            return
        }

        val reminder = ReminderEntity(
            id = rid,
            title = if (ar()) "قسط جمعية: ${gam3iya.title}" else "Gam3iya installment: ${gam3iya.title}",
            note = if (ar()) "تذكير تلقائي من رفيق" else "Automatic Rafeeq reminder",
            dueDate = date,
            category = ReminderCategory.MONEY.name,
            preAlerts = if (gam3iya.reminderDaysBefore >= 1) "ONE_DAY,ONE_HOUR" else "ONE_HOUR"
        )
        repository.insertReminder(reminder) // REPLACE upsert on the stable id
        AlarmManagerScheduler.scheduleReminderAlarm(app, reminder)
    }

    // Alarm Actions
    fun addAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val id = repository.insertAlarm(alarm)
            val fullAlarm = alarm.copy(id = id)
            AlarmManagerScheduler.scheduleExactAlarm(getApplication(), fullAlarm)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                AlarmManagerScheduler.scheduleExactAlarm(getApplication(), updated)
            } else {
                AlarmManagerScheduler.cancelAlarm(getApplication(), updated.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            AlarmManagerScheduler.cancelAlarm(getApplication(), alarm.id)
        }
    }

    // Work Note Actions
    fun addWorkNote(title: String, content: String, reminderTime: Long? = null) {
        viewModelScope.launch {
            repository.insertWorkNote(
                WorkNoteEntity(title = title, content = content, reminderTime = reminderTime)
            )
        }
    }

    fun toggleWorkNoteDone(note: WorkNoteEntity) {
        viewModelScope.launch {
            repository.updateWorkNote(note.copy(isDone = !note.isDone))
        }
    }

    fun deleteWorkNote(note: WorkNoteEntity) {
        viewModelScope.launch {
            repository.deleteWorkNote(note)
        }
    }

    fun incrementWater() {
        viewModelScope.launch { preferencesRepository.setWaterCount(waterCount.value + 1) }
    }

    // AI Chat
    /**
     * Stability sprint — reliable AI pipeline:
     *  - the user's message appears in the conversation IMMEDIATELY
     *    (optimistic append) and the typing indicator follows instantly;
     *  - single-flight: while a generation is active, new sends are
     *    ignored, so duplicate requests are impossible;
     *  - a hard 15s timeout cancels a hung generation and posts a
     *    friendly retry message — the conversation can never freeze;
     *  - repository-level failures (network / API / parsing) already
     *    return readable error replies, so nothing fails silently;
     *  - the API key comes from BuildConfig only (no Settings key).
     */
    fun sendAiMessage(userText: String) {
        if (userText.isBlank()) return
        if (_isAiLoading.value) return
        val isArabic = language.value == "ar"
        val previousHistory = _chatMessages.value
        lastUserMessage = userText

        // Optimistic UI: show the user's bubble right away.
        _chatMessages.value = previousHistory +
            GeminiContent(role = "user", parts = listOf(GeminiPart(text = userText)))
        persistChat()

        // Offline pre-check — fail fast with a clear message, never spin.
        if (!com.notification.app.data.remote.NetworkMonitor.isOnline(getApplication())) {
            appendModel(
                if (isArabic) "لا يوجد اتصال بالإنترنت. تحقق من الشبكة وحاول مرة أخرى."
                else "No internet connection. Check your network and try again."
            )
            return
        }

        _isAiLoading.value = true
        aiJob = viewModelScope.launch {
            val result = withTimeoutOrNull(AI_GENERATION_TIMEOUT_MS) {
                geminiRepository.sendMessage(
                    history = previousHistory,
                    userMessage = userText,
                    isArabic = isArabic,
                    onAlarmCreated = { alarm ->
                        AlarmManagerScheduler.scheduleExactAlarm(getApplication(), alarm)
                    },
                    onReminderCreated = { reminderId ->
                        repository.getReminderById(reminderId)?.let {
                            AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), it)
                        }
                    },
                    onLogWater = { incrementWater() },
                    onGam3iyaCreated = { gam3iyaId ->
                        repository.getGam3iyaById(gam3iyaId)?.let { syncGam3iyaReminder(it) }
                    }
                )
            }
            if (result != null) {
                _chatMessages.value = result.second
            } else {
                _chatMessages.value = _chatMessages.value + GeminiContent(
                    role = "model",
                    parts = listOf(
                        GeminiPart(
                            text = if (isArabic)
                                "استغرق الرد وقتًا أطول من المعتاد. اضغط \"إعادة\" للمحاولة مرة أخرى."
                            else "The response took longer than usual. Tap \"Regenerate\" to retry."
                        )
                    )
                )
            }
            _isAiLoading.value = false
            persistChat()
        }
    }

    /** Append a model bubble + persist (used for offline / stop notices). */
    private fun appendModel(text: String) {
        _chatMessages.value = _chatMessages.value +
            GeminiContent(role = "model", parts = listOf(GeminiPart(text = text)))
        _isAiLoading.value = false
        persistChat()
    }

    /** Stop an in-flight generation — the conversation never hangs. */
    fun stopAiGeneration() {
        if (!_isAiLoading.value) return
        aiJob?.cancel()
        aiJob = null
        appendModel(
            if (language.value == "ar") "تم إيقاف الرد."
            else "Response stopped."
        )
    }

    /** Regenerate — drop trailing model replies and resend the last user turn. */
    fun regenerateLastResponse() {
        if (_isAiLoading.value) return
        val lastUser = lastUserMessage ?: _chatMessages.value.lastOrNull { it.role == "user" }
            ?.parts?.firstOrNull()?.text ?: return
        // Trim history back to just before the last user message so we
        // don't stack duplicate user bubbles.
        val msgs = _chatMessages.value.toMutableList()
        while (msgs.isNotEmpty() && msgs.last().role != "user") msgs.removeAt(msgs.lastIndex)
        if (msgs.isNotEmpty() && msgs.last().role == "user") msgs.removeAt(msgs.lastIndex)
        _chatMessages.value = msgs
        persistChat()
        sendAiMessage(lastUser)
    }

    /** Clear the whole conversation (and its persisted copy). */
    fun clearChat() {
        aiJob?.cancel()
        _chatMessages.value = emptyList()
        _isAiLoading.value = false
        persistChat()
    }

    // User Preferences Actions
    fun setLanguage(lang: String) {
        viewModelScope.launch { preferencesRepository.setLanguage(lang) }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkMode(isDark) }
    }

    fun setUserAuth(name: String, email: String, isLoggedIn: Boolean) {
        viewModelScope.launch { preferencesRepository.setUserAuth(name, email, isLoggedIn) }
    }

    fun setAlarmRingtoneUri(uri: String) {
        viewModelScope.launch { preferencesRepository.setAlarmRingtoneUri(uri) }
    }

    private val backupRepository = com.notification.app.data.repository.BackupRepository(db)

    private val _backupState = MutableStateFlow<String?>(null)
    val backupState: StateFlow<String?> = _backupState.asStateFlow()

    fun triggerBackupSync() {
        if (_backupState.value == "syncing") return
        _backupState.value = "syncing"
        viewModelScope.launch {
            // Hard 30s ceiling so a stalled Firestore call can NEVER leave
            // the UI spinning forever — the sprint's non-negotiable rule.
            val result = withTimeoutOrNull(30_000L) { backupRepository.backupNow() }
            _backupState.value = when (result) {
                is com.notification.app.data.repository.BackupRepository.BackupResult.Success -> {
                    preferencesRepository.updateLastSyncTime(System.currentTimeMillis())
                    if (language.value == "ar") "success:تم رفع ${result.count} عنصر"
                    else "success:${result.count} items backed up"
                }
                is com.notification.app.data.repository.BackupRepository.BackupResult.Failure ->
                    "error: ${result.message}"
                null ->
                    "error: " + (if (language.value == "ar")
                        "استغرقت المزامنة وقتًا طويلًا — تأكد من الاتصال وحاول مجددًا"
                    else "Sync timed out — check your connection and try again")
            }
        }
    }

    // Restore — mirrors backup's 3-state contract (loading/success/failure)
    // with the same hard timeout so it can never spin forever.
    private val _restoreState = MutableStateFlow<String?>(null)
    val restoreState: StateFlow<String?> = _restoreState.asStateFlow()

    fun triggerRestore() {
        if (_restoreState.value == "syncing") return
        _restoreState.value = "syncing"
        viewModelScope.launch {
            val result = withTimeoutOrNull(30_000L) { backupRepository.restoreLatest() }
            _restoreState.value = when (result) {
                is com.notification.app.data.repository.BackupRepository.BackupResult.Success -> {
                    // Restore wiped + rewrote every table, so all alarm/reminder
                    // registrations are stale — re-arm them from the fresh data.
                    rescheduleEverythingAfterRestore()
                    if (language.value == "ar") "success:تمت استعادة ${result.count} عنصر"
                    else "success:${result.count} items restored"
                }
                is com.notification.app.data.repository.BackupRepository.BackupResult.Failure ->
                    "error: ${result.message}"
                null ->
                    "error: " + (if (language.value == "ar")
                        "استغرقت الاستعادة وقتًا طويلًا — تأكد من الاتصال وحاول مجددًا"
                    else "Restore timed out — check your connection and try again")
            }
        }
    }

    /**
     * Called right after a successful sign-in — on a NEW phone this is what
     * makes all your data appear: it pulls the cloud mirror down (replacing
     * local) and re-arms every alarm/reminder from the restored data.
     */
    fun restoreFromBackup() {
        viewModelScope.launch {
            val result = backupRepository.restoreLatest()
            if (result is com.notification.app.data.repository.BackupRepository.BackupResult.Success) {
                rescheduleEverythingAfterRestore()
            }
        }
    }

    // ── Sprint 3 — Local backup file (SAF, encrypted, checksummed) ─────────
    private val localBackup = com.notification.app.data.repository.LocalBackupManager(
        getApplication(), db, preferencesRepository
    )

    private val _fileBackupState = MutableStateFlow<String?>(null)
    val fileBackupState: StateFlow<String?> = _fileBackupState.asStateFlow()

    private val _fileRestoreState = MutableStateFlow<String?>(null)
    val fileRestoreState: StateFlow<String?> = _fileRestoreState.asStateFlow()

    private val _restorePreview =
        MutableStateFlow<com.notification.app.data.repository.LocalBackupManager.RestorePreview?>(null)
    val restorePreview: StateFlow<com.notification.app.data.repository.LocalBackupManager.RestorePreview?> =
        _restorePreview.asStateFlow()

    private fun ar() = language.value == "ar"

    /** Suggested backup file name for the SAF create-document picker. */
    fun suggestedBackupFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "Rafeeq_Backup_$stamp.rafeeq"
    }

    fun exportBackupToFile(uri: android.net.Uri) {
        if (_fileBackupState.value == "syncing") return
        _fileBackupState.value = "syncing"
        viewModelScope.launch {
            val result = withTimeoutOrNull(60_000L) {
                withContext(kotlinx.coroutines.Dispatchers.IO) { localBackup.exportToUri(uri) }
            }
            _fileBackupState.value = when (result) {
                is com.notification.app.data.repository.LocalBackupManager.Result.Success ->
                    if (ar()) "success:تم حفظ ${result.count} عنصر في الملف"
                    else "success:${result.count} items saved to file"
                is com.notification.app.data.repository.LocalBackupManager.Result.Failure ->
                    "error: ${result.reason}"
                null -> "error: " + (if (ar()) "استغرق الحفظ وقتًا طويلًا" else "Backup timed out")
            }
        }
    }

    /** Step 1 of restore — validate + build a preview for the confirm dialog. */
    fun prepareRestoreFromFile(uri: android.net.Uri) {
        if (_fileRestoreState.value == "syncing") return
        _fileRestoreState.value = "syncing"
        viewModelScope.launch {
            val validation = withTimeoutOrNull(60_000L) {
                withContext(kotlinx.coroutines.Dispatchers.IO) { localBackup.readAndValidate(uri) }
            }
            when (validation) {
                is com.notification.app.data.repository.LocalBackupManager.Result.Failure -> {
                    _fileRestoreState.value = "error: ${validation.reason}"
                }
                null -> _fileRestoreState.value =
                    "error: " + (if (ar()) "استغرقت القراءة وقتًا طويلًا" else "Reading timed out")
                else -> {
                    val preview = withContext(kotlinx.coroutines.Dispatchers.IO) { localBackup.preview(uri) }
                    if (preview == null) {
                        _fileRestoreState.value =
                            "error: " + (if (ar()) "تعذّر تحضير المعاينة" else "Couldn't prepare preview")
                    } else {
                        _fileRestoreState.value = null
                        _restorePreview.value = preview
                    }
                }
            }
        }
    }

    /** Step 2 — the user confirmed; apply the previewed restore, then re-arm. */
    fun confirmRestoreFromFile() {
        val preview = _restorePreview.value ?: return
        _restorePreview.value = null
        _fileRestoreState.value = "syncing"
        viewModelScope.launch {
            val result = withTimeoutOrNull(60_000L) {
                withContext(kotlinx.coroutines.Dispatchers.IO) { localBackup.applyRestore(preview.inner) }
            }
            _fileRestoreState.value = when (result) {
                is com.notification.app.data.repository.LocalBackupManager.Result.Success -> {
                    rescheduleEverythingAfterRestore()
                    if (ar()) "success:تمت استعادة ${result.count} عنصر" else "success:${result.count} items restored"
                }
                is com.notification.app.data.repository.LocalBackupManager.Result.Failure ->
                    "error: ${result.reason}"
                null -> "error: " + (if (ar()) "استغرقت الاستعادة وقتًا طويلًا" else "Restore timed out")
            }
        }
    }

    fun cancelRestorePreview() {
        _restorePreview.value = null
        _fileRestoreState.value = null
    }

    // ── Sprint 3 — automatic scheduled backup ──────────────────────────────
    val autoBackupFreq: StateFlow<String> = preferencesRepository.autoBackupFreqFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "OFF")
    val autoBackupCharging: StateFlow<Boolean> = preferencesRepository.autoBackupChargingFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val autoBackupWifi: StateFlow<Boolean> = preferencesRepository.autoBackupWifiFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val autoBackupTreeUri: StateFlow<String> = preferencesRepository.autoBackupTreeUriFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val autoBackupLast: StateFlow<Long> = preferencesRepository.autoBackupLastFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** Re-arm the WorkManager schedule from the latest persisted settings. */
    private suspend fun rearmAutoBackup() {
        com.notification.app.domain.backup.AutoBackupScheduler.reschedule(
            getApplication(),
            preferencesRepository.autoBackupFreqFlow.first(),
            preferencesRepository.autoBackupChargingFlow.first(),
            preferencesRepository.autoBackupWifiFlow.first()
        )
    }

    fun setAutoBackupFrequency(value: String) {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupFreq(value)
            rearmAutoBackup()
        }
    }

    fun setAutoBackupCharging(value: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupCharging(value)
            rearmAutoBackup()
        }
    }

    fun setAutoBackupWifi(value: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupWifi(value)
            rearmAutoBackup()
        }
    }

    /** The user granted a destination folder — persist the URI (with a
     *  persistable permission held by the Activity) and re-arm. */
    fun setAutoBackupFolder(treeUri: android.net.Uri) {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupTreeUri(treeUri.toString())
            rearmAutoBackup()
        }
    }

    /** After a restore, re-schedule alarms + pending reminders so restored
     *  items actually fire (their AlarmManager registrations are gone). */
    private suspend fun rescheduleEverythingAfterRestore() {
        val now = System.currentTimeMillis()
        repository.getActiveAlarms().forEach { alarm ->
            if (alarm.timeInMillis > now) AlarmManagerScheduler.scheduleExactAlarm(getApplication(), alarm)
        }
        repository.pendingReminders.first()
            .filter { !it.isArchived && it.dueDate > now }
            .forEach { AlarmManagerScheduler.scheduleReminderAlarm(getApplication(), it) }
    }
}
