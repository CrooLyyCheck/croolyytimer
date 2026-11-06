package pl.croolyy.timer.data

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CoverCandidate(val coverId: Long, val title: String, val author: String, val year: Int?)

/** Explicit, bounded human searches. No account, API key or application server. */
class OpenLibraryClient(private val cacheDir: File, private val fetch: (String, Int) -> ByteArray = ::request) {
    private val searchLock = Mutex()
    private val images = Semaphore(3)
    private var lastRequest = 0L
    private val searches = linkedMapOf<String, List<CoverCandidate>>()

    suspend fun search(title: String): List<CoverCandidate> = withContext(Dispatchers.IO) {
        val query = title.trim().take(300)
        require(query.length >= 2) { "Wpisz co najmniej dwa znaki tytułu." }
        searchLock.withLock {
            searches[query]?.let { return@withLock it }
            val wait = 1_100 - (System.nanoTime() / 1_000_000 - lastRequest)
            if (wait > 0) delay(wait)
            lastRequest = System.nanoTime() / 1_000_000
            val result = parseSearch(fetch(searchUrl(query), 2 * 1024 * 1024).toString(Charsets.UTF_8))
            if (searches.size >= 20) searches.remove(searches.keys.first())
            searches[query] = result
            result
        }
    }

    suspend fun image(candidate: CoverCandidate, large: Boolean = false): File = withContext(Dispatchers.IO) { images.withPermit {
        val directory = File(cacheDir, "open-library").apply { mkdirs() }
        val target = File(directory, "${candidate.coverId}-${if (large) "L" else "M"}.jpg")
        if (!target.isFile || target.length() == 0L) {
            val bytes = fetch(coverUrl(candidate.coverId, large), 10 * 1024 * 1024)
            val temp = File.createTempFile("download-", ".tmp", directory)
            try {
                temp.writeBytes(bytes)
                check(temp.renameTo(target) || target.isFile) { "Nie udało się zapisać okładki." }
            } finally { temp.delete() }
        }
        target
    } }

    companion object {
        fun searchUrl(query: String): String = "https://openlibrary.org/search.json?title=${URLEncoder.encode(query.trim().take(300), "UTF-8")}" +
            "&fields=title,author_name,cover_i,first_publish_year&limit=24&lang=pl"
        fun coverUrl(id: Long, large: Boolean): String {
            require(id > 0)
            return "https://covers.openlibrary.org/b/id/$id-${if (large) "L" else "M"}.jpg?default=false"
        }
        fun parseSearch(body: String): List<CoverCandidate> {
            val docs = Json.parseToJsonElement(body).jsonObject["docs"]?.jsonArray ?: return emptyList()
            return docs.mapNotNull { element ->
                val doc = element as? JsonObject ?: return@mapNotNull null
                val coverId = (doc["cover_i"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 } ?: return@mapNotNull null
                val title = (doc["title"] as? JsonPrimitive)?.contentOrNull?.take(300)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val author = (doc["author_name"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }?.joinToString(", ")?.take(300).orEmpty()
                CoverCandidate(coverId, title, author, (doc["first_publish_year"] as? JsonPrimitive)?.intOrNull)
            }.distinctBy { it.coverId }.take(12)
        }
        private fun request(address: String, maxBytes: Int): ByteArray {
            var url = URL(address)
            repeat(4) {
                require(url.protocol == "https" && url.host in setOf("openlibrary.org", "covers.openlibrary.org")) { "Nieprawidłowy adres okładki." }
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000; readTimeout = 20_000; instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "CrooLyyTimer/1.1 (+https://github.com/CrooLyyCheck/croolyytimer)")
                    setRequestProperty("Accept", if (url.host == "covers.openlibrary.org") "image/jpeg,image/*" else "application/json")
                }
                try {
                    when (connection.responseCode) {
                        in 300..399 -> { url = URL(url, connection.getHeaderField("Location") ?: throw IOException("Brak adresu okładki.")); return@repeat }
                        404 -> throw IOException("Ta okładka nie jest już dostępna. Wybierz inną propozycję.")
                        429 -> throw IOException("Wyszukiwarka jest zajęta. Spróbuj ponownie za chwilę.")
                        in 200..299 -> Unit
                        else -> throw IOException("Open Library jest chwilowo niedostępne. Spróbuj ponownie później.")
                    }
                    require(connection.contentLengthLong <= maxBytes) { "Pobrany plik jest za duży." }
                    return connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer); if (count < 0) break
                            require(output.size() + count <= maxBytes) { "Pobrany plik jest za duży." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                } finally { connection.disconnect() }
            }
            throw IOException("Nie udało się pobrać okładki.")
        }
    }
}
