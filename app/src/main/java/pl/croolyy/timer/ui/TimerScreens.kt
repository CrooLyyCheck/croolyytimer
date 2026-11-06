package pl.croolyy.timer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import pl.croolyy.timer.data.*
import pl.croolyy.timer.security.AccessState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.croolyy.timer.domain.SessionClock

@Composable fun TimerScreen(state: LibraryState, access: AccessState, vm: AppViewModel,
    onSettings: () -> Unit, onBooks: () -> Unit, onFinish: () -> Unit, onFocus: () -> Unit) {
    var delete by remember { mutableStateOf<GeneralSession?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SectionTitle("Chwila skupienia", "Odłóż pośpiech. Zostań z chwilą.") }
        if (state.active?.bookId == null && state.active != null) {
            item { TimerFace(state.active, state, vm, onFinish) }
        } else if (!access.unlocked) {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(Modifier.size(104.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Lock, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text("Dobra historia\nzaczyna się od książki", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Text("Lepiej używaj timera z zakładki Książki! 📚\n\nJeśli jednak, szczurku 🐀, naprawdę chcesz korzystać ze zwykłego timera, przejdź do Ustawień i wpisz hasło odblokowujące.",
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onSettings, Modifier.fillMaxWidth()) { Text("Przejdź do ustawień") }
                        TextButton(onBooks) { Icon(Icons.Outlined.AutoStories, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Otwórz swoją bibliotekę") }
                    }
                }
            }
        } else {
            item { SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Icon(Icons.Outlined.HourglassEmpty, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("00:00:00", fontSize = 48.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                    Text("Trochę czasu tylko dla Ciebie.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button({ vm.start(null, null) }, Modifier.fillMaxWidth().height(54.dp), enabled = state.active == null) {
                        Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Rozpocznij pomiar")
                    }
                    if (state.active != null) TextButton(onFocus) { Text("Przejdź do aktywnej sesji książki") }
                    Text(if (access.permanent) "Timer odblokowany na stałe" else "Dostęp do ${dateTime(access.expiresAt)}", style = MaterialTheme.typography.bodySmall)
                }
            } }
        }
        item { SectionTitle("Historia pomiarów", "Zwykły timer ma własną historię.") }
        if (state.generalSessions.isEmpty()) item { EmptyState(Icons.Outlined.History, "Czas jeszcze przed Tobą", "Zapisane pomiary pojawią się tutaj.") }
        items(state.generalSessions, key = { it.id }) { session ->
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(duration(session.durationMs), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(dateTime(session.startedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton({ delete = session }) { Icon(Icons.Outlined.DeleteOutline, "Usuń pomiar") }
                }
            }
        }
    }
    delete?.let { session -> ConfirmDialog("Usunąć pomiar?", "Pomiar ${duration(session.durationMs)} zniknie z historii.", onDismiss = { delete = null }) {
        vm.runAction { vm.repository.dao.deleteGeneral(session.id) }
    } }
}

@Composable fun TimerFace(active: ActiveSession, state: LibraryState, vm: AppViewModel, onFinish: () -> Unit) {
    val mark by vm.now.collectAsStateWithLifecycle()
    var reset by remember { mutableStateOf(false) }
    val book = state.books.find { it.id == active.bookId }
    val timer = state.timers.find { it.id == active.timerId }
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = .18f)
    val pulse by animateFloatAsState(if (active.running) 1f else .94f, spring(stiffness = Spring.StiffnessLow), label = "Rytm timera")
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (book != null) {
            Cover(book, book.cover?.let { vm.app.covers.file(it) }, Modifier.width(84.dp).height(126.dp))
            Text(book.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(timer?.name ?: "Czytanie", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Box(Modifier.sizeIn(maxWidth = 360.dp).fillMaxWidth().aspectRatio(1.5f).scale(pulse), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(ringColor, radius = size.minDimension * .45f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (active.running) "TERAZ JEST TWÓJ CZAS" else "CHWILA PRZERWY", fontSize = 10.sp, letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(duration(SessionClock.duration(active, mark)), fontSize = 40.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                if (book != null) Text("Od strony ${active.startPage ?: "—"}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (active.recoveredAfterReboot) SoftCard {
            Text("Odzyskana sesja", style = MaterialTheme.typography.titleMedium)
            Text("Sesję odzyskano jako wstrzymaną. Po restarcie telefonu zachowujemy czas do ostatniego zapisu. Brakujący czas możesz poprawić po zapisaniu sesji w historii.", style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(vm::pauseResume, Modifier.weight(1f).height(54.dp)) {
                Icon(if (active.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null)
                Spacer(Modifier.width(6.dp)); Text(if (active.running) "Pauza" else "Wznów")
            }
            Button(onFinish, Modifier.weight(1f).height(54.dp)) { Icon(Icons.Outlined.Stop, null); Spacer(Modifier.width(6.dp)); Text("Zakończ") }
        }
        TextButton({ reset = true }) { Icon(Icons.Outlined.RestartAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Resetuj pomiar") }
    }
    if (reset) ConfirmDialog("Zresetować pomiar?", "Bieżący czas i niezapisana sesja zostaną usunięte. Możesz potem uruchomić nowy pomiar.", "Resetuj", { reset = false }) {
        vm.runAction { vm.repository.discard() }
    }
}

@Composable fun FocusScreen(state: LibraryState, vm: AppViewModel, onFinish: () -> Unit, onBooks: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Zostań między stronami", style = MaterialTheme.typography.headlineMedium)
        state.active?.let { TimerFace(it, state, vm, onFinish) } ?: EmptyState(Icons.Outlined.CheckCircle,
            "Dobrze wykorzystany czas", "Sesja zakończona. Twoja historia czeka w bibliotece.") {
            Button(onBooks) { Text("Wróć do książek") }
        }
    }
}

@Composable fun ActiveDock(active: ActiveSession, state: LibraryState, vm: AppViewModel, onClick: () -> Unit) {
    val mark by vm.now.collectAsStateWithLifecycle()
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(state.books.find { it.id == active.bookId }?.title ?: "Zwykły timer", maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                Text("${duration(SessionClock.duration(active, mark))} · ${if (active.running) "w trakcie" else "pauza"}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(vm::pauseResume) { Icon(if (active.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (active.running) "Wstrzymaj sesję" else "Wznów sesję") }
        }
    }
}
