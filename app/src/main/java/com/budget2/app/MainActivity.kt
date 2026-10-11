package com.budget2.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
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
    var selectedTab by remember { mutableStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    var editingCost by remember { mutableStateOf<FixedCost?>(null) }
    var deletingCost by remember { mutableStateOf<FixedCost?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))

    fun updateCosts(updated: List<FixedCost>) {
        fixedCosts = updated
        saveFixedCosts(context, updated)
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            Column {
                Column(
                    Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("FIXKOSTEN", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        "${currentMonth.replaceFirstChar { it.titlecase(Locale.GERMANY) }} · wiederkehrende Zahlungen",
                        fontSize = 16.sp,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                    listOf("Fixkosten", "Settings").forEachIndexed { index, title ->
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
                modifier = Modifier.padding(padding),
                onToggleStatus = { cost, isPaid ->
                    updateCosts(updateFixedCostStatus(fixedCosts, cost.id, isPaid))
                },
                onResetStatuses = { confirmReset = true },
                onGoToSettings = { selectedTab = 1 },
            )
            else -> SettingsTab(
                fixedCosts = fixedCosts,
                modifier = Modifier.padding(padding),
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

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Alle als unbezahlt markieren?") },
            text = { Text("Der Status aller Fixkosten wird auf „unbezahlt“ gesetzt.") },
            confirmButton = {
                Button(
                    onClick = {
                        updateCosts(setAllFixedCostsStatus(fixedCosts, isPaid = false))
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
}

@Composable
private fun FixedCostsTab(
    fixedCosts: List<FixedCost>,
    modifier: Modifier = Modifier,
    onToggleStatus: (FixedCost, Boolean) -> Unit,
    onResetStatuses: () -> Unit,
    onGoToSettings: () -> Unit,
) {
    val unpaidCount = fixedCosts.count { !it.isPaid }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${fixedCosts.size} Zwecke", fontSize = 17.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("$unpaidCount unbezahlt", fontSize = 13.sp, color = if (unpaidCount > 0) UnpaidTint else PaidTint)
                }
                TextButton(onClick = onResetStatuses, enabled = fixedCosts.any { it.isPaid }) {
                    Text("Alle unbezahlt", fontSize = 12.sp)
                }
            }
        }
        item { FixedCostTableHeader() }
        if (fixedCosts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Noch keine Fixkosten vorhanden.", color = Ink, fontWeight = FontWeight.Medium)
                        Text("Lege deine Zwecke im Tab „Settings“ an.", color = Muted, fontSize = 13.sp)
                        TextButton(onClick = onGoToSettings) { Text("Zu Settings") }
                    }
                }
            }
        } else {
            items(fixedCosts.sortedWith(compareBy<FixedCost> { it.dueDay }.thenBy { it.name }), key = { it.id }) { cost ->
                FixedCostTableRow(cost, onToggleStatus)
            }
        }
    }
}

@Composable
private fun FixedCostTableHeader() {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TableCell("Zahlungsart", 72.dp)
        TableCell("Zweck", 65.dp)
        TableCell("Betrag", 65.dp)
        TableCell("Status", 76.dp)
        TableCell("Datum", 42.dp)
    }
}

@Composable
private fun FixedCostTableRow(cost: FixedCost, onToggleStatus: (FixedCost, Boolean) -> Unit) {
    val tint = if (cost.isPaid) PaidTint else UnpaidTint
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (cost.isPaid) PaidBackground else UnpaidBackground),
    ) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TableCell(cost.paymentType, 72.dp, color = Ink)
            TableCell(cost.name, 65.dp, color = Ink, bold = true)
            TableCell(money(cost.amount), 65.dp, color = Ink)
            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.Start) {
                Text(if (cost.isPaid) "bezahlt" else "unbezahlt", fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = cost.isPaid,
                    onCheckedChange = { onToggleStatus(cost, it) },
                    modifier = Modifier.height(32.dp),
                )
            }
            TableCell(formatDueDate(cost.dueDay), 42.dp, color = Ink)
        }
    }
}

@Composable
private fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, color: Color = Muted, bold: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(end = 8.dp),
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
    modifier: Modifier = Modifier,
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
                                "${cost.paymentType} · ${money(cost.amount)} · Tag ${cost.dueDay}",
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
    val valid = name.isNotBlank() && parsedAmount != null && parsedAmount > 0 &&
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

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)

private fun String.parseAmount(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private fun Double.inputValue(): String = toString()

private fun formatDueDate(dueDay: Int): String {
    val month = YearMonth.now()
    val day = when {
        dueDay < 0 -> (month.lengthOfMonth() + dueDay).coerceIn(1, month.lengthOfMonth())
        else -> dueDay.coerceIn(1, month.lengthOfMonth())
    }
    return LocalDate.of(month.year, month.month, day)
        .format(DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY))
}
