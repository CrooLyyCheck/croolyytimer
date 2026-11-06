package pl.croolyy.timer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import pl.croolyy.timer.data.ReadingSession
import pl.croolyy.timer.domain.validPage

@Composable fun PageDialog(start: Boolean, initialPage: Int, startPage: Int? = null,
    onDismiss: () -> Unit, onConfirm: (Int, Boolean) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initialPage.toString()) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val backwards = !start && startPage != null && (value.toIntOrNull() ?: Int.MAX_VALUE) < startPage
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (start) "Na której stronie aktualnie jesteś?" else "Na której stronie skończyłeś czytać?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!start) Text("Sesja jest wstrzymana. Wpisz ostatnią przeczytaną stronę.")
            OutlinedTextField(value, { value = it; error = null; acknowledged = false }, label = { Text("Numer strony") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                isError = error != null, supportingText = { error?.let { Text(it) } })
            if (backwards) {
                Text("Końcowa strona jest mniejsza od początkowej. Nie wyliczymy tempa dla tej sesji.", color = MaterialTheme.colorScheme.error)
                Row { Checkbox(acknowledged, { acknowledged = it }); Text("Potwierdzam ponowne czytanie wcześniejszego fragmentu.", Modifier.padding(top = 10.dp)) }
            }
        } },
        confirmButton = { TextButton(onClick = {
            runCatching { requireNotNull(validPage(value)) }.onSuccess {
                if (backwards && !acknowledged) error = "Potwierdź ponowne czytanie fragmentu."
                else onConfirm(it, acknowledged)
            }.onFailure { error = it.message }
        }) { Text(if (start) "Rozpocznij" else "Zapisz sesję") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
}

@Composable fun NameDialog(title: String, initial: String = "", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        OutlinedTextField(name, { if (it.length <= 150) name = it }, label = { Text("Nazwa (opcjonalnie)") }, singleLine = true)
    }, confirmButton = { TextButton(onClick = { onConfirm(name); onDismiss() }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
}

@Composable fun EditSessionDialog(session: ReadingSession, onDismiss: () -> Unit, onSave: (ReadingSession) -> Unit) {
    var start by rememberSaveable { mutableStateOf(session.startPage?.toString() ?: "") }
    var end by rememberSaveable { mutableStateOf(session.endPage?.toString() ?: "") }
    var seconds by rememberSaveable { mutableStateOf((session.durationMs / 1000).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var acknowledge by rememberSaveable { mutableStateOf(false) }
    val backwards = start.toIntOrNull() != null && end.toIntOrNull() != null && end.toInt() < start.toInt()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Popraw sesję") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Puste pole strony oznacza brak danych. Czas możesz skorygować np. po restarcie telefonu.")
            OutlinedTextField(start, { start = it; acknowledge = false }, label = { Text("Strona początkowa (opcjonalnie)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(end, { end = it; acknowledge = false }, label = { Text("Strona końcowa (opcjonalnie)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(seconds, { seconds = it }, label = { Text("Czas w sekundach") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            if (backwards) Row { Checkbox(acknowledge, { acknowledge = it }); Text("Potwierdzam ponowne czytanie fragmentu.") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        runCatching {
            val startValue = validPage(start, true); val endValue = validPage(end, true)
            require(!backwards || acknowledge) { "Potwierdź ponowne czytanie fragmentu." }
            val secs = seconds.toLongOrNull()
            require(secs != null && secs in 0..315_360_000L) { "Podaj poprawny czas w sekundach." }
            session.copy(startPage = startValue, endPage = endValue, durationMs = if (secs == session.durationMs / 1000) session.durationMs else secs * 1000)
        }.onSuccess { onSave(it); onDismiss() }.onFailure { error = it.message }
    }) { Text("Zapisz") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
}

@Composable fun BackupPasswordDialog(creating: Boolean, onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    // Deliberately not rememberSaveable: a password never enters saved instance state.
    var password by remember { mutableStateOf("") }; var repeated by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (creating) "Zaszyfruj kopię" else "Hasło kopii zapasowej") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (creating) "Zachowaj hasło w menedżerze haseł. Bez niego nie odzyskasz tej kopii." else "Wpisz hasło użyte do zaszyfrowania kopii.")
            OutlinedTextField(password, { password = it }, label = { Text("Hasło") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
            if (creating) OutlinedTextField(repeated, { repeated = it }, label = { Text("Powtórz hasło") },
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        if (creating && password.length < 8) error = "Użyj co najmniej 8 znaków."
        else if (creating && password != repeated) error = "Hasła są różne."
        else if (password.isEmpty()) error = "Wpisz hasło."
        else { val chars = password.toCharArray(); password = ""; repeated = ""; onConfirm(chars) }
    }) { Text(if (creating) "Utwórz kopię" else "Przywróć") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
}
