package com.budget2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DashboardStatusFilter {
    ALL,
    PAID,
    UNPAID,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = BudgetRepository(this)
        repository.initialize()
        setContent {
            BudgetTheme {
                FixedCostsApp(repository)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FixedCostsApp(repository: BudgetRepository) {
    var fixedCosts by remember { mutableStateOf(repository.loadFixedCosts()) }
    var irregularPayments by remember { mutableStateOf(repository.loadIrregularPayments()) }
    var dashboardSettings by remember { mutableStateOf(repository.loadDashboardSettings()) }
    var paidFixedCostsMap by remember { mutableStateOf(repository.loadPaidFixedCostsMap()) }
    var paidUnknownPaymentsMap by remember { mutableStateOf(repository.loadPaidUnknownPaymentsMap()) }
    var paidSalaryMap by remember { mutableStateOf(repository.loadPaidSalaryMap()) }

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var viewingMonthOffset by rememberSaveable { mutableStateOf(0) } // 0 = current, 1 = next
    val tabHistory = remember { mutableStateListOf<Int>() }

    var showEditor by remember { mutableStateOf(false) }
    var editingCost by remember { mutableStateOf<FixedCost?>(null) }
    var deletingCost by remember { mutableStateOf<FixedCost?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    var showIrregularEditor by remember { mutableStateOf(false) }
    var editingIrregular by remember { mutableStateOf<IrregularPayment?>(null) }
    var deletingIrregular by remember { mutableStateOf<IrregularPayment?>(null) }
    var confirmIrregularReset by remember { mutableStateOf(false) }

    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingCsvImport by remember { mutableStateOf<BudgetBackup?>(null) }
    val context = LocalContext.current

    fun reloadAllData() {
        fixedCosts = repository.loadFixedCosts()
        irregularPayments = repository.loadIrregularPayments()
        dashboardSettings = repository.loadDashboardSettings()
        paidFixedCostsMap = repository.loadPaidFixedCostsMap()
        paidUnknownPaymentsMap = repository.loadPaidUnknownPaymentsMap()
        paidSalaryMap = repository.loadPaidSalaryMap()
    }

    val csvExporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri == null) {
            backupMessage = "Export abgebrochen."
        } else {
            val payload = encodeBudgetCsv(repository.exportBackup())
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    // UTF-8 BOM keeps German/French accents correct when the CSV is opened in Excel.
                    stream.write(("\uFEFF" + payload).toByteArray(Charsets.UTF_8))
                } ?: error("Zieldatei nicht verfügbar")
            }
            backupMessage = result.fold(
                onSuccess = { "CSV gespeichert. Die Datei liegt am gewählten Ort, z. B. in Google Drive." },
                onFailure = { "Export fehlgeschlagen: ${it.message}" },
            )
        }
    }

    val csvImporter = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            backupMessage = "Import abgebrochen."
        } else {
            val payload = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Quelldatei nicht verfügbar")
            }
            backupMessage = payload.fold(
                onSuccess = { text ->
                    val backup = decodeBudgetCsv(text)
                    if (backup == null) {
                        "Keine gültige Sicherungsdatei erkannt."
                    } else {
                        pendingCsvImport = backup
                        null
                    }
                },
                onFailure = { "Import fehlgeschlagen: ${it.message}" },
            )
        }
    }

    val viewingDate = LocalDate.now().plusMonths(viewingMonthOffset.toLong())
    val viewingMonthName = viewingDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))
    val viewingMonthKey = monthKey(viewingDate)
    val salaryReceived = paidSalaryMap[viewingMonthKey] == true

    fun selectTab(index: Int) {
        if (index == selectedTab) return
        if (index == 0) {
            tabHistory.clear()
        } else {
            tabHistory.add(selectedTab)
        }
        selectedTab = index
    }

    val hasOpenDialog = showEditor || showIrregularEditor || deletingCost != null ||
        deletingIrregular != null || confirmReset || confirmIrregularReset ||
        pendingCsvImport != null
    BackHandler(enabled = !hasOpenDialog && (tabHistory.isNotEmpty() || viewingMonthOffset != 0)) {
        when {
            tabHistory.isNotEmpty() -> selectedTab = tabHistory.removeAt(tabHistory.lastIndex)
            viewingMonthOffset != 0 -> viewingMonthOffset = 0
        }
    }

    fun updateCosts(updated: List<FixedCost>) {
        fixedCosts = updated
        repository.saveFixedCosts(updated)
    }

    fun updateIrregular(updated: List<IrregularPayment>) {
        irregularPayments = updated
        repository.saveIrregularPayments(updated)
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
                Row(Modifier.fillMaxWidth().background(Color.White)) {
                    listOf("Dashboard", "Fixkosten", "Unregel-\nmässig", "Settings").forEachIndexed { index, title ->
                        val selected = selectedTab == index
                        Column(
                            Modifier
                                .weight(1f)
                                .selectable(
                                    selected = selected,
                                    onClick = { selectTab(index) },
                                    role = Role.Tab,
                                ),
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    title,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    color = if (selected) Forest else Muted,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (selected) Forest else Color.Transparent),
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Paper))
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> DashboardTab(
                fixedCosts = fixedCosts,
                irregularPayments = irregularPayments,
                settings = dashboardSettings,
                viewingDate = viewingDate,
                paidFixedCostsMap = paidFixedCostsMap,
                paidUnknownPaymentsMap = paidUnknownPaymentsMap,
                salaryReceived = salaryReceived,
                onToggleSalary = { received ->
                    paidSalaryMap = repository.toggleSalaryReceived(viewingDate, received)
                },
                modifier = Modifier.padding(padding),
            )
            1 -> FixedCostsTab(
                fixedCosts = fixedCosts,
                irregularPayments = irregularPayments,
                viewingDate = viewingDate,
                paidFixedCostsMap = paidFixedCostsMap,
                paidUnknownPaymentsMap = paidUnknownPaymentsMap,
                modifier = Modifier.padding(padding),
                onToggleFixedCost = { cost, isPaid ->
                    paidFixedCostsMap = repository.toggleFixedCostPaid(cost.id, viewingDate, isPaid)
                },
                onToggleIrregular = { payment, isPaid ->
                    val cleanMonth = payment.month.trim().lowercase()
                    if (cleanMonth == "unbekannt") {
                        paidUnknownPaymentsMap =
                            repository.toggleUnknownPaymentPaid(payment.id, viewingDate, isPaid)
                    } else {
                        updateIrregular(
                            updateIrregularPaymentStatus(irregularPayments, payment.id, isPaid),
                        )
                    }
                },
                onResetStatuses = { confirmReset = true },
                onGoToSettings = { selectTab(3) },
            )
            2 -> IrregularPaymentsTab(
                irregularPayments = irregularPayments,
                viewingDate = viewingDate,
                paidUnknownPaymentsMap = paidUnknownPaymentsMap,
                modifier = Modifier.padding(padding),
                onToggleStatus = { payment, isPaid ->
                    val cleanMonth = payment.month.trim().lowercase()
                    if (cleanMonth == "unbekannt") {
                        paidUnknownPaymentsMap =
                            repository.toggleUnknownPaymentPaid(payment.id, viewingDate, isPaid)
                    } else {
                        updateIrregular(
                            updateIrregularPaymentStatus(irregularPayments, payment.id, isPaid),
                        )
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
            3 -> SettingsTab(
                fixedCosts = fixedCosts,
                dashboardSettings = dashboardSettings,
                backupMessage = backupMessage,
                modifier = Modifier.padding(padding),
                onSaveDashboardSettings = { updated ->
                    dashboardSettings = updated
                    repository.saveDashboardSettings(updated)
                },
                onExportCsv = {
                    backupMessage = null
                    val stamp = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
                    csvExporter.launch("budget2-backup-$stamp.csv")
                },
                onImportCsv = {
                    backupMessage = null
                    csvImporter.launch(
                        arrayOf(
                            "text/csv",
                            "text/comma-separated-values",
                            "text/plain",
                            "*/*",
                        ),
                    )
                },
                onGoToDashboard = { selectTab(0) },
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
                        id = existing?.id ?: nextId(
                            fixedCosts.map { it.id },
                            paidFixedCostsMap.values.flatten(),
                        ),
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
                        id = existing?.id ?: nextId(
                            irregularPayments.map { it.id },
                            paidUnknownPaymentsMap.values.flatten(),
                        ),
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
                        paidFixedCostsMap = repository.resetFixedCostsPaid(viewingDate)
                        val unknownMap = paidUnknownPaymentsMap.toMutableMap()
                        unknownMap[monthKey(viewingDate)] = emptySet()
                        paidUnknownPaymentsMap = unknownMap
                        repository.savePaidUnknownPaymentsMap(unknownMap)
                        updateIrregular(
                            resetIrregularPaymentsForMonth(irregularPayments, viewingDate),
                        )
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
                        val unknownMap = paidUnknownPaymentsMap.toMutableMap()
                        unknownMap[monthKey(viewingDate)] = emptySet()
                        paidUnknownPaymentsMap = unknownMap
                        repository.savePaidUnknownPaymentsMap(unknownMap)
                        updateIrregular(
                            resetIrregularPaymentsForMonth(irregularPayments, viewingDate),
                        )
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

    pendingCsvImport?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingCsvImport = null },
            title = { Text("CSV-Import bestätigen?") },
            text = {
                Text(
                    "Alle vorhandenen Daten werden ersetzt durch: " +
                        "${backup.fixedCosts.size} Fixkosten, " +
                        "${backup.irregularPayments.size} unregelmäßige Zahlungen sowie " +
                        "Einstellungen und Bezahlstatus.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.importBackup(backup)
                        reloadAllData()
                        backupMessage = "CSV importiert: ${backup.fixedCosts.size} Fixkosten, " +
                            "${backup.irregularPayments.size} unregelmäßige Zahlungen."
                        pendingCsvImport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) { Text("Importieren") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCsvImport = null }) { Text("Abbrechen", color = Muted) }
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
    salaryReceived: Boolean,
    onToggleSalary: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewingMonthKey = monthKey(viewingDate)
    val paidFixedIds = paidFixedCostsMap[viewingMonthKey] ?: emptySet()
    val paidUnknownIds = paidUnknownPaymentsMap[viewingMonthKey] ?: emptySet()

    var statusFilter by remember { mutableStateOf(DashboardStatusFilter.ALL) }
    var selectedPaymentTypes by remember { mutableStateOf(emptySet<String>()) }

    val allPaymentTypes = PaymentTypes
    val activeIrregular = irregularPaymentsForMonth(irregularPayments, viewingDate)
    val filteredFixed = fixedCosts.filter { cost ->
        val matchesType = selectedPaymentTypes.isEmpty() || cost.paymentType in selectedPaymentTypes
        val isPaid = cost.id in paidFixedIds
        val matchesStatus = when (statusFilter) {
            DashboardStatusFilter.ALL -> true
            DashboardStatusFilter.PAID -> isPaid
            DashboardStatusFilter.UNPAID -> !isPaid
        }
        matchesType && matchesStatus
    }
    val filteredIrregular = activeIrregular.filter { payment ->
        val matchesType = selectedPaymentTypes.isEmpty() || payment.paymentType in selectedPaymentTypes
        val clean = payment.month.trim().lowercase()
        val isPaid = if (clean == "unbekannt") payment.id in paidUnknownIds else payment.isPaid
        val matchesStatus = when (statusFilter) {
            DashboardStatusFilter.ALL -> true
            DashboardStatusFilter.PAID -> isPaid
            DashboardStatusFilter.UNPAID -> !isPaid
        }
        matchesType && matchesStatus
    }

    val totals = paymentTypeTotals(
        fixedCosts = filteredFixed,
        irregularPayments = filteredIrregular,
        viewingDate = viewingDate,
        paidFixedCostIds = paidFixedIds,
        paidUnknownPaymentIds = paidUnknownIds,
    )
    val summary = calculateDashboardSummary(
        fixedCosts = filteredFixed,
        paidFixedCostIds = paidFixedIds,
        irregularPayments = filteredIrregular,
        paidUnknownPaymentIds = paidUnknownIds,
        settings = settings,
        today = LocalDate.now(),
        viewingDate = viewingDate,
    )
    val total = summary.totalCosts
    val paid = summary.paidFixedCosts
    val unpaid = (total - paid).coerceAtLeast(0.0)
    // Exact amount that can move to savings while keeping the daily target and buffer intact.
    val savingsTransfer = (summary.bufferToday - settings.minimumBuffer).coerceAtLeast(0.0)
    val canTransferToSavings = summary.dailyBudgetToday > DAILY_TARGET && savingsTransfer > 0.0

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
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Filter", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Nach Status und Zahlungart filtern", fontSize = 12.sp, color = Muted)
                    }
                    Text("Status", fontSize = 12.sp, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = statusFilter == DashboardStatusFilter.ALL,
                            onClick = { statusFilter = DashboardStatusFilter.ALL },
                            label = { Text("Alle") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Forest,
                                selectedLabelColor = Color.White,
                            ),
                        )
                        FilterChip(
                            selected = statusFilter == DashboardStatusFilter.PAID,
                            onClick = { statusFilter = DashboardStatusFilter.PAID },
                            label = { Text("Bezahlt") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PaidTint,
                                selectedLabelColor = Color.White,
                            ),
                        )
                        FilterChip(
                            selected = statusFilter == DashboardStatusFilter.UNPAID,
                            onClick = { statusFilter = DashboardStatusFilter.UNPAID },
                            label = { Text("Unbezahlt") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UnpaidTint,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                    Text("Zahlungsart", fontSize = 12.sp, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allPaymentTypes.forEach { type ->
                            val selected = type in selectedPaymentTypes
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedPaymentTypes = if (selected) {
                                        selectedPaymentTypes - type
                                    } else {
                                        selectedPaymentTypes + type
                                    }
                                },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Forest,
                                    selectedLabelColor = Color.White,
                                ),
                            )
                        }
                    }
                    if (statusFilter != DashboardStatusFilter.ALL || selectedPaymentTypes.isNotEmpty()) {
                        TextButton(onClick = {
                            statusFilter = DashboardStatusFilter.ALL
                            selectedPaymentTypes = emptySet()
                        }) {
                            Text("Filter zurücksetzen")
                        }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Gehaltseingang", fontSize = 15.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text("Gehalt für ${viewingDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.GERMANY))} erhalten?", fontSize = 12.sp, color = Muted)
                        }
                        Switch(
                            checked = salaryReceived,
                            onCheckedChange = onToggleSalary,
                        )
                    }
                }
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
                    if (canTransferToSavings) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaidBackground)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text("Auf Sparkonto überweisen", fontSize = 12.sp, color = Muted)
                            Text(
                                money(savingsTransfer),
                                fontSize = 22.sp,
                                color = PaidTint,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Exakter Betrag, damit ${money(DAILY_TARGET)} Tagesbudget und der " +
                                    "Mindestpuffer von ${money(settings.minimumBuffer)} erhalten bleiben.",
                                fontSize = 11.sp,
                                color = Muted,
                            )
                        }
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Kontostand & Puffer", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    DashboardMetric("Kontostand", money(settings.balance))
                    DashboardMetric("Gehalt erhalten", if (salaryReceived) "Ja" else "Nein", if (salaryReceived) PaidTint else UnpaidTint)
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
                            PaymentTypeBar(item)
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
                    PaymentStatusBar(paid, unpaid)
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
                    Text("Fixkosten im Dashboard", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sichtbare Zwecke nach aktueller Filtereinstellung",
                        fontSize = 12.sp,
                        color = Muted,
                    )
                    if (filteredFixed.isEmpty()) {
                        Text(
                            "Keine Zwecke für die aktuelle Filterauswahl.",
                            fontSize = 13.sp,
                            color = Muted,
                        )
                    } else {
                        filteredFixed.sortedBy { it.name }.forEach { cost ->
                            val isPaid = cost.id in paidFixedIds
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    Modifier.weight(0.7f),
                                ) {
                                    Text(
                                        cost.name,
                                        fontSize = 14.sp,
                                        color = Ink,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Column(
                                    Modifier.weight(0.3f),
                                ) {
                                    Text(
                                        if (isPaid) "bezahlt" else "unbezahlt",
                                        fontSize = 11.sp,
                                        color = if (isPaid) PaidTint else UnpaidTint,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Column(
                                    Modifier.weight(0.5f),
                                ) {
                                    Text(
                                        money(cost.amount),
                                        fontSize = 12.sp,
                                        color = Ink,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bezahlt: ${String.format("%.0f", summary.paidFixedCosts)} €", fontSize = 12.sp, color = PaidTint)
                        Text("Unbezahlt: ${String.format("%.0f", summary.remainingFixedCosts)} €", fontSize = 12.sp, color = UnpaidTint)
                    }
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
private fun PaymentTypeBar(total: PaymentTypeTotal) {
    val (paidPercent, unpaidPercent) = paymentStatusPercentages(total.paid, total.unpaid)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(total.paymentType, modifier = Modifier.weight(1f), fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium)
            Text("Gesamt: ${money(total.total)}", fontSize = 13.sp, color = Ink, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(99.dp)).background(Paper),
        ) {
            if (total.paid > 0) {
                Box(
                    Modifier.weight(total.paid.toFloat()).height(32.dp).background(PaidTint),
                    contentAlignment = Alignment.Center,
                ) {
                    if (paidPercent >= 15) {
                        Text("$paidPercent%", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
            if (total.unpaid > 0) {
                Box(
                    Modifier.weight(total.unpaid.toFloat()).height(32.dp).background(UnpaidTint),
                    contentAlignment = Alignment.Center,
                ) {
                    if (unpaidPercent >= 15) {
                        Text("$unpaidPercent%", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Bezahlt: $paidPercent% (${money(total.paid)})", fontSize = 11.sp, color = PaidTint, fontWeight = FontWeight.SemiBold)
            Text("Offen: $unpaidPercent% (${money(total.unpaid)})", fontSize = 11.sp, color = UnpaidTint, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PaymentStatusBar(paid: Double, unpaid: Double) {
    val (paidPercent, unpaidPercent) = paymentStatusPercentages(paid, unpaid)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Bezahlt", fontSize = 11.sp, color = Muted)
                Text(
                    "$paidPercent% · ${money(paid)}",
                    fontSize = 14.sp,
                    color = PaidTint,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("Unbezahlt", fontSize = 11.sp, color = Muted)
                Text(
                    "$unpaidPercent% · ${money(unpaid)}",
                    fontSize = 14.sp,
                    color = UnpaidTint,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(99.dp)).background(Paper),
        ) {
            if (paid > 0.0) {
                Box(
                    Modifier.weight(paid.toFloat()).height(30.dp).background(PaidTint),
                    contentAlignment = Alignment.Center,
                ) {
                    if (paidPercent >= 15) {
                        Text("$paidPercent%", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (unpaid > 0.0) {
                Box(
                    Modifier.weight(unpaid.toFloat()).height(30.dp).background(UnpaidTint),
                    contentAlignment = Alignment.Center,
                ) {
                    if (unpaidPercent >= 15) {
                        Text("$unpaidPercent%", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
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
    val viewingMonthKey = monthKey(viewingDate)
    val paidFixedIds = paidFixedCostsMap[viewingMonthKey] ?: emptySet()
    val paidUnknownIds = paidUnknownPaymentsMap[viewingMonthKey] ?: emptySet()

    val activeIrregular = irregularPaymentsForMonth(irregularPayments, viewingDate)
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
    val viewingMonthKey = monthKey(viewingDate)
    val paidUnknownIds = paidUnknownPaymentsMap[viewingMonthKey] ?: emptySet()

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
                    Text("$unpaidCount unbezahlt", fontSize = 13.sp, color = if (unpaidCount > 0) UnpaidTint else PaidTint)
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
            items(
                irregularPayments.sortedWith(
                    compareBy({ monthSortIndex(it.month) }, { it.name }),
                ),
                key = { it.id },
            ) { payment ->
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
    backupMessage: String?,
    modifier: Modifier = Modifier,
    onSaveDashboardSettings: (DashboardSettings) -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onGoToDashboard: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (FixedCost) -> Unit,
    onDelete: (FixedCost) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DashboardSettingsCard(
                settings = dashboardSettings,
                onSave = onSaveDashboardSettings,
                onSaved = onGoToDashboard,
            )
        }
        item {
            CsvBackupCard(
                message = backupMessage,
                onExport = onExportCsv,
                onImport = onImportCsv,
            )
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
                                "${cost.paymentType} · ${money(cost.amount)} · ${dueDayLabel(cost.dueDay)}",
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
    onSaved: () -> Unit,
) {
    var balance by remember(settings) { mutableStateOf(settings.balance.inputValue()) }
    var minimumBuffer by remember(settings) { mutableStateOf(settings.minimumBuffer.inputValue()) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val parsedBalance = balance.parseAmount()
    val parsedBuffer = minimumBuffer.parseAmount()
    val valid = parsedBalance != null && parsedBuffer != null && parsedBuffer >= 0.0

    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            delay(600)
            onSaved()
        }
    }

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
            AmountField("Aktueller Kontostand in Euro", balance) {
                balance = it
                showSavedMessage = false
            }
            AmountField("Mindestpuffer", minimumBuffer) {
                minimumBuffer = it
                showSavedMessage = false
            }
            Text("Zahltags-Offset: 0 Tage · Tagesziel: ${money(DAILY_TARGET)}", fontSize = 11.sp, color = Muted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (valid) {
                            onSave(
                                DashboardSettings(
                                    balance = requireNotNull(parsedBalance),
                                    minimumBuffer = requireNotNull(parsedBuffer),
                                ),
                            )
                            showSavedMessage = true
                        }
                    },
                    enabled = valid,
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) {
                    Text("Dashboard-Werte speichern")
                }
                if (showSavedMessage) {
                    Text("✓ Gespeichert!", fontSize = 12.sp, color = PaidTint, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CsvBackupCard(
    message: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("CSV-Sicherung", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(
                "Sichert alle Daten inklusive Einstellungen und Bezahlstatus. Beim Export und Import " +
                    "kann Google Drive als Speicherort gewählt werden.",
                fontSize = 12.sp,
                color = Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onExport,
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) {
                    Text("CSV exportieren", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onImport) {
                    Text("CSV importieren", fontSize = 12.sp)
                }
            }
            if (message != null) {
                Text(message, fontSize = 12.sp, color = Muted)
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
    val initialDueDay = initial?.dueDay?.takeIf { it != 0 } ?: 1
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var amount by remember { mutableStateOf(initial?.amount?.inputValue().orEmpty()) }
    var paymentType by remember { mutableStateOf(initial?.paymentType ?: PaymentTypes.first()) }
    var dueDay by remember { mutableStateOf(if (initialDueDay < 0) (-initialDueDay).toString() else initialDueDay.toString()) }
    var dueDayFromMonthEnd by remember { mutableStateOf(initialDueDay < 0) }
    val parsedAmount = amount.parseAmount()
    val parsedDueDay = dueDay.toIntOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount >= 0 &&
        parsedDueDay != null && parsedDueDay in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Zweck hinzufügen" else "Zweck bearbeiten", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                    onValueChange = { dueDay = it.filter(Char::isDigit).take(2) },
                    label = { Text("Tag im Monat") },
                    supportingText = { Text("1–31") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Vom Monatsende zählen", fontSize = 13.sp, color = Ink)
                        Text(
                            "Beispiel: 27 bedeutet 27 Tage vor Monatsende.",
                            fontSize = 11.sp,
                            color = Muted,
                        )
                    }
                    Switch(
                        checked = dueDayFromMonthEnd,
                        onCheckedChange = { dueDayFromMonthEnd = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (valid) {
                        val day = requireNotNull(parsedDueDay)
                        onSave(
                            paymentType,
                            name.trim(),
                            requireNotNull(parsedAmount),
                            if (dueDayFromMonthEnd) -day else day,
                        )
                    }
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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

