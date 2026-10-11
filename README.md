# Budget2

Eine native Budget-App fuer Android mit Jetpack Compose.

## Starten

Das Projekt in Android Studio oeffnen und die Gradle-Synchronisierung abwarten. Es benoetigt Android SDK 34. Anschliessend die App auf einem Emulator oder Android-Geraet starten.

Die Buchungen werden lokal auf dem Geraet gespeichert.

## APK herunterladen

GitHub Actions erstellt nach jedem Push auf `main` eine signierte Release-APK. Oeffne den Workflow **Build Android APK** unter **Actions**, waehle den neuesten erfolgreichen Lauf und lade unter **Artifacts** das Artefakt `app-release` herunter. Entpacke die ZIP-Datei und installiere `app-release.apk`. Du kannst den Build auch manuell mit **Run workflow** starten.

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

Der private Schluessel gehoert nicht ins Repository. Ohne die Secrets bricht der APK-Workflow mit einer klaren Fehlermeldung ab. Die lokal in Android Studio erstellten Debug-APKs haben eine andere Signatur und eignen sich nicht als Updates dieser Release-APK.

**Wichtig beim Wechsel:** Die bisherige Debug-APK wurde mit einem nicht dauerhaft gespeicherten Schluessel signiert. Um erstmals die Release-APK zu installieren, muss die alte App einmalig deinstalliert werden; dabei koennen lokal gespeicherte Buchungen verloren gehen. Danach funktionieren Updates mit den ueber GitHub Actions erstellten Release-APKs.