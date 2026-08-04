package com.notification.app.domain.calculator

import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.domain.model.LedgerTransactionType
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
}
