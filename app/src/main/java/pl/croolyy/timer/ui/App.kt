package pl.croolyy.timer.ui

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import androidx.navigation.createGraph
import pl.croolyy.timer.data.*
import java.time.LocalDate

@Composable fun CrooApp(vm: AppViewModel, connectGoogle: () -> Unit, disconnectGoogle: () -> Unit) {
    val state by vm.library.collectAsStateWithLifecycle()
    val access by vm.access.collectAsStateWithLifecycle()
    val operation by vm.operation.collectAsStateWithLifecycle()
    val lastBackup by vm.lastBackup.collectAsStateWithLifecycle()
    val connected by vm.driveConnected.collectAsStateWithLifecycle()
    val backups by vm.driveBackups.collectAsStateWithLifecycle()
    val staged by vm.stagedRestore.collectAsStateWithLifecycle()
    val encryptedRestore by vm.restoreEncrypted.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val context = LocalContext.current
    val openActive by vm.openActive.collectAsStateWithLifecycle()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.runAction { vm.app.notifications.refresh() }
    }
    LaunchedEffect(vm) { vm.notificationPermissionRequests.collect {
        if (android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    } }
    LaunchedEffect(openActive, state.active?.sessionId) {
        if (openActive && state.active != null) { nav.navigate("focus") { launchSingleTop = true }; vm.openActive.value = false }
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "timer"
    val snack = remember { SnackbarHostState() }
    var startTimer by remember { mutableStateOf<BookTimer?>(null) }
    var finishPrompt by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    var restorePassword by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val prepared = vm.consumeExport()
        if (uri == null) prepared?.password?.fill('\u0000')
        else if (prepared == null) vm.error("Eksport przerwany po zamknięciu aplikacji. Uruchom tworzenie kopii ponownie.")
        else vm.export(uri, prepared.password)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::stage) }
    val activity = requireNotNull(LocalActivity.current)
    val keepOn = state.settings.keepScreenOn && state.active?.running == true
    DisposableEffect(keepOn) {
        if (keepOn) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }
    LaunchedEffect(staged, operation) { if (staged != null && operation == null && !restorePassword) confirmRestore = true }
    fun navigate(tab: String) {
        nav.navigate(tab) { popUpTo(nav.graph.startDestinationId); launchSingleTop = true }
    }
    fun finish() {
        vm.runAction {
            vm.repository.pause()
            if (state.active?.bookId != null && state.settings.askAfter) finishPrompt = true
            else vm.finish(null, false)
        }
    }
    fun begin(timer: BookTimer) {
        if (state.active != null) { vm.error("Masz już aktywną sesję. Zakończ ją przed uruchomieniem kolejnej."); return }
        if (state.settings.askBefore) startTimer = timer
        else { vm.start(timer.id, null); nav.navigate("focus") { launchSingleTop = true } }
    }
    fun requestExport(cloud: Boolean) {
        if (state.settings.encryptBackup) backupPassword = if (cloud) "cloud" else "local"
        else if (cloud) vm.uploadDrive(null)
        else { vm.prepareExport(null); export.launch("CrooLyyTimer-${LocalDate.now()}.zip") }
    }
    CrooTheme(state.settings.theme) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background,
            topBar = { Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 14.dp)) { Brand() }
            } },
            bottomBar = { Column {
                if (state.active != null && route != "focus" && operation == null) ActiveDock(state.active!!, state, vm) { nav.navigate("focus") { launchSingleTop = true } }
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    val tabs = listOf(Triple("timer", "Stoper", Icons.Outlined.Timer), Triple("books", "Książki", Icons.Outlined.AutoStories), Triple("calendar", "Kalendarz", Icons.Outlined.CalendarMonth), Triple("settings", "Ustawienia", Icons.Outlined.Tune))
                    tabs.forEach { (key, title, icon) ->
                        val selected = when (key) { "books" -> route.startsWith("book") || route.startsWith("edit") || route == "new" || route == "focus"; "calendar" -> route.startsWith("calendar"); else -> route == key }
                        NavigationBarItem(selected, { navigate(key) }, icon = { Icon(icon, null) }, label = { Text(title) })
                    }
                }
            } }, snackbarHost = { SnackbarHost(snack) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                // Keep graph identity stable when a modal or database emission recomposes the shell.
                // Destination content reads the live Compose State delegates captured below.
                val graph = remember(nav, vm) { nav.createGraph(startDestination = "timer") {
                    composable("timer") { TimerScreen(state, access, vm, { navigate("settings") }, { navigate("books") }, ::finish, { nav.navigate("focus") }) }
                    composable("books") { BooksScreen(state, vm, { nav.navigate("book/$it") }, { nav.navigate("new") }) }
                    composable("book/{id}") { backStack ->
                        val book = state.books.find { it.id == backStack.arguments?.getString("id") }
                        if (book != null) BookDetailScreen(book, state, vm, { nav.popBackStack() }, { nav.navigate("edit/${book.id}") }, ::begin, { nav.navigate("focus") }, { nav.navigate("calendar/${book.id}") })
                        else EmptyState(Icons.Outlined.MenuBook, "Książka niedostępna", "Wróć do biblioteki.") { TextButton({ navigate("books") }) { Text("Biblioteka") } }
                    }
                    composable("new") { BookFormScreen(null, vm) { nav.popBackStack() } }
                    composable("edit/{id}") { backStack ->
                        state.books.find { it.id == backStack.arguments?.getString("id") }?.let { book -> BookFormScreen(book, vm) { nav.popBackStack() } }
                    }
                    composable("focus") { FocusScreen(state, vm, ::finish, { navigate("books") }) }
                    composable("calendar") { CalendarScreen(state, vm.app.covers::file, onBook = { nav.navigate("book/$it") }) }
                    composable("calendar/{id}") { stack -> CalendarScreen(state, vm.app.covers::file, stack.arguments?.getString("id"), onBook = { nav.navigate("book/$it") }) }
                    composable("settings") { SettingsScreen(state.settings, access, vm, lastBackup, connected, backups,
                        { requestExport(false) }, { import.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
                        connectGoogle, disconnectGoogle, { requestExport(true) }) }
                } }
                NavHost(nav, graph,
                    enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None })
            }
        }
        startTimer?.let { timer -> PageDialog(true, state.books.find { it.id == timer.bookId }?.currentPage ?: 0,
            onDismiss = { startTimer = null }) { page, _ ->
            vm.start(timer.id, page); startTimer = null; nav.navigate("focus") { launchSingleTop = true }
        } }
        if (finishPrompt) {
            val active = state.active
            if (active != null) PageDialog(false, state.books.find { it.id == active.bookId }?.currentPage ?: active.startPage ?: 0,
                active.startPage, { finishPrompt = false }) { page, backwards -> vm.finish(page, backwards); finishPrompt = false }
        }
        backupPassword?.let { target -> BackupPasswordDialog(true, { backupPassword = null }) { password ->
            backupPassword = null
            if (target == "cloud") vm.uploadDrive(password) else { vm.prepareExport(password); export.launch("CrooLyyTimer-${LocalDate.now()}-AES256.zip") }
        } }
        if (confirmRestore && staged != null && operation == null) AlertDialog(
            onDismissRequest = { confirmRestore = false; vm.cancelRestore() },
            title = { Text("Przywrócić kopię zapasową?") },
            text = { Text("Czy chcesz zastąpić obecne dane zawartością kopii zapasowej? Bieżąca biblioteka, ustawienia i aktywna sesja zostaną zastąpione dopiero po sprawdzeniu kopii.") },
            confirmButton = { TextButton({ confirmRestore = false; if (encryptedRestore) restorePassword = true else vm.restore(null) }) { Text("Zastąp dane") } },
            dismissButton = { TextButton({ confirmRestore = false; vm.cancelRestore() }) { Text("Anuluj") } })
        if (restorePassword && staged != null && operation == null) BackupPasswordDialog(false, { restorePassword = false; vm.cancelRestore() }) {
            restorePassword = false; vm.restore(it)
        }
        operation?.let { task -> AlertDialog(onDismissRequest = {}, title = { Text(task.label) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(progress = { task.progress }, Modifier.fillMaxWidth())
                Text("${(task.progress * 100).toInt()}% · Poczekaj na zakończenie operacji.")
            }
        }, confirmButton = {}) }
    }
}
