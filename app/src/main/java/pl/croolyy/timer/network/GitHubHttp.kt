package pl.croolyy.timer.network

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Public requests only: no GitHub account or embedded access token. */
object GitHubHttp {
    const val RAW = "https://raw.githubusercontent.com/CrooLyyCheck/croolyytimer/main/"
    const val REPO = "https://github.com/CrooLyyCheck/croolyytimer"
    private val hosts = setOf("raw.githubusercontent.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com", "github-releases.githubusercontent.com")
    fun allowed(address: String): Boolean = runCatching {
        val url = URL(address)
        url.protocol == "https" && url.host in hosts && url.userInfo == null && url.port in setOf(-1, 443)
    }.getOrDefault(false)
    suspend fun bytes(address: String, limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
        download(address, output, limit)
        return output.toByteArray()
    }
    suspend fun download(address: String, output: OutputStream, limit: Long, progress: (Long) -> Unit = {}): Long {
        var url = URL(address)
        repeat(6) {
            currentCoroutineContext().ensureActive()
            require(allowed(url.toExternalForm())) { "Nieprawidłowy adres pobierania." }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false; connectTimeout = 12_000; readTimeout = 20_000
                useCaches = false
                setRequestProperty("User-Agent", "CrooLyyTimer (+$REPO)")
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                when (connection.responseCode) {
                    in 300..399 -> { url = URL(url, connection.getHeaderField("Location") ?: throw IOException("Brak adresu pobierania.")); return@repeat }
                    in 200..299 -> Unit
                    else -> throw IOException("Nie można pobrać danych z GitHuba.")
                }
                require(connection.contentLengthLong <= limit) { "Pobrany plik jest za duży." }
                return connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024); var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer); if (count < 0) break
                        total += count; require(total <= limit) { "Pobrany plik jest za duży." }
                        output.write(buffer, 0, count); progress(total)
                    }
                    total
                }
            } finally { connection.disconnect() }
        }
        throw IOException("Nie udało się zakończyć pobierania.")
    }
}
