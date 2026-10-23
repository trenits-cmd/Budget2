package com.budget2.app

import java.text.NumberFormat
import java.util.Locale

internal fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)

internal fun String.parseAmount(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

internal fun Double.inputValue(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

internal fun dueDayLabel(dueDay: Int): String =
    if (dueDay < 0) "${-dueDay}. Tag vor Monatsende" else "$dueDay."
