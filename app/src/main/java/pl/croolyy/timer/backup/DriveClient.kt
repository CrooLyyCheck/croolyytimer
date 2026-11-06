package pl.croolyy.timer.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Serializable data class DriveBackup(val id: String, val name: String, val createdTime: String = "", val size: String = "0")
private val driveJson = Json { ignoreUnknownKeys = true }
class DriveAuthorizationExpired : Exception("Autoryzacja Google wygasła. Połącz konto ponownie.")

/** Real Drive v3 appDataFolder API; access tokens stay only in memory. */
class DriveClient(private val token: String) {
    companion object { const val SCOPE = "https://www.googleapis.com/auth/drive.appdata" }
    private fun connection(url: String, method: String = "GET"): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15_000; readTimeout = 60_000
            instanceFollowRedirects = false
            setRequestProperty("Authorization", "Bearer $token")
        }
    private fun checked(c: HttpURLConnection) {
        if (c.responseCode == 401) throw DriveAuthorizationExpired()
        require(c.responseCode in 200..299) {
            when (c.responseCode) {
                403 -> "Google Drive odmówił dostępu. Sprawdź zgodę i konfigurację OAuth."
                404 -> "Kopia nie istnieje na tym koncie."
                429 -> "Zbyt wiele żądań. Spróbuj za chwilę."
                else -> "Google Drive: błąd ${c.responseCode}. Spróbuj ponownie."
            }
        }
    }
    private fun jsonResponse(c: HttpURLConnection): JsonObject {
        checked(c)
        val bytes = c.inputStream.use { input -> java.io.ByteArrayOutputStream().use { out ->
            val buffer = ByteArray(8192); var total = 0
            while (true) { val n = input.read(buffer); if (n < 0) break
                total += n; require(total <= 2 * 1024 * 1024) { "Zbyt duża odpowiedź Google Drive." }
                out.write(buffer, 0, n)
            }
            out.toByteArray()
        } }
        return driveJson.parseToJsonElement(bytes.decodeToString()).jsonObject
    }
    suspend fun list(): List<DriveBackup> = withContext(Dispatchers.IO) {
        val result = mutableListOf<DriveBackup>(); var next: String? = null
        do {
            val endpoint = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=nextPageToken,files(id,name,createdTime,size)&orderBy=createdTime%20desc&pageSize=100" +
                (next?.let { "&pageToken=" + URLEncoder.encode(it, "UTF-8") } ?: "")
            val c = connection(endpoint)
            try {
                val json = jsonResponse(c)
                json["files"]?.jsonArray?.forEach { result += driveJson.decodeFromJsonElement<DriveBackup>(it) }
                next = json["nextPageToken"]?.jsonPrimitive?.contentOrNull
                require(result.size <= 10_000) { "Zbyt wiele kopii na koncie." }
            } finally { c.disconnect() }
        } while (next != null)
        result
    }
    suspend fun upload(file: File, name: String, progress: (Float) -> Unit): String = withContext(Dispatchers.IO) {
        require(file.length() in 1..BackupArchive.MAX_ARCHIVE_BYTES)
        val create = connection("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=id", "POST")
        val location: String
        try {
            create.doOutput = true
            create.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            create.setRequestProperty("X-Upload-Content-Type", "application/zip")
            create.setRequestProperty("X-Upload-Content-Length", file.length().toString())
            val metadata = buildJsonObject {
                put("name", name); put("mimeType", "application/zip")
                putJsonArray("parents") { add("appDataFolder") }
            }.toString().encodeToByteArray()
            create.setFixedLengthStreamingMode(metadata.size)
            create.outputStream.use { it.write(metadata) }; checked(create)
            location = requireNotNull(create.getHeaderField("Location")) { "Brak adresu wysyłania kopii." }
            val url = URL(location)
            require(url.protocol == "https" && url.host == "www.googleapis.com") { "Nieprawidłowy adres Google Drive." }
        } finally { create.disconnect() }
        val upload = connection(location, "PUT")
        try {
            upload.doOutput = true; upload.setFixedLengthStreamingMode(file.length())
            upload.setRequestProperty("Content-Type", "application/zip")
            upload.outputStream.use { out -> file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024); var sent = 0L
                while (true) { val n = input.read(buffer); if (n < 0) break; out.write(buffer, 0, n)
                    sent += n; progress(sent.toFloat() / file.length()) }
            } }
            jsonResponse(upload).getValue("id").jsonPrimitive.content
        } finally { upload.disconnect() }
    }
    private fun safeId(id: String): String { require(id.matches(Regex("[A-Za-z0-9_-]{1,200}"))); return id }
    suspend fun download(id: String, destination: File, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val c = connection("https://www.googleapis.com/drive/v3/files/${safeId(id)}?alt=media")
        try {
            checked(c)
            val length = c.contentLengthLong
            require(length <= BackupArchive.MAX_ARCHIVE_BYTES) { "Kopia jest zbyt duża." }
            c.inputStream.use { input -> destination.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024); var total = 0L
                while (true) { val n = input.read(buffer); if (n < 0) break; total += n
                    require(total <= BackupArchive.MAX_ARCHIVE_BYTES) { "Kopia jest zbyt duża." }
                    out.write(buffer, 0, n); if (length > 0) progress(total.toFloat() / length) }
            } }
        } catch (e: Exception) { destination.delete(); throw e }
        finally { c.disconnect() }
    }
    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        val c = connection("https://www.googleapis.com/drive/v3/files/${safeId(id)}", "DELETE")
        try { checked(c) } finally { c.disconnect() }
    }
}
