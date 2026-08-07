package com.notification.app.data.repository

import com.notification.app.BuildConfig
import com.notification.app.data.local.entities.AlarmEntity
import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.HabitEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.data.local.entities.ReminderEntity
import com.notification.app.data.remote.*
import com.notification.app.domain.calculator.Gam3iyaCalculator
import com.notification.app.domain.calculator.HabitCalculator
import com.notification.app.domain.calculator.LedgerCalculator
import com.notification.app.domain.model.LedgerTransactionType
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class GeminiRepository(
    private val notificationRepository: NotificationRepository
) {

    private val toolsDefinition = listOf(
        GeminiTool(
            functionDeclarations = listOf(
                GeminiFunctionDeclaration(
                    name = "getReminders",
                    description = "Get a list of all upcoming reminders and due dates in the app.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "addReminder",
                    description = "Create a new reminder. Do NOT compute epoch timestamps yourself — " +
                        "give the time as minutesFromNow for relative requests ('in 30 minutes', 'after an hour'), " +
                        "or as hour (0-23) and minute for a clock time ('at 7', 'at 20:30'); the app resolves the exact time.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "title" to mapOf("type" to "STRING", "description" to "Reminder title"),
                            "note" to mapOf("type" to "STRING", "description" to "Optional note"),
                            "minutesFromNow" to mapOf("type" to "NUMBER", "description" to "Minutes from now (for relative times)"),
                            "hour" to mapOf("type" to "NUMBER", "description" to "Clock hour 0-23 (device local time)"),
                            "minute" to mapOf("type" to "NUMBER", "description" to "Clock minute 0-59 (defaults to 0)"),
                            "category" to mapOf("type" to "STRING", "description" to "Category: MONEY, APPOINTMENT, BIRTHDAY, BILL, TUTORING, MEDICINE, WORK, EVENT, PERSONAL, or CUSTOM"),
                            "recurrence" to mapOf("type" to "STRING", "description" to "Repeat: NONE (default), DAILY, WEEKLY, MONTHLY or YEARLY. Use DAILY for medicines taken every day.")
                        ),
                        "required" to listOf("title")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "getDebtsAndLedger",
                    description = "Get summary of all debts, loans, and net balances per person.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "addLedgerTransaction",
                    description = "Record a personal debt or loan with a person. When dueInDays is given, " +
                        "the app also creates a due-date reminder automatically so the user is alerted on time.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "personName" to mapOf("type" to "STRING", "description" to "Name of the person"),
                            "transactionType" to mapOf("type" to "STRING", "description" to "Type: GAVE_THEM (I lent money / money owed TO me), THEY_GAVE_BACK (they repaid), THEY_GAVE_ME (I borrowed / money owed BY me), I_GAVE_BACK (I repaid)"),
                            "amount" to mapOf("type" to "NUMBER", "description" to "Transaction amount in EGP"),
                            "dueInDays" to mapOf("type" to "NUMBER", "description" to "Days from now until the repayment date (optional; creates an automatic reminder)"),
                            "note" to mapOf("type" to "STRING", "description" to "Optional note / reason")
                        ),
                        "required" to listOf("personName", "transactionType", "amount")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "getGam3iyaInfo",
                    description = "Get the status of the gam3iya (savings circle) the user takes part in: their monthly installment, how many they paid of how many, their turn number, and when they collect. Use this for any gam3iya question.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "createGam3iya",
                    description = "Track a gam3iya (savings circle) the user takes part in. Rafeeq will remind them of every monthly installment and of their collection turn. Needs: name, monthly installment, and how many months; turn number and organizer name are optional.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "title" to mapOf("type" to "STRING", "description" to "Gam3iya name"),
                            "monthlyInstallment" to mapOf("type" to "NUMBER", "description" to "The user's monthly installment amount"),
                            "months" to mapOf("type" to "NUMBER", "description" to "How many months the circle runs"),
                            "myTurnNumber" to mapOf("type" to "NUMBER", "description" to "Which month the user collects (1-based, optional)"),
                            "organizerName" to mapOf("type" to "STRING", "description" to "Who organizes the circle (optional)"),
                            "note" to mapOf("type" to "STRING", "description" to "Optional note")
                        ),
                        "required" to listOf("title", "monthlyInstallment", "months")
                    )
                ),
                // Phase E — the assistant reads ALL modules.
                GeminiFunctionDeclaration(
                    name = "getFinancialItems",
                    description = "Get all tracked bills, installments and subscriptions with amounts, due dates and paid status.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "addFinancialItem",
                    description = "Track a new bill, installment or subscription. A due-date reminder is created and scheduled automatically. For an INSTALLMENT give totalPrice, downPayment and monthlyAmount; for a BILL or SUBSCRIPTION give amount. Give the due date as dueInDays from now (e.g. 'due in 10 days' → 10, 'next month' → 30).",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "type" to mapOf("type" to "STRING", "description" to "BILL, INSTALLMENT or SUBSCRIPTION"),
                            "title" to mapOf("type" to "STRING", "description" to "Company / item / service name"),
                            "amount" to mapOf("type" to "NUMBER", "description" to "Bill amount or subscription monthly amount"),
                            "totalPrice" to mapOf("type" to "NUMBER", "description" to "INSTALLMENT: total price"),
                            "downPayment" to mapOf("type" to "NUMBER", "description" to "INSTALLMENT: down payment already paid"),
                            "monthlyAmount" to mapOf("type" to "NUMBER", "description" to "INSTALLMENT: monthly payment"),
                            "dueInDays" to mapOf("type" to "NUMBER", "description" to "Days from now until the next due / renewal date"),
                            "seller" to mapOf("type" to "STRING", "description" to "Optional seller / store / provider"),
                            "recurring" to mapOf("type" to "BOOLEAN", "description" to "True for a recurring bill/subscription"),
                            "currency" to mapOf("type" to "STRING", "description" to "Currency code, defaults to EGP")
                        ),
                        "required" to listOf("type", "title")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "getHabits",
                    description = "Get the user's habits with current streaks and whether each is done today.",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "addHabit",
                    description = "Create a new daily habit for the user to track.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "title" to mapOf("type" to "STRING", "description" to "Habit name, e.g. 'Read 10 pages'"),
                            "emoji" to mapOf("type" to "STRING", "description" to "Optional single emoji for the habit")
                        ),
                        "required" to listOf("title")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "logWater",
                    description = "Record that the user drank one glass of water (adds to today's water tracker).",
                    parameters = mapOf("type" to "OBJECT", "properties" to emptyMap<String, Any>())
                ),
                GeminiFunctionDeclaration(
                    name = "completeHabitToday",
                    description = "Mark one of the user's habits as completed for today (by its name).",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "habitName" to mapOf("type" to "STRING", "description" to "Name (or part of the name) of the habit to check off")
                        ),
                        "required" to listOf("habitName")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "setSmartAlarm",
                    description = "Set an alarm. Do NOT compute epoch timestamps yourself — " +
                        "give minutesFromNow for relative requests ('in 5 minutes', 'wake me in an hour'), " +
                        "or hour (0-23) and minute for a clock time ('6 AM' → hour 6, 'wake me 7:30' → hour 7 minute 30); " +
                        "the app resolves the exact time in the device's own clock.",
                    parameters = mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "title" to mapOf("type" to "STRING", "description" to "Alarm label/title"),
                            "minutesFromNow" to mapOf("type" to "NUMBER", "description" to "Minutes from now (for relative alarms)"),
                            "hour" to mapOf("type" to "NUMBER", "description" to "Clock hour 0-23 (device local time)"),
                            "minute" to mapOf("type" to "NUMBER", "description" to "Clock minute 0-59 (defaults to 0)")
                        ),
                        "required" to listOf("title")
                    )
                )
            )
        )
    )

    suspend fun sendMessage(
        history: List<GeminiContent>,
        userMessage: String,
        customApiKey: String? = null,
        isArabic: Boolean = false,
        onAlarmCreated: suspend (AlarmEntity) -> Unit = {},
        onReminderCreated: suspend (Long) -> Unit = {},
        onLogWater: suspend () -> Unit = {},
        onGam3iyaCreated: suspend (Long) -> Unit = {},
    ): Pair<String, List<GeminiContent>> {
        // كل المفاتيح تعيش في GitHub Secrets → BuildConfig — لا تُعرض أبدًا
        // في واجهة التطبيق ولا تُسجَّل في اللوج. GEMINI_API_KEY_2/3 حصص مجانية
        // إضافية يتدوَّر عليها تلقائيًا، وGROQ_API_KEY هو العقل الاحتياطي.
        val apiKeys = buildList {
            add(if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY)
            add(BuildConfig.GEMINI_API_KEY_2)
            add(BuildConfig.GEMINI_API_KEY_3)
        }
        val fallbackApiKey = BuildConfig.GROQ_API_KEY
        val updatedHistory = history.toMutableList()

        // Append user message
        updatedHistory.add(
            GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))
        )

        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = """You are Rafeeq (رفيق) — the in-app assistant of a personal memory app. The app's whole purpose: the user tells you things to remember (medicines, tasks, personal debts with people, bills/installments, a gam3iya they take part in, habits, water, alarms) and the app reminds them at the right time. Everything you save with a tool becomes a REAL scheduled notification on the device.

TONE: professional, warm, concise — polite Arabic or clean English matching the user's language. No slang, no emojis, no filler. Never mention AI providers or internal names like Gemini.

HOW YOU WORK — guided conversations. When the user wants to record something and details are missing, ask for the missing details ONLY, briefly (one short message, at most two questions). Never invent a value the user didn't say. Once you have everything, CALL THE TOOL immediately — never claim something was saved without a tool call — then confirm in one short sentence using the tool's result.

THE CORE FLOWS:
1. MEDICINE ("ذكرني بدواء"): collect (a) medicine name, (b) how many times per day, (c) the time of each dose. Then call addReminder once PER dose time: title "دواء: <name>", category MEDICINE, recurrence DAILY, hour/minute of that dose. Confirm all doses in one line.
2. DEBT ("سجل دين"): collect (a) direction — is the money owed TO the user (لك: أنت سلّفت) or BY the user (عليك: أنت اقترضت)? (b) the person's name, (c) the amount, (d) when it will be repaid. Then call addLedgerTransaction: GAVE_THEM when the money is owed to the user, THEY_GAVE_ME when the user owes it; pass dueInDays computed from the repayment date the user gave (relative to now). The app auto-creates the repayment reminder. Confirm with the date.
3. TASK ("سجل مهمة"): collect (a) what the task is, (b) when it is due (and whether it repeats). Then call addReminder with a fitting category (WORK, APPOINTMENT, PERSONAL…) and hour/minute or minutesFromNow. Confirm.
4. "ما مهامي اليوم" / what's due: call getReminders and answer with a short list of only what is due today plus anything overdue.

Other abilities when asked: bills/installments/subscriptions (addFinancialItem/getFinancialItems), gam3iya status (getGam3iyaInfo/createGam3iya), habits (addHabit/completeHabitToday/getHabits), water (logWater), alarms (setSmartAlarm). Answer questions about the user's data ONLY from tool reads — never guess.

TIME RULES: never compute epoch timestamps yourself; always pass minutesFromNow OR hour+minute and let the device resolve them. Current device time: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}."""
                )
            )
        )

        try {
            val request = GeminiRequest(
                contents = updatedHistory,
                tools = toolsDefinition,
                systemInstruction = systemInstruction
            )

            val response = generateWithRetry(apiKeys, request)
            val candidateContent = response.candidates?.firstOrNull()?.content

            if (candidateContent != null) {
                updatedHistory.add(candidateContent)
                val part = candidateContent.parts.firstOrNull()

                if (part?.functionCall != null) {
                    val fnCall = part.functionCall
                    // The tool runs NOW — its effect (alarm set, reminder
                    // added…) is already committed regardless of what the
                    // second round does.
                    val toolResult = executeTool(fnCall, isArabic, onAlarmCreated, onReminderCreated, onLogWater, onGam3iyaCreated)

                    // Provide function response back to model
                    val toolResponseContent = GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(
                                functionResponse = GeminiFunctionResponse(
                                    name = fnCall.name,
                                    response = mapOf("result" to toolResult)
                                )
                            )
                        )
                    )
                    updatedHistory.add(toolResponseContent)

                    // Second turn after tool call — ISOLATED. If the newer
                    // model rejects the follow-up (e.g. a signature quirk) or
                    // the network blips, we still confirm what actually
                    // happened instead of throwing away a completed action.
                    val finalText = try {
                        val followUpRequest = GeminiRequest(
                            contents = updatedHistory,
                            tools = toolsDefinition,
                            systemInstruction = systemInstruction
                        )
                        val followUpResponse = generateWithRetry(apiKeys, followUpRequest)
                        followUpResponse.candidates?.firstOrNull()?.content?.parts
                            ?.firstOrNull { !it.text.isNullOrBlank() }?.text
                            ?: toolResult.toString()
                    } catch (e: Exception) {
                        // Tool already succeeded — report its result plainly.
                        toolResult.toString()
                    }

                    updatedHistory.add(
                        GeminiContent(role = "model", parts = listOf(GeminiPart(text = finalText)))
                    )
                    return Pair(finalText, updatedHistory)
                } else if (part?.text != null) {
                    return Pair(part.text, updatedHistory)
                }
            }

            // No parsable candidate — surface it as a VISIBLE model bubble
            // (returning text without appending it left the chat empty).
            val fallback = "تعذّرت معالجة الرد هذه المرة. أعد صياغة رسالتك وحاول مجددًا.\nCouldn't process that. Please rephrase and try again."
            updatedHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = fallback))))
            return Pair(fallback, updatedHistory)
        } catch (e: Exception) {
            // العقل الاحتياطي: جيميناي وقع بالكامل — لو فيه مفتاح مزود تاني
            // في الإعدادات، رفيق يرد منه (نص فقط) بدل ما يعتذر.
            if (!fallbackApiKey.isNullOrBlank()) {
                try {
                    val text = fallbackChat(fallbackApiKey, systemInstruction, updatedHistory)
                    if (!text.isNullOrBlank()) {
                        updatedHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = text))))
                        return Pair(text, updatedHistory)
                    }
                } catch (_: Exception) {
                    // Fall through to the friendly primary-error message.
                }
            }
            val errorText = friendlyError(e, isArabic)
            updatedHistory.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = errorText))))
            return Pair(errorText, updatedHistory)
        }
    }

    /**
     * Text-only conversation through the OpenAI-compatible fallback
     * provider. Tools are not available there, so the system prompt tells
     * the model to answer from context and warmly defer any create-requests.
     */
    private suspend fun fallbackChat(
        key: String,
        system: GeminiContent,
        history: List<GeminiContent>
    ): String? {
        val messages = buildList {
            add(
                OpenAiMessage(
                    role = "system",
                    content = (system.parts.firstOrNull()?.text ?: "") +
                        " IMPORTANT: your action tools are temporarily unavailable, so you can only TALK right now. Answer questions from the conversation. If the user asks you to create/save something, warmly tell them to try again in a few minutes."
                )
            )
            history.forEach { c ->
                val text = c.parts.firstOrNull { !it.text.isNullOrBlank() }?.text ?: return@forEach
                add(OpenAiMessage(role = if (c.role == "model") "assistant" else "user", content = text))
            }
        }
        val response = FallbackAiClient.service.chat(
            authorization = "Bearer $key",
            request = OpenAiChatRequest(model = FallbackAiService.MODEL, messages = messages)
        )
        return response.choices?.firstOrNull()?.message?.content?.trim()
    }

    /**
     * Maps any pipeline failure to a professional, localized message — the
     * user always learns WHAT went wrong (network, quota, key, server…)
     * and never sees a raw stack trace or a silent empty bubble. Internal
     * exception text is never surfaced (security).
     */
    /**
     * One automatic retry for TRANSIENT failures only (server 5xx or a
     * timeout) with a short backoff — recovers from a blip without the
     * user noticing. Permanent errors (bad key, quota, 4xx) throw
     * immediately so the mapped message shows at once.
     */
    /**
     * تدوير المفاتيح — quota exhaustion never silences Rafeeq:
     *  Round 1: the newest flash model on EVERY key (built-in + the extra
     *           keys the user added in Settings) — each key is its own
     *           free-quota bucket, so the next key answers when one is dry.
     *  Round 2: the lite model (a separate quota bucket again) on every key.
     * A key that fails for any reason just yields to the next one; only
     * when the whole ladder is exhausted does the error surface (and the
     * caller may still fall back to the second provider).
     */
    private suspend fun generateWithRetry(apiKeys: List<String>, request: GeminiRequest): GeminiResponse {
        var lastError: Exception? = null
        val keys = apiKeys.filter { it.isNotBlank() }.distinct().ifEmpty { listOf("") }
        for (model in listOf(GeminiApiService.PRIMARY_MODEL, GeminiApiService.LITE_MODEL)) {
            for (key in keys) {
                try {
                    return RetrofitClient.geminiService.generateContent(model, key, request)
                } catch (e: Exception) {
                    lastError = e
                }
            }
            // Brief pause between model rounds — a burst 429 often clears.
            kotlinx.coroutines.delay(600)
        }
        throw lastError ?: IllegalStateException("unreachable")
    }

    private fun friendlyError(e: Exception, isArabic: Boolean): String {
        val http = (e as? retrofit2.HttpException)?.code()
        return when {
            e is java.net.UnknownHostException || e is java.net.ConnectException ->
                if (isArabic) "لا يوجد اتصال بالإنترنت. تحقق من الشبكة وحاول مرة أخرى."
                else "No internet connection. Check your network and try again."
            e is java.net.SocketTimeoutException || e is java.io.InterruptedIOException ->
                if (isArabic) "استغرق الرد وقتًا أطول من المتوقع. حاول مرة أخرى."
                else "The response took longer than expected. Please try again."
            http == 400 || http == 403 ->
                if (isArabic) "تعذّر الاتصال بخدمة المساعد. حاول لاحقًا."
                else "Couldn't connect to the assistant service. Try again later."
            http == 429 ->
                if (isArabic) "الخدمة تستقبل طلبات كثيرة حاليًا. حاول بعد دقيقة."
                else "The service is handling high demand. Try again in a minute."
            http != null && http >= 500 ->
                if (isArabic) "الخدمة غير متاحة مؤقتًا. حاول بعد قليل."
                else "The service is temporarily unavailable. Try again shortly."
            e is com.squareup.moshi.JsonDataException ->
                if (isArabic) "تعذّرت معالجة الرد. أعد صياغة سؤالك وحاول مجددًا."
                else "Couldn't process the reply. Rephrase and try again."
            else ->
                if (isArabic) "تعذّر الوصول للمساعد. تحقق من الاتصال وحاول مرة أخرى."
                else "Couldn't reach the assistant. Check your connection and try again."
        }
    }

    private suspend fun executeTool(
        call: GeminiFunctionCall,
        isArabic: Boolean,
        onAlarmCreated: suspend (AlarmEntity) -> Unit,
        onReminderCreated: suspend (Long) -> Unit,
        onLogWater: suspend () -> Unit,
        onGam3iyaCreated: suspend (Long) -> Unit
    ): Any {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return when (call.name) {
            "getReminders" -> {
                val reminders = notificationRepository.allReminders.first()
                if (reminders.isEmpty()) "No reminders found."
                else reminders.map { "${it.title} (Due: ${dateFormat.format(Date(it.dueDate))})" }
            }
            "addReminder" -> {
                val title = call.args["title"]?.toString() ?: "New Reminder"
                val note = call.args["note"]?.toString() ?: ""
                // Time is resolved on-device from minutesFromNow / hour+minute,
                // never from a model-computed epoch (which drifts by hours).
                val due = resolveTriggerTime(call.args, defaultOffsetMs = 3600000)
                val cat = call.args["category"]?.toString() ?: "CUSTOM"
                val recurrence = call.args["recurrence"]?.toString()
                    ?.uppercase()?.takeIf { it in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY") }
                    ?: "NONE"

                val id = notificationRepository.insertReminder(
                    ReminderEntity(title = title, note = note, dueDate = due, category = cat, recurrence = recurrence)
                )
                // Schedule the alert so an AI-created reminder actually fires.
                onReminderCreated(id)
                val repeatLabel = when (recurrence) {
                    "DAILY" -> if (isArabic) " ويتكرر يوميًا" else ", repeating daily"
                    "WEEKLY" -> if (isArabic) " ويتكرر أسبوعيًا" else ", repeating weekly"
                    "MONTHLY" -> if (isArabic) " ويتكرر شهريًا" else ", repeating monthly"
                    "YEARLY" -> if (isArabic) " ويتكرر سنويًا" else ", repeating yearly"
                    else -> ""
                }
                if (isArabic) "تم إنشاء التذكير \"$title\" في ${dateFormat.format(Date(due))}$repeatLabel."
                else "Reminder '$title' created for ${dateFormat.format(Date(due))}$repeatLabel."
            }
            "getDebtsAndLedger" -> {
                val persons = notificationRepository.allPersons.first()
                if (persons.isEmpty()) "No ledger records found."
                else {
                    persons.map { person ->
                        val txs = notificationRepository.getTransactionsForPerson(person.id).first()
                        val summary = LedgerCalculator.calculateNetBalance(txs)
                        "${person.name}: Status=${summary.status}, Net=${summary.netAmount} EGP"
                    }
                }
            }
            "addLedgerTransaction" -> {
                val name = call.args["personName"]?.toString() ?: "Contact"
                val type = call.args["transactionType"]?.toString() ?: "GAVE_THEM"
                val amount = (call.args["amount"] as? Number)?.toDouble() ?: 0.0
                val note = call.args["note"]?.toString() ?: ""
                val dueInDays = (call.args["dueInDays"] as? Number)?.toDouble() ?: 0.0

                val persons = notificationRepository.allPersons.first()
                var person = persons.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (person == null) {
                    val personId = notificationRepository.insertPerson(PersonEntity(name = name))
                    person = PersonEntity(id = personId, name = name)
                }

                // A promised repayment date becomes a real scheduled reminder
                // (same pipeline as tasks), linked to the transaction.
                var dueDate = 0L
                var reminderId: Long? = null
                if (dueInDays > 0) {
                    dueDate = System.currentTimeMillis() + (dueInDays * 24 * 60 * 60 * 1000).toLong()
                    val iOwe = type == "THEY_GAVE_ME"
                    reminderId = notificationRepository.insertReminder(
                        ReminderEntity(
                            title = if (isArabic) {
                                if (iOwe) "سداد ${amount.toLong()} ج.م لـ${person.name}"
                                else "تحصيل ${amount.toLong()} ج.م من ${person.name}"
                            } else {
                                if (iOwe) "Repay ${amount.toLong()} EGP to ${person.name}"
                                else "Collect ${amount.toLong()} EGP from ${person.name}"
                            },
                            note = note,
                            dueDate = dueDate,
                            category = "MONEY"
                        )
                    )
                    onReminderCreated(reminderId)
                }

                notificationRepository.insertLedgerTransaction(
                    LedgerTransactionEntity(
                        personId = person.id,
                        type = type,
                        amount = amount,
                        date = System.currentTimeMillis(),
                        note = note,
                        dueDate = dueDate,
                        linkedReminderId = reminderId
                    )
                )
                val duePart = if (dueDate > 0) {
                    if (isArabic) " مع تذكير بالسداد في ${dateFormat.format(Date(dueDate))}"
                    else " with a repayment reminder on ${dateFormat.format(Date(dueDate))}"
                } else ""
                if (isArabic) "تم تسجيل ${amount.toLong()} ج.م باسم ${person.name}$duePart."
                else "Recorded ${amount.toLong()} EGP with ${person.name}$duePart."
            }
            "getGam3iyaInfo" -> {
                val gam3iyas = notificationRepository.allGam3iyas.first()
                if (gam3iyas.isEmpty()) "The user doesn't track any gam3iya yet."
                else {
                    gam3iyas.map { g ->
                        val st = Gam3iyaCalculator.computeStatus(g, emptyList())
                        val cur = g.currency
                        val sb = StringBuilder()
                        sb.append("Gam3iya '${g.title}': my installment ${if (g.myInstallmentAmount > 0) g.myInstallmentAmount else g.monthlyInstallment} $cur, ")
                        sb.append("paid ${g.myPaidInstallments} of ${st.durationMonths} installments. ")
                        if (g.myTurnNumber > 0) sb.append("My turn is #${g.myTurnNumber}. ")
                        if (g.organizerName.isNotBlank()) sb.append("Organizer: ${g.organizerName}. ")
                        if (st.nextCollectionDate > 0) sb.append("My collection on ${dateFormat.format(Date(st.nextCollectionDate))}. ")
                        if (st.isFinished) sb.append("FINISHED.")
                        sb.toString()
                    }
                }
            }
            "createGam3iya" -> {
                val title = call.args["title"]?.toString()?.trim().orEmpty()
                val monthly = (call.args["monthlyInstallment"] as? Number)?.toDouble() ?: 0.0
                val months = (call.args["months"] as? Number)?.toInt() ?: 0
                if (title.isBlank() || monthly <= 0 || months <= 0) {
                    if (isArabic) "أحتاج اسم الجمعية وقسطك الشهري وعدد الشهور علشان أسجلها لك."
                    else "I need the gam3iya name, your monthly installment and the number of months."
                } else {
                    val organizer = call.args["organizerName"]?.toString().orEmpty()
                    val myTurn = (call.args["myTurnNumber"] as? Number)?.toInt() ?: 0
                    val note = call.args["note"]?.toString().orEmpty()
                    val now = System.currentTimeMillis()
                    val gam3iya = Gam3iyaEntity(
                        title = title, totalAmount = monthly * months, monthlyInstallment = monthly,
                        membersCount = 0, startDate = now, mode = "PARTICIPANT",
                        durationMonths = months, note = note, createdAt = now,
                        organizerName = organizer, myInstallmentAmount = monthly, myTurnNumber = myTurn
                    )
                    val id = notificationRepository.insertGam3iya(gam3iya)
                    onGam3iyaCreated(id)
                    if (isArabic) "تم تسجيل جمعية \"$title\" — $months شهرًا بقسط ${monthly.toLong()} ج.م، مع تذكير شهري بالقسط."
                    else "Gam3iya '$title' saved — $months months at ${monthly.toLong()} EGP, with a monthly reminder."
                }
            }
            // Phase E — read-only views over the financial + habit modules.
            "getFinancialItems" -> {
                val items = notificationRepository.allFinancialItems.first()
                if (items.isEmpty()) "No bills, installments or subscriptions tracked."
                else {
                    items.sortedBy { it.dueDate }.map {
                        val amount = if (it.monthlyAmount > 0) it.monthlyAmount else it.amount
                        "${it.title} [${it.type}]: $amount EGP, due ${dateFormat.format(Date(it.dueDate))}, ${if (it.isPaid) "PAID" else "UNPAID"}" +
                            (if (it.remaining > 0) ", remaining ${it.remaining} EGP" else "")
                    }
                }
            }
            "addFinancialItem" -> {
                val title = call.args["title"]?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    if (isArabic) "أحتاج اسم الفاتورة أو القسط أو الاشتراك." else "I need a name for the bill, installment or subscription."
                } else {
                    val type = when (call.args["type"]?.toString()?.uppercase()) {
                        "INSTALLMENT" -> "INSTALLMENT"
                        "SUBSCRIPTION" -> "SUBSCRIPTION"
                        else -> "BILL"
                    }
                    val currency = call.args["currency"]?.toString()?.takeUnless { it.isNullOrBlank() } ?: "EGP"
                    val amount = (call.args["amount"] as? Number)?.toDouble() ?: 0.0
                    val totalPrice = (call.args["totalPrice"] as? Number)?.toDouble() ?: 0.0
                    val downPayment = (call.args["downPayment"] as? Number)?.toDouble() ?: 0.0
                    val monthlyAmount = (call.args["monthlyAmount"] as? Number)?.toDouble() ?: 0.0
                    val seller = call.args["seller"]?.toString().orEmpty()
                    val recurring = (call.args["recurring"] as? Boolean) ?: (type == "SUBSCRIPTION")
                    val days = (call.args["dueInDays"] as? Number)?.toLong() ?: 30L
                    val dueDate = System.currentTimeMillis() + days.coerceAtLeast(0) * 24L * 3600_000L

                    // Mirror MainViewModel.financialReminderFor so an AI-created
                    // money item gets the same scheduled due-date notification.
                    val noteLabel = when (type) {
                        "INSTALLMENT" -> if (isArabic) "قسط مستحق" else "Installment due"
                        "SUBSCRIPTION" -> if (isArabic) "تجديد اشتراك" else "Subscription renewal"
                        else -> if (isArabic) "فاتورة مستحقة" else "Bill due"
                    }
                    val recurrence = if (recurring || type == "INSTALLMENT") "MONTHLY" else "NONE"
                    val reminderId = notificationRepository.insertReminder(
                        ReminderEntity(
                            title = title, note = noteLabel, dueDate = dueDate,
                            category = "BILL", recurrence = recurrence
                        )
                    )
                    onReminderCreated(reminderId)

                    notificationRepository.insertFinancialItem(
                        FinancialItemEntity(
                            type = type, title = title, amount = amount,
                            totalPrice = totalPrice, downPayment = downPayment,
                            monthlyAmount = monthlyAmount,
                            remaining = if (type == "INSTALLMENT") (totalPrice - downPayment).coerceAtLeast(0.0) else 0.0,
                            dueDate = dueDate, seller = seller, recurring = recurring,
                            currency = currency, linkedReminderId = reminderId
                        )
                    )
                    val shown = if (monthlyAmount > 0) monthlyAmount else amount
                    if (isArabic) "تمت إضافة \"$title\"${if (shown > 0) " (${shown.toLong()} $currency)" else ""}، وسيصلك تنبيه عند الاستحقاق."
                    else "Added '$title'${if (shown > 0) " (${shown.toLong()} $currency)" else ""}. You'll be alerted when it's due."
                }
            }
            "getHabits" -> {
                val habits = notificationRepository.allHabits.first()
                if (habits.isEmpty()) "No habits tracked yet."
                else {
                    val logs = notificationRepository.allHabitLogs.first()
                    val today = HabitCalculator.dayStartOf()
                    val daysByHabit = logs.groupBy({ it.habitId }, { it.dayStart }).mapValues { it.value.toSet() }
                    habits.map { habit ->
                        val days = daysByHabit[habit.id] ?: emptySet()
                        "${habit.emoji} ${habit.title}: streak ${HabitCalculator.currentStreak(days, today)} days, " +
                            (if (today in days) "done today" else "NOT done today")
                    }
                }
            }
            "addHabit" -> {
                val title = call.args["title"]?.toString()?.trim().orEmpty()
                if (title.isBlank()) {
                    "Cannot create a habit without a name."
                } else {
                    val emoji = call.args["emoji"]?.toString()?.trim().takeUnless { it.isNullOrBlank() } ?: "✅"
                    notificationRepository.insertHabit(HabitEntity(title = title, emoji = emoji))
                    if (isArabic) "تمت إضافة عادة \"$title\". تجدها في شاشة العادات وعلى الرئيسية."
                    else "Habit '$title' created. Find it in Habits and on the dashboard."
                }
            }
            "completeHabitToday" -> {
                val query = call.args["habitName"]?.toString()?.trim().orEmpty()
                val habits = notificationRepository.allHabits.first()
                val habit = habits.firstOrNull { it.title.contains(query, ignoreCase = true) }
                if (habit == null) {
                    "No habit matching '$query'. Current habits: ${habits.joinToString(", ") { it.title }.ifBlank { "none" }}"
                } else {
                    notificationRepository.setHabitDone(habit.id, HabitCalculator.dayStartOf(), true)
                    val days = notificationRepository.allHabitLogs.first()
                        .filter { it.habitId == habit.id }.map { it.dayStart }.toSet()
                    val streak = HabitCalculator.currentStreak(days)
                    if (isArabic) "تم تسجيل إنجاز \"${habit.title}\" اليوم. سلسلتك الحالية: $streak ${if (streak == 1) "يوم" else "أيام"}."
                    else "'${habit.title}' checked off for today. Current streak: $streak day${if (streak == 1) "" else "s"}."
                }
            }
            "logWater" -> {
                onLogWater()
                if (isArabic) "تم تسجيل كوب ماء." else "Logged a glass of water."
            }
            "setSmartAlarm" -> {
                val title = call.args["title"]?.toString() ?: "Smart Alarm"
                val targetMillis = resolveTriggerTime(call.args, defaultOffsetMs = 600000)

                val alarm = AlarmEntity(
                    title = title,
                    timeInMillis = targetMillis,
                    isEnabled = true,
                    createdViaAi = true
                )
                val alarmId = notificationRepository.insertAlarm(alarm)
                val fullAlarm = alarm.copy(id = alarmId)
                onAlarmCreated(fullAlarm)
                if (isArabic) "ضبطت منبه \"$title\" الساعة ${dateFormat.format(Date(targetMillis))} ⏰"
                else "Alarm '$title' set for ${dateFormat.format(Date(targetMillis))} ⏰"
            }
            else -> "Unknown function call: ${call.name}"
        }
    }

    /**
     * Resolves a trigger time from the model's arguments WITHOUT trusting it
     * to do epoch arithmetic (which drifted by hours in practice):
     *  • minutesFromNow  → now + N minutes
     *  • hour [+ minute] → the next occurrence of that clock time in the
     *    device's OWN timezone (today if still ahead, else tomorrow)
     *  • otherwise       → now + defaultOffsetMs
     * A legacy absolute epoch (timestampMillis/dueDateMillis) is honored
     * only if it is actually in the future.
     */
    private fun resolveTriggerTime(args: Map<String, Any?>, defaultOffsetMs: Long): Long {
        val now = System.currentTimeMillis()
        (args["minutesFromNow"] as? Number)?.let { mins ->
            if (mins.toLong() > 0) return now + mins.toLong() * 60_000L
        }
        (args["hour"] as? Number)?.let { h ->
            val hour = h.toInt().coerceIn(0, 23)
            val minute = (args["minute"] as? Number)?.toInt()?.coerceIn(0, 59) ?: 0
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }
        val legacy = (args["timestampMillis"] as? Number)?.toLong()
            ?: (args["dueDateMillis"] as? Number)?.toLong()
        if (legacy != null && legacy > now) return legacy
        return now + defaultOffsetMs
    }
}
