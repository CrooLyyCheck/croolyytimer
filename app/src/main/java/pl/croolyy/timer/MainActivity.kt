package pl.croolyy.timer

import android.os.Bundle
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
    private val googleConsent = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            runCatching { authorization.getAuthorizationResultFromIntent(result.data).accessToken }
                .onSuccess { token -> if (token != null) vm.authorized(token) else vm.error("Google nie udostępnił dostępu do kopii.") }
                .onFailure { vm.error("Nie udało się połączyć z Google Drive. Sprawdź konfigurację OAuth.") }
        } else vm.error("Połączenie z Google Drive anulowane.")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { vm.visibleTicker() } }
        setContent { CrooApp(vm, ::connectGoogle, ::disconnectGoogle) }
    }
    override fun onStop() { vm.backgroundCheckpoint(); super.onStop() }
    private fun connectGoogle() {
        authorization.authorize(AuthorizationRequest.builder().setRequestedScopes(scopes).build())
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    result.pendingIntent?.let { googleConsent.launch(IntentSenderRequest.Builder(it).build()) }
                        ?: vm.error("Google nie udostępnił okna zgody.")
                } else result.accessToken?.let(vm::authorized) ?: vm.error("Brak dostępu do Google Drive.")
            }
            .addOnFailureListener { vm.error("Nie można połączyć z Google Drive. Sprawdź internet, usługi Google Play i konfigurację OAuth (pakiet i SHA certyfikatu).") }
    }
    private fun disconnectGoogle() {
        authorization.revokeAccess(RevokeAccessRequest.builder().setScopes(scopes).build())
            .addOnSuccessListener { vm.disconnected(); vm.error("Konto Google zostało rozłączone.") }
            .addOnFailureListener { vm.error("Nie udało się cofnąć zgody Google. Sprawdź internet i spróbuj ponownie.") }
    }
}
