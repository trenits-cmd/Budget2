package com.budget2.app

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

internal val PaymentTypes = listOf("Überweisen", "Abbuchung", "Umbuchung")

internal val MonthOptions = listOf("unbekannt", "Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")

internal data class FixedCost(
    val id: Long,
    val paymentType: String,
    val name: String,
    val amount: Double,
    val isPaid: Boolean,
    val dueDay: Int,
)

internal data class IrregularPayment(
    val id: Long,
    val month: String,
    val paymentType: String,
    val name: String,
    val amount: Double,
    val isPaid: Boolean,
)

internal data class IncomeReceipt(
    val id: Long,
    val targetMonth: String,
    val name: String,
    val amount: Double,
    val date: LocalDate,
)

internal fun isActiveMonth(monthStr: String, viewingDate: LocalDate = LocalDate.now()): Boolean {
    val clean = monthStr.trim().lowercase()
    if (clean == "unbekannt" || clean.isEmpty()) return true
    val viewingMonthName = viewingDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMANY)).lowercase()
    val viewingMonthNumber = viewingDate.monthValue.toString()
    return clean == viewingMonthName || clean == viewingMonthNumber
}

internal fun matchesMonth(targetMonth: String, viewingDate: LocalDate): Boolean {
    val clean = targetMonth.trim().lowercase()
    val viewingMonthName = viewingDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMANY)).lowercase()
    return clean == viewingMonthName
}

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
    val unknownMonthIrregularPayments: List<IrregularPayment> = emptyList(),
)

internal fun calculateDashboardSummary(
    fixedCosts: List<FixedCost>,
    paidFixedCostIds: Set<Long>? = null,
    irregularPayments: List<IrregularPayment> = emptyList(),
    paidUnknownPaymentIds: Set<Long> = emptySet(),
    incomeReceipts: List<IncomeReceipt> = emptyList(),
    settings: DashboardSettings,
    today: LocalDate = LocalDate.now(),
    viewingDate: LocalDate = LocalDate.now(),
): DashboardSummary {
    val month = YearMonth.from(viewingDate)
    val monthEnd = month.atEndOfMonth()
    val cutoffDate = if (month.monthValue == 2) {
        month.atDay(27)
    } else {
        monthEnd.minusDays(3)
    }
    val daysRemaining = ChronoUnit.DAYS.between(today, cutoffDate).toInt()
    val safeDaysRemaining = daysRemaining.coerceAtLeast(1)
    val safeDaysTomorrow = (daysRemaining - 1).coerceAtLeast(1)
    
    val activeIrregular = irregularPayments.filter { isActiveMonth(it.month, viewingDate) }
    val budgetIrregular = activeIrregular.filter { payment ->
        val clean = payment.month.trim().lowercase()
        if (clean == "unbekannt") YearMonth.from(viewingDate) == YearMonth.from(today)
        else isActiveMonth(payment.month, viewingDate)
    }

    val paid = fixedCosts.sumOf { cost ->
        val isPaid = paidFixedCostIds?.contains(cost.id) ?: cost.isPaid
        if (isPaid) cost.amount else 0.0
    } + budgetIrregular.filter { 
        val clean = it.month.trim().lowercase()
        if (clean == "unbekannt") it.id in paidUnknownPaymentIds else it.isPaid
    }.sumOf { it.amount }

    val total = fixedCosts.sumOf { it.amount } + budgetIrregular.sumOf { it.amount }
    val remaining = (total - paid).roundToInt().toDouble()

    val viewingIncome = incomeReceipts.filter { matchesMonth(it.targetMonth, viewingDate) }.sumOf { it.amount }
    val effectiveBalance = settings.balance + viewingIncome
    
    val dailyBudgetToday = ((effectiveBalance - remaining - settings.minimumBuffer) / safeDaysRemaining)
        .roundToInt().toDouble()
    val dailyBudgetTomorrow = ((effectiveBalance - remaining - settings.minimumBuffer) / safeDaysTomorrow)
        .roundToInt().toDouble()
    val daysForTarget = daysRemaining.coerceAtLeast(0)
    val bufferToday = effectiveBalance - (DAILY_TARGET * daysForTarget + remaining)
    val bufferTomorrow = effectiveBalance - (DAILY_TARGET * (daysForTarget - 1).coerceAtLeast(0) + remaining)
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
        balanceMinusBuffer = effectiveBalance - bufferToday,
        targetBalanceToday = targetBalanceToday,
        targetBalanceTomorrow = targetBalanceToday - DAILY_TARGET - 5.0,
    )
}

internal const val DAILY_TARGET = 60.0

internal fun paymentTypeTotals(
    fixedCosts: List<FixedCost>,
    irregularPayments: List<IrregularPayment> = emptyList(),
    viewingDate: LocalDate = LocalDate.now(),
    paidFixedCostIds: Set<Long>? = null,
    paidUnknownPaymentIds: Set<Long> = emptySet(),
): List<PaymentTypeTotal> {
    val activeIrregular = irregularPayments.filter { isActiveMonth(it.month, viewingDate) }
    val allCosts = mutableListOf<Triple<String, Double, Boolean>>()
    fixedCosts.forEach { cost ->
        val isPaid = paidFixedCostIds?.contains(cost.id) ?: cost.isPaid
        allCosts.add(Triple(cost.paymentType, cost.amount, isPaid))
    }
    activeIrregular.forEach { payment ->
        val clean = payment.month.trim().lowercase()
        val isPaid = if (clean == "unbekannt") payment.id in paidUnknownPaymentIds else payment.isPaid
        allCosts.add(Triple(payment.paymentType, payment.amount, isPaid))
    }
    
    return PaymentTypes.mapNotNull { paymentType ->
        val costs = allCosts.filter { it.first == paymentType }
        if (costs.isEmpty()) {
            null
        } else {
            val paid = costs.filter { it.third }.sumOf { it.second }
            val unpaid = costs.filterNot { it.third }.sumOf { it.second }
            PaymentTypeTotal(paymentType, paid + unpaid, paid, unpaid)
        }
    }
}

internal fun updateFixedCostStatus(
    fixedCosts: List<FixedCost>,
    id: Long,
    isPaid: Boolean,
): List<FixedCost> = fixedCosts.map { cost ->
    if (cost.id == id) cost.copy(isPaid = isPaid) else cost
}

internal fun updateIrregularPaymentStatus(
    payments: List<IrregularPayment>,
    id: Long,
    isPaid: Boolean,
): List<IrregularPayment> = payments.map { payment ->
    if (payment.id == id) payment.copy(isPaid = isPaid) else payment
}

internal fun setAllFixedCostsStatus(
    fixedCosts: List<FixedCost>,
    isPaid: Boolean,
): List<FixedCost> = fixedCosts.map { cost ->
    cost.copy(isPaid = isPaid)
}

internal fun setAllIrregularPaymentsStatus(
    payments: List<IrregularPayment>,
    isPaid: Boolean,
): List<IrregularPayment> = payments.map { payment ->
    payment.copy(isPaid = isPaid)
}
