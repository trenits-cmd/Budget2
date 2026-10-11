package com.budget2.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetModelsTest {
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
}
