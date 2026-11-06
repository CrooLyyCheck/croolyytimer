package pl.croolyy.timer.security

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import pl.croolyy.timer.network.GitHubHttp
import java.util.Base64

class AdminVerificationUnavailable : Exception("Nie można sprawdzić hasła administratora. Połącz się z internetem i spróbuj ponownie.")

object RemoteAdminVerifier {
    fun parse(body: String): AdminVerifier = Json.decodeFromString<AdminVerifier>(body).also {
        require(it.version == 19 && it.memoryKiB == 65536 && it.iterations == 3 && it.parallelism == 1)
        require(Base64.getDecoder().decode(it.salt).size == 16 && Base64.getDecoder().decode(it.hash).size == 32)
    }
    suspend fun fetch(): AdminVerifier = withContext(Dispatchers.IO) {
        try {
            // A fresh lookup for each administrator attempt; no old password cached offline.
            parse(GitHubHttp.bytes(GitHubHttp.RAW + "config/admin-verifier.json?t=${System.currentTimeMillis()}", 4096).toString(Charsets.UTF_8))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            throw AdminVerificationUnavailable()
        }
    }
}
