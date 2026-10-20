package com.budget2.app

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

internal val PaymentTypes = listOf("Überweisen", "Abbuchung", "Umbuchung")

internal data class FixedCost(
    val id: Long,
    val paymentType: String,
    val name: String,
    val amount: Double,
    val isPaid: Boolean,
    val dueDay: Int,
)

internal val DefaultFixedCosts = listOf(
    FixedCost(1, "Überweisen", "Loyer", 600.00, true, -27),
    FixedCost(2, "Überweisen", "Économies", 250.00, true, -27),
    FixedCost(3, "Überweisen", "Mama", 250.00, true, -27),
    FixedCost(4, "Überweisen", "Appli", 20.00, true, -27),
    FixedCost(5, "Abbuchung", "Molotov", 10.99, true, -28),
    FixedCost(6, "Abbuchung", "Spotify", 14.00, false, -28),
    FixedCost(7, "Umbuchung", "Impôts", 38.00, true, 1),
    FixedCost(8, "Abbuchung", "Free", 15.99, true, 4),
    FixedCost(9, "Abbuchung", "Free Box", 69.00, true, 5),
    FixedCost(10, "Abbuchung", "Sport", 29.90, true, 5),
    FixedCost(11, "Abbuchung", "Oxfam", 10.00, true, 5),
    FixedCost(12, "Abbuchung", "Assurance vie", 250.00, true, 10),
    FixedCost(13, "Abbuchung", "Psy 1", 0.00, true, 10),
    FixedCost(14, "Abbuchung", "Psy 2", 0.00, true, 10),
    FixedCost(15, "Umbuchung", "Krav Maga", 30.00, true, 10),
    FixedCost(16, "Abbuchung", "TCL", 76.00, true, 11),
    FixedCost(17, "Abbuchung", "YouTube", 13.00, false, 24),
)

internal data class PaymentTypeTotal(
    val paymentType: String,
    val total: Double,
    val paid: Double,
    val unpaid: Double,
)

internal data class DashboardSettings(
    val balance: Double = 0.0,
    val minimumBuffer: Double = 0.0,
)

internal data class DashboardSummary(
    val cutoffDate: LocalDate,
    val daysRemaining: Int,
    val paidFixedCosts: Double,
    val remainingFixedCosts: Double,
    val dailyBudgetToday: Double,
    val dailyBudgetTomorrow: Double,
    val bufferToday: Double,
    val bufferTomorrow: Double,
    val balanceMinusBuffer: Double,
    val targetBalanceToday: Double,
    val targetBalanceTomorrow: Double,
)

internal fun calculateDashboardSummary(
    fixedCosts: List<FixedCost>,
    settings: DashboardSettings,
    today: LocalDate = LocalDate.now(),
): DashboardSummary {
    val month = YearMonth.from(today)
    val monthEnd = month.atEndOfMonth()
    val cutoffDate = if (month.monthValue == 2) {
        month.atDay(27)
    } else {
        monthEnd.minusDays(3)
    }
    val daysRemaining = ChronoUnit.DAYS.between(today, cutoffDate).toInt()
    val safeDaysRemaining = daysRemaining.coerceAtLeast(1)
    val safeDaysTomorrow = (daysRemaining - 1).coerceAtLeast(1)
    val paid = fixedCosts.filter { it.isPaid }.sumOf { it.amount }
    val total = fixedCosts.sumOf { it.amount }
    val remaining = (total - paid).roundToInt().toDouble()
    val dailyBudgetToday = ((settings.balance - remaining - settings.minimumBuffer) / safeDaysRemaining)
        .roundToInt().toDouble()
    val dailyBudgetTomorrow = ((settings.balance - remaining - settings.minimumBuffer) / safeDaysTomorrow)
        .roundToInt().toDouble()
    val daysForTarget = daysRemaining.coerceAtLeast(0)
    val bufferToday = settings.balance - (DAILY_TARGET * daysForTarget + remaining)
    val bufferTomorrow = settings.balance - (DAILY_TARGET * (daysForTarget - 1).coerceAtLeast(0) + remaining)
    val targetBalanceToday = DAILY_TARGET * daysForTarget + settings.minimumBuffer + remaining

    return DashboardSummary(
        cutoffDate = cutoffDate,
        daysRemaining = daysRemaining,
        paidFixedCosts = paid,
        remainingFixedCosts = remaining,
        dailyBudgetToday = dailyBudgetToday,
        dailyBudgetTomorrow = dailyBudgetTomorrow,
        bufferToday = bufferToday,
        bufferTomorrow = bufferTomorrow,
        balanceMinusBuffer = settings.balance - bufferToday,
        targetBalanceToday = targetBalanceToday,
        targetBalanceTomorrow = targetBalanceToday - DAILY_TARGET - 5.0,
    )
}

internal const val DAILY_TARGET = 60.0

internal fun paymentTypeTotals(fixedCosts: List<FixedCost>): List<PaymentTypeTotal> =
    PaymentTypes.mapNotNull { paymentType ->
        val costs = fixedCosts.filter { it.paymentType == paymentType }
        if (costs.isEmpty()) {
            null
        } else {
            val paid = costs.filter { it.isPaid }.sumOf { it.amount }
            val unpaid = costs.filterNot { it.isPaid }.sumOf { it.amount }
            PaymentTypeTotal(paymentType, paid + unpaid, paid, unpaid)
        }
    }

internal fun updateFixedCostStatus(
    fixedCosts: List<FixedCost>,
    id: Long,
    isPaid: Boolean,
): List<FixedCost> = fixedCosts.map { cost ->
    if (cost.id == id) cost.copy(isPaid = isPaid) else cost
}

internal fun setAllFixedCostsStatus(
    fixedCosts: List<FixedCost>,
    isPaid: Boolean,
): List<FixedCost> = fixedCosts.map { it.copy(isPaid = isPaid) }
