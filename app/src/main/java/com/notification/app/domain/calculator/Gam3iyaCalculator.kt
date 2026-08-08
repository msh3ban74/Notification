package com.notification.app.domain.calculator

import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import com.notification.app.data.local.entities.Gam3iyaPaymentEntity
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

/**
 * Sprint 4 — the live, automatically-computed state of a gam3iya, driving
 * the dashboard, calendar, reports and AI answers. Everything here is
 * derived (never stored), so it is always correct after any edit.
 */
data class Gam3iyaStatus(
    val durationMonths: Int,
    val currentMonthIndex: Int,        // 1-based; 0 before it starts
    val remainingMonths: Int,
    val projectedEndDate: Long,
    val nextCollector: Gam3iyaMemberEntity?,
    val nextCollectionDate: Long,      // 0 if none
    val collectedCount: Int,
    val paidThisMonthCount: Int,
    val lateMembers: List<Gam3iyaMemberEntity>,
    val upcomingCollections: List<Gam3iyaMemberEntity>, // future, not yet collected, soonest first
    val totalPool: Double,
    val totalPaid: Double,
    val remainingAmount: Double,
    val progressFraction: Float,       // 0f..1f
    val isFinished: Boolean
)

object Gam3iyaCalculator {

    fun calculateMemberPayoutDate(startDate: Long, turnMonth: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDate
            add(Calendar.MONTH, turnMonth - 1)
        }
        return cal.timeInMillis
    }

    /** Effective duration in months, honouring an explicit override. */
    fun durationOf(gam3iya: Gam3iyaEntity): Int {
        if (gam3iya.durationMonths > 0) return gam3iya.durationMonths
        val base = if (gam3iya.monthlyInstallment > 0)
            ceil(gam3iya.totalAmount / gam3iya.monthlyInstallment).toInt()
        else gam3iya.membersCount
        return base.coerceAtLeast(gam3iya.membersCount).coerceAtLeast(1)
    }

    /** Whole months elapsed between two epochs (0 if end &lt;= start). */
    private fun monthsBetween(start: Long, end: Long): Int {
        if (end <= start) return 0
        val a = Calendar.getInstance().apply { timeInMillis = start }
        val b = Calendar.getInstance().apply { timeInMillis = end }
        var months = (b.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 12 +
            (b.get(Calendar.MONTH) - a.get(Calendar.MONTH))
        if (b.get(Calendar.DAY_OF_MONTH) < a.get(Calendar.DAY_OF_MONTH)) months--
        return max(0, months)
    }

    /**
     * The full live status. For PARTICIPANT mode `members` is empty and the
     * numbers come from the gam3iya's own my* fields.
     */
    fun computeStatus(
        gam3iya: Gam3iyaEntity,
        members: List<Gam3iyaMemberEntity>,
        payments: List<Gam3iyaPaymentEntity> = emptyList(),
        now: Long = System.currentTimeMillis()
    ): Gam3iyaStatus {
        val duration = durationOf(gam3iya)
        val endCal = Calendar.getInstance().apply {
            timeInMillis = gam3iya.startDate; add(Calendar.MONTH, duration)
        }
        val projectedEnd = endCal.timeInMillis
        val elapsed = monthsBetween(gam3iya.startDate, now)
        val currentMonthIndex = if (now < gam3iya.startDate) 0 else (elapsed + 1).coerceAtMost(duration)
        val remainingMonths = (duration - elapsed).coerceIn(0, duration)

        if (gam3iya.mode == "PARTICIPANT") {
            val perInstallment = if (gam3iya.myInstallmentAmount > 0) gam3iya.myInstallmentAmount
            else gam3iya.monthlyInstallment
            val paidCount = gam3iya.myPaidInstallments.coerceIn(0, duration)
            val totalPool = perInstallment * duration
            val totalPaid = perInstallment * paidCount
            val myTurnDate = if (gam3iya.myCollectionDate > 0) gam3iya.myCollectionDate
            else calculateMemberPayoutDate(gam3iya.startDate, gam3iya.myTurnNumber.coerceAtLeast(1))
            val collected = gam3iya.myTurnNumber in 1..elapsed
            return Gam3iyaStatus(
                durationMonths = duration,
                currentMonthIndex = currentMonthIndex,
                remainingMonths = remainingMonths,
                projectedEndDate = projectedEnd,
                nextCollector = null,
                nextCollectionDate = if (!collected) myTurnDate else 0,
                collectedCount = if (collected) 1 else 0,
                paidThisMonthCount = 0,
                lateMembers = emptyList(),
                upcomingCollections = emptyList(),
                totalPool = totalPool,
                totalPaid = totalPaid,
                remainingAmount = (totalPool - totalPaid).coerceAtLeast(0.0),
                progressFraction = if (duration > 0) paidCount.toFloat() / duration else 0f,
                isFinished = gam3iya.status == "COMPLETED" || paidCount >= duration
            )
        }

        // MANAGER mode
        val installmentOf = { m: Gam3iyaMemberEntity ->
            if (m.installmentAmount > 0) m.installmentAmount else gam3iya.monthlyInstallment
        }
        val collected = members.filter { it.isPayoutReceived }
        val notCollected = members.filter { !it.isPayoutReceived }
        val nextCollector = notCollected.minByOrNull { it.turnMonth }
        val upcoming = notCollected
            .filter { it.payoutDate >= now }
            .sortedBy { it.payoutDate }
        // Late = their collection turn has passed but not marked collected,
        // OR flagged late, OR unpaid installment while their month is due.
        val late = members.filter { m ->
            m.isLate || (!m.isPayoutReceived && m.payoutDate in 1 until now) ||
                (!m.isInstallmentPaidThisMonth && m.turnMonth <= currentMonthIndex && !m.isPayoutReceived)
        }.distinctBy { it.id }

        val poolFallback = if (gam3iya.totalAmount > 0) gam3iya.totalAmount
        else members.sumOf { installmentOf(it) } * duration.coerceAtLeast(1)
        val totalPaid = if (payments.isNotEmpty())
            payments.filter { it.type == "INSTALLMENT" }.sumOf { it.amount }
        else members.count { it.isInstallmentPaidThisMonth } * gam3iya.monthlyInstallment
        val remaining = (poolFallback - totalPaid).coerceAtLeast(0.0)
        val progress = if (members.isNotEmpty()) collected.size.toFloat() / members.size else 0f

        return Gam3iyaStatus(
            durationMonths = duration,
            currentMonthIndex = currentMonthIndex,
            remainingMonths = remainingMonths,
            projectedEndDate = projectedEnd,
            nextCollector = nextCollector,
            nextCollectionDate = nextCollector?.payoutDate ?: 0,
            collectedCount = collected.size,
            paidThisMonthCount = members.count { it.isInstallmentPaidThisMonth },
            lateMembers = late,
            upcomingCollections = upcoming,
            totalPool = poolFallback,
            totalPaid = totalPaid,
            remainingAmount = remaining,
            progressFraction = progress.coerceIn(0f, 1f),
            isFinished = gam3iya.status == "COMPLETED" ||
                (members.isNotEmpty() && collected.size >= members.size)
        )
    }
}
