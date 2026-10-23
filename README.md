<p align="center">
  <img src="docs/logo.png" width="112" alt="Budget App Logo" />
</p>

# Budget2

Budget2 ist eine native Android-App zur Verwaltung monatlicher Fixkosten, unregelmäßiger Zahlungen und des verfügbaren Monatsbudgets. Die Oberfläche ist mit Kotlin und Jetpack Compose umgesetzt; alle Daten bleiben lokal auf dem Gerät.

## Funktionen

- **Dashboard:** Gesamtkosten, bezahlte und offene Beträge, Gehaltseingang, Tagesbudget für heute und morgen, Kontostand, Mindestpuffer, Soll-Kontostand-Prognosen sowie Anteile nach Zahlungsart und Status.
- **Fixkosten:** Wiederkehrende Zahlungen mit Zahlungsart, Zweck, Betrag, Fälligkeitstag und Bezahlstatus. Zahlungen für den angezeigten Monat werden automatisch mit aufgeführt.
- **Unregelmäßige Zahlungen:** Zahlungen mit festem Monat oder dem Status **„unbekannt“**. Sie erscheinen zusätzlich im Fixkosten-Tab und fließen in das Monatsbudget ein.
- **Settings:** Kontostand, Mindestpuffer, Anlegen, Bearbeiten und Löschen aller Fixkosten-Zwecke.
- **Monatsansicht:** Umschalten zwischen aktuellem und nächstem Monat.
- **Status pro Monat und Jahr:** Bezahlte Fixkosten, unbekannte Zahlungen und Gehaltseingang werden getrennt für jeden Monat gespeichert (Schlüsselformat `yyyy-MM`). Ältere, monatsnamenbasierte Einträge werden beim Start automatisch übernommen.
- **Zurücksetzen:** Alle Zahlungen des angezeigten Monats mit einem Tap wieder auf „unbezahlt“ setzen.
- **Offline & lokal:** Keine Internet-Berechtigung, kein Konto, keine Cloud-Anbindung. Die Speicherung erfolgt in `SharedPreferences`.
- **Adaptives App-Icon:** Eigenes Logo passend zur Farbwelt des Dashboards, inklusive runder und monochromer Variante.

## Voraussetzungen

| Werkzeug | Version |
| --- | --- |
| Android Studio | mit Unterstützung für AGP 8.2 |
| JDK | 17 |
| Android SDK | Platform 34 (`compileSdk`/`targetSdk`) |
| Gradle | 8.9 (über den Wrapper) |
| Kotlin | 1.9.22 |
| Compose BOM | 2024.02.00 |
| Mindest-Android | 6.0 (API 23) |

## Bauen

Die Gradle-Synchronisierung in Android Studio ausführen oder direkt über die Konsole bauen.

### Debug-APK

```sh
./gradlew assembleDebug
```

Unter Windows:

```powershell
.\gradlew.bat assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

### Tests und Lint

```sh
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Die Unit-Tests prüfen unter anderem die Dashboard-Berechnungen, die Monatszuordnung und die Migration alter Monatsschlüssel.

### Release-APK

```sh
./gradlew assembleRelease
```

Ausgabe: `app/build/outputs/apk/release/app-release.apk`.

Für eine eigene Signatur müssen alle vier Umgebungsvariablen gesetzt sein:

| Variable | Inhalt |
| --- | --- |
| `ANDROID_KEYSTORE_PATH` | Pfad zur `.jks`-Datei |
| `ANDROID_KEYSTORE_PASSWORD` | Passwort des Keystores |
| `ANDROID_KEY_ALIAS` | Alias des Schlüssels |
| `ANDROID_KEY_PASSWORD` | Passwort des Schlüssels |

Fehlt die Konfiguration vollständig, wird die Release-APK mit dem Debug-Schlüssel signiert. Das ist nur zum lokalen Testen geeignet; ein späterer Wechsel des Signaturschlüssels verhindert Updates über eine bereits installierte APK. Sind nur einzelne Variablen gesetzt, bricht der Build mit einer klaren Fehlermeldung ab.

### GitHub Actions

Der Workflow **Build Android APK** (`.github/workflows/build-apk.yml`) baut bei jedem Push auf `main` und über **Run workflow** eine Release-APK. Die vier Signaturwerte können als Repository-Secrets hinterlegt werden (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`). Ohne Secrets wird die APK mit dem temporären Debug-Schlüssel des Runners signiert. Das Artefakt heißt `app-release`.

## Projektstruktur

```text
Budget2/
├─ app/src/main/java/com/budget2/app/
│  ├─ MainActivity.kt      # Compose-Oberfläche, Dialoge, Persistenz
│  └─ BudgetModels.kt      # Datenmodelle, Monatslogik, Berechnungen
├─ app/src/main/res/
│  ├─ drawable/            # Logo und adaptive Icon-Ebenen
│  ├─ mipmap-*/            # Legacy-Launcher-Icons
│  └─ values/              # App-Name, Farben, Theme
├─ app/src/test/           # JVM-Unit-Tests
├─ docs/logo.png           # Logo-Vorschau für diese README
└─ .github/workflows/      # Release-APK über GitHub Actions
```

## Datenspeicherung

Alle Daten liegen im privaten `SharedPreferences`-File `budget`:

| Schlüssel | Inhalt |
| --- | --- |
| `fixedCosts` | Liste der Fixkosten als JSON |
| `irregularPayments` | Liste der unregelmäßigen Zahlungen als JSON |
| `dashboardSettings` | Kontostand und Mindestpuffer |
| `paidFixedCostsPerMonth` | Bezahlte Fixkosten pro Monat (`yyyy-MM`) |
| `paidUnknownPaymentsPerMonth` | Bezahlte „unbekannte“ Zahlungen pro Monat |
| `paidSalaryMap` | Gehaltseingang pro Monat |

Beim ersten Start werden 17 Fixkosten aus `DefaultFixedCosts` angelegt. Diese Einträge dienen als Startbestand und können in den Settings bearbeitet oder gelöscht werden. Die App nutzt `allowBackup`; abhängig von den Geräteeinstellungen kann Android die Daten daher in ein Cloud-Backup aufnehmen.

## Berechnungen im Dashboard

- **Stichtag:** 27. Februar im Februar, sonst der dritte Tag vor Monatsende (z. B. 28. Oktober).
- **Verbleibende Tage:** Differenz zwischen heutigem Datum und Stichtag.
- **Tagesbudget:** `(Kontostand − offene Kosten − Mindestpuffer) / verbleibende Tage`, kaufmännisch auf ganze Euro gerundet. Für morgen wird ein Tag weniger angesetzt.
- **Puffer:** `Kontostand − (60 € × verbleibende Tage + offene Kosten)`.
- **Soll-Kontostand:** `60 € × verbleibende Tage + Mindestpuffer + offene Kosten`; für morgen entsprechend ein Tag weniger.
- **Offene Kosten:** Summe aller Fixkosten und der für den Monat relevanten unregelmäßigen Zahlungen minus bereits bezahlter Beträge, gerundet auf ganze Euro.

Das Tagesziel von 60 € ist als `DAILY_TARGET` in `BudgetModels.kt` hinterlegt.

## Bekannte Einschränkungen

- Unregelmäßige Zahlungen speichern nur einen Monatsnamen, kein Jahr. Eine Zahlung für „Oktober“ erscheint daher in jedem Oktober; ihr Bezahlstatus gilt monatsübergreifend.
- Der Bezahlstatus unregelmäßiger Zahlungen mit festem Monat ist nicht jahresabhängig. Nur Fixkosten, „unbekannte“ Zahlungen und das Gehalt werden pro `yyyy-MM` gespeichert.
- „Unbekannt“ Zahlungen wirken nur im aktuell angezeigten Monat auf das Budget. Beim Blick auf den nächsten Monat werden sie nicht in die Prognose eingerechnet.
- Der Schalter **Gehaltseingang** speichert nur den Status für den Monat. Er verändert die Kontostands- und Budgetberechnungen derzeit nicht; das Gehalt muss weiterhin im Kontostand berücksichtigt werden.
- Die Monatsansicht bietet nur „aktueller Monat“ und „nächster Monat“.
- Die App ist auf Deutsch und auf den Euro (Locale `de_DE`) ausgelegt.
