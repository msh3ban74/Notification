package com.notification.app.domain.calculator

import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.LedgerTransactionEntity
import com.notification.app.data.local.entities.PersonEntity
import com.notification.app.domain.model.LedgerTransactionType
import java.text.SimpleDateFormat
import java.util.*

object StatementExporter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun generatePersonLedgerStatement(
        person: PersonEntity,
        transactions: List<LedgerTransactionEntity>
    ): String {
        val summary = LedgerCalculator.calculateNetBalance(transactions)
        val sb = StringBuilder()

        sb.append("========================================\n")
        sb.append("       NOTIFICATION - APP LEDGER        \n")
        sb.append("========================================\n")
        sb.append("Statement for: ${person.name}\n")
        sb.append("Date: ${dateFormat.format(Date())}\n")
        sb.append("----------------------------------------\n\n")

        sb.append("TRANSACTION HISTORY:\n")
        if (transactions.isEmpty()) {
            sb.append("No transactions recorded.\n")
        } else {
            transactions.forEachIndexed { idx, tx ->
                val typeStr = when (LedgerTransactionType.fromString(tx.type)) {
                    LedgerTransactionType.GAVE_THEM -> "I gave them (Lent)"
                    LedgerTransactionType.THEY_GAVE_BACK -> "They repaid me"
                    LedgerTransactionType.THEY_GAVE_ME -> "They gave me (Borrowed)"
                    LedgerTransactionType.I_GAVE_BACK -> "I repaid them"
                }
                sb.append("${idx + 1}. [${dateFormat.format(Date(tx.date))}] $typeStr: ${tx.amount} EGP")
                if (tx.note.isNotBlank()) {
                    sb.append(" (${tx.note})")
                }
                sb.append("\n")
            }
        }

        sb.append("\n----------------------------------------\n")
        sb.append("FINANCIAL SUMMARY:\n")
        sb.append("Total Lent: ${summary.totalLent} EGP\n")
        sb.append("Total Repaid to Me: ${summary.totalRepaidToMe} EGP\n")
        sb.append("Total Borrowed: ${summary.totalBorrowed} EGP\n")
        sb.append("Total I Repaid: ${summary.totalIRepaid} EGP\n")
        sb.append("----------------------------------------\n")

        val statusText = when (summary.status) {
            LedgerStatus.THEY_OWE_ME -> "NET RESULT: They owe me ${summary.netAmount} EGP"
            LedgerStatus.I_OWE_THEM -> "NET RESULT: I owe them ${summary.netAmount} EGP"
            LedgerStatus.SETTLED -> "NET RESULT: Settled / Even (0 EGP)"
        }
        sb.append("$statusText\n")
        sb.append("========================================\n")
        sb.append("Generated via Notification App")

        return sb.toString()
    }

    fun generateGam3iyaStatement(
        gam3iya: Gam3iyaEntity,
        members: List<Gam3iyaMemberEntity>
    ): String {
        val summary = Gam3iyaCalculator.calculateSummary(gam3iya, members)
        val sb = StringBuilder()

        sb.append("========================================\n")
        sb.append("       NOTIFICATION - GAM3IYA REPORT     \n")
        sb.append("========================================\n")
        sb.append("Gam3iya Title: ${gam3iya.title}\n")
        sb.append("Total Amount: ${gam3iya.totalAmount} EGP\n")
        sb.append("Monthly Installment: ${gam3iya.monthlyInstallment} EGP\n")
        sb.append("Members Count: ${gam3iya.membersCount}\n")
        sb.append("Start Date: ${dateFormat.format(Date(gam3iya.startDate))}\n")
        sb.append("Projected End Date: ${dateFormat.format(Date(summary.projectedEndDate))}\n")
        sb.append("----------------------------------------\n\n")

        sb.append("ROTATION & PAYOUT SCHEDULE:\n")
        members.forEachIndexed { idx, m ->
            val status = if (m.isPayoutReceived) "[COMPLETED]" else "[PENDING]"
            sb.append("${idx + 1}. Month ${m.turnMonth} - ${m.memberName} | Date: ${dateFormat.format(Date(m.payoutDate))} $status\n")
        }

        sb.append("\n----------------------------------------\n")
        sb.append("PROGRESS:\n")
        sb.append("Paid Installments: ${summary.installmentsPaidCount} / ${summary.durationMonths}\n")
        sb.append("Total Paid: ${summary.totalPaidAmount} EGP\n")
        sb.append("Total Remaining: ${summary.totalRemainingAmount} EGP\n")
        sb.append("========================================\n")
        sb.append("Generated via Notification App")

        return sb.toString()
    }
}
