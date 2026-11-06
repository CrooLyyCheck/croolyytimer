package pl.croolyy.timer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pl.croolyy.timer.BuildConfig
import pl.croolyy.timer.update.*

@Composable fun UpdateCard(state: UpdateState, sessionActive: Boolean, installing: Boolean,
    onCheck: () -> Unit, onDownload: () -> Unit, onInstall: () -> Unit, onCancel: () -> Unit,
    onAutomatic: (Boolean) -> Unit, onPreview: (Boolean) -> Unit) {
    val links = LocalUriHandler.current
    SoftCard(Modifier.fillMaxWidth().testTag("update-card")) {
        Text("Aktualizacje", style = MaterialTheme.typography.titleLarge)
        Text("Zainstalowana wersja: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingSwitch("Sprawdzaj nowe wersje przy uruchomieniu", checked = state.automatic, change = onAutomatic)
        if (!state.busy) SettingSwitch("Uwzględniaj wersje testowe", checked = state.preview, change = onPreview)
        when (state.phase) {
            UpdatePhase.CHECKING -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Sprawdzanie dostępności…") }
            UpdatePhase.DOWNLOADING -> {
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text("Pobieranie aktualizacji · ${(state.progress * 100).toInt()}%")
                TextButton(onCancel) { Text("Anuluj pobieranie") }
            }
            UpdatePhase.VERIFYING -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Sprawdzanie pobranego pliku…") }
            UpdatePhase.IDLE -> Unit
        }
        state.release?.let { release ->
            HorizontalDivider()
            Text("Nowa wersja ${release.versionName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (release.notes.isNotBlank()) Text(release.notes, style = MaterialTheme.typography.bodySmall)
            if (state.ready) {
                Text("Aktualizacja jest pobrana i gotowa.", style = MaterialTheme.typography.bodySmall)
                Button(onInstall, Modifier.fillMaxWidth(), enabled = !state.busy && !sessionActive && !installing) {
                    Icon(Icons.Outlined.SystemUpdate, null); Spacer(Modifier.width(8.dp)); Text(if (installing) "Otwieranie instalatora…" else "Zainstaluj aktualizację")
                }
                if (sessionActive) Text("Zakończ aktywną sesję, aby zainstalować aktualizację.", style = MaterialTheme.typography.bodySmall)
            } else Button(onDownload, Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Pobierz aktualizację") }
            TextButton({ runCatching { links.openUri(release.releaseUrl) } }) { Text("Informacje o wydaniu") }
        } ?: if (state.checkedAt > 0 && !state.busy) {
            Text("Brak nowszej wersji w wybranym kanale.", style = MaterialTheme.typography.bodySmall)
        } else Unit
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        OutlinedButton(onCheck, Modifier.fillMaxWidth(), enabled = !state.busy && !installing) { Text("Sprawdź aktualizacje") }
        if (state.checkedAt > 0) Text("Ostatnie sprawdzenie: ${dateTime(state.checkedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
