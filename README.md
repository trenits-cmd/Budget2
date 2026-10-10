# Budget2

Eine native Budget-App fuer Android mit Jetpack Compose.

## Starten

Das Projekt in Android Studio oeffnen und die Gradle-Synchronisierung abwarten. Es benoetigt Android SDK 35. Anschliessend die App auf einem Emulator oder Android-Geraet starten.

Die Buchungen werden lokal auf dem Geraet gespeichert.

## APK herunterladen

Nach jedem Push auf `main` erstellt GitHub Actions eine Debug-APK. Den Workflow findest du im Repository unter **Actions**. Oeffne den neuesten erfolgreichen Lauf und lade unter **Artifacts** das Artefakt `Budget2-debug-apk` herunter. Die APK liegt darin unter `app-debug.apk`.

Du kannst den Build auch manuell unter **Actions** mit **Build Android APK** und **Run workflow** starten. Das Artefakt wird entsprechend der Aufbewahrungsrichtlinie des Repositorys gespeichert.