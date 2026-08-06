package com.notification.app.domain.calculator

import com.notification.app.data.local.entities.FinancialItemEntity
import com.notification.app.data.local.entities.FinancialPaymentEntity
import java.util.Calendar
import kotlin.math.ceil

/**
 * Sprint 6 — installment payment-plan math. Everything is derived from the
 * item plus its logged payments, so the plan is always correct after any
 * edit or new payment.
 */
data class InstallmentPlan(
    val totalPrice: Double,
    val downPayment: Double,
    val monthlyAmount: Double,
    val totalPaid: Double,          // down payment + all logged payments
    val remainingAmount: Double,
    val totalInstallments: Int,
    val paidInstallments: Int,
    val remainingInstallments: Int,
    val completionFraction: Float,  // 0f..1f
    val interestPaid: Double,
    val nextPaymentDate: Long,      // 0 if none / finished
    val lastPaymentDate: Long,      // 0 if none
    val expectedFinishDate: Long,
    val isFinished: Boolean
)

object FinancialCalculator {

    fun computePlan(
        item: FinancialItemEntity,
        payments: List<FinancialPaymentEntity> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): InstallmentPlan {
        val monthly = item.monthlyAmount
        val paidFromPayments = payments.sumOf { it.amount }
        val totalPaid = (item.downPayment + paidFromPayments)
        val principalToPay = (item.totalPrice - item.downPayment).coerceAtLeast(0.0)
        val remainingAmount = (item.totalPrice - totalPaid).coerceAtLeast(0.0)

        val totalInstallments = if (monthly > 0) ceil(principalToPay / monthly).toInt() else 0
        val remainingInstallments = if (monthly > 0) ceil(remainingAmount / monthly).toInt() else 0
        val paidInstallments = (totalInstallments - remainingInstallments).coerceIn(0, totalInstallments)

        val completion = if (item.totalPrice > 0) (totalPaid / item.totalPrice).toFloat().coerceIn(0f, 1f) else 0f
        val interestPaid = (item.interestAmount * completion).coerceAtLeast(0.0)

        val lastPayment = payments.maxByOrNull { it.date }?.date ?: 0L
        val finished = item.isPaid || remainingAmount <= 0.01
        val nextPayment = if (finished) 0L else item.dueDate

        val expectedFinish = if (finished) now else {
            val base = if (item.dueDate > now) item.dueDate else now
            Calendar.getInstance().apply {
                timeInMillis = base
                if (remainingInstallments > 1) add(Calendar.MONTH, remainingInstallments - 1)
            }.timeInMillis
        }

        return InstallmentPlan(
            totalPrice = item.totalPrice,
            downPayment = item.downPayment,
            monthlyAmount = monthly,
            totalPaid = totalPaid,
            remainingAmount = remainingAmount,
            totalInstallments = totalInstallments,
            paidInstallments = paidInstallments,
            remainingInstallments = remainingInstallments,
            completionFraction = completion,
            interestPaid = interestPaid,
            nextPaymentDate = nextPayment,
            lastPaymentDate = lastPayment,
            expectedFinishDate = expectedFinish,
            isFinished = finished
        )
    }

    /** Global installment statistics for the stats screen + dashboard. */
    fun computeStats(
        items: List<FinancialItemEntity>,
        paymentsByItem: Map<Long, List<FinancialPaymentEntity>>,
        now: Long = System.currentTimeMillis()
    ): InstallmentStats {
        val installments = items.filter { it.type == "INSTALLMENT" && !it.isArchived }
        var totalRemaining = 0.0
        var totalPaidAll = 0.0
        var completionSum = 0f
        var dueToday = 0; var dueWeek = 0; var dueMonth = 0; var overdue = 0
        val dayEnd = now + 24L * 3600_000
        val weekEnd = now + 7L * 24 * 3600_000
        val monthEnd = now + 30L * 24 * 3600_000
        installments.forEach { item ->
            val plan = computePlan(item, paymentsByItem[item.id] ?: emptyList(), now)
            totalRemaining += plan.remainingAmount
            totalPaidAll += plan.totalPaid
            completionSum += plan.completionFraction
            if (!plan.isFinished && item.dueDate > 0) {
                when {
                    item.dueDate < now -> overdue++
                    item.dueDate <= dayEnd -> dueToday++
                    item.dueDate <= weekEnd -> dueWeek++
                    item.dueDate <= monthEnd -> dueMonth++
                }
            }
        }
        val completionRate = if (installments.isNotEmpty()) completionSum / installments.size else 0f
        val paidCount = installments.count { computePlan(it, paymentsByItem[it.id] ?: emptyList(), now).isFinished }

        // last-12-months paid volume
        val monthly = LinkedHashMap<String, Double>()
        for (i in 11 downTo 0) {
            val c = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, -i) }
            monthly["${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}"] = 0.0
        }
        paymentsByItem.values.flatten().forEach { p ->
            val c = Calendar.getInstance().apply { timeInMillis = p.date }
            val key = "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}"
            if (monthly.containsKey(key)) monthly[key] = (monthly[key] ?: 0.0) + p.amount
        }

        return InstallmentStats(
            totalCount = installments.size,
            paidCount = paidCount,
            remainingCount = (installments.size - paidCount).coerceAtLeast(0),
            totalRemaining = totalRemaining,
            totalPaid = totalPaidAll,
            dueToday = dueToday, dueThisWeek = dueWeek, dueThisMonth = dueMonth, overdue = overdue,
            completionRate = completionRate,
            monthlyPaid = monthly
        )
    }
}

data class InstallmentStats(
    val totalCount: Int,
    val paidCount: Int,
    val remainingCount: Int,
    val totalRemaining: Double,
    val totalPaid: Double,
    val dueToday: Int,
    val dueThisWeek: Int,
    val dueThisMonth: Int,
    val overdue: Int,
    val completionRate: Float,
    val monthlyPaid: Map<String, Double>
)
