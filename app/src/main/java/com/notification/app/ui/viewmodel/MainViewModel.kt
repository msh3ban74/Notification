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
import com.notification.app.domain.model.AiSuggestion
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

    // Sprint 6 — Executive Dashboard: upcoming gam3iya payouts widget.
    val allGam3iyaMembers: StateFlow<List<Gam3iyaMemberEntity>> = repository.allGam3iyaMembers
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

    // Dashboard "Rafeeq Suggestions" — produced by the EXISTING Gemini
    // pipeline from real data (see GeminiRepository.generateDashboardSuggestions).
    private val _aiSuggestions = MutableStateFlow<List<AiSuggestion>>(emptyList())
    val aiSuggestions: StateFlow<List<AiSuggestion>> = _aiSuggestions.asStateFlow()

    private val _aiSuggestionsLoading = MutableStateFlow(false)
    val aiSuggestionsLoading: StateFlow<Boolean> = _aiSuggestionsLoading.asStateFlow()

    // Sprint 6 — TTL cache for AI suggestions: within the TTL the cached
    // list is served instantly with no network call; pull-to-refresh
    // passes force=true to bypass. The UI is never blocked either way.
    private var aiSuggestionsFetchedAt = 0L

    /**
     * Refreshes the dashboard AI suggestions. Reuses the existing Gemini
     * repository and API-key resolution; on failure the previous list is
     * kept (or stays empty, letting the dashboard fall back to its local
     * rule-based insights).
     */
    fun refreshAiSuggestions(isArabic: Boolean, force: Boolean = false) {
        if (_aiSuggestionsLoading.value) return
        val fresh = System.currentTimeMillis() - aiSuggestionsFetchedAt < AI_SUGGESTIONS_TTL_MS
        if (!force && _aiSuggestions.value.isNotEmpty() && fresh) return
        viewModelScope.launch {
            _aiSuggestionsLoading.value = true
            val result = geminiRepository.generateDashboardSuggestions(
                isArabic = isArabic
            )
            if (result.isNotEmpty()) {
                _aiSuggestions.value = result
                aiSuggestionsFetchedAt = System.currentTimeMillis()
            }
            _aiSuggestionsLoading.value = false
        }
    }

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
        _aiSuggestions.value = emptyList()
        aiSuggestionsFetchedAt = 0L
        _isAiLoading.value = false
        persistChat()
    }

    companion object {
        /** How long cached AI suggestions stay fresh before a silent re-fetch. */
        const val AI_SUGGESTIONS_TTL_MS: Long = 15 * 60 * 1000L

        /** Hard ceiling for a single AI generation before a friendly cancel.
         *  30s covers a tool round-trip plus one transient retry; the OkHttp
         *  call timeout (25s) usually trips first, so this is the backstop. */
        const val AI_GENERATION_TIMEOUT_MS: Long = 30_000L
    }

    // Water counter state
    private val _waterCount = MutableStateFlow(0)
    val waterCount: StateFlow<Int> = _waterCount.asStateFlow()

    init {
        // Stability sprint — runtime verification of the key pipeline
        // (GitHub Secret -> .env -> BuildConfig). Logs availability ONLY.
        Log.i("Rafeeq", "API Key Available = " + BuildConfig.GEMINI_API_KEY.isNotBlank())
        updatePrayerTimes()
        loadChat()
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
        viewModelScope.launch { repository.insertPerson(person) }
    }

    fun addLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            repository.insertLedgerTransaction(tx)
        }
    }

    /** Sprint 5 — edit flow for debts: updates an existing ledger transaction in place. */
    fun updateLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
            repository.updateLedgerTransaction(tx)
        }
    }

    fun deleteLedgerTransaction(tx: LedgerTransactionEntity) {
        viewModelScope.launch {
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
                    date = System.currentTimeMillis(),
                    note = note,
                    linkedReminderId = linkedReminderId
                )
            )
        }
    }

    // Gam3iya Actions
    fun getMembersForGam3iya(id: Long): Flow<List<Gam3iyaMemberEntity>> = repository.getMembersForGam3iya(id)

    fun createGam3iya(
        title: String,
        totalAmount: Double,
        monthlyInstallment: Double,
        memberNamesWithTurns: List<Pair<String, Int>>,
        startDate: Long
    ) {
        viewModelScope.launch {
            val gam3iya = Gam3iyaEntity(
                title = title,
                totalAmount = totalAmount,
                monthlyInstallment = monthlyInstallment,
                membersCount = memberNamesWithTurns.size,
                startDate = startDate,
                createdAt = System.currentTimeMillis()
            )
            repository.createGam3iyaWithMembers(gam3iya, memberNamesWithTurns)
        }
    }

    // ── Sprint 4 — professional Gam3iya management ─────────────────────
    val allGam3iyaPayments: StateFlow<List<Gam3iyaPaymentEntity>> =
        repository.allGam3iyaPayments.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allGam3iyaAttachments: StateFlow<List<Gam3iyaAttachmentEntity>> =
        repository.allGam3iyaAttachments.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getPaymentsForGam3iya(id: Long): Flow<List<Gam3iyaPaymentEntity>> =
        repository.getPaymentsForGam3iya(id)
    fun getAttachmentsForGam3iya(id: Long): Flow<List<Gam3iyaAttachmentEntity>> =
        repository.getAttachmentsForGam3iya(id)

    /** Create a MANAGER-mode gam3iya with a fully-detailed member list.
     *  Returns nothing; the list flow updates reactively. */
    fun createManagerGam3iya(gam3iya: Gam3iyaEntity, members: List<Gam3iyaMemberEntity>) {
        viewModelScope.launch {
            val base = gam3iya.copy(
                mode = "MANAGER",
                membersCount = members.size,
                createdAt = if (gam3iya.createdAt == 0L) System.currentTimeMillis() else gam3iya.createdAt
            )
            val id = repository.insertGam3iya(base)
            members.forEach { m ->
                val payout = if (m.payoutDate > 0) m.payoutDate
                else Gam3iyaCalculator.calculateMemberPayoutDate(base.startDate, m.turnMonth)
                repository.insertGam3iyaMember(m.copy(gam3iyaId = id, payoutDate = payout))
            }
            syncGam3iyaReminder(base.copy(id = id, membersCount = members.size))
        }
    }

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

    fun setGam3iyaStatus(gam3iya: Gam3iyaEntity, status: String) {
        viewModelScope.launch {
            val updated = gam3iya.copy(status = status)
            repository.updateGam3iya(updated)
            syncGam3iyaReminder(updated)
        }
    }

    fun addGam3iyaMember(gam3iyaId: Long, member: Gam3iyaMemberEntity) {
        viewModelScope.launch {
            val payout = if (member.payoutDate > 0) member.payoutDate else {
                val g = repository.getGam3iyaById(gam3iyaId)
                if (g != null) Gam3iyaCalculator.calculateMemberPayoutDate(g.startDate, member.turnMonth) else 0L
            }
            repository.insertGam3iyaMember(member.copy(gam3iyaId = gam3iyaId, payoutDate = payout))
        }
    }

    fun updateGam3iyaMember(member: Gam3iyaMemberEntity) {
        viewModelScope.launch { repository.updateGam3iyaMember(member) }
    }

    fun deleteGam3iyaMember(member: Gam3iyaMemberEntity) {
        viewModelScope.launch { repository.deleteGam3iyaMember(member) }
    }

    /** Mark this member's installment paid/unpaid for the current month and
     *  log a payment record when marking paid. */
    fun markMemberInstallmentPaid(gam3iya: Gam3iyaEntity, member: Gam3iyaMemberEntity, paid: Boolean) {
        viewModelScope.launch {
            repository.updateGam3iyaMember(member.copy(isInstallmentPaidThisMonth = paid, isLate = if (paid) false else member.isLate))
            if (paid) {
                val amount = if (member.installmentAmount > 0) member.installmentAmount else gam3iya.monthlyInstallment
                repository.insertGam3iyaPayment(
                    Gam3iyaPaymentEntity(
                        gam3iyaId = gam3iya.id, memberId = member.id,
                        monthIndex = Gam3iyaCalculator.computeStatus(gam3iya, emptyList()).currentMonthIndex,
                        amount = amount, date = System.currentTimeMillis(), type = "INSTALLMENT"
                    )
                )
            }
        }
    }

    /** Mark this member as having collected (received) their payout. */
    fun markMemberCollected(gam3iya: Gam3iyaEntity, member: Gam3iyaMemberEntity, collected: Boolean) {
        viewModelScope.launch {
            repository.updateGam3iyaMember(member.copy(isPayoutReceived = collected))
            if (collected) {
                val amount = if (gam3iya.totalAmount > 0) gam3iya.totalAmount else gam3iya.monthlyInstallment * member.turnMonth
                repository.insertGam3iyaPayment(
                    Gam3iyaPaymentEntity(
                        gam3iyaId = gam3iya.id, memberId = member.id,
                        monthIndex = member.turnMonth, amount = amount,
                        date = System.currentTimeMillis(), type = "COLLECTION"
                    )
                )
            }
            // Collecting advances the next collector → reschedule the reminder.
            syncGam3iyaReminder(gam3iya)
        }
    }

    fun toggleMemberLate(member: Gam3iyaMemberEntity, late: Boolean) {
        viewModelScope.launch { repository.updateGam3iyaMember(member.copy(isLate = late)) }
    }

    /** PARTICIPANT mode — record that I paid one more installment. */
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

    fun deleteGam3iyaPayment(payment: Gam3iyaPaymentEntity) {
        viewModelScope.launch { repository.deleteGam3iyaPayment(payment) }
    }

    fun addGam3iyaAttachment(gam3iyaId: Long, memberId: Long, uri: String, kind: String, label: String) {
        viewModelScope.launch {
            repository.insertGam3iyaAttachment(
                Gam3iyaAttachmentEntity(
                    gam3iyaId = gam3iyaId, memberId = memberId, uri = uri,
                    kind = kind, label = label, createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteGam3iyaAttachment(a: Gam3iyaAttachmentEntity) {
        viewModelScope.launch { repository.deleteGam3iyaAttachment(a) }
    }

    // ── Sprint 4 (phase 5) — notifications/alarms for a gam3iya ────────
    // A single rolling "next event" reminder per gam3iya, upserted with a
    // stable id derived from the gam3iya id (no schema change, no
    // duplicates). It fires before + on the next collection (manager) or
    // the next installment/collection (participant), reusing the app's one
    // reminder+alarm pipeline.
    private fun gam3iyaReminderId(gam3iyaId: Long): Long = 900_000_000L + gam3iyaId

    /** (Re)compute and (re)schedule the next-event reminder for a gam3iya. */
    private suspend fun syncGam3iyaReminder(gam3iya: Gam3iyaEntity) {
        val rid = gam3iyaReminderId(gam3iya.id)
        val app = getApplication<android.app.Application>()
        val now = System.currentTimeMillis()

        // Cancel path: reminders off, archived, or completed.
        if (!gam3iya.reminderEnabled || gam3iya.status != "ACTIVE") {
            AlarmManagerScheduler.cancelReminderAlarm(app, rid)
            repository.deleteReminderById(rid)
            return
        }

        val members = if (gam3iya.mode == "PARTICIPANT") emptyList()
        else repository.getMembersForGam3iya(gam3iya.id).first()
        val status = Gam3iyaCalculator.computeStatus(gam3iya, members)

        val (date, label) = if (gam3iya.mode == "PARTICIPANT") {
            val d = if (gam3iya.myCollectionDate > now) gam3iya.myCollectionDate
            else Gam3iyaCalculator.calculateMemberPayoutDate(gam3iya.startDate, gam3iya.myPaidInstallments + 1)
            d to (if (ar()) "قسط جمعية: ${gam3iya.title}" else "Gam3iya installment: ${gam3iya.title}")
        } else {
            val nc = status.nextCollector
            (status.nextCollectionDate) to (
                if (nc != null) (if (ar()) "قبض جمعية: ${nc.memberName}" else "Gam3iya payout: ${nc.memberName}")
                else (if (ar()) "جمعية: ${gam3iya.title}" else "Gam3iya: ${gam3iya.title}")
                )
        }

        if (date <= now) {
            AlarmManagerScheduler.cancelReminderAlarm(app, rid)
            repository.deleteReminderById(rid)
            return
        }

        val reminder = ReminderEntity(
            id = rid,
            title = label,
            note = if (ar()) "تذكير تلقائي من الجمعية" else "Automatic gam3iya reminder",
            dueDate = date,
            category = ReminderCategory.MONEY.name,
            preAlerts = if (gam3iya.reminderDaysBefore >= 1) "ONE_DAY,ONE_HOUR" else "ONE_HOUR"
        )
        repository.insertReminder(reminder) // REPLACE upsert on the stable id
        AlarmManagerScheduler.scheduleReminderAlarm(app, reminder)
    }

    /** Public trigger used by create/edit/mark flows. */
    fun refreshGam3iyaReminder(gam3iya: Gam3iyaEntity) {
        viewModelScope.launch { syncGam3iyaReminder(gam3iya) }
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
        _waterCount.value++
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
                if (isArabic) "لا يوجد اتصال بالإنترنت — تأكد من الشبكة وحاول مجددًا 🌐"
                else "No internet connection — check your network and try again 🌐"
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
                    onLogWater = { incrementWater() }
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
                                "استغرق الرد وقتًا أطول من المعتاد فأوقفته — اضغط \"إعادة\" للمحاولة 🙏"
                            else "That took longer than usual, so I stopped it — tap \"Regenerate\" to retry 🙏"
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
            if (language.value == "ar") "أوقفت الرد. اضغط \"إعادة\" وقت ما تحب."
            else "Stopped. Tap \"Regenerate\" whenever you like."
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
                is com.notification.app.data.repository.BackupRepository.BackupResult.Success ->
                    if (language.value == "ar") "success:تمت استعادة ${result.count} عنصر"
                    else "success:${result.count} items restored"
                is com.notification.app.data.repository.BackupRepository.BackupResult.Failure ->
                    "error: ${result.message}"
                null ->
                    "error: " + (if (language.value == "ar")
                        "استغرقت الاستعادة وقتًا طويلًا — تأكد من الاتصال وحاول مجددًا"
                    else "Restore timed out — check your connection and try again")
            }
        }
    }

    /** Called right after a successful sign-in to pull any existing cloud backup down. */
    fun restoreFromBackup() {
        viewModelScope.launch {
            backupRepository.restoreLatest()
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
