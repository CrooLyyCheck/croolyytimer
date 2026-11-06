package pl.croolyy.timer

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import pl.croolyy.timer.notifications.SessionNotifications
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import pl.croolyy.timer.backup.DriveClient
import pl.croolyy.timer.ui.AppViewModel
import pl.croolyy.timer.ui.CrooApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private val authorization by lazy { Identity.getAuthorizationClient(this) }
    private val scopes = listOf(Scope(DriveClient.SCOPE))
    private val installPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (packageManager.canRequestPackageInstalls()) installUpdate()
        else vm.error("Zezwolenie na instalację nie zostało przyznane. Możesz spróbować ponownie w ustawieniach aplikacji.")
    }
    private val installer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val googleConsent = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            runCatching { authorization.getAuthorizationResultFromIntent(result.data).accessToken }
                .onSuccess { token -> if (token != null) vm.authorized(token) else vm.error("Google nie udostępnił dostępu do kopii.") }
                .onFailure { vm.error("Nie udało się połączyć z Google Drive. Spróbuj ponownie później.") }
        } else vm.error("Połączenie z Google Drive anulowane.")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.action == SessionNotifications.OPEN) vm.openActive.value = true
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { vm.visibleTicker() } }
        setContent { CrooApp(vm, ::connectGoogle, ::disconnectGoogle, ::installUpdate) }
    }
    override fun onStart() { super.onStart(); (application as CrooLyyApp).updates.check(manual = false) }
    private fun installUpdate() {
        if (vm.installingUpdate.value) return
        vm.installingUpdate.value = true
        vm.runAction {
            try {
                require(vm.repository.dao.active() == null) { "Zakończ aktywną sesję przed instalacją aktualizacji." }
                if (!packageManager.canRequestPackageInstalls()) {
                    installPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                } else {
                    val intent = vm.app.updates.installIntent()
                    require(vm.repository.dao.active() == null) { "Zakończ aktywną sesję przed instalacją aktualizacji." }
                    installer.launch(intent)
                }
            } catch (e: android.content.ActivityNotFoundException) { vm.error("Nie można otworzyć instalatora Androida na tym urządzeniu.") }
            catch (e: SecurityException) { vm.error("Android zablokował instalację. Sprawdź ustawienia instalowania aplikacji.") }
            finally { vm.installingUpdate.value = false }
        }
    }
    override fun onStop() { vm.backgroundCheckpoint(); super.onStop() }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { (application as CrooLyyApp).notifications.refresh() }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == SessionNotifications.OPEN) vm.openActive.value = true
    }
    private fun connectGoogle() {
        authorization.authorize(AuthorizationRequest.builder().setRequestedScopes(scopes).build())
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    result.pendingIntent?.let { googleConsent.launch(IntentSenderRequest.Builder(it).build()) }
                        ?: vm.error("Google nie udostępnił okna zgody.")
                } else result.accessToken?.let(vm::authorized) ?: vm.error("Brak dostępu do Google Drive.")
            }
            .addOnFailureListener { vm.error("Nie można połączyć z Google Drive. Sprawdź internet i spróbuj ponownie.") }
    }
    private fun disconnectGoogle() {
        authorization.revokeAccess(RevokeAccessRequest.builder().setScopes(scopes).build())
            .addOnSuccessListener { vm.disconnected(); vm.error("Konto Google zostało rozłączone.") }
            .addOnFailureListener { vm.error("Nie udało się cofnąć zgody Google. Sprawdź internet i spróbuj ponownie.") }
    }
}
