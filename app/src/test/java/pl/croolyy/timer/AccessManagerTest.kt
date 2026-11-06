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
