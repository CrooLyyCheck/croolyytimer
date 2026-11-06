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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    val links = LocalUriHandler.current
    val context = LocalContext.current
    fun notificationSettings(promoted: Boolean = false) {
        val action = if (promoted && android.os.Build.VERSION.SDK_INT >= 36) android.provider.Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS
            else android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
        runCatching { context.startActivity(android.content.Intent(action).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)) }
            .onFailure { vm.error("Nie można otworzyć ustawień powiadomień.") }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.runAction { vm.app.notifications.refresh() } else notificationSettings()
    }
    fun openLink(path: String) {
        runCatching { links.openUri("https://github.com/CrooLyyCheck/croolyytimer/blob/main/$path") }
            .onFailure { vm.error("Nie można otworzyć przeglądarki.") }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        SectionTitle("Po Twojemu", "Małe ustawienia. Dobry rytm czytania.")
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.LockOpen, null, tint = MaterialTheme.colorScheme.primary)
                Text("Odblokowanie zwykłego stopera", style = MaterialTheme.typography.titleLarge)
            }
            Text(when { access.permanent -> "Stoper odblokowany na stałe"
                access.unlocked -> "Dostęp wygasa: ${dateTime(access.expiresAt)}"
                else -> "Zwykły stoper jest zablokowany. Stopery książek są zawsze dostępne." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(password, { if (it.length <= 1024) password = it }, Modifier.fillMaxWidth(), label = { Text("Hasło odblokowujące") },
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
            Button({ val chars = password.toCharArray(); password = ""; vm.unlock(chars) }, enabled = password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Odblokuj stoper") }
            if (access.cooldownUntil > access.trustedNow) Text("Przerwa po nieudanych próbach: ${(access.cooldownUntil - access.trustedNow + 999) / 1000} s.", style = MaterialTheme.typography.bodySmall)
        }
        SoftCard {
            Text("Twój sposób czytania", style = MaterialTheme.typography.titleLarge)
            SettingSwitch("Wymagaj numeru strony przed rozpoczęciem stopera", checked = settings.askBefore) { enabled -> vm.updateSettings { it.copy(askBefore = enabled) } }
            HorizontalDivider()
            SettingSwitch("Wymagaj numeru strony po zakończeniu stopera", checked = settings.askAfter) { enabled -> vm.updateSettings { it.copy(askAfter = enabled) } }
            Text("Oba pytania działają niezależnie. Strony możesz później poprawić w historii książki.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SoftCard {
            Text("Światło i nastrój", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "light" to "Jasny", "dark" to "Ciemny").forEach { (key, label) ->
                    FilterChip(settings.theme == key, { vm.updateSettings { it.copy(theme = key) } }, label = { Text(label) })
                }
            }
            SettingSwitch("Nie wygaszaj ekranu podczas sesji", "Dotyczy tylko widocznej aplikacji i aktywnego pomiaru.", settings.keepScreenOn) { enabled -> vm.updateSettings { it.copy(keepScreenOn = enabled) } }
        }
        SoftCard {
            Text("Stoper poza aplikacją", style = MaterialTheme.typography.titleLarge)
            Text("Czas i przyciski w powiadomieniu, także przy zablokowanym ekranie.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton({
                if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                    permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else notificationSettings()
            }, Modifier.fillMaxWidth()) { Text("Ustawienia powiadomień") }
            if (android.os.Build.VERSION.SDK_INT >= 36) TextButton({ notificationSettings(true) }) { Text("Widoczność na pasku systemowym") }
        }
        SoftCard {
            Text("Kopie zapasowe", style = MaterialTheme.typography.titleLarge)
            Text("Twoje książki, okładki i historia w jednym ZIP-ie.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingSwitch("Zaszyfruj kopię zapasową hasłem", "AES-256 · hasło podasz przy tworzeniu każdej kopii.", settings.encryptBackup) { enabled -> vm.updateSettings { it.copy(encryptBackup = enabled) } }
            if (!settings.encryptBackup) Text("Każda osoba mająca niezaszyfrowany plik ZIP może odczytać zawarte w nim dane.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onExport, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Utwórz kopię zapasową") }
            OutlinedButton(onImport, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Przywróć kopię zapasową") }
            Text(if (lastBackup > 0) "Ostatnia kopia: ${dateTime(lastBackup)}" else "Nie utworzono jeszcze kopii zapasowej.", style = MaterialTheme.typography.bodySmall)
        }
        SoftCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Outlined.CloudQueue, null); Text("Google Drive", style = MaterialTheme.typography.titleLarge) }
            Text("Zachowaj kopię biblioteki na swoim koncie Google.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        SoftCard(Modifier.fillMaxWidth().testTag("about-card")) {
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
        text = { Text("Twoja biblioteka i pomiary działają offline, bez reklam i analityki. Książki, okładki i historia są przechowywane w telefonie. Gdy szukasz okładki, wpisany tytuł trafia do Open Library. Wybrany obraz zapisujemy w aplikacji. Konto Google jest potrzebne wyłącznie do kopii na Google Drive. Kopia ZIP zawiera bibliotekę i ustawienia, ale nie odblokowanie stopera. Usunięcie aplikacji usuwa lokalne dane — zachowaj kopię zapasową.") },
        confirmButton = { TextButton({ privacy = false }) { Text("Rozumiem") } })
    if (licenses) AlertDialog(onDismissRequest = { licenses = false }, title = { Text("Licencja i biblioteki") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CrooLyyTimer © 2026 CrooLyyCheck")
            TextButton({ openLink("LICENSE") }) { Text("Licencja aplikacji"); Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp)) }
            TextButton({ openLink("THIRD_PARTY_NOTICES.md") }) { Text("Biblioteki i ich licencje"); Spacer(Modifier.width(8.dp)); Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp)) }
        }
    },
        confirmButton = { TextButton({ licenses = false }) { Text("Zamknij") } })
}
