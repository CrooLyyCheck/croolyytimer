package pl.croolyy.timer.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import pl.croolyy.timer.CrooLyyApp
import pl.croolyy.timer.backup.*
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.SessionClock
import java.io.File

data class Operation(val label: String, val progress: Float = 0f)
data class PendingExport(val password: CharArray?)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    val app = application as CrooLyyApp
    val repository = app.repository
    val library = repository.state.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryState())
    val access = app.access.state
    val lastBackup = app.backup.lastBackup.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()
    private val notificationPermissionChannel = Channel<Unit>(Channel.BUFFERED)
    val notificationPermissionRequests = notificationPermissionChannel.receiveAsFlow()
    val openActive = MutableStateFlow(false)
    val unlocking = MutableStateFlow(false)
    val installingUpdate = MutableStateFlow(false)
    val operation = MutableStateFlow<Operation?>(null)
    val now = MutableStateFlow(app.clock.now())
    val driveBackups = MutableStateFlow<List<DriveBackup>>(emptyList())
    val driveConnected = MutableStateFlow(false)
    val stagedRestore = MutableStateFlow<File?>(null)
    val restoreEncrypted = MutableStateFlow(false)
    private var drive: DriveClient? = null
    private var pendingExport: PendingExport? = null
    fun prepareExport(password: CharArray?) {
        pendingExport?.password?.fill('\u0000'); pendingExport = PendingExport(password)
    }
    fun consumeExport(): PendingExport? = pendingExport.also { pendingExport = null }

    init {
        app.notifications.start()
        runAction { repository.checkpoint(); app.access.refresh() }
    }
    /** Only while the Activity is visible. No service or background polling is needed for timing. */
    suspend fun visibleTicker() = coroutineScope {
        var tick = 0
        while (isActive) {
            now.value = app.clock.now()
            if (tick++ % 40 == 0) {
                try { repository.checkpoint(); app.access.refresh() }
                catch (e: Exception) { if (e is CancellationException) throw e; messageChannel.send("Nie udało się zapisać stanu: ${e.message}") }
            }
            delay(250)
        }
    }
    fun backgroundCheckpoint() { app.scope.launch { runCatching { repository.checkpoint() } } }
    fun runAction(action: suspend () -> Unit) { viewModelScope.launch {
        try { action() } catch (e: Exception) { if (e is CancellationException) throw e; notify(e.message ?: "Operacja nie powiodła się.") }
    } }
    suspend fun notify(message: String) { messageChannel.send(message) }
    fun error(message: String) { runAction { notify(message) } }
    fun unlock(password: CharArray) {
        if (unlocking.value) { password.fill('\u0000'); return }
        unlocking.value = true
        runAction { try { notify(app.access.unlock(password)) } finally { password.fill('\u0000'); unlocking.value = false } }
    }
    fun start(timerId: String?, page: Int?) { runAction {
        if (timerId == null) { app.access.refresh(); require(access.value.unlocked) { "Odblokuj zwykły stoper w ustawieniach." } }
        repository.start(timerId, page)
        notificationPermissionChannel.send(Unit)
    } }
    fun pauseResume() { runAction {
        val active = repository.dao.active() ?: return@runAction
        if (active.running) repository.pause() else {
            if (active.bookId == null) { app.access.refresh(); require(access.value.unlocked) { "Odblokuj stoper przed wznowieniem." } }
            repository.resume()
        }
    } }
    fun finish(page: Int?, backwards: Boolean) { runAction { repository.finish(page, backwards); notify("Sesja zapisana. Dobry czas z książką!") } }
    fun saveBook(book: Book, uri: Uri?, clearCover: Boolean, done: () -> Unit) { runAction {
        val cover = if (uri != null) app.covers.import(uri) else if (clearCover) null else book.cover
        repository.saveBook(book.copy(cover = cover)); done()
    } }
    fun settings(settings: AppSettings) { runAction { repository.settings(settings) } }
    fun updateSettings(change: (AppSettings) -> AppSettings) { runAction { repository.updateSettings(change) } }
    fun elapsed(active: ActiveSession) = SessionClock.duration(active, now.value)
    private fun task(label: String, body: suspend (progress: (Float) -> Unit) -> Unit) {
        if (operation.value != null) { error("Poczekaj na zakończenie bieżącej operacji."); return }
        operation.value = Operation(label)
        viewModelScope.launch {
            try { body { operation.value = Operation(label, it.coerceIn(0f, 1f)) } }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e is DriveAuthorizationExpired) { drive = null; driveConnected.value = false }
                notify(when (e) {
                    is java.net.UnknownHostException, is java.net.SocketTimeoutException, is java.net.ConnectException -> "Brak połączenia z Google Drive. Sprawdź internet i spróbuj ponownie."
                    is net.lingala.zip4j.exception.ZipException -> "Nie można odczytać ZIP. Sprawdź hasło i poprawność kopii."
                    else -> e.message ?: "Operacja nie powiodła się. Twoje obecne dane zostały zachowane."
                })
            } finally { operation.value = null }
        }
    }
    fun export(uri: Uri, password: CharArray?) { task("Tworzenie kopii") { progress ->
        try { app.backup.export(uri, password, progress); notify("Kopia zapasowa została zapisana.") }
        finally { password?.fill('\u0000') }
    } }
    fun stage(uri: Uri) { task("Sprawdzanie archiwum") { _ ->
        val file = app.backup.stage(uri)
        stagedRestore.value?.delete()
        stagedRestore.value = file
        restoreEncrypted.value = withContext(Dispatchers.IO) { app.backup.archive.isEncrypted(file) }
    } }
    fun cancelRestore() { stagedRestore.value?.delete(); stagedRestore.value = null }
    fun restore(password: CharArray?) { val file = stagedRestore.value ?: return
        task("Przywracanie danych") { progress ->
            app.backup.restore(file, password, progress)
            cancelRestore(); notify("Przywrócono książki, okładki, stopery, sesje i ustawienia.")
        }
    }
    fun authorized(token: String) { drive = DriveClient(token); driveConnected.value = true; listDrive() }
    fun disconnected() { drive = null; driveConnected.value = false; driveBackups.value = emptyList() }
    fun listDrive() { task("Pobieranie listy kopii") { _ -> driveBackups.value = requireNotNull(drive) { "Połącz konto Google." }.list() } }
    fun uploadDrive(password: CharArray?) { task("Kopia w Google Drive") { progress ->
        try {
            val client = requireNotNull(drive) { "Połącz konto Google." }
            val file = app.backup.create(password) { progress(it * .4f) }
            try {
                val name = "CrooLyyTimer-${java.time.LocalDateTime.now().toString().replace(':', '-')}.zip"
                client.upload(file, name) { progress(.4f + it * .6f) }
                app.backup.markBackup(); driveBackups.value = client.list(); notify("Kopia została wysłana do Google Drive.")
            } finally { file.delete() }
        } finally { password?.fill('\u0000') }
    } }
    fun downloadDrive(backup: DriveBackup) { task("Pobieranie kopii") { progress ->
        val file = File.createTempFile("drive-restore-", ".zip", app.cacheDir)
        try {
            requireNotNull(drive).download(backup.id, file, progress)
            val encrypted = withContext(Dispatchers.IO) { app.backup.archive.isEncrypted(file) }
            stagedRestore.value?.delete(); stagedRestore.value = file; restoreEncrypted.value = encrypted
        } catch (e: Exception) { file.delete(); throw e }
    } }
    fun deleteDrive(backup: DriveBackup) { task("Usuwanie kopii z Google Drive") { _ ->
        requireNotNull(drive).delete(backup.id); driveBackups.value = requireNotNull(drive).list(); notify("Usunięto wybraną kopię.")
    } }
    override fun onCleared() { pendingExport?.password?.fill('\u0000'); stagedRestore.value?.delete(); super.onCleared() }
}
