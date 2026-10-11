package com.budget2.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetModelsTest {
    @Test
    fun calculateBudget_usesCurrentMonthAndRoundsValues() {
        val settings = BudgetSettings(
            balance = 2500.0,
            safetyBuffer = 300.0,
            netSalary = 3200.0,
            paydayDay = 15,
            fixedDailyAmount = 50.0,
        )
        val fixedCosts = listOf(
            FixedCost(1, "Überweisen", "Miete", 1200.0, true, 1),
            FixedCost(2, "Abbuchung", "Strom", 80.0, false, 10),
        )

        val summary = calculateBudget(
            settings = settings,
            fixedCosts = fixedCosts,
            today = LocalDate.of(2024, 4, 5),
        )

        assertEquals(1280.0, summary.totalFixedCosts, 0.0)
        assertEquals(1200.0, summary.paidFixedCosts, 0.0)
        assertEquals(80.0, summary.unpaidFixedCosts, 0.0)
        assertEquals(1920.0, summary.monthlySavings, 0.0)
        assertEquals(2200.0, summary.balanceAfterBuffer, 0.0)
        assertEquals(10, summary.daysUntilPayday)
        assertEquals(212.0, summary.dailyBudget, 0.0)
        assertEquals(880.0, summary.targetBalance, 0.0)
    }

    @Test
    fun calculateBudget_supportsMonthOffset() {
        val settings = BudgetSettings(
            balance = 1000.0,
            safetyBuffer = 200.0,
            netSalary = 2500.0,
            paydayDay = 20,
            fixedDailyAmount = 30.0,
        )

        val summary = calculateBudget(
            settings = settings,
            fixedCosts = emptyList(),
            today = LocalDate.of(2024, 1, 25),
            monthOffset = 1,
        )

        assertEquals(19, summary.daysUntilPayday)
        assertEquals(42.0, summary.dailyBudget, 0.0)
        assertEquals(770.0, summary.targetBalance, 0.0)
    }
}
