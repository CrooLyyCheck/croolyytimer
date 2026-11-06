package pl.croolyy.timer.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.FileProvider
import pl.croolyy.timer.data.*
import java.io.File
import java.time.*

@Composable fun BooksScreen(state: LibraryState, vm: AppViewModel, onBook: (String) -> Unit, onAdd: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val books = state.books.filter {
        (query.isBlank() || it.title.contains(query, true) || it.author.contains(query, true)) &&
            (filter == 0 || (filter == 1 && !it.finished) || (filter == 2 && it.finished))
    }
    val today = LocalDate.now()
    val todayMs = state.sessions.filter { Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate() == today }.sumOf { it.durationMs }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SectionTitle("Twoja biblioteka", "Każda strona ma swój czas.") {
            FilledIconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.Add, "Dodaj książkę") }
        } }
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric(shortTime(todayMs), "czytania dzisiaj", Modifier.weight(1f))
                    Metric(shortTime(state.sessions.sumOf { it.durationMs }), "łącznie", Modifier.weight(1f))
                    Metric(state.books.count { it.finished }.toString(), "ukończone", Modifier.weight(.8f))
                }
            }
        }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Znajdź tytuł lub autora") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = RoundedCornerShape(18.dp)) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Wszystkie", "W czytaniu", "Ukończone").forEachIndexed { index, text ->
                FilterChip(filter == index, { filter = index }, label = { Text(text) })
            }
        } }
        if (books.isEmpty()) item {
            EmptyState(Icons.Outlined.AutoStories, if (state.books.isEmpty()) "Miejsce na Twoją historię" else "Nie ma tu jeszcze książek",
                if (state.books.isEmpty()) "Dodaj pierwszą książkę. My przypilnujemy czasu, Ty zanurz się w lekturze." else "Zmień wyszukiwanie lub wybierz inny filtr.") {
                if (state.books.isEmpty()) Button(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Dodaj pierwszą książkę") }
            }
        }
        items(books, key = { it.id }) { book ->
            Surface(onClick = { onBook(book.id) }, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f)), modifier = Modifier.animateItem()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Cover(book, book.cover?.let { vm.app.covers.file(it) }, Modifier.width(90.dp).height(135.dp))
                    Column(Modifier.weight(1f).heightIn(min = 135.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (book.finished) Text("UKOŃCZONA", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.sp)
                        Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(book.author.ifBlank { "Autor niepodany" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Spacer(Modifier.weight(1f))
                        Progress(book)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Outlined.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(shortTime(state.sessions.filter { it.bookId == book.id }.sumOf { it.durationMs }), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item { Text("Jeszcze jeden rozdział?", Modifier.fillMaxWidth().padding(8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable fun BookDetailScreen(book: Book, state: LibraryState, vm: AppViewModel, onBack: () -> Unit,
    onEdit: () -> Unit, onStart: (BookTimer) -> Unit, onActive: () -> Unit) {
    var addingTimer by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<BookTimer?>(null) }
    var deleteTimer by remember { mutableStateOf<BookTimer?>(null) }
    var deleteBook by remember { mutableStateOf(false) }
    var editSession by remember { mutableStateOf<ReadingSession?>(null) }
    var deleteSession by remember { mutableStateOf<ReadingSession?>(null) }
    var timerFilter by rememberSaveable(book.id) { mutableStateOf<String?>(null) }
    val sessions = state.sessions.filter { it.bookId == book.id }
    val timers = state.timers.filter { it.bookId == book.id }
    val filtered = sessions.filter { timerFilter == null || it.timerId == timerFilter }
    val time = sessions.sumOf { it.durationMs }
    val pageSessions = sessions.filter { (it.pages ?: 0) > 0 && it.durationMs > 0 }
    val pace = if (pageSessions.isNotEmpty()) pageSessions.sumOf { it.durationMs } / 60_000.0 / pageSessions.sumOf { it.pages!! } else null
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Wróć") }
            Spacer(Modifier.weight(1f))
            IconButton(onEdit) { Icon(Icons.Outlined.Edit, "Edytuj książkę") }
            IconButton({ deleteBook = true }) { Icon(Icons.Outlined.DeleteOutline, "Usuń książkę") }
        } }
        item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Cover(book, book.cover?.let { vm.app.covers.file(it) }, Modifier.width(146.dp).height(219.dp))
            Text(book.title, style = MaterialTheme.typography.headlineLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(book.author.ifBlank { "Autor niepodany" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilterChip(book.finished, { vm.runAction { vm.repository.saveBook(book.copy(finished = !book.finished)) } },
                label = { Text(if (book.finished) "Książka ukończona" else "Oznacz jako ukończoną") },
                leadingIcon = { Icon(if (book.finished) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(18.dp)) })
        } }
        item { SoftCard {
            Progress(book)
            Row(Modifier.fillMaxWidth()) {
                Metric(shortTime(time), "czas czytania", Modifier.weight(1f)); Metric(sessions.size.toString(), "sesje", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                Metric(if (sessions.any { it.pages != null }) sessions.mapNotNull { it.pages }.sum().toString() else "—", "strony z pełnymi danymi", Modifier.weight(1f))
                Metric(if (sessions.isNotEmpty()) shortTime(time / sessions.size) else "—", "średnia sesja", Modifier.weight(1f))
            }
            Text(if (pace != null) "Średnie tempo: ${"%.1f".format(Polish, pace)} min/stronę" else "Tempo pojawi się po sesji z pełnymi danymi stron.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { SectionTitle("Twoje timery", "Osobny rytm na każdą okazję.") {
            IconButton({ addingTimer = true }) { Icon(Icons.Outlined.Add, "Dodaj timer") }
        } }
        if (timers.isEmpty()) item { Text("Dodaj pierwszy timer tej książki.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(timers, key = { it.id }) { timer ->
            val timerSessions = sessions.filter { it.timerId == timer.id }
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Cover(book, book.cover?.let { vm.app.covers.file(it) }, Modifier.width(40.dp).height(60.dp))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(timer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${shortTime(timerSessions.sumOf { it.durationMs })} · ${timerSessions.size} sesji", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton({ rename = timer }) { Icon(Icons.Outlined.Edit, "Zmień nazwę timera ${timer.name}", Modifier.size(18.dp)) }
                    IconButton({ deleteTimer = timer }) { Icon(Icons.Outlined.DeleteOutline, "Usuń timer ${timer.name}", Modifier.size(18.dp)) }
                }
                Button(onClick = { if (state.active?.timerId == timer.id) onActive() else onStart(timer) }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp))
                    Text(if (state.active?.timerId == timer.id) "Wróć do sesji" else "Rozpocznij czytanie")
                }
                if (timerSessions.isNotEmpty()) TextButton(onClick = { timerFilter = if (timerFilter == timer.id) null else timer.id }) {
                    Text(if (timerFilter == timer.id) "Pokaż całą historię" else "Pokaż historię tego timera")
                }
            }
        }
        item { SectionTitle("Historia czytania", timerFilter?.let { timers.find { t -> t.id == it }?.name } ?: "Wszystkie sesje tej książki") }
        if (filtered.isEmpty()) item { EmptyState(Icons.Outlined.History, "Pierwszy rozdział przed Tobą", "Uruchom timer, a Twoja historia pojawi się tutaj.") }
        items(filtered, key = { it.id }) { session ->
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(dateTime(session.startedAt), style = MaterialTheme.typography.labelLarge)
                        Text(timers.find { it.id == session.timerId }?.name ?: "Timer", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton({ editSession = session }) { Icon(Icons.Outlined.Edit, "Popraw sesję", Modifier.size(18.dp)) }
                    IconButton({ deleteSession = session }) { Icon(Icons.Outlined.DeleteOutline, "Usuń sesję", Modifier.size(18.dp)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(duration(session.durationMs), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("str. ${session.startPage ?: "—"} → ${session.endPage ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(session.pages?.let { "$it przeczytanych stron" } ?: "Brak pełnych danych stron / ponowne czytanie", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (addingTimer) NameDialog("Nowy timer", onDismiss = { addingTimer = false }) { vm.runAction { vm.repository.addTimer(book.id, it) } }
    rename?.let { timer -> NameDialog("Zmień nazwę timera", timer.name, { rename = null }) { vm.runAction { vm.repository.renameTimer(timer, it) } } }
    deleteTimer?.let { timer -> ConfirmDialog("Usunąć timer?", "Zniknie timer „${timer.name}” i jego historia sesji.", onDismiss = { deleteTimer = null }) {
        vm.runAction { vm.repository.deleteTimer(timer.id); if (timerFilter == timer.id) timerFilter = null }
    } }
    if (deleteBook) ConfirmDialog("Usunąć książkę?", "Usuniesz „${book.title}”, wszystkie jej timery i historię czytania.", onDismiss = { deleteBook = false }) {
        vm.runAction { vm.repository.deleteBook(book.id); onBack() }
    }
    editSession?.let { EditSessionDialog(it, { editSession = null }) { edited -> vm.runAction { vm.repository.editSession(edited) } } }
    deleteSession?.let { s -> ConfirmDialog("Usunąć sesję?", "Ten pomiar zniknie z historii i statystyk. Aktualna strona książki pozostanie bez zmian.", onDismiss = { deleteSession = null }) {
        vm.runAction { vm.repository.dao.deleteSession(s.id) }
    } }
}

@Composable fun BookFormScreen(existing: Book?, vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val draftId = rememberSaveable { newId() }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var author by rememberSaveable { mutableStateOf(existing?.author ?: "") }
    var total by rememberSaveable { mutableStateOf(existing?.totalPages?.toString() ?: "") }
    var current by rememberSaveable { mutableStateOf(existing?.currentPage?.toString() ?: "0") }
    var uri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var clearCover by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        if (selected != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(selected, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            uri = selected.toString(); clearCover = false
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { uri = cameraUri; clearCover = false } else cameraPath?.let { File(it).delete() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Wróć") }
            Text(if (existing == null) "Nowa książka" else "Edytuj książkę", style = MaterialTheme.typography.headlineMedium)
        }
        Text("Daj jej miejsce w swojej bibliotece.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(title, { if (it.length <= 300) title = it }, Modifier.fillMaxWidth(), label = { Text("Tytuł książki *") }, singleLine = true, isError = title.isBlank() && error != null)
        OutlinedTextField(author, { if (it.length <= 300) author = it }, Modifier.fillMaxWidth(), label = { Text("Autor (opcjonalnie)") }, singleLine = true)
        OutlinedTextField(total, { total = it }, Modifier.fillMaxWidth(), label = { Text("Liczba stron (opcjonalnie)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        OutlinedTextField(current, { current = it }, Modifier.fillMaxWidth(), label = { Text("Aktualna strona") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        SoftCard {
            Text("Okładka", style = MaterialTheme.typography.titleLarge)
            Text(when { uri != null -> "Wybrano zdjęcie. Zostanie skopiowane do aplikacji przy zapisie."
                existing?.cover != null && !clearCover -> "Książka ma własną okładkę."
                else -> "Bez zdjęcia stworzymy dla Ciebie autorską okładkę." }, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { gallery.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text("Wybierz zdjęcie lub plik")
            }
            OutlinedButton(onClick = {
                runCatching {
                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                    val file = File.createTempFile("cover-", ".jpg", dir)
                    cameraPath = file.path
                    val selected = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    cameraUri = selected.toString(); camera.launch(selected)
                }.onFailure { error = "Nie można uruchomić aparatu. Wybierz zdjęcie z plików." }
            }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Zrób zdjęcie okładki") }
            if (uri != null || (existing?.cover != null && !clearCover)) TextButton({ uri = null; clearCover = true }) { Text("Użyj okładki zastępczej") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            runCatching {
                require(title.isNotBlank()) { "Podaj tytuł książki." }
                val totalPages = if (total.isBlank()) null else total.toIntOrNull()?.takeIf { it > 0 } ?: error("Podaj dodatnią liczbę stron lub zostaw puste pole.")
                val page = current.toIntOrNull()?.takeIf { it >= 0 } ?: error("Aktualna strona musi wynosić 0 lub więcej.")
                (existing ?: Book(id = draftId, title = title)).copy(title = title, author = author, totalPages = totalPages, currentPage = page)
            }.onSuccess { book ->
                saving = true
                vm.runAction {
                    try {
                        val cover = if (uri != null) vm.app.covers.import(Uri.parse(uri)) else if (clearCover) null else book.cover
                        vm.repository.saveBook(book.copy(cover = cover))
                        cameraPath?.let { File(it).delete() }
                        onBack()
                    } finally { saving = false }
                }
            }.onFailure { error = it.message }
        }, Modifier.fillMaxWidth().height(54.dp), enabled = !saving) {
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Zapisz książkę")
        }
    }
}
