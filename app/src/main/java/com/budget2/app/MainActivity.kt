package com.budget2.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Forest = Color(0xFF0F172A)
private val Paper = Color(0xFFF4F7FB)
private val Ink = Color(0xFF102033)
private val Muted = Color(0xFF62748A)
private val PaidBackground = Color(0xFFE7F6EC)
private val UnpaidBackground = Color(0xFFFFE9E6)
private val PaidTint = Color(0xFF28734A)
private val UnpaidTint = Color(0xFFB34135)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Forest,
                    onPrimary = Color.White,
                    background = Paper,
                    surface = Color.White,
                    onSurface = Ink,
                ),
            ) {
                FixedCostsApp(this)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FixedCostsApp(context: Context) {
    var fixedCosts by remember { mutableStateOf(loadFixedCosts(context)) }
    var irregularPayments by remember { mutableStateOf(loadIrregularPayments(context)) }
    var dashboardSettings by remember { mutableStateOf(loadDashboardSettings(context)) }
    var paidFixedCostsMap by remember { mutableStateOf(loadPaidFixedCostsMap(context)) }
    var paidUnknownPaymentsMap by remember { mutableStateOf(loadPaidUnknownPaymentsMap(context)) }

    var selectedTab by remember { mutableStateOf(0) }
    var viewingMonthOffset by remember { mutableStateOf(0) } // 0 = current month, 1 = next month

    var showEditor by remember { mutableStateOf(false) }
    var editingCost by remember { mutableStateOf<FixedCost?>(null) }
    var deletingCost by remember { mutableStateOf<FixedCost?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    var showIrregularEditor by remember { mutableStateOf(false) }
    var editingIrregular by remember { mutableStateOf<IrregularPayment?>(null) }
    var deletingIrregular by remember { mutableStateOf<IrregularPayment?>(null) }
    var confirmIrregularReset by remember { mutableStateOf(false) }

    val viewingDate = LocalDate.now().plusMonths(viewingMonthOffset.toLong())
    val viewingMonthName = viewingDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))

    fun updateCosts(updated: List<FixedCost>) {
        fixedCosts = updated
        saveFixedCosts(context, updated)
    }

    fun updateIrregular(updated: List<IrregularPayment>) {
        irregularPayments = updated
        saveIrregularPayments(context, updated)
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("FIXKOSTEN", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                viewingMonthName.replaceFirstChar { it.titlecase(Locale.GERMANY) },
                                fontSize = 15.sp,
                                color = Ink,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewingMonthOffset = if (viewingMonthOffset == 0) 1 else 0 },
                        colors = ButtonDefaults.textButtonColors(containerColor = Paper, contentColor = Ink),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(if (viewingMonthOffset == 0) "Nächster Monat ➔" else "◀ Aktueller Monat", fontSize = 11.sp)
                    }
                }
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                    listOf("Fixkosten", "Unregelmäßig", "Dashboard", "Settings").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> FixedCostsTab(
                fixedCosts = fixedCosts,
                irregularPayments = irregularPayments,
                viewingDate = viewingDate,
                paidFixedCostsMap = paidFixedCostsMap,
                paidUnknownPaymentsMap = paidUnknownPaymentsMap,
                modifier = Modifier.padding(padding),
                onToggleFixedCost = { cost, isPaid ->
                    paidFixedCostsMap = toggleFixedCostPaid(context, cost.id, viewingDate, isPaid)
                },
                onToggleIrregular = { payment, isPaid ->
                    val cleanMonth = payment.month.trim().lowercase()
                    if (cleanMonth == "unbekannt") {
                        paidUnknownPaymentsMap = toggleUnknownPaymentPaid(context, payment.id, viewingDate, isPaid)
                    } else {
                        updateIrregular(updateIrregularPaymentStatus(irregularPayments, payment.id, isPaid))
                    }
                },
                onResetStatuses = { confirmReset = true },
                onGoToSettings = { selectedTab = 3 },
            )
            1 -> IrregularPaymentsTab(
                irregularPayments = irregularPayments,
                viewingDate = viewingDate,
                paidUnknownPaymentsMap = paidUnknownPaymentsMap,
                modifier = Modifier.padding(padding),
                onToggleStatus = { payment, isPaid ->
                    val cleanMonth = payment.month.trim().lowercase()
                    if (cleanMonth == "unbekannt") {
                        paidUnknownPaymentsMap = toggleUnknownPaymentPaid(context, payment.id, viewingDate, isPaid)
                    } else {
                        updateIrregular(updateIrregularPaymentStatus(irregularPayments, payment.id, isPaid))
                    }
                },
                onResetStatuses = { confirmIrregularReset = true },
                onAdd = {
                    editingIrregular = null
                    showIrregularEditor = true
                },
                onEdit = { payment ->
                    editingIrregular = payment
                    showIrregularEditor = true
                },
                onDelete = { deletingIrregular = it },
            )
            2 -> DashboardTab(
                fixedCosts = fixedCosts,
                irregularPayments = irregularPayments,
                settings = dashboardSettings,
                viewingDate = viewingDate,
                paidFixedCostsMap = paidFixedCostsMap,
                paidUnknownPaymentsMap = paidUnknownPaymentsMap,
                modifier = Modifier.padding(padding),
            )
            3 -> SettingsTab(
                fixedCosts = fixedCosts,
                dashboardSettings = dashboardSettings,
                modifier = Modifier.padding(padding),
                onSaveDashboardSettings = { updated ->
                    dashboardSettings = updated
                    saveDashboardSettings(context, updated)
                },
                onAdd = {
                    editingCost = null
                    showEditor = true
                },
                onEdit = { cost ->
                    editingCost = cost
                    showEditor = true
                },
                onDelete = { deletingCost = it },
            )
        }
    }

    if (showEditor) {
        key(editingCost?.id) {
            FixedCostEditorDialog(
                initial = editingCost,
                onDismiss = { showEditor = false },
                onSave = { paymentType, name, amount, dueDay ->
                    val existing = editingCost
                    val updatedCost = FixedCost(
                        id = existing?.id ?: ((fixedCosts.maxOfOrNull { it.id } ?: 0L) + 1L),
                        paymentType = paymentType,
                        name = name,
                        amount = amount,
                        isPaid = existing?.isPaid ?: false,
                        dueDay = dueDay,
                    )
                    val updated = if (existing == null) {
                        fixedCosts + updatedCost
                    } else {
                        fixedCosts.map { if (it.id == existing.id) updatedCost else it }
                    }
                    updateCosts(updated)
                    showEditor = false
                },
            )
        }
    }

    if (showIrregularEditor) {
        key(editingIrregular?.id) {
            IrregularPaymentEditorDialog(
                initial = editingIrregular,
                onDismiss = { showIrregularEditor = false },
                onSave = { month, paymentType, name, amount ->
                    val existing = editingIrregular
                    val updatedPayment = IrregularPayment(
                        id = existing?.id ?: ((irregularPayments.maxOfOrNull { it.id } ?: 0L) + 1L),
                        month = month,
                        paymentType = paymentType,
                        name = name,
                        amount = amount,
                        isPaid = existing?.isPaid ?: false,
                    )
                    val updated = if (existing == null) {
                        irregularPayments + updatedPayment
                    } else {
                        irregularPayments.map { if (it.id == existing.id) updatedPayment else it }
                    }
                    updateIrregular(updated)
                    showIrregularEditor = false
                },
            )
        }
    }

    deletingCost?.let { cost ->
        AlertDialog(
            onDismissRequest = { deletingCost = null },
            title = { Text("Zweck löschen?") },
            text = { Text("„${cost.name}“ wird aus deinen Fixkosten und Settings entfernt.") },
            confirmButton = {
                Button(
                    onClick = {
                        updateCosts(fixedCosts.filterNot { it.id == cost.id })
                        deletingCost = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnpaidTint),
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCost = null }) { Text("Abbrechen", color = Muted) }
            },
            containerColor = Color.White,
        )
    }

    deletingIrregular?.let { payment ->
        AlertDialog(
            onDismissRequest = { deletingIrregular = null },
            title = { Text("Unregelmäßige Zahlung löschen?") },
            text = { Text("„${payment.name}“ wird gelöscht.") },
            confirmButton = {
                Button(
                    onClick = {
                        updateIrregular(irregularPayments.filterNot { it.id == payment.id })
                        deletingIrregular = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnpaidTint),
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deletingIrregular = null }) { Text("Abbrechen", color = Muted) }
            },
            containerColor = Color.White,
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Alle für diesen Monat als unbezahlt markieren?") },
            text = { Text("Der Status aller Fixkosten und Zahlungen für ${viewingMonthName} wird zurückgesetzt.") },
            confirmButton = {
                Button(
                    onClick = {
                        paidFixedCostsMap = resetFixedCostsPaid(context, viewingDate)
                        val monthKey = getMonthKey(viewingDate)
                        val unknownMap = paidUnknownPaymentsMap.toMutableMap()
                        unknownMap[monthKey] = emptySet()
                        paidUnknownPaymentsMap = unknownMap
                        savePaidUnknownPaymentsMap(context, unknownMap)

                        val updatedIrregular = irregularPayments.map { payment ->
                            if (isActiveMonth(payment.month, viewingDate) && payment.month.trim().lowercase() != "unbekannt") {
                                payment.copy(isPaid = false)
                            } else {
                                payment
                            }
                        }
                        updateIrregular(updatedIrregular)
                        confirmReset = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) { Text("Zurücksetzen") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Abbrechen", color = Muted) }
            },
            containerColor = Color.White,
        )
    }

    if (confirmIrregularReset) {
        AlertDialog(
            onDismissRequest = { confirmIrregularReset = false },
            title = { Text("Alle unregelmäßigen als unbezahlt markieren?") },
            text = { Text("Der Status aller unregelmäßigen Zahlungen für ${viewingMonthName} wird zurückgesetzt.") },
            confirmButton = {
                Button(
                    onClick = {
                        val monthKey = getMonthKey(viewingDate)
                        val unknownMap = paidUnknownPaymentsMap.toMutableMap()
                        unknownMap[monthKey] = emptySet()
                        paidUnknownPaymentsMap = unknownMap
                        savePaidUnknownPaymentsMap(context, unknownMap)

                        val updatedIrregular = irregularPayments.map { payment ->
                            if (isActiveMonth(payment.month, viewingDate) && payment.month.trim().lowercase() != "unbekannt") {
                                payment.copy(isPaid = false)
                            } else {
                                payment
                            }
                        }
                        updateIrregular(updatedIrregular)
                        confirmIrregularReset = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) { Text("Zurücksetzen") }
            },
            dismissButton = {
                TextButton(onClick = { confirmIrregularReset = false }) { Text("Abbrechen", color = Muted) }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun DashboardTab(
    fixedCosts: List<FixedCost>,
    irregularPayments: List<IrregularPayment>,
    settings: DashboardSettings,
    viewingDate: LocalDate,
    paidFixedCostsMap: Map<String, Set<Long>>,
    paidUnknownPaymentsMap: Map<String, Set<Long>>,
    modifier: Modifier = Modifier,
) {
    val monthKey = getMonthKey(viewingDate)
    val paidFixedIds = paidFixedCostsMap[monthKey] ?: emptySet()
    val paidUnknownIds = paidUnknownPaymentsMap[monthKey] ?: emptySet()

    val totals = paymentTypeTotals(fixedCosts)
    val summary = calculateDashboardSummary(
        fixedCosts = fixedCosts,
        paidFixedCostIds = paidFixedIds,
        irregularPayments = irregularPayments,
        paidUnknownPaymentIds = paidUnknownIds,
        settings = settings,
        today = LocalDate.now(),
        viewingDate = viewingDate,
    )
    val activeIrregular = irregularPayments.filter { isActiveMonth(it.month, viewingDate) }
    val total = fixedCosts.sumOf { it.amount } + activeIrregular.sumOf { it.amount }
    
    val paid = fixedCosts.filter { it.id in paidFixedIds }.sumOf { it.amount } +
        activeIrregular.filter { payment ->
            val clean = payment.month.trim().lowercase()
            if (clean == "unbekannt") payment.id in paidUnknownIds else payment.isPaid
        }.sumOf { it.amount }

    val unpaid = total - paid
    val maxPaymentTypeTotal = totals.maxOfOrNull { it.total } ?: 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dashboard", fontSize = 20.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                Text("Kostenübersicht für ${viewingDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))}", fontSize = 13.sp, color = Muted)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardSummary(
                    label = "Gesamtkosten",
                    value = money(total),
                    modifier = Modifier.weight(1f),
                    tint = Forest,
                )
                DashboardSummary(
                    label = "Unbezahlt",
                    value = money(unpaid),
                    modifier = Modifier.weight(1f),
                    tint = UnpaidTint,
                )
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Planung bis zum Zahltag", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Stichtag ${summary.cutoffDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY))} · " +
                                "${summary.daysRemaining.coerceAtLeast(0)} Tage verbleibend",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardSummary(
                            label = "Tagesbudget heute",
                            value = money(summary.dailyBudgetToday),
                            modifier = Modifier.weight(1f),
                            tint = Forest,
                        )
                        DashboardSummary(
                            label = "Tagesbudget morgen",
                            value = money(summary.dailyBudgetTomorrow),
                            modifier = Modifier.weight(1f),
                            tint = Forest,
                        )
                    }
                    DashboardMetric("Bezahlte Fixkosten (inkl. Monats-Zahlungen)", money(summary.paidFixedCosts), PaidTint)
                    DashboardMetric("Restliche Fixkosten", money(summary.remainingFixedCosts), UnpaidTint)
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Kontostand & Puffer", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    DashboardMetric("Kontostand", money(settings.balance))
                    DashboardMetric("Puffer heute berechnet", money(summary.bufferToday))
                    DashboardMetric("Puffer morgen berechnet", money(summary.bufferTomorrow))
                    DashboardMetric("Kontostand minus Puffer", money(summary.balanceMinusBuffer))
                    DashboardMetric("Soll-Kontostand heute", money(summary.targetBalanceToday))
                    DashboardMetric("Soll-Kontostand morgen", money(summary.targetBalanceTomorrow))
                    Text(
                        "Berechnet mit ${money(DAILY_TARGET)} Tagesziel und Mindestpuffer ${money(settings.minimumBuffer)}.",
                        fontSize = 11.sp,
                        color = Muted,
                    )
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Nach Zahlungsart", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Gesamtbetrag und Anteil bezahlt/unbezahlt", fontSize = 12.sp, color = Muted)
                    }
                    if (totals.isEmpty()) {
                        Text("Noch keine Fixkosten vorhanden.", fontSize = 13.sp, color = Muted)
                    } else {
                        totals.forEach { item ->
                            PaymentTypeBar(item, maxPaymentTypeTotal)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        DashboardLegend(color = PaidTint, label = "Bezahlt")
                        DashboardLegend(color = UnpaidTint, label = "Unbezahlt")
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Zahlungsstatus", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Anteil an den gesamten Kosten", fontSize = 12.sp, color = Muted)
                    }
                    StatusBar("Bezahlt", paid, total, PaidTint)
                    StatusBar("Unbezahlt", unpaid, total, UnpaidTint)
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, valueColor: Color = Ink) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Muted)
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DashboardSummary(label: String, value: String, modifier: Modifier, tint: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, fontSize = 12.sp, color = Muted)
            Text(value, fontSize = 17.sp, color = tint, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun PaymentTypeBar(total: PaymentTypeTotal, maxTotal: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(total.paymentType, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium)
            Text(money(total.total), fontSize = 13.sp, color = Ink, fontWeight = FontWeight.SemiBold)
        }
        Box(
            Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(99.dp)).background(PaidBackground),
            contentAlignment = Alignment.CenterStart,
        ) {
            val fraction = if (maxTotal > 0) (total.total / maxTotal).toFloat().coerceIn(0f, 1f) else 0f
            if (fraction > 0f) {
                Row(
                    Modifier.fillMaxWidth(fraction).height(14.dp).clip(RoundedCornerShape(99.dp)),
                ) {
                    if (total.paid > 0) {
                        Box(Modifier.weight(total.paid.toFloat()).fillMaxSize().background(PaidTint))
                    }
                    if (total.unpaid > 0) {
                        Box(Modifier.weight(total.unpaid.toFloat()).fillMaxSize().background(UnpaidTint))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bezahlt ${money(total.paid)}", fontSize = 10.sp, color = PaidTint)
            Text("Offen ${money(total.unpaid)}", fontSize = 10.sp, color = UnpaidTint)
        }
    }
}

@Composable
private fun StatusBar(label: String, amount: Double, total: Double, tint: Color) {
    val fraction = if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink)
            Text(money(amount), fontSize = 13.sp, color = tint, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("${(fraction * 100).toInt()}%", fontSize = 12.sp, color = Muted)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(Paper)) {
            if (fraction > 0f) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxSize().clip(RoundedCornerShape(99.dp)).background(tint))
            }
        }
    }
}

@Composable
private fun DashboardLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(99.dp)).background(color))
        Text(label, fontSize = 11.sp, color = Muted)
    }
}

@Composable
private fun FixedCostsTab(
    fixedCosts: List<FixedCost>,
    irregularPayments: List<IrregularPayment>,
    viewingDate: LocalDate,
    paidFixedCostsMap: Map<String, Set<Long>>,
    paidUnknownPaymentsMap: Map<String, Set<Long>>,
    modifier: Modifier = Modifier,
    onToggleFixedCost: (FixedCost, Boolean) -> Unit,
    onToggleIrregular: (IrregularPayment, Boolean) -> Unit,
    onResetStatuses: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    val monthKey = getMonthKey(viewingDate)
    val paidFixedIds = paidFixedCostsMap[monthKey] ?: emptySet()
    val paidUnknownIds = paidUnknownPaymentsMap[monthKey] ?: emptySet()

    val activeIrregular = irregularPayments.filter { isActiveMonth(it.month, viewingDate) }
    val totalCount = fixedCosts.size + activeIrregular.size

    val paidFixedCount = fixedCosts.count { it.id in paidFixedIds }
    val paidIrregularCount = activeIrregular.count { payment ->
        val clean = payment.month.trim().lowercase()
        if (clean == "unbekannt") payment.id in paidUnknownIds else payment.isPaid
    }
    val unpaidCount = totalCount - (paidFixedCount + paidIrregularCount)
    val hasPaid = (paidFixedCount + paidIrregularCount) > 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("$totalCount Zwecke (für ${viewingDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMANY))})", fontSize = 17.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("$unpaidCount unbezahlt", fontSize = 13.sp, color = if (unpaidCount > 0) UnpaidTint else PaidTint)
                }
                TextButton(onClick = onResetStatuses, enabled = hasPaid) {
                    Text("Alle unbezahlt", fontSize = 12.sp)
                }
            }
        }
        item { FixedCostTableHeader() }
        if (fixedCosts.isEmpty() && activeIrregular.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Noch keine Fixkosten oder Zahlungen für diesen Monat.", color = Ink, fontWeight = FontWeight.Medium)
                        Text("Lege Fixkosten in Settings oder unregelmäßige Zahlungen im Tab an.", color = Muted, fontSize = 13.sp)
                        TextButton(onClick = onGoToSettings) { Text("Zu Settings") }
                    }
                }
            }
        } else {
            items(fixedCosts, key = { "fc_${it.id}" }) { cost ->
                val isPaid = cost.id in paidFixedIds
                FixedCostTableRow(cost, isPaid, onToggleFixedCost)
            }
            items(activeIrregular, key = { "irr_${it.id}" }) { payment ->
                val clean = payment.month.trim().lowercase()
                val isPaid = if (clean == "unbekannt") payment.id in paidUnknownIds else payment.isPaid
                IrregularPaymentTableRowInFixkosten(payment, isPaid, onToggleIrregular)
            }
        }
    }
}

@Composable
private fun IrregularPaymentsTab(
    irregularPayments: List<IrregularPayment>,
    viewingDate: LocalDate,
    paidUnknownPaymentsMap: Map<String, Set<Long>>,
    modifier: Modifier = Modifier,
    onToggleStatus: (IrregularPayment, Boolean) -> Unit,
    onResetStatuses: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (IrregularPayment) -> Unit,
    onDelete: (IrregularPayment) -> Unit,
) {
    val monthKey = getMonthKey(viewingDate)
    val paidUnknownIds = paidUnknownPaymentsMap[monthKey] ?: emptySet()

    val unpaidCount = irregularPayments.count { payment ->
        val clean = payment.month.trim().lowercase()
        val isPaid = if (clean == "unbekannt") payment.id in paidUnknownIds else payment.isPaid
        !isPaid
    }
    val hasPaid = irregularPayments.any { payment ->
        val clean = payment.month.trim().lowercase()
        if (clean == "unbekannt") payment.id in paidUnknownIds else payment.isPaid
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${irregularPayments.size} unregelmäßige Zahlungen", fontSize = 17.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("$unpaidCount unbezahlt (in ${viewingDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMANY))})", fontSize = 13.sp, color = if (unpaidCount > 0) UnpaidTint else PaidTint)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onResetStatuses, enabled = hasPaid) {
                        Text("Alle unbezahlt", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onAdd,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Forest),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Neu", fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TableCell("Monat", Modifier.weight(1.1f))
                TableCell("Zahlungsart", Modifier.weight(1.1f))
                TableCell("Zweck", Modifier.weight(1.3f))
                TableCell("Betrag", Modifier.weight(1.0f))
                TableCell("Status", Modifier.weight(1.05f))
            }
        }
        if (irregularPayments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Noch keine unregelmäßigen Zahlungen vorhanden.", color = Ink, fontWeight = FontWeight.Medium)
                        Text("Füge Zahlungen mit spezifischem Monat oder „unbekannt“ hinzu.", color = Muted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(irregularPayments.sortedBy { it.month }, key = { it.id }) { payment ->
                val clean = payment.month.trim().lowercase()
                val isPaid = if (clean == "unbekannt") payment.id in paidUnknownIds else payment.isPaid
                IrregularPaymentTableRow(payment, isPaid, onToggleStatus, onEdit, onDelete)
            }
        }
    }
}

@Composable
private fun FixedCostTableHeader() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableCell("Zahlungsart", Modifier.weight(1.2f))
        TableCell("Zweck", Modifier.weight(1.3f))
        TableCell("Betrag", Modifier.weight(1.1f))
        TableCell("Status", Modifier.weight(1.05f))
        TableCell("Datum", Modifier.weight(0.75f))
    }
}

@Composable
private fun FixedCostTableRow(cost: FixedCost, isPaid: Boolean, onToggleStatus: (FixedCost, Boolean) -> Unit) {
    val tint = if (isPaid) PaidTint else UnpaidTint
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPaid) PaidBackground else UnpaidBackground),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TableCell(cost.paymentType, Modifier.weight(1.2f), color = Ink)
            TableCell(cost.name, Modifier.weight(1.3f), color = Ink, bold = true)
            TableCell(money(cost.amount), Modifier.weight(1.1f), color = Ink)
            Column(Modifier.weight(1.05f), horizontalAlignment = Alignment.Start) {
                Text(if (isPaid) "bezahlt" else "unbezahlt", fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = isPaid,
                    onCheckedChange = { onToggleStatus(cost, it) },
                    modifier = Modifier.height(32.dp),
                )
            }
            TableCell(cost.dueDay.toString(), Modifier.weight(0.75f), color = Ink)
        }
    }
}

@Composable
private fun IrregularPaymentTableRowInFixkosten(payment: IrregularPayment, isPaid: Boolean, onToggleStatus: (IrregularPayment, Boolean) -> Unit) {
    val tint = if (isPaid) PaidTint else UnpaidTint
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPaid) PaidBackground else UnpaidBackground),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TableCell(payment.paymentType, Modifier.weight(1.2f), color = Ink)
            TableCell("${payment.name} (Unregelmäßig)", Modifier.weight(1.3f), color = Ink, bold = true)
            TableCell(money(payment.amount), Modifier.weight(1.1f), color = Ink)
            Column(Modifier.weight(1.05f), horizontalAlignment = Alignment.Start) {
                Text(if (isPaid) "bezahlt" else "unbezahlt", fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = isPaid,
                    onCheckedChange = { onToggleStatus(payment, it) },
                    modifier = Modifier.height(32.dp),
                )
            }
            TableCell(payment.month, Modifier.weight(0.75f), color = Muted)
        }
    }
}

@Composable
private fun IrregularPaymentTableRow(
    payment: IrregularPayment,
    isPaid: Boolean,
    onToggleStatus: (IrregularPayment, Boolean) -> Unit,
    onEdit: (IrregularPayment) -> Unit,
    onDelete: (IrregularPayment) -> Unit,
) {
    val tint = if (isPaid) PaidTint else UnpaidTint
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPaid) PaidBackground else UnpaidBackground),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TableCell(payment.month, Modifier.weight(1.1f), color = Ink, bold = true)
            TableCell(payment.paymentType, Modifier.weight(1.1f), color = Ink)
            TableCell(payment.name, Modifier.weight(1.3f), color = Ink, bold = true)
            TableCell(money(payment.amount), Modifier.weight(1.0f), color = Ink)
            Column(Modifier.weight(1.05f), horizontalAlignment = Alignment.Start) {
                Text(if (isPaid) "bezahlt" else "unbezahlt", fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = isPaid,
                    onCheckedChange = { onToggleStatus(payment, it) },
                    modifier = Modifier.height(32.dp),
                )
            }
            IconButton(onClick = { onEdit(payment) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Bearbeiten", tint = Forest, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { onDelete(payment) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Löschen", tint = UnpaidTint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TableCell(text: String, modifier: Modifier, color: Color = Muted, bold: Boolean = false) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 3.dp),
        fontSize = 10.sp,
        color = color,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SettingsTab(
    fixedCosts: List<FixedCost>,
    dashboardSettings: DashboardSettings,
    modifier: Modifier = Modifier,
    onSaveDashboardSettings: (DashboardSettings) -> Unit,
    onAdd: () -> Unit,
    onEdit: (FixedCost) -> Unit,
    onDelete: (FixedCost) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardSettingsCard(dashboardSettings, onSaveDashboardSettings)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Zwecke verwalten", fontSize = 17.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("Hier gepflegte Daten erscheinen bei den Fixkosten.", fontSize = 12.sp, color = Muted)
                }

                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Hinzufügen", fontSize = 12.sp)
                }
            }
        }
        if (fixedCosts.isEmpty()) {
            item {
                Text(
                    "Noch keine Zwecke angelegt. Füge zum Beispiel Miete oder Strom hinzu.",
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Muted,
                    fontSize = 13.sp,
                )
            }
        } else {
            items(fixedCosts.sortedBy { it.name }, key = { it.id }) { cost ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(cost.name, fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${cost.paymentType} · ${money(cost.amount)} · Datum ${cost.dueDay}",
                                fontSize = 12.sp,
                                color = Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onEdit(cost) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Zweck bearbeiten", tint = Forest)
                        }
                        IconButton(onClick = { onDelete(cost) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Zweck löschen", tint = UnpaidTint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSettingsCard(
    settings: DashboardSettings,
    onSave: (DashboardSettings) -> Unit,
) {
    var balance by remember(settings) { mutableStateOf(settings.balance.toInt().toString()) }
    var minimumBuffer by remember(settings) { mutableStateOf(settings.minimumBuffer.toInt().toString()) }
    val parsedBalance = balance.trim().toIntOrNull()?.toDouble() ?: 0.0
    val parsedBuffer = minimumBuffer.trim().toIntOrNull()?.toDouble() ?: 0.0
    val valid = parsedBuffer >= 0

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Dashboard-Eingaben", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(
                "Diese Werte werden für Tagesbudget und Pufferberechnung verwendet.",
                fontSize = 12.sp,
                color = Muted,
            )
            AmountField("Aktueller Kontostand in Euro", balance, keyboardType = KeyboardType.Number) { balance = it }
            AmountField("Mindestpuffer (Tabellenblatt B25)", minimumBuffer, keyboardType = KeyboardType.Number) { minimumBuffer = it }
            Text("Zahltags-Offset: 0 Tage · Tagesziel: ${money(DAILY_TARGET)}", fontSize = 11.sp, color = Muted)
            Button(
                onClick = {
                    if (valid) {
                        onSave(DashboardSettings(parsedBalance, parsedBuffer))
                    }
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) {
                Text("Dashboard-Werte speichern")
            }
        }
    }
}

@Composable
private fun FixedCostEditorDialog(
    initial: FixedCost?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Int) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember { mutableStateOf(initial?.amount?.inputValue().orEmpty()) }
    var paymentType by remember { mutableStateOf(initial?.paymentType ?: PaymentTypes.first()) }
    var dueDay by remember { mutableStateOf(initial?.dueDay?.takeIf { it != 0 }?.toString() ?: "1") }
    val parsedAmount = amount.parseAmount()
    val parsedDueDay = dueDay.toIntOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount >= 0 &&
        parsedDueDay != null && parsedDueDay in -31..31 && parsedDueDay != 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Zweck hinzufügen" else "Zweck bearbeiten", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Zweck") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField("Betrag in Euro", amount) { amount = it }
                DropdownField("Zahlungsart", paymentType, PaymentTypes) { paymentType = it }
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it },
                    label = { Text("Datum (Tag im Monat)") },
                    supportingText = { Text("1–31; negative Werte zählen vom Monatsende rückwärts.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (valid) onSave(
                        paymentType,
                        name.trim(),
                        requireNotNull(parsedAmount),
                        requireNotNull(parsedDueDay),
                    )
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen", color = Muted) } },
        containerColor = Color.White,
    )
}

@Composable
private fun IrregularPaymentEditorDialog(
    initial: IrregularPayment?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit,
) {
    val defaultMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMANY))
    var month by remember { mutableStateOf(initial?.month ?: defaultMonth) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember { mutableStateOf(initial?.amount?.inputValue().orEmpty()) }
    var paymentType by remember { mutableStateOf(initial?.paymentType ?: PaymentTypes.first()) }
    
    val parsedAmount = amount.parseAmount()
    val valid = name.isNotBlank() && month.isNotBlank() && parsedAmount != null && parsedAmount >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Unregelmäßige Zahlung hinzufügen" else "Unregelmäßige Zahlung bearbeiten", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownField("Monat", month, MonthOptions) { month = it }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Verwendungszweck") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField("Betrag in Euro", amount) { amount = it }
                DropdownField("Zahlungsart", paymentType, PaymentTypes) { paymentType = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (valid) onSave(
                        month.trim(),
                        paymentType,
                        name.trim(),
                        requireNotNull(parsedAmount),
                    )
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen", color = Muted) } },
        containerColor = Color.White,
    )
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DropdownField(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = Muted)
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(containerColor = Paper, contentColor = Ink),
            ) {
                Text(selected, modifier = Modifier.weight(1f))
                Text("⌄", color = Muted)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun getMonthKey(viewingDate: LocalDate): String {
    return viewingDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMANY)).lowercase()
}

private fun loadPaidFixedCostsMap(context: Context): Map<String, Set<Long>> {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("paidFixedCostsPerMonth", null)
        ?: return emptyMap()
    val map = mutableMapOf<String, Set<Long>>()
    val jsonObj = JSONObject(stored)
    jsonObj.keys().forEach { monthKey ->
        val jsonArray = jsonObj.getJSONArray(monthKey)
        val set = mutableSetOf<Long>()
        for (i in 0 until jsonArray.length()) {
            set.add(jsonArray.getLong(i))
        }
        map[monthKey] = set
    }
    return map
}

private fun savePaidFixedCostsMap(context: Context, map: Map<String, Set<Long>>) {
    val jsonObj = JSONObject()
    map.forEach { (monthKey, set) ->
        val jsonArray = JSONArray()
        set.forEach { jsonArray.put(it) }
        jsonObj.put(monthKey, jsonArray)
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE)
        .edit()
        .putString("paidFixedCostsPerMonth", jsonObj.toString())
        .apply()
}

private fun loadPaidUnknownPaymentsMap(context: Context): Map<String, Set<Long>> {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("paidUnknownPaymentsPerMonth", null)
        ?: return emptyMap()
    val map = mutableMapOf<String, Set<Long>>()
    val jsonObj = JSONObject(stored)
    jsonObj.keys().forEach { monthKey ->
        val jsonArray = jsonObj.getJSONArray(monthKey)
        val set = mutableSetOf<Long>()
        for (i in 0 until jsonArray.length()) {
            set.add(jsonArray.getLong(i))
        }
        map[monthKey] = set
    }
    return map
}

private fun savePaidUnknownPaymentsMap(context: Context, map: Map<String, Set<Long>>) {
    val jsonObj = JSONObject()
    map.forEach { (monthKey, set) ->
        val jsonArray = JSONArray()
        set.forEach { jsonArray.put(it) }
        jsonObj.put(monthKey, jsonArray)
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE)
        .edit()
        .putString("paidUnknownPaymentsPerMonth", jsonObj.toString())
        .apply()
}

private fun toggleFixedCostPaid(context: Context, costId: Long, viewingDate: LocalDate, isPaid: Boolean): Map<String, Set<Long>> {
    val map = loadPaidFixedCostsMap(context).toMutableMap()
    val monthKey = getMonthKey(viewingDate)
    val set = map[monthKey]?.toMutableSet() ?: mutableSetOf()
    if (isPaid) set.add(costId) else set.remove(costId)
    map[monthKey] = set
    savePaidFixedCostsMap(context, map)
    return map
}

private fun resetFixedCostsPaid(context: Context, viewingDate: LocalDate): Map<String, Set<Long>> {
    val map = loadPaidFixedCostsMap(context).toMutableMap()
    val monthKey = getMonthKey(viewingDate)
    map[monthKey] = emptySet()
    savePaidFixedCostsMap(context, map)
    return map
}

private fun toggleUnknownPaymentPaid(context: Context, paymentId: Long, viewingDate: LocalDate, isPaid: Boolean): Map<String, Set<Long>> {
    val map = loadPaidUnknownPaymentsMap(context).toMutableMap()
    val monthKey = getMonthKey(viewingDate)
    val set = map[monthKey]?.toMutableSet() ?: mutableSetOf()
    if (isPaid) set.add(paymentId) else set.remove(paymentId)
    map[monthKey] = set
    savePaidUnknownPaymentsMap(context, map)
    return map
}

private fun loadFixedCosts(context: Context): List<FixedCost> {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("fixedCosts", null)
        ?: return DefaultFixedCosts
    val json = JSONArray(stored)
    return List(json.length()) { index ->
        val item = json.getJSONObject(index)
        FixedCost(
            id = item.getLong("id"),
            paymentType = item.getString("paymentType"),
            name = item.getString("name"),
            amount = item.getDouble("amount"),
            isPaid = item.getBoolean("isPaid"),
            dueDay = item.getInt("dueDay"),
        )
    }
}

private fun loadIrregularPayments(context: Context): List<IrregularPayment> {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("irregularPayments", null)
        ?: return emptyList()
    val json = JSONArray(stored)
    return List(json.length()) { index ->
        val item = json.getJSONObject(index)
        IrregularPayment(
            id = item.getLong("id"),
            month = item.optString("month", "unbekannt"),
            paymentType = item.getString("paymentType"),
            name = item.getString("name"),
            amount = item.getDouble("amount"),
            isPaid = item.optBoolean("isPaid", false),
        )
    }
}

private fun loadDashboardSettings(context: Context): DashboardSettings {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE)
        .getString("dashboardSettings", null)
        ?: return DashboardSettings()
    val json = JSONObject(stored)
    return DashboardSettings(
        balance = json.getDouble("balance"),
        minimumBuffer = json.getDouble("minimumBuffer"),
    )
}

private fun saveDashboardSettings(context: Context, settings: DashboardSettings) {
    val json = JSONObject().apply {
        put("balance", settings.balance)
        put("minimumBuffer", settings.minimumBuffer)
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE)
        .edit()
        .putString("dashboardSettings", json.toString())
        .apply()
}

private fun saveFixedCosts(context: Context, fixedCosts: List<FixedCost>) {
    val json = JSONArray()
    fixedCosts.forEach { cost ->
        json.put(
            JSONObject().apply {
                put("id", cost.id)
                put("paymentType", cost.paymentType)
                put("name", cost.name)
                put("amount", cost.amount)
                put("isPaid", cost.isPaid)
                put("dueDay", cost.dueDay)
            },
        )
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE)
        .edit()
        .putString("fixedCosts", json.toString())
        .apply()
}

private fun saveIrregularPayments(context: Context, payments: List<IrregularPayment>) {
    val json = JSONArray()
    payments.forEach { payment ->
        json.put(
            JSONObject().apply {
                put("id", payment.id)
                put("month", payment.month)
                put("paymentType", payment.paymentType)
                put("name", payment.name)
                put("amount", payment.amount)
                put("isPaid", payment.isPaid)
            },
        )
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE)
        .edit()
        .putString("irregularPayments", json.toString())
        .apply()
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)

private fun String.parseAmount(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private fun Double.inputValue(): String = toString()
