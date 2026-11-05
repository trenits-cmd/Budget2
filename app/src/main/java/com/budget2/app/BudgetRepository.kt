package com.budget2.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Local persistence for all budget data. The app intentionally has no network access. */
internal class BudgetRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Migrates old month keys and seeds the default costs on the first launch. */
    fun initialize(today: LocalDate = LocalDate.now()) {
        migrateLegacyMonthStorage(today)
        if (!preferences.contains(KEY_FIXED_COSTS)) {
            saveFixedCosts(DefaultFixedCosts)
            val paidMap = loadPaidFixedCostsMap().toMutableMap()
            val currentMonth = monthKey(today)
            if (currentMonth !in paidMap) {
                paidMap[currentMonth] = DefaultFixedCosts
                    .filter { it.isPaid }
                    .map { it.id }
                    .toSet()
                savePaidFixedCostsMap(paidMap)
            }
        }
    }

    fun loadFixedCosts(): List<FixedCost> {
        val stored = preferences.getString(KEY_FIXED_COSTS, null) ?: return DefaultFixedCosts
        return runCatching {
            val json = JSONArray(stored)
            List(json.length()) { index ->
                val item = json.getJSONObject(index)
                FixedCost(
                    id = item.optLong(KEY_ID),
                    paymentType = item.optString(KEY_PAYMENT_TYPE, PaymentTypes.first()),
                    name = item.optString(KEY_NAME),
                    amount = item.optDouble(KEY_AMOUNT, 0.0),
                    isPaid = item.optBoolean(KEY_IS_PAID, false),
                    dueDay = item.optInt(KEY_DUE_DAY, 1),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveFixedCosts(fixedCosts: List<FixedCost>) {
        preferences.edit().putString(KEY_FIXED_COSTS, fixedCostsToJson(fixedCosts)).apply()
    }

    private fun fixedCostsToJson(fixedCosts: List<FixedCost>): String {
        val json = JSONArray()
        fixedCosts.forEach { cost ->
            json.put(
                JSONObject().apply {
                    put(KEY_ID, cost.id)
                    put(KEY_PAYMENT_TYPE, cost.paymentType)
                    put(KEY_NAME, cost.name)
                    put(KEY_AMOUNT, cost.amount)
                    put(KEY_IS_PAID, cost.isPaid)
                    put(KEY_DUE_DAY, cost.dueDay)
                },
            )
        }
        return json.toString()
    }

    fun loadIrregularPayments(): List<IrregularPayment> {
        val stored = preferences.getString(KEY_IRREGULAR_PAYMENTS, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(stored)
            List(json.length()) { index ->
                val item = json.getJSONObject(index)
                IrregularPayment(
                    id = item.optLong(KEY_ID),
                    month = item.optString(KEY_MONTH, "unbekannt"),
                    paymentType = item.optString(KEY_PAYMENT_TYPE, PaymentTypes.first()),
                    name = item.optString(KEY_NAME),
                    amount = item.optDouble(KEY_AMOUNT, 0.0),
                    isPaid = item.optBoolean(KEY_IS_PAID, false),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveIrregularPayments(payments: List<IrregularPayment>) {
        preferences.edit().putString(KEY_IRREGULAR_PAYMENTS, irregularPaymentsToJson(payments)).apply()
    }

    private fun irregularPaymentsToJson(payments: List<IrregularPayment>): String {
        val json = JSONArray()
        payments.forEach { payment ->
            json.put(
                JSONObject().apply {
                    put(KEY_ID, payment.id)
                    put(KEY_MONTH, payment.month)
                    put(KEY_PAYMENT_TYPE, payment.paymentType)
                    put(KEY_NAME, payment.name)
                    put(KEY_AMOUNT, payment.amount)
                    put(KEY_IS_PAID, payment.isPaid)
                },
            )
        }
        return json.toString()
    }

    fun loadDashboardSettings(): DashboardSettings {
        val stored = preferences.getString(KEY_DASHBOARD_SETTINGS, null)
            ?: return DashboardSettings()
        return runCatching {
            val json = JSONObject(stored)
            DashboardSettings(
                balance = json.optDouble(KEY_BALANCE, 0.0),
                minimumBuffer = json.optDouble(KEY_MINIMUM_BUFFER, 0.0),
            )
        }.getOrDefault(DashboardSettings())
    }

    fun saveDashboardSettings(settings: DashboardSettings) {
        preferences.edit().putString(KEY_DASHBOARD_SETTINGS, settingsToJson(settings)).apply()
    }

    private fun settingsToJson(settings: DashboardSettings): String =
        JSONObject().apply {
            put(KEY_BALANCE, settings.balance)
            put(KEY_MINIMUM_BUFFER, settings.minimumBuffer)
        }.toString()

    fun loadPaidFixedCostsMap(): Map<String, Set<Long>> =
        loadLongSetMap(KEY_PAID_FIXED_COSTS)

    fun savePaidFixedCostsMap(map: Map<String, Set<Long>>) =
        saveLongSetMap(KEY_PAID_FIXED_COSTS, map)

    fun loadPaidUnknownPaymentsMap(): Map<String, Set<Long>> =
        loadLongSetMap(KEY_PAID_UNKNOWN_PAYMENTS)

    fun savePaidUnknownPaymentsMap(map: Map<String, Set<Long>>) =
        saveLongSetMap(KEY_PAID_UNKNOWN_PAYMENTS, map)

    fun loadPaidSalaryMap(): Map<String, Boolean> {
        val stored = preferences.getString(KEY_PAID_SALARY, null) ?: return emptyMap()
        return runCatching {
            val map = mutableMapOf<String, Boolean>()
            val json = JSONObject(stored)
            for (month in json.keys()) {
                map[month] = json.optBoolean(month, false)
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun savePaidSalaryMap(map: Map<String, Boolean>) {
        preferences.edit().putString(KEY_PAID_SALARY, salaryMapToJson(map)).apply()
    }

    private fun salaryMapToJson(map: Map<String, Boolean>): String =
        JSONObject().apply {
            map.forEach { (month, received) -> put(month, received) }
        }.toString()

    fun toggleFixedCostPaid(
        costId: Long,
        viewingDate: LocalDate,
        isPaid: Boolean,
    ): Map<String, Set<Long>> {
        val map = loadPaidFixedCostsMap().toMutableMap()
        val month = monthKey(viewingDate)
        val paidIds = map[month].orEmpty().toMutableSet()
        if (isPaid) paidIds.add(costId) else paidIds.remove(costId)
        map[month] = paidIds
        savePaidFixedCostsMap(map)
        return map
    }

    fun resetFixedCostsPaid(viewingDate: LocalDate): Map<String, Set<Long>> {
        val map = loadPaidFixedCostsMap().toMutableMap()
        map[monthKey(viewingDate)] = emptySet()
        savePaidFixedCostsMap(map)
        return map
    }

    fun toggleUnknownPaymentPaid(
        paymentId: Long,
        viewingDate: LocalDate,
        isPaid: Boolean,
    ): Map<String, Set<Long>> {
        val map = loadPaidUnknownPaymentsMap().toMutableMap()
        val month = monthKey(viewingDate)
        val paidIds = map[month].orEmpty().toMutableSet()
        if (isPaid) paidIds.add(paymentId) else paidIds.remove(paymentId)
        map[month] = paidIds
        savePaidUnknownPaymentsMap(map)
        return map
    }

    fun toggleSalaryReceived(
        viewingDate: LocalDate,
        received: Boolean,
    ): Map<String, Boolean> {
        val map = loadPaidSalaryMap().toMutableMap()
        map[monthKey(viewingDate)] = received
        savePaidSalaryMap(map)
        return map
    }

    private fun loadLongSetMap(preferenceName: String): Map<String, Set<Long>> {
        val stored = preferences.getString(preferenceName, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(stored)
            val map = mutableMapOf<String, Set<Long>>()
            for (month in json.keys()) {
                val array = json.optJSONArray(month) ?: continue
                val ids = mutableSetOf<Long>()
                for (index in 0 until array.length()) {
                    ids.add(array.optLong(index))
                }
                map[month] = ids
            }
            map
        }.getOrDefault(emptyMap())
    }

    private fun saveLongSetMap(preferenceName: String, map: Map<String, Set<Long>>) {
        preferences.edit().putString(preferenceName, longSetMapToJson(map)).apply()
    }

    private fun longSetMapToJson(map: Map<String, Set<Long>>): String =
        JSONObject().apply {
            map.forEach { (month, ids) ->
                val array = JSONArray()
                ids.forEach { array.put(it) }
                put(month, array)
            }
        }.toString()

    /** Reads every stored dataset so it can be written to a CSV backup file. */
    fun exportBackup(): BudgetBackup = BudgetBackup(
        fixedCosts = loadFixedCosts(),
        irregularPayments = loadIrregularPayments(),
        paidFixedCosts = loadPaidFixedCostsMap(),
        paidUnknownPayments = loadPaidUnknownPaymentsMap(),
        paidSalary = loadPaidSalaryMap(),
        settings = loadDashboardSettings(),
    )

    /**
     * Replaces every stored dataset with the content of an imported backup.
     * All six datasets are written through one editor, so the app never keeps a
     * half-imported state if a single write fails.
     */
    fun importBackup(backup: BudgetBackup) {
        preferences.edit()
            .putString(KEY_FIXED_COSTS, fixedCostsToJson(backup.fixedCosts))
            .putString(KEY_IRREGULAR_PAYMENTS, irregularPaymentsToJson(backup.irregularPayments))
            .putString(KEY_DASHBOARD_SETTINGS, settingsToJson(backup.settings))
            .putString(KEY_PAID_FIXED_COSTS, longSetMapToJson(backup.paidFixedCosts))
            .putString(KEY_PAID_UNKNOWN_PAYMENTS, longSetMapToJson(backup.paidUnknownPayments))
            .putString(KEY_PAID_SALARY, salaryMapToJson(backup.paidSalary))
            .apply()
    }

    /**
     * Older app versions stored paid states under bare German month names ("oktober").
     * They are migrated to the year-aware "yyyy-MM" format so another year does not
     * inherit last year's payment status.
     */
    private fun migrateLegacyMonthStorage(today: LocalDate) {
        listOf(KEY_PAID_FIXED_COSTS, KEY_PAID_UNKNOWN_PAYMENTS).forEach { name ->
            val stored = preferences.getString(name, null) ?: return@forEach
            runCatching {
                val source = JSONObject(stored)
                val merged = linkedMapOf<String, MutableSet<Long>>()
                for (oldKey in source.keys()) {
                    val newKey = migrateLegacyMonthKey(oldKey, today)
                    val ids = merged.getOrPut(newKey) { mutableSetOf() }
                    val array = source.optJSONArray(oldKey) ?: continue
                    for (index in 0 until array.length()) {
                        ids.add(array.optLong(index))
                    }
                }
                val target = JSONObject()
                merged.forEach { (month, ids) ->
                    val array = JSONArray()
                    ids.forEach { array.put(it) }
                    target.put(month, array)
                }
                if (target.toString() != source.toString()) {
                    preferences.edit().putString(name, target.toString()).apply()
                }
            }
        }

        val storedSalary = preferences.getString(KEY_PAID_SALARY, null) ?: return
        runCatching {
            val source = JSONObject(storedSalary)
            val target = JSONObject()
            for (oldKey in source.keys()) {
                target.put(
                    migrateLegacyMonthKey(oldKey, today),
                    source.optBoolean(oldKey, false),
                )
            }
            if (target.toString() != source.toString()) {
                preferences.edit().putString(KEY_PAID_SALARY, target.toString()).apply()
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "budget"

        const val KEY_FIXED_COSTS = "fixedCosts"
        const val KEY_IRREGULAR_PAYMENTS = "irregularPayments"
        const val KEY_DASHBOARD_SETTINGS = "dashboardSettings"
        const val KEY_PAID_FIXED_COSTS = "paidFixedCostsPerMonth"
        const val KEY_PAID_UNKNOWN_PAYMENTS = "paidUnknownPaymentsPerMonth"
        const val KEY_PAID_SALARY = "paidSalaryMap"

        const val KEY_ID = "id"
        const val KEY_MONTH = "month"
        const val KEY_PAYMENT_TYPE = "paymentType"
        const val KEY_NAME = "name"
        const val KEY_AMOUNT = "amount"
        const val KEY_IS_PAID = "isPaid"
        const val KEY_DUE_DAY = "dueDay"
        const val KEY_BALANCE = "balance"
        const val KEY_MINIMUM_BUFFER = "minimumBuffer"
    }
}
