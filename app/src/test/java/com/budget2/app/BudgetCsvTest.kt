package com.budget2.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCsvTest {
    private fun sampleBackup() = BudgetBackup(
        fixedCosts = listOf(
            FixedCost(1, "Überweisen", "Miete", 600.0, true, -27),
            FixedCost(2, "Abbuchung", "Miete, Strom", 80.55, false, 10),
            FixedCost(3, "Umbuchung", "Zweck \"Spezial\"", 0.0, true, 1),
        ),
        irregularPayments = listOf(
            IrregularPayment(7, "Oktober", "Abbuchung", "Geschenk", 42.5, false),
            IrregularPayment(8, "unbekannt", "Überweisen", "Arzt", 15.0, true),
        ),
        paidFixedCosts = mapOf("2026-10" to setOf(1L, 3L)),
        paidUnknownPayments = mapOf("2026-10" to setOf(8L)),
        paidSalary = mapOf("2026-10" to true, "2026-11" to false),
        settings = DashboardSettings(balance = 1234.56, minimumBuffer = 300.0),
    )

    @Test
    fun encodeThenDecode_restoresEveryValue() {
        val decoded = decodeBudgetCsv(encodeBudgetCsv(sampleBackup()))

        assertEquals(sampleBackup(), decoded)
    }

    @Test
    fun encodedText_isReadableCsvWithHeader() {
        val lines = encodeBudgetCsv(sampleBackup()).trim().lines()

        assertEquals(CSV_HEADER, lines.first())
        assertTrue(lines.any { it.startsWith("Fix,1,Überweisen,Miete,") })
        assertTrue(lines.any { it.startsWith("PaidFix,2026-10,1") })
        assertTrue(lines.any { it.startsWith("Salary,2026-10,true") })
        assertTrue(lines.any { it.startsWith("Settings,1234.56,300") })
    }

    @Test
    fun namesWithCommaAndQuotes_surviveTheRoundTrip() {
        val decoded = decodeBudgetCsv(encodeBudgetCsv(sampleBackup()))

        assertEquals("Miete, Strom", decoded?.fixedCosts?.get(1)?.name)
        assertEquals("Zweck \"Spezial\"", decoded?.fixedCosts?.get(2)?.name)
    }

    @Test
    fun decode_ignoresUnknownRowsAndBrokenLines() {
        val csv = buildString {
            appendLine(CSV_HEADER)
            appendLine("Unbekannt,1,2,3")
            appendLine("Fix,keine-zahl,Abbuchung,Kaputt,1.0,false,4")
            appendLine("Fix,5,Abbuchung,Handy,19.99,false,12")
        }

        val decoded = decodeBudgetCsv(csv)

        assertEquals(1, decoded?.fixedCosts?.size)
        assertEquals("Handy", decoded?.fixedCosts?.first()?.name)
    }

    @Test
    fun decode_returnsNullWhenNothingUsable() {
        assertNull(decodeBudgetCsv(""))
        assertNull(decodeBudgetCsv("irgendein,text\nohne,datensätze"))
    }

    @Test
    fun decode_acceptsFileWithByteOrderMark() {
        val csv = "\uFEFF" + encodeBudgetCsv(sampleBackup())

        assertEquals(3, decodeBudgetCsv(csv)?.fixedCosts?.size)
    }

    @Test
    fun parseCsvLine_handlesQuotedFields() {
        assertEquals(
            listOf("Fix", "1", "Miete, Strom", "10.5"),
            parseCsvLine("Fix,1,\"Miete, Strom\",10.5"),
        )
        assertEquals(
            listOf("a", "sagt \"hallo\""),
            parseCsvLine("a,\"sagt \"\"hallo\"\"\""),
        )
    }
}
