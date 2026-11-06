package pl.croolyy.timer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import pl.croolyy.timer.BuildConfig
import pl.croolyy.timer.backup.DriveBackup
import pl.croolyy.timer.data.AppSettings
import pl.croolyy.timer.security.AccessState

@Composable fun SettingSwitch(title: String, subtitle: String? = null, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        }
        Switch(checked, change, modifier = Modifier.testTag(title))
    }
}

@Composable fun SettingsScreen(settings: AppSettings, access: AccessState, vm: AppViewModel,
    lastBackup: Long, connected: Boolean, backups: List<DriveBackup>,
    onExport: () -> Unit, onImport: () -> Unit, onConnect: () -> Unit, onDisconnect: () -> Unit, onDriveUpload: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var deleteBackup by remember { mutableStateOf<DriveBackup?>(null) }
    var privacy by remember { mutableStateOf(false) }
    var licenses by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        SectionTitle("Po Twojemu", "Małe ustawienia. Dobry rytm czytania.")
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                Text("Odblokowanie zwykłego timera", style = MaterialTheme.typography.titleLarge)
            }
            Text(when { access.permanent -> "Timer odblokowany na stałe"
                access.unlocked -> "Dostęp wygasa: ${dateTime(access.expiresAt)}"
                else -> "Zwykły timer jest zablokowany. Timery książek są zawsze dostępne." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(password, { if (it.length <= 1024) password = it }, Modifier.fillMaxWidth(), label = { Text("Hasło odblokowujące") },
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
            Button({ val chars = password.toCharArray(); password = ""; vm.unlock(chars) }, enabled = password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Odblokuj timer") }
            if (access.cooldownUntil > access.trustedNow) Text("Przerwa po nieudanych próbach: ${(access.cooldownUntil - access.trustedNow + 999) / 1000} s.", style = MaterialTheme.typography.bodySmall)
        }
        SoftCard {
            Text("Twój sposób czytania", style = MaterialTheme.typography.titleLarge)
            SettingSwitch("Wymagaj numeru strony przed rozpoczęciem timera", checked = settings.askBefore) { vm.settings(settings.copy(askBefore = it)) }
            HorizontalDivider()
            SettingSwitch("Wymagaj numeru strony po zakończeniu timera", checked = settings.askAfter) { vm.settings(settings.copy(askAfter = it)) }
            Text("Oba pytania działają niezależnie. Strony możesz później poprawić w historii książki.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SoftCard {
            Text("Światło i nastrój", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "light" to "Jasny", "dark" to "Ciemny").forEach { (key, label) ->
                    FilterChip(settings.theme == key, { vm.settings(settings.copy(theme = key)) }, label = { Text(label) })
                }
            }
            SettingSwitch("Nie wygaszaj ekranu podczas sesji", "Dotyczy tylko widocznej aplikacji i aktywnego pomiaru.", settings.keepScreenOn) { vm.settings(settings.copy(keepScreenOn = it)) }
        }
        SoftCard {
            Text("Kopie zapasowe", style = MaterialTheme.typography.titleLarge)
            Text("Twoje książki, okładki i historia w jednym ZIP-ie.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingSwitch("Zaszyfruj kopię zapasową hasłem", "AES-256 · hasło podasz przy tworzeniu każdej kopii.", settings.encryptBackup) { vm.settings(settings.copy(encryptBackup = it)) }
            if (!settings.encryptBackup) Text("Każda osoba mająca niezaszyfrowany plik ZIP może odczytać zawarte w nim dane.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onExport, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Utwórz kopię zapasową") }
            OutlinedButton(onImport, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Przywróć kopię zapasową") }
            Text(if (lastBackup > 0) "Ostatnia kopia: ${dateTime(lastBackup)}" else "Nie utworzono jeszcze kopii zapasowej.", style = MaterialTheme.typography.bodySmall)
        }
        SoftCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Outlined.CloudQueue, null); Text("Google Drive", style = MaterialTheme.typography.titleLarge) }
            Text("Opcjonalna kopia w ukrytym folderze aplikacji. Wymaga konfiguracji OAuth przez właściciela aplikacji — instrukcja jest w repozytorium. Główne funkcje działają bez konta.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!connected) Button(onConnect, Modifier.fillMaxWidth()) { Text("Połącz konto Google") }
            else {
                Text("Połączono z Google Drive · tylko dane tej aplikacji", style = MaterialTheme.typography.labelLarge)
                Button(onDriveUpload, Modifier.fillMaxWidth()) { Text("Utwórz kopię w Google Drive") }
                OutlinedButton(vm::listDrive, Modifier.fillMaxWidth()) { Text("Wyświetl dostępne kopie") }
                TextButton(onDisconnect) { Text("Rozłącz konto Google") }
                if (backups.isEmpty()) Text("Brak dostępnych kopii. Utwórz pierwszą kopię na tym koncie.", style = MaterialTheme.typography.bodySmall)
                backups.forEach { backup ->
                    HorizontalDivider()
                    Text(backup.name, style = MaterialTheme.typography.labelLarge)
                    Text("${backup.createdTime} · ${(backup.size.toLongOrNull() ?: 0) / 1024} KB", style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton({ vm.downloadDrive(backup) }) { Text("Przywróć kopię z Google Drive") }
                        IconButton({ deleteBackup = backup }) { Icon(Icons.Outlined.DeleteOutline, "Usuń kopię ${backup.name}") }
                    }
                }
            }
        }
        SoftCard {
            Brand()
            Text("Wersja ${BuildConfig.VERSION_NAME} · Android 8.0+", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            TextButton({ privacy = true }) { Text("Prywatność danych") }
            TextButton({ licenses = true }) { Text("Licencja i biblioteki") }
        }
        Text("Dobrze być tu, gdzie czytasz.", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
    }
    deleteBackup?.let { backup -> ConfirmDialog("Usunąć kopię z Google Drive?", "Usuniesz trwale tylko wybraną kopię: ${backup.name}. Pozostałe kopie i dane w telefonie zostaną zachowane.", onDismiss = { deleteBackup = null }) { vm.deleteDrive(backup) } }
    if (privacy) AlertDialog(onDismissRequest = { privacy = false }, title = { Text("Twoje dane należą do Ciebie") },
        text = { Text("CrooLyyTimer działa offline, bez reklam i analityki. Dane i okładki są w prywatnej pamięci aplikacji. Internet służy wyłącznie opcjonalnym kopiom Google Drive. Token Google pozostaje w pamięci procesu. ZIP zawiera Twoją bibliotekę; lokalne odblokowanie timera nie jest eksportowane. Usunięcie aplikacji usuwa lokalne dane — zachowaj własny backup.") },
        confirmButton = { TextButton({ privacy = false }) { Text("Rozumiem") } })
    if (licenses) AlertDialog(onDismissRequest = { licenses = false }, title = { Text("Licencje") }, text = { Text("CrooLyyTimer © 2026 CrooLyyCheck. Wszelkie prawa zastrzeżone.\n\nAndroidX / Compose / Room / DataStore / Navigation: Apache 2.0\nKotlin / kotlinx: Apache 2.0\nZip4j: Apache 2.0\nBouncy Castle: MIT\nGoogle Play Services: warunki Google Play Services\n\nPełna lista i odnośniki: THIRD_PARTY_NOTICES.md w repozytorium.") },
        confirmButton = { TextButton({ licenses = false }) { Text("Zamknij") } })
}
