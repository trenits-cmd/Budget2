package com.budget2.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private val Forest = Color(0xFF183A30)
private val Leaf = Color(0xFFA9D7A0)
private val Paper = Color(0xFFF5F4EF)
private val Ink = Color(0xFF1C2924)
private val Muted = Color(0xFF78817B)
private val Line = Color(0xFFE8E7E0)
private val Categories = listOf("Lebensmittel", "Wohnen", "Mobilität", "Freizeit", "Sonstiges")
private const val MonthlyBudget = 2400.0

private data class Entry(
    val id: Long,
    val title: String,
    val category: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: String,
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
    var showDialog by remember { mutableStateOf(false) }
    val spent = entries.filterNot { it.isIncome }.sumOf { it.amount }
    val income = entries.filter { it.isIncome }.sumOf { it.amount }
    val remaining = MonthlyBudget - spent
    val month = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY))

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            Surface(color = Paper, shadowElevation = 8.dp) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
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
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Forest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = Leaf, modifier = Modifier.size(23.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("BUDGET", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Forest)
                        Text(month.replaceFirstChar { it.titlecase(Locale.GERMANY) }, fontSize = 13.sp, color = Muted)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("●", color = Color(0xFF56A477), fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Aktuell", fontSize = 12.sp, color = Muted)
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Forest),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("NOCH VERFÜGBAR", fontSize = 11.sp, color = Color(0xFFB9C9BF), fontWeight = FontWeight.SemiBold)
                            Text(money(remaining), fontSize = 30.sp, color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
                            Text("von ${money(MonthlyBudget)} Monatsbudget", fontSize = 12.sp, color = Color(0xFFB9C9BF))
                        }
                        BudgetRing(progress = (spent / MonthlyBudget).toFloat().coerceIn(0f, 1f))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile("Ausgaben", spent, Icons.Outlined.ArrowUpward, Color(0xFFB85C45), Modifier.weight(1f))
                    SummaryTile("Einnahmen", income, Icons.Outlined.ArrowDownward, Color(0xFF43856A), Modifier.weight(1f))
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
        }
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
}

@Composable
private fun BudgetRing(progress: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(94.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(Color(0xFF49645A), -90f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Leaf, -90f, progress * 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("genutzt", fontSize = 10.sp, color = Color(0xFFB9C9BF))
        }
    }
}

@Composable
private fun SummaryTile(title: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
                Text(title, fontSize = 12.sp, color = Muted)
            }
            Text(money(amount), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.weight(1f))
        Text(trailing, fontSize = 12.sp, color = Muted)
    }
}

@Composable
private fun BudgetLine(name: String, amount: Double, limit: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text(money(amount), fontSize = 12.sp, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(" / ${money(limit)}", fontSize = 12.sp, color = Muted)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Line)) {
            Box(Modifier.fillMaxWidth((amount / limit).toFloat().coerceIn(0f, 1f)).height(7.dp).clip(CircleShape).background(color))
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
    return runCatching {
        val json = JSONArray(stored)
        List(json.length()) { index ->
            val item = json.getJSONObject(index)
            Entry(
                item.getLong("id"), item.getString("title"), item.getString("category"),
                item.getDouble("amount"), item.getBoolean("income"), item.getString("date"),
            )
        }
    }.getOrElse { sampleEntries() }
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