package com.notification.app.domain.calculator

import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.domain.model.LedgerTransactionType
import java.util.Calendar
import kotlin.math.abs

enum class LedgerStatus {
    THEY_OWE_ME,
    I_OWE_THEM,
    SETTLED
}

data class LedgerSummary(
    val netAmount: Double,
    val status: LedgerStatus,
    val totalLent: Double,
    val totalBorrowed: Double,
    val totalRepaidToMe: Double,
    val totalIRepaid: Double
)

object LedgerCalculator {

    fun calculateNetBalance(transactions: List<LedgerTransactionEntity>): LedgerSummary {
        var totalLent = 0.0        // GAVE_THEM
        var totalRepaidToMe = 0.0 // THEY_GAVE_BACK
        var totalBorrowed = 0.0   // THEY_GAVE_ME
        var totalIRepaid = 0.0    // I_GAVE_BACK

        for (tx in transactions) {
            when (LedgerTransactionType.fromString(tx.type)) {
                LedgerTransactionType.GAVE_THEM -> totalLent += tx.amount
                LedgerTransactionType.THEY_GAVE_BACK -> totalRepaidToMe += tx.amount
                LedgerTransactionType.THEY_GAVE_ME -> totalBorrowed += tx.amount
                LedgerTransactionType.I_GAVE_BACK -> totalIRepaid += tx.amount
            }
        }

        // Net Owed To User = (totalLent - totalRepaidToMe) - (totalBorrowed - totalIRepaid)
        val userOwedAmount = (totalLent - totalRepaidToMe)
        val userOwesAmount = (totalBorrowed - totalIRepaid)
        val netAmount = userOwedAmount - userOwesAmount

        val status = when {
            netAmount > 0.01 -> LedgerStatus.THEY_OWE_ME
            netAmount < -0.01 -> LedgerStatus.I_OWE_THEM
            else -> LedgerStatus.SETTLED
        }

        return LedgerSummary(
            netAmount = abs(netAmount),
            status = status,
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            totalRepaidToMe = totalRepaidToMe,
            totalIRepaid = totalIRepaid
        )
    }

    /**
     * Sprint 5 — global statistics across ALL persons/transactions for the
     * Debts statistics screen and the dashboard. Receivable/payable net out
     * per person (a person can't both owe and be owed net).
     */
    fun computeGlobalStats(
        transactions: List<LedgerTransactionEntity>,
        now: Long = System.currentTimeMillis()
    ): LedgerGlobalStats {
        var totalReceivable = 0.0
        var totalPayable = 0.0
        transactions.groupBy { it.personId }.forEach { (_, txs) ->
            val s = calculateNetBalance(txs)
            when (s.status) {
                LedgerStatus.THEY_OWE_ME -> totalReceivable += s.netAmount
                LedgerStatus.I_OWE_THEM -> totalPayable += s.netAmount
                LedgerStatus.SETTLED -> {}
            }
        }

        val lent = transactions.filter { LedgerTransactionType.fromString(it.type) == LedgerTransactionType.GAVE_THEM }.sumOf { it.amount }
        val repaidToMe = transactions.filter { LedgerTransactionType.fromString(it.type) == LedgerTransactionType.THEY_GAVE_BACK }.sumOf { it.amount }
        val borrowed = transactions.filter { LedgerTransactionType.fromString(it.type) == LedgerTransactionType.THEY_GAVE_ME }.sumOf { it.amount }
        val iRepaid = transactions.filter { LedgerTransactionType.fromString(it.type) == LedgerTransactionType.I_GAVE_BACK }.sumOf { it.amount }

        val repayments = transactions.filter {
            val t = LedgerTransactionType.fromString(it.type)
            t == LedgerTransactionType.THEY_GAVE_BACK || t == LedgerTransactionType.I_GAVE_BACK
        }
        val averagePayment = if (repayments.isNotEmpty()) repayments.sumOf { it.amount } / repayments.size else 0.0
        val collectionRate = if (lent > 0) (repaidToMe / lent).coerceIn(0.0, 1.0) else 0.0

        // This month activity (absolute volume of all transactions this month).
        val monthStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val thisMonth = transactions.filter { it.date in monthStart..now }.sumOf { it.amount }

        // Overdue: transactions with a due date already passed.
        val overdue = transactions.filter { it.dueDate in 1 until now }
        val overdueAmount = overdue.sumOf { it.amount }

        // Last-12-months net volume (lent+borrowed minus repayments) for charts.
        val monthly = LinkedHashMap<String, Double>()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        for (i in 11 downTo 0) {
            val c = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, -i) }
            monthly["${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}"] = 0.0
        }
        transactions.forEach { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.date }
            val key = "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}"
            if (monthly.containsKey(key)) monthly[key] = (monthly[key] ?: 0.0) + tx.amount
        }

        return LedgerGlobalStats(
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            totalLent = lent, totalBorrowed = borrowed,
            totalRepaidToMe = repaidToMe, totalIRepaid = iRepaid,
            thisMonthVolume = thisMonth,
            overdueCount = overdue.size, overdueAmount = overdueAmount,
            averagePayment = averagePayment, collectionRate = collectionRate,
            monthlyVolume = monthly
        )
    }
}

data class LedgerGlobalStats(
    val totalReceivable: Double,
    val totalPayable: Double,
    val totalLent: Double,
    val totalBorrowed: Double,
    val totalRepaidToMe: Double,
    val totalIRepaid: Double,
    val thisMonthVolume: Double,
    val overdueCount: Int,
    val overdueAmount: Double,
    val averagePayment: Double,
    val collectionRate: Double,          // 0f..1f
    val monthlyVolume: Map<String, Double> // last 12 months, chronological
)
