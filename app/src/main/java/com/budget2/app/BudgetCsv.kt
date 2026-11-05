package com.budget2.app

/**
 * Complete CSV backup of the app data. The file is plain text, one record per
 * line, comma separated. Fields containing a comma, quote or line break are
 * quoted and inner quotes are doubled, so names like `Miete, Strom` survive a
 * round trip.
 *
 * Record layout (first column = record type):
 * - `Fix,id,paymentType,name,amount,isPaid,dueDay`
 * - `Irreg,id,month,paymentType,name,amount,isPaid`
 * - `PaidFix,month,id`
 * - `PaidUnknown,month,id`
 * - `Salary,month,true|false`
 * - `Settings,balance,minimumBuffer`
 */
internal data class BudgetBackup(
    val fixedCosts: List<FixedCost>,
    val irregularPayments: List<IrregularPayment>,
    val paidFixedCosts: Map<String, Set<Long>>,
    val paidUnknownPayments: Map<String, Set<Long>>,
    val paidSalary: Map<String, Boolean>,
    val settings: DashboardSettings,
)

internal const val CSV_HEADER = "Typ,Feld1,Feld2,Feld3,Feld4,Feld5,Feld6"

internal fun encodeBudgetCsv(backup: BudgetBackup): String = buildString {
    append(CSV_HEADER)
    append("\r\n")
    backup.fixedCosts.forEach { cost ->
        appendCsvRow(
            "Fix",
            cost.id.toString(),
            cost.paymentType,
            cost.name,
            csvNumber(cost.amount),
            cost.isPaid.toString(),
            cost.dueDay.toString(),
        )
    }
    backup.irregularPayments.forEach { payment ->
        appendCsvRow(
            "Irreg",
            payment.id.toString(),
            payment.month,
            payment.paymentType,
            payment.name,
            csvNumber(payment.amount),
            payment.isPaid.toString(),
        )
    }
    backup.paidFixedCosts.forEach { (month, ids) ->
        ids.sorted().forEach { id -> appendCsvRow("PaidFix", month, id.toString()) }
    }
    backup.paidUnknownPayments.forEach { (month, ids) ->
        ids.sorted().forEach { id -> appendCsvRow("PaidUnknown", month, id.toString()) }
    }
    backup.paidSalary.forEach { (month, received) ->
        appendCsvRow("Salary", month, received.toString())
    }
    appendCsvRow("Settings", csvNumber(backup.settings.balance), csvNumber(backup.settings.minimumBuffer))
}

/**
 * Parses a backup produced by [encodeBudgetCsv]. Unknown record types and
 * malformed rows are ignored so a partially edited file still restores what it
 * can. Returns `null` when the text contains no usable record at all.
 */
internal fun decodeBudgetCsv(text: String): BudgetBackup? {
    val fixedCosts = mutableListOf<FixedCost>()
    val irregularPayments = mutableListOf<IrregularPayment>()
    val paidFixedCosts = mutableMapOf<String, MutableSet<Long>>()
    val paidUnknownPayments = mutableMapOf<String, MutableSet<Long>>()
    val paidSalary = mutableMapOf<String, Boolean>()
    var settings: DashboardSettings? = null
    var recognisedAny = false

    parseCsvRecords(text.removePrefix("\uFEFF")).forEach { fields ->
        if (fields.isEmpty()) return@forEach
        when (fields[0].trim()) {
            "Fix" -> {
                if (fields.size >= 7) {
                    runCatching {
                        fixedCosts += FixedCost(
                            id = fields[1].trim().toLong(),
                            paymentType = fields[2],
                            name = fields[3],
                            amount = fields[4].trim().toDouble(),
                            isPaid = fields[5].trim().toBooleanStrictOrNull() ?: false,
                            dueDay = fields[6].trim().toInt(),
                        )
                        recognisedAny = true
                    }
                }
            }
            "Irreg" -> {
                if (fields.size >= 7) {
                    runCatching {
                        irregularPayments += IrregularPayment(
                            id = fields[1].trim().toLong(),
                            month = fields[2],
                            paymentType = fields[3],
                            name = fields[4],
                            amount = fields[5].trim().toDouble(),
                            isPaid = fields[6].trim().toBooleanStrictOrNull() ?: false,
                        )
                        recognisedAny = true
                    }
                }
            }
            "PaidFix" -> {
                if (fields.size >= 3) {
                    runCatching {
                        paidFixedCosts.getOrPut(fields[1].trim()) { mutableSetOf() }
                            .add(fields[2].trim().toLong())
                        recognisedAny = true
                    }
                }
            }
            "PaidUnknown" -> {
                if (fields.size >= 3) {
                    runCatching {
                        paidUnknownPayments.getOrPut(fields[1].trim()) { mutableSetOf() }
                            .add(fields[2].trim().toLong())
                        recognisedAny = true
                    }
                }
            }
            "Salary" -> {
                if (fields.size >= 2) {
                    val month = fields[1].trim()
                    val received = fields.getOrNull(2)?.trim()?.toBooleanStrictOrNull() ?: false
                    if (month.isNotEmpty()) {
                        paidSalary[month] = received
                        recognisedAny = true
                    }
                }
            }
            "Settings" -> {
                val balance = fields.getOrNull(1)?.trim()?.toDoubleOrNull()
                val buffer = fields.getOrNull(2)?.trim()?.toDoubleOrNull()
                if (balance != null || buffer != null) {
                    settings = DashboardSettings(
                        balance = balance ?: 0.0,
                        minimumBuffer = buffer ?: 0.0,
                    )
                    recognisedAny = true
                }
            }
        }
    }

    if (!recognisedAny) return null
    return BudgetBackup(
        fixedCosts = fixedCosts,
        irregularPayments = irregularPayments,
        paidFixedCosts = paidFixedCosts.mapValues { it.value.toSet() },
        paidUnknownPayments = paidUnknownPayments.mapValues { it.value.toSet() },
        paidSalary = paidSalary,
        settings = settings ?: DashboardSettings(),
    )
}

private fun StringBuilder.appendCsvRow(vararg values: String) {
    values.forEachIndexed { index, value ->
        if (index > 0) append(',')
        append(escapeCsvField(value))
    }
    append("\r\n")
}

private fun escapeCsvField(value: String): String {
    val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needsQuotes) return value
    return '"' + value.replace("\"", "\"\"") + '"'
}

private fun csvNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/** Splits one CSV line, honouring quoted fields with doubled quotes. */
internal fun parseCsvLine(line: String): List<String> =
    parseCsvRecords(line).firstOrNull().orEmpty()

/**
 * Splits a complete CSV document into records. Quoted fields may contain commas,
 * doubled quotes and line breaks, so a name such as "Café\nImpôts" survives a
 * round trip. Blank lines are ignored.
 */
internal fun parseCsvRecords(text: String): List<List<String>> {
    if (text.isEmpty()) return emptyList()
    val records = mutableListOf<List<String>>()
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var index = 0

    fun finishRecord() {
        fields += current.toString()
        current.clear()
        if (fields.any { it.isNotEmpty() }) records += fields.toList()
        fields.clear()
    }

    while (index < text.length) {
        val char = text[index]
        when {
            char == '"' -> {
                if (inQuotes && text.getOrNull(index + 1) == '"') {
                    current.append('"')
                    index++
                } else {
                    inQuotes = !inQuotes
                }
            }
            !inQuotes && char == ',' -> {
                fields += current.toString()
                current.clear()
            }
            !inQuotes && (char == '\n' || char == '\r') -> {
                if (char == '\r' && text.getOrNull(index + 1) == '\n') index++
                finishRecord()
            }
            else -> current.append(char)
        }
        index++
    }
    finishRecord()
    return records
}
