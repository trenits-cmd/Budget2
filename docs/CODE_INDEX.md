# Code-Index Budget2 (KI-Einstieg)

> Zweck dieser Datei: Eine KI soll den Aufbau der App in wenigen Minuten verstehen,
> ohne alle Quelldateien vollständig zu lesen. Bei jeder Code-Änderung muss diese
> Datei im selben Commit aktualisiert werden.

## 1. Projekt auf einen Blick

- **Typ:** Native Android-App, eine Activity, Jetpack Compose, Material 3.
- **Sprache:** Kotlin.
- **Paket:** `com.budget2.app`.
- **Persistenz:** ausschließlich lokal über `SharedPreferences` (Name: `budget`), JSON-serialisiert.
- **Netzwerk:** keine Internet-Berechtigung. CSV-Export/Import läuft über Android SAF
  (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`); Google Drive ist dort ein wählbarer Anbieter.
- **Monatslogik:** Statusdaten werden mit `yyyy-MM` verschlüsselt, z. B. `2026-11`.
- **UI-Sprache:** Deutsch. Beträge in Euro, deutsche Zahlenformatierung.

## 2. Dateien und Verantwortlichkeiten

| Datei | Rolle | Wichtige Symbole |
|---|---|---|
| `app/src/main/java/com/budget2/app/MainActivity.kt` | Gesamte Compose-UI, Navigation, Dialoge, CSV-Launcher, App-Zustand | `MainActivity`, `FixedCostsApp`, `DashboardTab`, `FixedCostsTab`, `IrregularPaymentsTab`, `SettingsTab`, `CsvBackupCard`, `DashboardSettingsCard` |
| `BudgetModels.kt` | Domänenmodelle, Berechnungen, Monatslogik, IDs | `FixedCost`, `IrregularPayment`, `DashboardSettings`, `DashboardSummary`, `calculateDashboardSummary`, `paymentStatusPercentages`, `monthKey`, `nextId` |
| `BudgetRepository.kt` | Alle Persistenzzugriffe, Migration, Seeding, Backup-Import/-Export | `BudgetRepository`, `initialize`, `exportBackup`, `importBackup`, `migrateLegacyMonthStorage` |
| `BudgetCsv.kt` | CSV-Serialisierung und -Parsing des vollständigen Backups | `BudgetBackup`, `CSV_HEADER`, `encodeBudgetCsv`, `decodeBudgetCsv`, `parseCsvLine`, `parseCsvRecords` |
| `BudgetFormatting.kt` | Währungs- und Eingabeformatierung | `money`, `inputValue`, `parseAmount`, `dueDayLabel` |
| `BudgetTheme.kt` | Farben und Compose-Theme | `Forest`, `Ink`, `Muted`, `PaidTint`, `UnpaidTint`, `WarningTint`, `WarningBackground`, `Paper` |
| `res/...` | Android-Ressourcen (Icon, Theme, Strings) | `ic_launcher`, `Theme.Budget2` |
| `app/src/test/.../BudgetCsvTest.kt` | Roundtrip- und Robustheitstests für CSV | `encodeThenDecode_restoresEveryValue` |
| `app/src/test/.../BudgetModelsTest.kt` | Tests für Berechnungen und Monatslogik | – |
| `docs/CODE_INDEX.md` | Diese KI-Dokumentation | – |

## 3. Datenfluss (wichtig für Änderungen)

```text
SharedPreferences ("budget")
        │  load*/save* (JSON)
        ▼
BudgetRepository ──────────────► FixedCostsApp (Compose-State)
        │                              │
        │ exportBackup()               │ UI-Aktionen (Toggle, Dialoge)
        │ importBackup()               ▼
        ▼                        Tabs / Cards / Dialoge
BudgetBackup ◄── encode/decode ── CSV-Datei (SAF, optional Google Drive)
```

Regeln:

1. **Jede Persistenz** läuft über `BudgetRepository`; UI-Code spricht nicht direkt mit `SharedPreferences`.
2. **Jede Datenänderung** aktualisiert zuerst den lokalen Compose-State und speichert danach über das Repository.
3. **CSV** enthält alle sechs Datensätze aus `BudgetBackup`, damit ein Import den kompletten Zustand ersetzt.
4. **Kein Netzwerk:** Drive-Zugriff erfolgt ausschließlich über den vom Nutzer gewählten SAF-Dokumentanbieter.

## 4. Persistenz-Schlüssel (`SharedPreferences`, Datei `budget`)

| Key | Typ | Inhalt |
|---|---|---|
| `fixedCosts` | JSON-Array | Fixkosten (`FixedCost`) |
| `irregularPayments` | JSON-Array | Unregelmäßige Zahlungen (`IrregularPayment`) |
| `dashboardSettings` | JSON-Objekt | `balance`, `minimumBuffer` |
| `paidFixedCostsPerMonth` | JSON-Objekt | Monat → Array von Fixkosten-IDs |
| `paidUnknownPaymentsPerMonth` | JSON-Objekt | Monat → Array unregelmäßiger IDs |
| `paidSalaryMap` | JSON-Objekt | Monat → Boolean (Gehalt erhalten) |

Migration: Alte Schlüssel mit deutschen Monatsnamen (z. B. `oktober`) werden beim Start
über `migrateLegacyMonthKey` zu `yyyy-MM` umgeschrieben.

## 5. UI-Struktur

- `FixedCostsApp` hält den zentralen Zustand und die Navigation.
- Tabs (Index): `0 = Dashboard`, `1 = Fixkosten`, `2 = Unregelmäßig`, `3 = Settings`.
- `selectedTab` und `viewingMonthOffset` sind `rememberSaveable`; `tabHistory` ist die Zurück-Historie.
- Settings-Reihenfolge:
  1. `DashboardSettingsCard` (Kontostand, Mindestpuffer, „Dashboard-Werte speichern“)
  2. `CsvBackupCard` (CSV exportieren/importieren, Statusmeldung)
  3. „Zwecke verwalten“ mit Fixkostenliste
- Dashboard-Reihenfolge: Filter → Gehaltseingang → Gesamtkosten/Unbezahlt → Planung bis zum Zahltag
  → Kontostand & Puffer → Nach Zahlungsart → Zahlungsstatus → **Fixkosten im Dashboard** (bewusst zuletzt).
- Überschreitet das Tagesbudget `DAILY_TARGET` (60 €), zeigt die Planungskarte den exakten Betrag
  „Auf Sparkonto überweisen“: `settings.balance - alle offenen Kosten - 60 € × verbleibende Tage
  - minimumBuffer`. Die Rechnung nutzt bewusst **alle** Fixkosten, nicht die gefilterte Liste.
  Damit bleiben Tagesbudget, restliche Zahlungen und Mindestpuffer erhalten.
- Die Tage bis zum Gehalt stehen als farbiger Badge in der Planungskarte:
  `> 10` rot (`UnpaidTint`), `8–10` gelb (`WarningTint`), `<= 7` grün (`PaidTint`).
- Import ist zweistufig: Datei lesen → `pendingCsvImport` → Bestätigungsdialog → `repository.importBackup()` → `reloadAllData()`.
- Export-Dateiname: `budget2-backup-yyyy-MM-dd_HHmm.csv`, UTF-8 mit BOM für Excel-kompatible Sonderzeichen.

## 6. CSV-Format (`BudgetCsv.kt`)

Erste Spalte ist der Datensatztyp. Kommas, Anführungszeichen und Zeilenumbrüche in
Feldern werden CSV-konform maskiert. Beim Lesen werden unbekannte/kaputte Zeilen ignoriert.
Eine Datei ohne verwertbaren Datensatz liefert `null`.

| Typ | Spalten |
|---|---|
| `Fix` | `id,paymentType,name,amount,isPaid,dueDay` |
| `Irreg` | `id,month,paymentType,name,amount,isPaid` |
| `PaidFix` | `month,id` |
| `PaidUnknown` | `month,id` |
| `Salary` | `month,true/false` |
| `Settings` | `balance,minimumBuffer` |

Header: `Typ,Feld1,Feld2,Feld3,Feld4,Feld5,Feld6`.
`dueDay` ist positiv (Tag im Monat) oder negativ (Tag vor Monatsende).

## 7. Invarianten und Fallstricke

- `dueDay < 0` bedeutet „Tag vor Monatsende“; die UI zeigt den absoluten Wert.
- Unregelmäßige Zahlung mit Monat `unbekannt` zählt nur im aktuellen Monat und wird
  über `paidUnknownPaymentsPerMonth` bezahlt markiert.
- `nextId` berücksichtigt auch IDs aus Bezahlstatus-Maps, damit neue Einträge keinen
  alten Status erben.
- Prozentwerte: bezahlter Anteil wird gerundet, unbezahlter Anteil ist `100 - paid`.
- Balken tragen innen nur Prozentwerte; Beträge stehen darunter, damit nichts abgeschnitten wird.
- Release-APK wird ohne konfigurierten Keystore mit dem Debug-Key signiert.
- Zeichensatz: UTF-8. Der CSV-Export schreibt ein BOM, der Import entfernt es. Deutsche und
  französische Sonderzeichen (`ä ö ü ß é è ê à ç ô î`) sowie Zeilenumbrüche in gequoteten
  Feldern bleiben beim Roundtrip erhalten.
- `importBackup` ersetzt alle sechs Datensätze in einem einzigen `SharedPreferences.Editor`,
  damit kein halb importierter Zustand entsteht.

## 8. Änderungs-Checkliste für KI

Bei jeder Änderung:

1. Betroffene Datei in Abschnitt 2 prüfen und bei neuen Dateien ergänzen.
2. Neuen/geänderten Persistenz-Key in Abschnitt 4 dokumentieren.
3. Neuen UI-Flow oder Dialog in Abschnitt 5 ergänzen.
4. CSV-Format-Änderungen in Abschnitt 6 und in `BudgetCsvTest` nachziehen (inkl. Sonderzeichen
   und mehrzeiliger Felder).
5. Invarianten in Abschnitt 7 aktualisieren, falls sich Regeln ändern.
6. `docs/CODE_INDEX.md` im selben Commit wie den Code ändern.
7. Danach `gradlew.bat :app:testDebugUnitTest` und `:app:assembleRelease` ausführen.

## 9. Build und Release

```powershell
$env:JAVA_HOME = "C:\Users\andi-\.jdks\jbr-21.0.11"
cd Budget2
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleRelease
```

- APK: `app/build/outputs/apk/release/app-release.apk`
- Release-Tag-Schema: `v1.DDMMYYYY_HHMM` (Beispiel `v1.02112026_0203`).
- Aktuelle App-Version steht in `app/build.gradle.kts` (`versionCode`, `versionName`).
