# Budget2

Budget2 ist eine native Android-App zur Verwaltung monatlicher Fixkosten, unregelmäßiger Zahlungen und des verfügbaren Monatsbudgets. Die Oberfläche ist mit Kotlin und Jetpack Compose umgesetzt. Die Daten werden lokal auf dem Gerät gespeichert; CSV-Sicherungen können optional über die Android-Dateiauswahl in Google Drive abgelegt oder von dort geladen werden.

## Funktionen

- **Dashboard:** Gesamtkosten, bezahlte und offene Beträge, Gehaltseingang, Tagesbudget für heute und morgen, Kontostand, Mindestpuffer, Soll-Kontostand-Prognosen sowie Anteile nach Zahlungsart und Status. Der Zahlungsstatus wird als gestapelter Bezahlt/Unbezahlt-Balken dargestellt.
- **Fixkosten:** Wiederkehrende Zahlungen mit Zahlungsart, Zweck, Betrag, Fälligkeitstag (Tag im Monat oder Tag vor Monatsende) und Bezahlstatus. Zahlungen für den angezeigten Monat werden automatisch mit aufgeführt.
- **Unregelmäßige Zahlungen:** Zahlungen mit festem Monat oder dem Status **"unbekannt"**. Sie erscheinen zusätzlich im Fixkosten-Tab und fließen in das Monatsbudget ein.
- **Settings:** Kontostand, Mindestpuffer, CSV-Export/Import und Verwaltung aller Fixkosten-Zwecke.
- **CSV-Sicherung:** Alle Daten inklusive Einstellungen und Bezahlstatus werden als CSV exportiert bzw. wieder importiert. Der Speicherort wird über die Android-Dateiauswahl gewählt, sodass auch Google Drive möglich ist. Vor dem Import wird das Überschreiben bestätigt.
- **Monatsansicht:** Umschalten zwischen aktuellem und nächstem Monat. Die App merkt sich die Tab-Historie und behält den ausgewählten Monat beim Zurücknavigieren im Blick.
- **Zurück-Navigation:** Die System-Zurücktaste navigiert erst innerhalb der Tabs und setzt anschließend beim Verlassen des "Nächster Monat"-Zustands in den aktuellen Monat zurück, bevor sie die App minimiert.
- **Status pro Monat und Jahr:** Bezahlte Fixkosten, unbekannte Zahlungen und Gehaltseingang werden getrennt für jeden Monat gespeichert (Schlüsselformat `yyyy-MM`).

## Architektur

- `com.budget2.app` enthält nur das Paket der App.
- `BudgetModels.kt`: Domänenmodelle, Berechnungen (`calculateDashboardSummary`, `paymentStatusPercentages`), Hilfsfunktionen und ID-Generierung.
- `BudgetRepository.kt`: Alle `SharedPreferences`-Zugriffe, JSON-(De)Serialisierung, Legacy-Migration, Initial-Seeding der Standard-Fixkosten und das vollständige Backup (`exportBackup`/`importBackup`).
- `BudgetCsv.kt`: CSV-Format des Komplett-Backups, Encoding/Decoding inklusive korrektem Quoting.
- `BudgetFormatting.kt`: Währungsformatierung, Betragseingabe und Labels für den Fälligkeitstag.
- `BudgetTheme.kt`: App-Farben und Theme.
- `MainActivity.kt`: Jetpack-Compose-UI, Navigation, Dialoge, CSV-Dateiauswahl und Interaktionen.
- `docs/CODE_INDEX.md`: KI-lesbares Inhaltsverzeichnis mit Datenfluss, Persistenz-Keys, CSV-Format, Invarianten und Änderungs-Checkliste. Diese Datei wird bei Code-Änderungen mit aktualisiert.

## Entwicklung

### Voraussetzungen
- Android Studio mit JDK 17 oder neuer (getestet mit JBR 21)
- Android SDK (z. B. `C:\Users\<User>\AppData\Local\Android\Sdk`)
- Für lokale Builds: `JAVA_HOME=C:\Users\<User>\.jdks\jbr-21.0.11`, `ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`

### Build
Debug-APK:
```bash
gradlew.bat :app:assembleDebug
```

Release-APK (mit lokal konfiguriertem Keystore falls gesetzt, sonst mit dem Debug-Key signiert):
```bash
gradlew.bat :app:assembleRelease
```

Unit-Tests:
```bash
gradlew.bat :app:testDebugUnitTest
```

### Testen auf dem physischen Smartphone
Auf dem Desktop liegt der Shortcut **Budget2 Handy-Test.lnk** (Icon `docs/logo.ico`). Das Skript `scripts/Test-BudgetOnPhone.ps1` führt aus:
1. Debug-APK bauen
2. Auf verbundenem Gerät installieren (`adb install -r`, bestehende App-Daten bleiben erhalten, wenn der Build mit dem gleichen Debug-Key signiert ist)
3. App starten

ADB-Gerät muss verbunden und autorisiert sein. Der Shortcut funktioniert direkt ohne manuelle Befehle.

### Releases
- Aktueller Release: [Budget2 v1.05112026_1050](https://github.com/trenits-cmd/Budget2/releases/tag/v1.05112026_1050) (`Budget2-1.4-release.apk`), App-Version 1.4. Release-Tags folgen dem Schema `v1.DDMMYYYY_HHMM`.
- Release-Builds werden lokal mit dem Debug-Key signiert, bis ein stabiler Release-Keystore konfiguriert ist. Updates funktionieren nur, wenn derselbe Signaturschlüssel verwendet wird.
- Der GitHub-Workflow `.github/workflows/build-apk.yml` baut bei jedem Push auf `main` ein Release-APK und lädt es als Artifact hoch.

## Optimierungstipps

### Performance
- **Listen optimieren:** `LazyColumn` wird bereits genutzt; bei sehr großen Listen können `key` stabiler gesetzt und `contentType` ergänzt werden.
- **Zustand reduzieren:** Aktuell wird viel Zustand direkt in `MainActivity` gehalten. Ein `ViewModel` + `StateFlow` würde Konfigurationsänderungen robuster machen und Logik testbarer halten.
- **Repository-Migration:** `BudgetRepository` trennt bereits Persistenz; bei wachsenden Daten könnten Room oder DataStore erwogen werden (aktuell reicht `SharedPreferences` für diese Appgröße).

### UX/UI
- **Barrierefreiheit:** Fehlende `contentDescription` an manchen Icons ergänzen, Switches mit Labels verknüpfen und Kontrast prüfen.
- **Eingabevalidierung:** Das neue Monatsende-Toggle für Fälligkeitstage verbessert die UX. Für Beträge könnten Live-Fehlermeldungen und eine Währungsvorschau ergänzt werden.
- **Responsives Layout:** Auf sehr kleinen oder großen Displays könnten Abstände und Schriftgrößen leicht angepasst werden.
- **Rotation/Zustand:** Tab, Monat und Dialoge überleben Rotation bereits durch `rememberSaveable`; komplexere Dialogzustände könnten bei Bedarf ebenfalls gespeichert werden.

### Code-Struktur & Qualität
- **UI-Aufteilung:** Für noch bessere Lesbarkeit könnten `DashboardTab`, `FixedCostsTab`, `IrregularPaymentsTab` und `SettingsTab` in eigene Dateien ausgelagert werden.
- **Reine Funktionen testen:** Neben Modellen wurden ID-Generierung und Monatsreset getestet. Persistenz-Logik lässt sich mit Robolectric oder `SharedPreferences`-Fakes weiter abdecken.
- **Formatierung/Encoding:** Die UI bleibt Deutsch; bei konsistenter UTF-8-Pflege bleiben Sonderzeichen stabil.
- **Robustere ID-Vergabe:** Durch Berücksichtigung reservierter IDs aus gelöschten Einträgen wird verhindert, dass neue Einträge alten Bezahlstatus erben.

### Zusätzliche Features
- **Weitere Exportformate:** Neben dem vorhandenen CSV-Backup wäre ein JSON-Export für externe Tools denkbar.
- **Wiederkehrende Budgets:** Erinnerungen für offene Zahlungen oder Widgets für Tagesbudget.
- **Mehrjährige Ansicht:** Da Zahlungen pro `yyyy-MM` gespeichert werden, ließe sich eine Jahresübersicht leicht ergänzen.
- **Kategorien/Tags:** Zusätzliche Filterung nach Kategorie oder Projekt.
