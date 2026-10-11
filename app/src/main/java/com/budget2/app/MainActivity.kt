package com.budget2.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
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
private val ForestDeep = Color(0xFF0B1220)
private val ForestSoft = Color(0xFF1D3557)
private val Leaf = Color(0xFF34D399)
private val LeafSoft = Color(0xFFB7F7D2)
private val Coral = Color(0xFFFE7F6D)
private val CoralSoft = Color(0xFFFFD5CC)
private val Sand = Color(0xFFF3F8FF)
private val Paper = Color(0xFFF4F7FB)
private val PaperWarm = Color(0xFFEAF1FF)
private val Ink = Color(0xFF102033)
private val Muted = Color(0xFF62748A)
private val Line = Color(0xFFE2EAF5)
private val AccentGlow = Color(0xFF7DD3FC)
private val Categories = listOf("Lebensmittel", "Wohnen", "Mobilität", "Freizeit", "Sonstiges")

private data class Entry(
    val id: Long,
    val title: String,
    val category: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: String,
)

private data class BudgetBackup(
    val entries: List<Entry>,
    val fixedCosts: List<FixedCost>,
    val irregularExpenses: List<IrregularExpense>,
    val settings: BudgetSettings,
    val monthOffset: Int,
)

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
                    secondary = Leaf,
                ),
            ) {
                BudgetScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetScreen(context: Context) {
    var entries by remember { mutableStateOf(loadEntries(context)) }
    var fixedCosts by remember { mutableStateOf(loadFixedCosts(context)) }
    var irregularExpenses by remember { mutableStateOf(loadIrregularExpenses(context)) }
    var settings by remember { mutableStateOf(loadBudgetSettings(context)) }
    var monthOffset by remember { mutableStateOf(loadMonthOffset(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFixedCostDialog by remember { mutableStateOf(false) }
    var showIrregularDialog by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<BudgetBackup?>(null) }
    val exportBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            try {
                val backup = BudgetBackup(entries, fixedCosts, irregularExpenses, settings, monthOffset)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(backup.toJson().toString(2))
                } ?: error("Die Sicherungsdatei konnte nicht geöffnet werden.")
                backupMessage = "Sicherung wurde erstellt. Bewahre die Datei an einem sicheren Ort auf."
            } catch (error: Exception) {
                backupMessage = "Sicherung konnte nicht erstellt werden: ${error.localizedMessage ?: "Unbekannter Fehler"}"
            }
        }
    }
    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            try {
                val contents = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Die Sicherungsdatei konnte nicht geöffnet werden.")
                pendingBackup = parseBudgetBackup(JSONObject(contents))
            } catch (error: Exception) {
                backupMessage = "Sicherung konnte nicht gelesen werden: ${error.localizedMessage ?: "Unbekannter Fehler"}"
            }
        }
    }
    val spent = entries.filterNot { it.isIncome }.sumOf { it.amount }
    val income = entries.filter { it.isIncome }.sumOf { it.amount }
    val budget = calculateBudget(settings, fixedCosts, monthOffset = monthOffset)
    val expensesProgress = if (budget.totalFixedCosts > 0) {
        (budget.paidFixedCosts / budget.totalFixedCosts).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val month = LocalDate.now().plusMonths(monthOffset.toLong())
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            Surface(color = Paper, shadowElevation = 8.dp) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Neue Buchung", fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Leaf, AccentGlow))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("BUDGET", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestSoft)
                        Text(month.replaceFirstChar { it.titlecase(Locale.GERMANY) }, fontSize = 13.sp, color = Muted)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Forest.copy(alpha = 0.06f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("●", color = Leaf, fontSize = 10.sp)
                            Spacer(Modifier.width(6.dp))
                            MonthSelector(monthOffset) { selected ->
                                monthOffset = selected
                                saveMonthOffset(context, selected)
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Forest),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(ForestSoft, ForestDeep))),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("DEIN TAGESBUDGET", fontSize = 11.sp, color = Color(0xFFB9C9BF), fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                                Text(money(budget.dailyBudget), fontSize = 30.sp, color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
                                Text("für ${budget.daysUntilPayday} Tage bis zum Gehalt", fontSize = 12.sp, color = Color(0xFFB9C9BF))
                            }
                            BudgetRing(progress = expensesProgress)
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        SectionTitle("Übersicht", "3 Sektionen")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryTile("Offene Fixkosten", budget.unpaidFixedCosts, Icons.Outlined.Schedule, Color(0xFFB85C45), Modifier.weight(1f))
                            SummaryTile("Konto − Puffer", budget.balanceAfterBuffer, Icons.Outlined.AccountBalanceWallet, Color(0xFF43856A), Modifier.weight(1f))
                            SummaryTile("Eingang", income, Icons.Outlined.ArrowDownward, Color(0xFF43856A), Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionTitle("Budget & Ziele", money(budget.totalFixedCosts))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sparen nach Fixkosten", fontSize = 13.sp, color = Muted)
                            Text(money(budget.monthlySavings), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bereits bezahlte Fixkosten", fontSize = 13.sp, color = Muted)
                            Text(money(budget.paidFixedCosts), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Soll-Kontostand heute", fontSize = 13.sp, color = Muted)
                            Text(money(budget.targetBalance), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                        TextButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Kontostand, Puffer und Gehalt einstellen")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { exportBackup.launch("budget2-sicherung.json") }) {
                                Text("Sicherung erstellen")
                            }
                            TextButton(onClick = { importBackup.launch(arrayOf("application/json", "text/*")) }) {
                                Text("Sicherung wiederherstellen")
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Fixkosten", "${fixedCosts.size} gesamt", Modifier.weight(1f))
                        TextButton(onClick = { showFixedCostDialog = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Hinzufügen", fontSize = 12.sp)
                        }
                    }
                    if (fixedCosts.isEmpty()) {
                        Text("Noch keine Fixkosten eingetragen.", fontSize = 13.sp, color = Muted)
                    } else {
                        fixedCosts.sortedWith(compareBy<FixedCost> { it.isPaid }.thenBy { it.dueDay }).forEach { cost ->
                            FixedCostRow(cost) {
                                val updated = fixedCosts.map {
                                    if (it.id == cost.id) it.copy(isPaid = !it.isPaid) else it
                                }
                                fixedCosts = updated
                                saveFixedCosts(context, updated)
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Ausgaben", "${irregularExpenses.size} gesamt", Modifier.weight(1f))
                        TextButton(onClick = { showIrregularDialog = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Hinzufügen", fontSize = 12.sp)
                        }
                    }
                    if (irregularExpenses.isEmpty()) {
                        Text("Noch keine unregelmäßigen Ausgaben eingetragen.", fontSize = 13.sp, color = Muted)
                    } else {
                        irregularExpenses.sortedBy { it.dueDate }.forEach { expense ->
                            IrregularExpenseRow(expense)
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle("Deine Budgets", "Diesen Monat")
                    BudgetLine("Lebensmittel", spentFor(entries, "Lebensmittel"), 350.0, Color(0xFFE29B60))
                    BudgetLine("Wohnen", spentFor(entries, "Wohnen"), 1100.0, Color(0xFF6287A1))
                    BudgetLine("Mobilität", spentFor(entries, "Mobilität"), 150.0, Color(0xFF9A8CB6))
                    BudgetLine("Freizeit", spentFor(entries, "Freizeit"), 200.0, Color(0xFFBE7D82))
                }
            }
            item { SectionTitle("Letzte Buchungen", "${entries.size} gesamt") }
            items(entries.sortedByDescending { it.id }.take(8), key = { it.id }) { entry ->
                EntryRow(entry)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile("Gebuchte Ausgaben", spent, Icons.Outlined.ArrowUpward, Color(0xFFB85C45), Modifier.weight(1f))
                    SummaryTile("Gebuchte Einnahmen", income, Icons.Outlined.ArrowDownward, Color(0xFF43856A), Modifier.weight(1f))
                }
            }
        }
    }

    if (showSettingsDialog) {
        BudgetSettingsDialog(
            initial = settings,
            onDismiss = { showSettingsDialog = false },
            onSave = { updated ->
                settings = updated
                saveBudgetSettings(context, updated)
                showSettingsDialog = false
            },
        )
    }
    if (showFixedCostDialog) {
        FixedCostDialog(
            onDismiss = { showFixedCostDialog = false },
            onSave = { cost ->
                val updated = listOf(cost) + fixedCosts
                fixedCosts = updated
                saveFixedCosts(context, updated)
                showFixedCostDialog = false
            },
        )
    }
    if (showIrregularDialog) {
        IrregularExpenseDialog(
            onDismiss = { showIrregularDialog = false },
            onSave = { expense ->
                val updated = listOf(expense) + irregularExpenses
                irregularExpenses = updated
                saveIrregularExpenses(context, updated)
                showIrregularDialog = false
            },
        )
    }
    if (showDialog) {
        AddEntryDialog(
            onDismiss = { showDialog = false },
            onSave = { title, amount, category, isIncome ->
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd. MMM", Locale.GERMANY))
                val updated = listOf(Entry(System.currentTimeMillis(), title, category, amount, isIncome, today)) + entries
                entries = updated
                saveEntries(context, updated)
                showDialog = false
            },
        )
    }
    pendingBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text("Sicherung wiederherstellen?") },
            text = { Text("Deine aktuellen Buchungen, Fixkosten, Ausgaben und Einstellungen werden durch die Sicherung ersetzt.") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            saveBackup(context, backup)
                            entries = backup.entries
                            fixedCosts = backup.fixedCosts
                            irregularExpenses = backup.irregularExpenses
                            settings = backup.settings
                            monthOffset = backup.monthOffset
                            pendingBackup = null
                            backupMessage = "Sicherung wurde wiederhergestellt."
                        } catch (error: Exception) {
                            pendingBackup = null
                            backupMessage = "Sicherung konnte nicht wiederhergestellt werden: ${error.localizedMessage ?: "Unbekannter Fehler"}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) { Text("Wiederherstellen") }
            },
            dismissButton = { TextButton(onClick = { pendingBackup = null }) { Text("Abbrechen", color = Muted) } },
            containerColor = Color.White,
        )
    }
    backupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { backupMessage = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { backupMessage = null }) { Text("OK") } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun BudgetRing(progress: Float) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(94.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(Color(0xFF49645A), -90f, 360f, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Leaf, -90f, safeProgress * 360f, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(safeProgress * 100).toInt()}%", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("genutzt", fontSize = 10.sp, color = Color(0xFFB9C9BF))
        }
    }
}

@Composable
private fun SummaryTile(title: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Text(title, fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(money(amount), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.weight(1f))
        Text(trailing, fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun FixedCostRow(cost: FixedCost, onTogglePaid: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(cost.name, fontSize = 14.sp, color = Ink, fontWeight = FontWeight.Medium)
                Text(
                    "${cost.paymentType} · ${dueDayLabel(cost.dueDay)}",
                    fontSize = 11.sp,
                    color = Muted,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(money(cost.amount), fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onTogglePaid) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (cost.isPaid) Color(0xFF43856A) else Color(0xFFB85C45),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (cost.isPaid) "bezahlt" else "unbezahlt",
                        fontSize = 11.sp,
                        color = if (cost.isPaid) Color(0xFF43856A) else Color(0xFFB85C45),
                    )
                }
            }
        }
    }
}

@Composable
private fun IrregularExpenseRow(expense: IrregularExpense) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(expense.name, fontSize = 14.sp, color = Ink, fontWeight = FontWeight.Medium)
                Text("${expense.account} · ${expense.dueDate} · ${expense.count}×", fontSize = 11.sp, color = Muted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(money(expense.total), fontSize = 14.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                if (expense.count > 1) Text("${money(expense.amount)} je Ausgabe", fontSize = 10.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun MonthSelector(monthOffset: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(if (monthOffset == 0) "aktueller Monat" else "nächster Monat", fontSize = 11.sp, color = Muted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0 to "aktueller Monat", 1 to "nächster Monat").forEach { (offset, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(offset)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DropdownField(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column {
            Text(label, fontSize = 12.sp, color = Muted)
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = Paper, contentColor = Ink),
            ) {
                Text(selected, modifier = Modifier.weight(1f))
                Text("⌄", color = Muted)
            }
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

@Composable
private fun BudgetSettingsDialog(
    initial: BudgetSettings,
    onDismiss: () -> Unit,
    onSave: (BudgetSettings) -> Unit,
) {
    var balance by remember { mutableStateOf(initial.balance.inputValue()) }
    var buffer by remember { mutableStateOf(initial.safetyBuffer.inputValue()) }
    var salary by remember { mutableStateOf(initial.netSalary.inputValue()) }
    var payday by remember { mutableStateOf(initial.paydayDay.toString()) }
    var fixedDaily by remember { mutableStateOf(initial.fixedDailyAmount.inputValue()) }
    val parsedBalance = balance.parseAmount()
    val parsedBuffer = buffer.parseAmount()
    val parsedSalary = salary.parseAmount()
    val parsedPayday = payday.toIntOrNull()
    val parsedFixedDaily = fixedDaily.parseAmount()
    val valid = parsedBalance != null && parsedBalance >= 0 &&
        parsedBuffer != null && parsedBuffer >= 0 &&
        parsedSalary != null && parsedSalary >= 0 &&
        parsedPayday != null && parsedPayday in 1..31 &&
        parsedFixedDaily != null && parsedFixedDaily >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Budget einstellen", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountField("Kontostand über alle Konten", balance) { balance = it }
                AmountField("Sicherheits-Puffer", buffer) { buffer = it }
                AmountField("Netto-Gehalt", salary) { salary = it }
                OutlinedTextField(
                    value = payday,
                    onValueChange = { payday = it },
                    label = { Text("Gehaltstag (1–31)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountField("Fester Tageswert für Soll-Konto", fixedDaily) { fixedDaily = it }
                Text("Tage bis zum Gehalt werden aus dem Gehaltstag berechnet.", fontSize = 11.sp, color = Muted)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BudgetSettings(
                            balance = parsedBalance!!,
                            safetyBuffer = parsedBuffer!!,
                            netSalary = parsedSalary!!,
                            paydayDay = parsedPayday!!,
                            fixedDailyAmount = parsedFixedDaily!!,
                        ),
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
private fun FixedCostDialog(onDismiss: () -> Unit, onSave: (FixedCost) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(PaymentTypes.first()) }
    var status by remember { mutableStateOf("unbezahlt") }
    var dueDay by remember { mutableStateOf("1") }
    val parsedAmount = amount.parseAmount()
    val parsedDueDay = dueDay.toIntOrNull()
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0 &&
        parsedDueDay != null && parsedDueDay in -31..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fixkosten hinzufügen", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Bezeichnung") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                AmountField("Betrag in Euro", amount) { amount = it }
                DropdownField("Zahlungsart", paymentType, PaymentTypes) { paymentType = it }
                DropdownField("Status", status, listOf("bezahlt", "unbezahlt")) { status = it }
                OutlinedTextField(
                    dueDay,
                    { dueDay = it },
                    label = { Text("Fälligkeitstag (−31 bis 31)") },
                    supportingText = { Text("Negative Werte zählen rückwärts vom Monatsende.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        FixedCost(
                            id = System.currentTimeMillis(),
                            paymentType = paymentType,
                            name = name.trim(),
                            amount = parsedAmount!!,
                            isPaid = status == "bezahlt",
                            dueDay = parsedDueDay!!,
                        ),
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
private fun IrregularExpenseDialog(onDismiss: () -> Unit, onSave: (IrregularExpense) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var account by remember { mutableStateOf(ExpenseAccounts.first()) }
    var dueDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var count by remember { mutableStateOf("1") }
    val parsedAmount = amount.parseAmount()
    val parsedCount = count.toIntOrNull()
    val validDate = runCatching { LocalDate.parse(dueDate) }.isSuccess
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0 &&
        parsedCount != null && parsedCount > 0 && validDate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unregelmäßige Ausgabe", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Bezeichnung") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                AmountField("Betrag je Ausgabe", amount) { amount = it }
                DropdownField("Konto", account, ExpenseAccounts) { account = it }
                OutlinedTextField(
                    dueDate,
                    { dueDate = it },
                    label = { Text("Fälligkeitsdatum (JJJJ-MM-TT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    count,
                    { count = it },
                    label = { Text("Anzahl") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        IrregularExpense(
                            id = System.currentTimeMillis(),
                            name = name.trim(),
                            amount = parsedAmount!!,
                            account = account,
                            dueDate = dueDate,
                            count = parsedCount!!,
                        ),
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
private fun AmountField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BudgetLine(name: String, amount: Double, limit: Double, color: Color) {
    val progress = if (limit <= 0.0) 0f else (amount / limit).toFloat().coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(money(amount), fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(" / ${money(limit)}", fontSize = 12.sp, color = Muted)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Line)) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun EntryRow(entry: Entry) {
    val iconColor = if (entry.isIncome) Color(0xFF43856A) else Color(0xFFB85C45)
    val icon = if (entry.isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(iconColor.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entry.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.category} · ${entry.date}", fontSize = 11.sp, color = Muted)
        }
        Text(
            (if (entry.isIncome) "+" else "−") + money(entry.amount),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = iconColor,
        )
    }
}

@Composable
private fun AddEntryDialog(onDismiss: () -> Unit, onSave: (String, Double, String, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Categories.first()) }
    var isIncome by remember { mutableStateOf(false) }
    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Neue Buchung", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Schliessen") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false to "Ausgabe", true to "Einnahme").forEach { (incomeOption, label) ->
                        val selected = isIncome == incomeOption
                        Button(
                            onClick = { isIncome = incomeOption },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Forest else Paper,
                                contentColor = if (selected) Color.White else Ink,
                            ),
                        ) { Text(label, fontSize = 12.sp) }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Beschreibung") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Betrag in Euro") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Kategorie", fontSize = 12.sp, color = Muted)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Categories.chunked(3).forEach { rowCategories ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowCategories.forEach { category ->
                                val selected = selectedCategory == category
                                TextButton(
                                    onClick = { selectedCategory = category },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (selected) Leaf.copy(alpha = 0.45f) else Paper,
                                        contentColor = Ink,
                                    ),
                                ) { Text(category, fontSize = 10.sp, maxLines = 1) }
                            }
                            repeat(3 - rowCategories.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank() && parsedAmount != null && parsedAmount > 0) onSave(title.trim(), parsedAmount, selectedCategory, isIncome) },
                enabled = title.isNotBlank() && parsedAmount != null && parsedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Forest),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen", color = Muted) } },
        containerColor = Color.White,
    )
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)

private fun spentFor(entries: List<Entry>, category: String): Double =
    entries.filter { !it.isIncome && it.category == category }.sumOf { it.amount }

private fun loadEntries(context: Context): List<Entry> {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("entries", null)
        ?: return sampleEntries()
    val json = JSONArray(stored)
    return List(json.length()) { index ->
        val item = json.getJSONObject(index)
        Entry(
            item.getLong("id"), item.getString("title"), item.getString("category"),
            item.getDouble("amount"), item.getBoolean("income"), item.getString("date"),
        )
    }
}

private fun saveEntries(context: Context, entries: List<Entry>) {
    val json = JSONArray()
    entries.forEach { entry ->
        json.put(JSONObject().apply {
            put("id", entry.id)
            put("title", entry.title)
            put("category", entry.category)
            put("amount", entry.amount)
            put("income", entry.isIncome)
            put("date", entry.date)
        })
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE).edit().putString("entries", json.toString()).apply()
}

private fun loadBudgetSettings(context: Context): BudgetSettings {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("settings", null)
        ?: return BudgetSettings()
    val json = JSONObject(stored)
    return BudgetSettings(
        balance = json.getDouble("balance"),
        safetyBuffer = json.getDouble("safetyBuffer"),
        netSalary = json.getDouble("netSalary"),
        paydayDay = json.getInt("paydayDay"),
        fixedDailyAmount = json.getDouble("fixedDailyAmount"),
    )
}

private fun loadMonthOffset(context: Context): Int =
    context.getSharedPreferences("budget", Context.MODE_PRIVATE).getInt("monthOffset", 0).coerceIn(0, 1)

private fun saveMonthOffset(context: Context, monthOffset: Int) {
    context.getSharedPreferences("budget", Context.MODE_PRIVATE).edit().putInt("monthOffset", monthOffset).apply()
}

private fun saveBudgetSettings(context: Context, settings: BudgetSettings) {
    val json = JSONObject().apply {
        put("balance", settings.balance)
        put("safetyBuffer", settings.safetyBuffer)
        put("netSalary", settings.netSalary)
        put("paydayDay", settings.paydayDay)
        put("fixedDailyAmount", settings.fixedDailyAmount)
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE).edit().putString("settings", json.toString()).apply()
}

private fun loadFixedCosts(context: Context): List<FixedCost> {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("fixedCosts", null)
        ?: return emptyList()
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

private fun saveFixedCosts(context: Context, fixedCosts: List<FixedCost>) {
    val json = JSONArray()
    fixedCosts.forEach { cost ->
        json.put(JSONObject().apply {
            put("id", cost.id)
            put("paymentType", cost.paymentType)
            put("name", cost.name)
            put("amount", cost.amount)
            put("isPaid", cost.isPaid)
            put("dueDay", cost.dueDay)
        })
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE).edit().putString("fixedCosts", json.toString()).apply()
}

private fun loadIrregularExpenses(context: Context): List<IrregularExpense> {
    val stored = context.getSharedPreferences("budget", Context.MODE_PRIVATE).getString("irregularExpenses", null)
        ?: return emptyList()
    val json = JSONArray(stored)
    return List(json.length()) { index ->
        val item = json.getJSONObject(index)
        IrregularExpense(
            id = item.getLong("id"),
            name = item.getString("name"),
            amount = item.getDouble("amount"),
            account = item.getString("account"),
            dueDate = item.getString("dueDate"),
            count = item.getInt("count"),
        )
    }
}

private fun saveIrregularExpenses(context: Context, expenses: List<IrregularExpense>) {
    val json = JSONArray()
    expenses.forEach { expense ->
        json.put(JSONObject().apply {
            put("id", expense.id)
            put("name", expense.name)
            put("amount", expense.amount)
            put("account", expense.account)
            put("dueDate", expense.dueDate)
            put("count", expense.count)
        })
    }
    context.getSharedPreferences("budget", Context.MODE_PRIVATE)
        .edit().putString("irregularExpenses", json.toString()).apply()
}

private fun BudgetBackup.toJson(): JSONObject = JSONObject().apply {
    put("formatVersion", 1)
    put("entries", JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("category", entry.category)
                put("amount", entry.amount)
                put("income", entry.isIncome)
                put("date", entry.date)
            })
        }
    })
    put("fixedCosts", JSONArray().apply {
        fixedCosts.forEach { cost ->
            put(JSONObject().apply {
                put("id", cost.id)
                put("paymentType", cost.paymentType)
                put("name", cost.name)
                put("amount", cost.amount)
                put("isPaid", cost.isPaid)
                put("dueDay", cost.dueDay)
            })
        }
    })
    put("irregularExpenses", JSONArray().apply {
        irregularExpenses.forEach { expense ->
            put(JSONObject().apply {
                put("id", expense.id)
                put("name", expense.name)
                put("amount", expense.amount)
                put("account", expense.account)
                put("dueDate", expense.dueDate)
                put("count", expense.count)
            })
        }
    })
    put("settings", JSONObject().apply {
        put("balance", settings.balance)
        put("safetyBuffer", settings.safetyBuffer)
        put("netSalary", settings.netSalary)
        put("paydayDay", settings.paydayDay)
        put("fixedDailyAmount", settings.fixedDailyAmount)
    })
    put("monthOffset", monthOffset)
}

private fun saveBackup(context: Context, backup: BudgetBackup) {
    val json = backup.toJson()
    val saved = context.getSharedPreferences("budget", Context.MODE_PRIVATE).edit()
        .putString("entries", json.getJSONArray("entries").toString())
        .putString("fixedCosts", json.getJSONArray("fixedCosts").toString())
        .putString("irregularExpenses", json.getJSONArray("irregularExpenses").toString())
        .putString("settings", json.getJSONObject("settings").toString())
        .putInt("monthOffset", backup.monthOffset)
        .commit()
    check(saved) { "Die Daten konnten nicht dauerhaft gespeichert werden." }
}

private fun parseBudgetBackup(json: JSONObject): BudgetBackup {
    require(json.getInt("formatVersion") == 1) { "Unbekanntes Sicherungsformat." }
    val entriesJson = json.getJSONArray("entries")
    val entries = List(entriesJson.length()) { index ->
        val item = entriesJson.getJSONObject(index)
        Entry(
            id = item.getLong("id"),
            title = item.getString("title").also { require(it.isNotBlank()) },
            category = item.getString("category").also { require(it in Categories) },
            amount = item.getDouble("amount").also { require(it.isFinite() && it > 0) },
            isIncome = item.getBoolean("income"),
            date = item.getString("date"),
        )
    }
    val fixedCostsJson = json.getJSONArray("fixedCosts")
    val fixedCosts = List(fixedCostsJson.length()) { index ->
        val item = fixedCostsJson.getJSONObject(index)
        FixedCost(
            id = item.getLong("id"),
            paymentType = item.getString("paymentType").also { require(it in PaymentTypes) },
            name = item.getString("name").also { require(it.isNotBlank()) },
            amount = item.getDouble("amount").also { require(it.isFinite() && it > 0) },
            isPaid = item.getBoolean("isPaid"),
            dueDay = item.getInt("dueDay").also { require(it in -31..31) },
        )
    }
    val irregularExpensesJson = json.getJSONArray("irregularExpenses")
    val irregularExpenses = List(irregularExpensesJson.length()) { index ->
        val item = irregularExpensesJson.getJSONObject(index)
        IrregularExpense(
            id = item.getLong("id"),
            name = item.getString("name").also { require(it.isNotBlank()) },
            amount = item.getDouble("amount").also { require(it.isFinite() && it > 0) },
            account = item.getString("account").also { require(it in ExpenseAccounts) },
            dueDate = item.getString("dueDate").also { LocalDate.parse(it) },
            count = item.getInt("count").also { require(it > 0) },
        )
    }
    val settingsJson = json.getJSONObject("settings")
    val settings = BudgetSettings(
        balance = settingsJson.getDouble("balance").also { require(it.isFinite() && it >= 0) },
        safetyBuffer = settingsJson.getDouble("safetyBuffer").also { require(it.isFinite() && it >= 0) },
        netSalary = settingsJson.getDouble("netSalary").also { require(it.isFinite() && it >= 0) },
        paydayDay = settingsJson.getInt("paydayDay").also { require(it in 1..31) },
        fixedDailyAmount = settingsJson.getDouble("fixedDailyAmount").also { require(it.isFinite() && it >= 0) },
    )
    val monthOffset = json.getInt("monthOffset").also { require(it in 0..1) }
    return BudgetBackup(entries, fixedCosts, irregularExpenses, settings, monthOffset)
}

private fun String.parseAmount(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private fun Double.inputValue(): String = toString()

private fun dueDayLabel(day: Int): String = when {
    day < 0 -> "Monatsende − ${-day} Tage"
    day == 0 -> "Kein Fälligkeitstag"
    else -> "Tag $day"
}

private fun sampleEntries(): List<Entry> {
    val today = LocalDate.now()
    fun date(daysAgo: Long) = today.minusDays(daysAgo).format(DateTimeFormatter.ofPattern("dd. MMM", Locale.GERMANY))
    return listOf(
        Entry(6, "Wocheneinkauf", "Lebensmittel", 68.40, false, date(0)),
        Entry(5, "Monatsmiete", "Wohnen", 920.00, false, date(1)),
        Entry(4, "Deutschlandticket", "Mobilität", 49.00, false, date(2)),
        Entry(3, "Wochenmarkt", "Lebensmittel", 31.20, false, date(3)),
        Entry(2, "Musik-Abo", "Freizeit", 10.99, false, date(4)),
        Entry(1, "Gehalt", "Sonstiges", 2650.00, true, date(5)),
    )
}