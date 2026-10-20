# Budget2

Eine native Android-App zur Verwaltung monatlicher Fixkosten und unregelmäßiger Zahlungen mit Jetpack Compose.

## Features

- **Fixkosten (Tab 0):** Verwaltung wiederkehrender monatlicher Zahlungen mit Zahlungsart, Zweck, Betrag, Bezahlstatus und Fälligkeitsdatum. Aktuelle Monats-Zahlungen aus den unregelmäßigen Zahlungen werden automatisch hier mit einbezogen.
- **Unregelmäßige Zahlungen (Tab 1):** Verwaltung von Zahlungen mit frei wählbarem Monat oder dem Status **„unbekannt“**. Zahlungen für den aktuellen Monat fließen automatisch in das Monatsbudget und den Fixkosten-Tab ein; Zahlungen mit Monat „unbekannt“ werden direkt im Dashboard angezeigt.
- **Dashboard (Tab 2):** Kostenübersicht, Planung bis zum Zahltag (Stichtag, verbleibende Tage, Tagesbudget heute/morgen), Kontostand, Mindestpuffer, Soll-Kontostand-Prognosen sowie grafische Anteile nach Zahlungsarten und Status.
- **Settings (Tab 3):** Pflege von Kontostand, Mindestpuffer und Verwalten aller Fixkosten-Zwecke. Ganzzahl-Eingaben mit Standardwert 0.
- **App-Icon:** Eigenes individuelles App-Logo für den Homescreen und App-Drawer.

## Starten & APK bauen

Das Projekt in Android Studio öffnen und die Gradle-Synchronisierung abwarten. Zum Bauen benötigt es Android SDK 34. Die App läuft ab Android 6.0 (API 23).

### APK lokal generieren

Du kannst die Debug-APK direkt mit Gradle erstellen:

```sh
./gradlew assembleDebug
```

Die fertige APK befindet sich unter:
`app/build/outputs/apk/debug/app-debug.apk`

### Release-APK über GitHub Actions

GitHub Actions erstellt nach jedem Push auf `main` eine Release-APK. Öffne den Workflow **Build Android APK** unter **Actions**, wähle den neuesten erfolgreichen Lauf und lade unter **Artifacts** das Artefakt `app-release` herunter.
