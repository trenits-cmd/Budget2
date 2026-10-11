# Budget2

Eine native Android-App zur Verwaltung monatlicher Fixkosten mit Jetpack Compose.
Im Tab **Fixkosten** werden Zahlungsart, Zweck, Betrag, Bezahlstatus und Datum
angezeigt. Unter **Settings** lassen sich Zwecke anlegen, bearbeiten und loeschen.
Unbezahlte Eintraege sind rot, bezahlte gruen markiert; der Status kann einzeln
oder fuer alle Eintraege zurueckgesetzt werden.

## Starten

Das Projekt in Android Studio oeffnen und die Gradle-Synchronisierung abwarten. Zum Bauen benoetigt es Android SDK 34. Die App laeuft ab Android 6.0 (API 23); anschliessend kannst du sie auf einem passenden Emulator oder Android-Geraet starten.

Die Fixkosten und Bezahlstatus werden lokal auf dem Geraet gespeichert.

## APK herunterladen

GitHub Actions erstellt nach jedem Push auf `main` eine Release-APK. Oeffne den Workflow **Build Android APK** unter **Actions**, waehle den neuesten erfolgreichen Lauf und lade unter **Artifacts** das Artefakt `app-release` herunter. Entpacke die ZIP-Datei und installiere `app-release.apk`. Du kannst den Build auch manuell mit **Run workflow** starten.

Ohne konfigurierte Signatur-Secrets signiert der Workflow die APK mit dem temporaeren Debug-Schluessel des Runners. Diese APK ist installierbar, aber nicht dauerhaft aktualisierbar: Ein spaeterer Workflow-Lauf kann einen anderen Schluessel verwenden. Android verweigert dann das Update; die App muss deinstalliert werden, wobei lokal gespeicherte Buchungen verloren gehen koennen. Ein lokaler `./gradlew assembleRelease`-Build ohne Release-Keystore wird ebenfalls mit dem lokalen Debug-Schluessel signiert. Fuer dauerhaft aktualisierbare APKs richte die unten beschriebenen Signatur-Secrets ein.

### Signaturschluessel einmalig einrichten

Android erlaubt Updates nur, wenn jede APK mit demselben Schluessel signiert ist. Erstelle einmalig einen privaten Schluessel auf einem sicheren Rechner und bewahre die Keystore-Datei dauerhaft und sicher auf:

```sh
keytool -genkeypair -v -keystore budget2-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias budget2
base64 budget2-release.jks | tr -d '\n'
```

Fuege in GitHub unter **Settings > Secrets and variables > Actions > New repository secret** folgende Repository-Secrets hinzu:

- `ANDROID_KEYSTORE_BASE64`: die komplette Ausgabe des `base64`-Befehls
- `ANDROID_KEYSTORE_PASSWORD`: das beim Erstellen des Keystores gesetzte Keystore-Passwort
- `ANDROID_KEY_ALIAS`: `budget2` (oder der beim Erstellen gewaehlte Alias)
- `ANDROID_KEY_PASSWORD`: das beim Erstellen gesetzte Schluessel-Passwort

Der private Schluessel gehoert nicht ins Repository. Sind keine dieser Secrets gesetzt, baut der Workflow mit dem temporaeren Debug-Schluessel weiter. Sind nur einige Secrets gesetzt, bricht er mit einer klaren Fehlermeldung ab. Die lokal in Android Studio erstellten Debug-APKs haben eine andere Signatur und eignen sich nicht als Updates der mit diesem Release-Schluessel signierten APK.

**Wichtig beim Wechsel:** Wenn Android eine installierte Version wegen einer abweichenden Signatur nicht aktualisieren kann, muss die alte App vor der Installation deinstalliert werden; dabei koennen lokal gespeicherte Fixkosten verloren gehen. Mit dauerhaft eingerichteten Signatur-Secrets funktionieren Updates mit den ueber GitHub Actions erstellten Release-APKs.
