package com.budget2.app

internal val PaymentTypes = listOf("Überweisen", "Abbuchung", "Umbuchung")

internal data class FixedCost(
    val id: Long,
    val paymentType: String,
    val name: String,
    val amount: Double,
    val isPaid: Boolean,
    val dueDay: Int,
)

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
