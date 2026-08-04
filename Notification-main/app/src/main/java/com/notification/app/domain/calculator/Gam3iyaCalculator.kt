package com.notification.app.domain.calculator

import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import java.util.*
import kotlin.math.ceil

data class Gam3iyaSummary(
    val durationMonths: Int,
    val projectedEndDate: Long,
    val installmentsPaidCount: Int,
    val installmentsRemainingCount: Int,
    val totalPaidAmount: Double,
    val totalRemainingAmount: Double
)

object Gam3iyaCalculator {

    fun calculateSummary(
        gam3iya: Gam3iyaEntity,
        members: List<Gam3iyaMemberEntity>
    ): Gam3iyaSummary {
        val duration = if (gam3iya.monthlyInstallment > 0) {
            ceil(gam3iya.totalAmount / gam3iya.monthlyInstallment).toInt().coerceAtLeast(gam3iya.membersCount)
        } else {
            gam3iya.membersCount
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = gam3iya.startDate
            add(Calendar.MONTH, duration)
        }
        val projectedEndDate = cal.timeInMillis

        var paidCount = 0
        var remainingCount = 0

        val currentMonthEpoch = System.currentTimeMillis()
        members.forEach { m ->
            if (m.isPayoutReceived || m.payoutDate <= currentMonthEpoch) {
                paidCount++
            } else {
                remainingCount++
            }
        }

        val totalPaid = paidCount * gam3iya.monthlyInstallment
        val totalRemaining = (duration - paidCount).coerceAtLeast(0) * gam3iya.monthlyInstallment

        return Gam3iyaSummary(
            durationMonths = duration,
            projectedEndDate = projectedEndDate,
            installmentsPaidCount = paidCount,
            installmentsRemainingCount = (duration - paidCount).coerceAtLeast(0),
            totalPaidAmount = totalPaid,
            totalRemainingAmount = totalRemaining
        )
    }

    fun calculateMemberPayoutDate(startDate: Long, turnMonth: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDate
            add(Calendar.MONTH, turnMonth - 1)
        }
        return cal.timeInMillis
    }
}
