package pl.croolyy.timer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import pl.croolyy.timer.data.*
import java.io.File

@Composable fun CoverSearchDialog(initialTitle: String, client: OpenLibraryClient, onDismiss: () -> Unit, onChoose: (File) -> Unit) {
    var query by remember { mutableStateOf(initialTitle) }
    var results by remember { mutableStateOf<List<CoverCandidate>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf<Long?>(null) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val links = LocalUriHandler.current
    fun search() {
        if (searching || choosing != null) return
        scope.launch {
            searching = true; error = null; results = emptyList()
            try { results = client.search(query); searched = true }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                error = if (e is java.io.IOException) "Nie udało się wyszukać okładek. Sprawdź internet i spróbuj ponownie." else e.message
            } finally { searching = false }
        }
    }
    Dialog(onDismissRequest = { if (choosing == null) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 680.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Znajdź okładkę", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    IconButton(onDismiss, enabled = choosing == null) { Icon(Icons.Outlined.Close, "Zamknij wyszukiwanie") }
                }
                Text("Wyszukaj tytuł i wybierz pasujące wydanie.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(query, { query = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Tytuł do wyszukania") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { search() }), enabled = !searching && choosing == null)
                Button(::search, Modifier.fillMaxWidth(), enabled = query.trim().length >= 2 && !searching && choosing == null) {
                    Icon(Icons.Outlined.Search, null); Spacer(Modifier.width(8.dp)); Text("Szukaj okładek")
                }
                if (searching || choosing != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (choosing != null) Text("Pobieranie wybranej okładki…", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (results.isEmpty() && !searching) Text(if (searched) "Nie znaleziono okładek. Spróbuj krótszego lub oryginalnego tytułu." else "Propozycje pojawią się po wyszukaniu.",
                    Modifier.padding(vertical = 18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (results.isNotEmpty()) LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f, fill = false).heightIn(min = 180.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(results, key = { it.coverId }) { candidate ->
                        var file by remember(candidate.coverId) { mutableStateOf<File?>(null) }
                        var imageError by remember(candidate.coverId) { mutableStateOf(false) }
                        LaunchedEffect(candidate.coverId) {
                            try { file = client.image(candidate) }
                            catch (e: Exception) { if (e is CancellationException) throw e; imageError = true }
                        }
                        Surface(onClick = {
                            choosing = candidate.coverId; error = null
                            scope.launch {
                                try { onChoose(client.image(candidate, large = true)) }
                                catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    error = if (e is java.io.IOException) "Nie udało się pobrać okładki. Sprawdź internet i wybierz inną propozycję lub spróbuj ponownie."
                                        else e.message ?: "Nie udało się pobrać okładki. Spróbuj ponownie."
                                } finally { choosing = null }
                            }
                        }, modifier = Modifier.testTag("cover-candidate-${candidate.coverId}"), enabled = choosing == null, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Cover(Book(id = candidate.coverId.toString(), title = candidate.title, author = candidate.author), file,
                                    Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                                Text(candidate.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(candidate.author.ifBlank { "Autor niepodany" }, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                candidate.year?.let { Text(it.toString(), style = MaterialTheme.typography.labelSmall) }
                                if (imageError) Text("Podgląd niedostępny", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                TextButton({ runCatching { links.openUri("https://openlibrary.org") }.onFailure { error = "Nie można otworzyć przeglądarki." } }) {
                    Text("Okładki i dane: Open Library"); Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.OpenInNew, null, Modifier.size(14.dp))
                }
            }
        }
    }
}
