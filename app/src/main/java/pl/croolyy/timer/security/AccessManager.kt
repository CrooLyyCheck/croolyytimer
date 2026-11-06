package pl.croolyy.timer.security

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import pl.croolyy.timer.domain.AppClock
import pl.croolyy.timer.domain.TimeMark
import java.security.MessageDigest
import java.util.Base64

private val Context.accessStore by preferencesDataStore("local_access")
const val SEVEN_DAYS_MS = 168L * 60 * 60 * 1000

@Serializable
data class AdminVerifier(val salt: String, val hash: String, val memoryKiB: Int = 65536,
    val iterations: Int = 3, val parallelism: Int = 1, val version: Int = 19) {
    fun verify(password: CharArray): Boolean {
        require(memoryKiB in 65536..262144 && iterations in 3..10 && parallelism in 1..4 && version == 19)
        val expected = Base64.getDecoder().decode(hash)
        require(expected.size == 32)
        val saltBytes = Base64.getDecoder().decode(salt)
        require(saltBytes.size >= 16)
        val output = ByteArray(expected.size)
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(version).withSalt(saltBytes).withMemoryAsKB(memoryKiB)
            .withIterations(iterations).withParallelism(parallelism).build()
        try {
            Argon2BytesGenerator().apply { init(parameters) }.generateBytes(password, output)
            return MessageDigest.isEqual(expected, output)
        } finally { output.fill(0); parameters.clear() }
    }
}

data class AccessState(val unlocked: Boolean = false, val permanent: Boolean = false,
    val expiresAt: Long = 0, val cooldownUntil: Long = 0, val trustedNow: Long = 0)

/** Offline feature gate; intentionally excluded from user backups. */
class AccessManager(context: Context, private val clock: AppClock,
    private val verifierOverride: AdminVerifier? = null,
    storeOverride: androidx.datastore.core.DataStore<Preferences>? = null) {
    private val store = storeOverride ?: context.accessStore
    private val verifier by lazy { verifierOverride ?: Json.decodeFromString<AdminVerifier>(
        context.assets.open("admin_verifier.json").bufferedReader().use { it.readText() }) }
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(AccessState())
    val state = mutable.asStateFlow()

    private object Keys {
        val permanent = booleanPreferencesKey("permanent")
        val until = longPreferencesKey("until")
        val highWater = longPreferencesKey("trusted_wall")
        val elapsed = longPreferencesKey("anchor_elapsed")
        val boot = intPreferencesKey("boot")
        val failures = intPreferencesKey("failures")
        val cooldown = longPreferencesKey("cooldown")
    }
    private fun trusted(p: MutablePreferences, now: TimeMark): Long {
        val previous = p[Keys.highWater] ?: now.wall
        val oldElapsed = p[Keys.elapsed] ?: now.elapsed
        val sameBoot = p[Keys.boot] == now.boot && now.elapsed >= oldElapsed
        // A large clock rollback across a reboot expires temporary access rather than extending it.
        if (!sameBoot && now.wall + 120_000 < previous) p[Keys.until] = 0
        val trusted = maxOf(now.wall, previous, if (sameBoot) previous + now.elapsed - oldElapsed else previous)
        p[Keys.highWater] = trusted; p[Keys.elapsed] = now.elapsed; p[Keys.boot] = now.boot
        return trusted
    }
    private fun publish(p: Preferences, now: Long) {
        val permanent = p[Keys.permanent] ?: false
        val until = p[Keys.until] ?: 0
        mutable.value = AccessState(permanent || until > now, permanent, until, p[Keys.cooldown] ?: 0, now)
    }
    suspend fun refresh() = mutex.withLock {
        var now = 0L
        val p = store.edit { now = trusted(it, clock.now()) }
        publish(p, now)
    }
    suspend fun unlock(password: CharArray): String = mutex.withLock {
        try {
            require(password.size <= 1024) { "Hasło jest zbyt długie." }
            var now = 0L
            val before = store.edit { now = trusted(it, clock.now()) }
            publish(before, now)
            val cooldown = before[Keys.cooldown] ?: 0
            require(cooldown <= now) { "Za dużo prób. Spróbuj za ${(cooldown - now + 999) / 1000} s." }
            val user = password.contentEquals("takchce".toCharArray())
            val admin = !user && withContext(Dispatchers.Default) { verifier.verify(password) }
            val p = store.edit {
                now = trusted(it, clock.now())
                if (user || admin) {
                    it[Keys.failures] = 0; it[Keys.cooldown] = 0
                    if (admin) it[Keys.permanent] = true
                    else if (it[Keys.permanent] != true) it[Keys.until] = now + SEVEN_DAYS_MS
                } else {
                    val failures = ((it[Keys.failures] ?: 0) + 1).coerceAtMost(12)
                    it[Keys.failures] = failures
                    if (failures >= 5) it[Keys.cooldown] = now + (30_000L * (1L shl (failures - 5))).coerceAtMost(3_600_000L)
                }
            }
            publish(p, now)
            require(user || admin) { "Nieprawidłowe hasło." }
            if (mutable.value.permanent) "Timer odblokowany na stałe" else "Timer odblokowany na 7 dni"
        } finally { password.fill('\u0000') }
    }
}
