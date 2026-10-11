package com.budget2.app

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

internal val PaymentTypes = listOf("Überweisen", "Abbuchung", "Umbuchung")
internal val ExpenseAccounts = listOf("BoursoBank", "Revolut", "Ing")

internal data class FixedCost(
    val id: Long,
    val paymentType: String,
    val name: String,
    val amount: Double,
    val isPaid: Boolean,
    val dueDay: Int,
)

internal data class IrregularExpense(
    val id: Long,
    val name: String,
    val amount: Double,
    val account: String,
    val dueDate: String,
    val count: Int,
) {
    val total: Double get() = amount * count
}

internal data class BudgetSettings(
    val balance: Double = 0.0,
    val safetyBuffer: Double = 300.0,
    val netSalary: Double = 0.0,
    val paydayDay: Int = 1,
    val fixedDailyAmount: Double = 0.0,
)

internal data class BudgetSummary(
    val totalFixedCosts: Double,
    val paidFixedCosts: Double,
    val unpaidFixedCosts: Double,
    val monthlySavings: Double,
    val balanceAfterBuffer: Double,
    val daysUntilPayday: Int,
    val dailyBudget: Double,
    val targetBalance: Double,
)

internal fun sumFixedCosts(
    fixedCosts: List<FixedCost>,
    paymentType: String? = null,
    isPaid: Boolean? = null,
): Double = fixedCosts
    .filter { (paymentType == null || it.paymentType == paymentType) && (isPaid == null || it.isPaid == isPaid) }
    .sumOf { it.amount }

internal fun calculateBudget(
    settings: BudgetSettings,
    fixedCosts: List<FixedCost>,
    today: LocalDate = LocalDate.now(),
    monthOffset: Int = 0,
): BudgetSummary {
    val paidAmount = sumFixedCosts(fixedCosts, isPaid = true)
    val unpaidAmount = sumFixedCosts(fixedCosts, isPaid = false)
    val totalFixed = excelRound(paidAmount + unpaidAmount)
    val unpaidFixed = totalFixed - paidAmount

    val referenceDate = if (monthOffset == 0) {
        today
    } else {
        YearMonth.from(today).plusMonths(monthOffset.toLong()).atDay(1)
    }
    val currentMonth = YearMonth.from(referenceDate)
    val paydayThisMonth = currentMonth.atDay(settings.paydayDay.coerceIn(1, currentMonth.lengthOfMonth()))
    val nextPayday = if (paydayThisMonth.isBefore(referenceDate)) {
        val nextMonth = currentMonth.plusMonths(1)
        nextMonth.atDay(settings.paydayDay.coerceIn(1, nextMonth.lengthOfMonth()))
    } else {
        paydayThisMonth
    }
    val daysUntilPayday = ChronoUnit.DAYS.between(referenceDate, nextPayday).toInt().coerceAtLeast(1)
    val dailyBudget = excelRound((settings.balance - unpaidFixed - settings.safetyBuffer) / daysUntilPayday)

    return BudgetSummary(
        totalFixedCosts = totalFixed,
        paidFixedCosts = paidAmount,
        unpaidFixedCosts = unpaidFixed,
        monthlySavings = settings.netSalary - totalFixed,
        balanceAfterBuffer = settings.balance - settings.safetyBuffer,
        daysUntilPayday = daysUntilPayday,
        dailyBudget = dailyBudget,
        targetBalance = settings.fixedDailyAmount * daysUntilPayday +
            settings.safetyBuffer + unpaidFixed,
    )
}

private fun excelRound(value: Double): Double =
    if (value < 0) -(-value).roundToLong().toDouble() else value.roundToLong().toDouble()
