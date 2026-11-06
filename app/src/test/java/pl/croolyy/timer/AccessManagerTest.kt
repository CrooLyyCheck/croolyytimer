package pl.croolyy.timer

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.croolyy.timer.domain.*
import pl.croolyy.timer.security.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = android.app.Application::class)
class AccessManagerTest {
    @Test fun waitingForTheRepositoryDoesNotBlockClockRefreshAndRotationUsesTheNewVerifier() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "rotation-${java.util.UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val vector = Json.decodeFromString<AdminVerifier>(javaClass.getResource("/admin_test_vector.json")!!.readText())
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var policy = vector
        val access = AccessManager(context, AppClock { TimeMark(1_800_000_000_000, 1000, 1) }, storeOverride = store,
            verifierProvider = { entered.complete(Unit); release.await(); policy })
        try {
            val pending = async { access.unlock("test-owner-password-do-not-use".toCharArray()) }
            entered.await()
            withTimeout(5_000) { access.refresh() }
            release.complete(Unit); pending.await()
            assertTrue(access.state.value.permanent)
            policy = vector.copy(hash = java.util.Base64.getEncoder().encodeToString(ByteArray(32)))
            assertTrue(runCatching { access.unlock("test-owner-password-do-not-use".toCharArray()) }.isFailure)
            assertTrue(access.state.value.permanent)
            policy = vector
            access.unlock("test-owner-password-do-not-use".toCharArray())
        } finally { release.complete(Unit); scope.cancel(); file.delete() }
        Unit
    }
    @Test fun remotePasswordRotationIsFetchedForEveryAttemptAndNetworkFailureDoesNotConsumeAttempts() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "remote-access-${java.util.UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val vector = Json.decodeFromString<AdminVerifier>(javaClass.getResource("/admin_test_vector.json")!!.readText())
        var calls = 0; var offline = true
        val access = AccessManager(context, AppClock { TimeMark(1_800_000_000_000, 1000, 1) }, storeOverride = store,
            verifierProvider = { calls++; if (offline) throw AdminVerificationUnavailable(); vector })
        try {
            repeat(6) {
                val password = "test-owner-password-do-not-use".toCharArray()
                assertTrue(runCatching { access.unlock(password) }.exceptionOrNull() is AdminVerificationUnavailable)
                assertTrue(password.all { it == '\u0000' })
            }
            offline = false
            access.unlock("test-owner-password-do-not-use".toCharArray())
            assertTrue(access.state.value.permanent)
            assertEquals(7, calls)
            access.unlock("takchce".toCharArray())
            assertEquals(7, calls)
            assertTrue(access.state.value.permanent)
        } finally { scope.cancel(); file.delete() }
    }
    @Test fun remotePolicyRejectsExcessiveArgonParametersBeforeHashing() {
        val body = javaClass.getResource("/admin_test_vector.json")!!.readText()
        assertEquals(65536, RemoteAdminVerifier.parse(body).memoryKiB)
        assertThrows(IllegalArgumentException::class.java) { RemoteAdminVerifier.parse(body.replace("65536", "262144")) }
    }
    @Test fun sevenDaysExpiryRollbackThrottleAndPermanentUnlockPersist() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "access-${java.util.UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val vector = Json.decodeFromString<AdminVerifier>(javaClass.getResource("/admin_test_vector.json")!!.readText())
        var now = TimeMark(1_700_000_000_000, 1000, 1)
        fun manager() = AccessManager(context, AppClock { now }, vector, store)
        try {
            val access = manager(); access.refresh(); assertFalse(access.state.value.unlocked)
            access.unlock("takchce".toCharArray())
            assertEquals(now.wall + SEVEN_DAYS_MS, access.state.value.expiresAt)
            now = now.copy(wall = now.wall - 999999, elapsed = now.elapsed + SEVEN_DAYS_MS - 1)
            access.refresh(); assertTrue(access.state.value.unlocked)
            now = now.copy(elapsed = now.elapsed + 1); access.refresh(); assertFalse(access.state.value.unlocked)
            access.unlock("takchce".toCharArray())
            val fresh = manager(); fresh.refresh(); assertTrue(fresh.state.value.unlocked)
            now = now.copy(boot = 2, elapsed = 10, wall = 1000)
            fresh.refresh(); assertFalse(fresh.state.value.unlocked)
            repeat(5) { assertTrue(runCatching { fresh.unlock("wrong".toCharArray()) }.isFailure) }
            assertTrue(runCatching { fresh.unlock("takchce".toCharArray()) }.isFailure)
            now = now.copy(elapsed = now.elapsed + 31_000)
            fresh.unlock("test-owner-password-do-not-use".toCharArray())
            assertTrue(fresh.state.value.permanent)
            now = now.copy(boot = 3, elapsed = 1, wall = 0)
            val afterRestart = manager(); afterRestart.refresh()
            assertTrue(afterRestart.state.value.permanent)
            assertTrue(afterRestart.state.value.unlocked)
        } finally { scope.cancel(); file.delete() }
    }
    @Test fun pythonArgon2idVerifierMatchesAndroidAndRejectsOtherPasswords() {
        val vector = Json.decodeFromString<AdminVerifier>(javaClass.getResource("/admin_test_vector.json")!!.readText())
        assertTrue(vector.verify("test-owner-password-do-not-use".toCharArray()))
        assertFalse(vector.verify("different-password".toCharArray()))
    }
}
