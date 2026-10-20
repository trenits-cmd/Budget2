package com.budget2.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetModelsTest {
    @Test
    fun defaultFixedCosts_includeProvidedItemsAndStatuses() {
        assertEquals(17, DefaultFixedCosts.size)
        assertEquals(15, DefaultFixedCosts.count { it.isPaid })
        assertEquals(2, DefaultFixedCosts.count { !it.isPaid })
        assertEquals(0.0, DefaultFixedCosts[12].amount, 0.0)
        assertEquals(-27, DefaultFixedCosts.first().dueDay)
        assertEquals("YouTube", DefaultFixedCosts.last().name)
    }

    @Test
    fun updateFixedCostStatus_changesOnlyTheSelectedCost() {
        val costs = listOf(
            FixedCost(1, "Überweisen", "Miete", 1200.0, false, 1),
            FixedCost(2, "Abbuchung", "Strom", 80.0, false, 10),
        )

        assertEquals(
            listOf(costs[0].copy(isPaid = true), costs[1]),
            updateFixedCostStatus(costs, id = 1, isPaid = true),
        )
    }

    @Test
    fun setAllFixedCostsStatus_resetsEveryCostToUnpaid() {
        val costs = listOf(
            FixedCost(1, "Überweisen", "Miete", 1200.0, true, 1),
            FixedCost(2, "Abbuchung", "Strom", 80.0, true, 10),
        )

        assertEquals(
            costs.map { it.copy(isPaid = false) },
            setAllFixedCostsStatus(costs, isPaid = false),
        )
    }

    @Test
    fun paymentTypeTotals_sumsPaidAndUnpaidCostsByPaymentType() {
        val totals = paymentTypeTotals(
            listOf(
                FixedCost(1, "Überweisen", "Loyer", 600.0, true, -27),
                FixedCost(2, "Überweisen", "Économies", 250.0, true, -27),
                FixedCost(3, "Abbuchung", "Spotify", 14.0, false, -28),
                FixedCost(4, "Abbuchung", "Molotov", 10.99, true, -28),
                FixedCost(5, "Umbuchung", "Impôts", 38.0, true, 1),
            ),
        )

        assertEquals(listOf("Überweisen", "Abbuchung", "Umbuchung"), totals.map { it.paymentType })
        assertEquals(850.0, totals[0].total, 0.001)
        assertEquals(850.0, totals[0].paid, 0.001)
        assertEquals(0.0, totals[0].unpaid, 0.001)
        assertEquals(24.99, totals[1].total, 0.001)
        assertEquals(10.99, totals[1].paid, 0.001)
        assertEquals(14.0, totals[1].unpaid, 0.001)
        assertEquals(38.0, totals[2].total, 0.001)
    }

    @Test
    fun calculateDashboardSummary_matchesOctoberSheetCalculations() {
        val summary = calculateDashboardSummary(
            fixedCosts = listOf(
                FixedCost(1, "Überweisen", "Miete", 600.0, true, -27),
                FixedCost(2, "Abbuchung", "Abo", 24.99, false, 10),
            ),
            settings = DashboardSettings(balance = 1000.0, minimumBuffer = 100.0),
            today = LocalDate.of(2026, 10, 11),
            viewingDate = LocalDate.of(2026, 10, 11),
        )

        assertEquals(LocalDate.of(2026, 10, 28), summary.cutoffDate)
        assertEquals(17, summary.daysRemaining)
        assertEquals(600.0, summary.paidFixedCosts, 0.0)
        assertEquals(25.0, summary.remainingFixedCosts, 0.0)
        assertEquals(51.0, summary.dailyBudgetToday, 0.0)
        assertEquals(55.0, summary.dailyBudgetTomorrow, 0.0)
        assertEquals(-45.0, summary.bufferToday, 0.0)
        assertEquals(15.0, summary.bufferTomorrow, 0.0)
        assertEquals(1045.0, summary.balanceMinusBuffer, 0.0)
        assertEquals(1145.0, summary.targetBalanceToday, 0.0)
        assertEquals(1080.0, summary.targetBalanceTomorrow, 0.0)
    }

    @Test
    fun calculateDashboardSummary_includesCurrentMonthAndUnknownIrregularPayments() {
        val summary = calculateDashboardSummary(
            fixedCosts = listOf(
                FixedCost(1, "Überweisen", "Miete", 600.0, true, -27),
            ),
            irregularPayments = listOf(
                IrregularPayment(1, "Oktober", "Abbuchung", "Sonderausgabe", 100.0, true),
                IrregularPayment(2, "unbekannt", "Überweisen", "Bonus", 50.0, false),
            ),
            settings = DashboardSettings(balance = 1000.0, minimumBuffer = 100.0),
            today = LocalDate.of(2026, 10, 11),
            viewingDate = LocalDate.of(2026, 10, 11),
        )

        assertEquals(700.0, summary.paidFixedCosts, 0.0)
        assertEquals(50.0, summary.remainingFixedCosts, 0.0)
    }

    @Test
    fun calculateDashboardSummary_usesFebruaryTwentySeventhAsCutoff() {
        val summary = calculateDashboardSummary(
            fixedCosts = emptyList(),
            settings = DashboardSettings(),
            today = LocalDate.of(2025, 2, 20),
            viewingDate = LocalDate.of(2025, 2, 20),
        )

        assertEquals(LocalDate.of(2025, 2, 27), summary.cutoffDate)
        assertEquals(7, summary.daysRemaining)
    }
}
