package pl.croolyy.timer.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import pl.croolyy.timer.data.*
import java.io.File
import java.io.Closeable
import java.nio.file.Files
import java.util.UUID

@Serializable data class BackupEntry(val size: Long, val sha256: String)
@Serializable data class BackupManifest(val format: String = "CrooLyyTimer", val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(), val files: Map<String, BackupEntry>)
data class VerifiedBackup(val snapshot: LibrarySnapshot, val covers: Map<String, File>, private val staging: File) : Closeable {
    override fun close() { staging.deleteRecursively() }
}

/** No extractAll: only allowlisted entries are streamed with independent expansion limits. */
class BackupArchive {
    private val json = Json { encodeDefaults = true }
    companion object {
        const val MAX_ARCHIVE_BYTES = 256L * 1024 * 1024
        const val MAX_EXPANDED_BYTES = 512L * 1024 * 1024
        const val MAX_DATA_BYTES = 32L * 1024 * 1024
        const val MAX_ENTRIES = 10_002
    }
    fun create(destination: File, snapshot: LibrarySnapshot, coverFile: (String) -> File,
        password: CharArray? = null, progress: (Float) -> Unit = {}) {
        validate(snapshot)
        val staging = Files.createTempDirectory(requireNotNull(destination.absoluteFile.parentFile).toPath(), "export-").toFile()
        try {
        val data = File(staging, "data.json").apply { writeText(json.encodeToString(snapshot)) }
        require(data.length() <= MAX_DATA_BYTES) { "Zbyt duża biblioteka." }
        val files = linkedMapOf("data.json" to data)
        snapshot.books.mapNotNull { it.cover }.distinct().forEach { name ->
            val source = coverFile(name)
            require(source.isFile && source.length() <= 10L * 1024 * 1024) { "Brak lub zbyt duża okładka: $name" }
            require(sha256File(source) + ".jpg" == name) { "Uszkodzona okładka." }
            files["covers/$name"] = source
        }
        require(files.size + 1 <= MAX_ENTRIES && files.values.sumOf { it.length() } <= MAX_EXPANDED_BYTES) { "Kopia jest zbyt duża." }
        val manifest = BackupManifest(files = files.mapValues { BackupEntry(it.value.length(), sha256File(it.value)) })
        files["manifest.json"] = File(staging, "manifest.json").apply { writeText(json.encodeToString(manifest)) }
        require(password == null || password.size >= 8) { "Hasło kopii musi mieć co najmniej 8 znaków." }
        if (destination.exists()) check(destination.delete())
        try {
            ZipFile(destination, password).use { zip ->
                files.entries.forEachIndexed { index, (name, source) ->
                    val params = ZipParameters().apply {
                        fileNameInZip = name
                        if (password != null) {
                            isEncryptFiles = true; encryptionMethod = EncryptionMethod.AES
                            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                        }
                    }
                    source.inputStream().use { zip.addStream(it, params) }
                    progress((index + 1f) / files.size)
                }
            }
            require(destination.length() <= MAX_ARCHIVE_BYTES) { "Archiwum jest zbyt duże." }
        } catch (e: Exception) { destination.delete(); throw e }
        } finally { staging.deleteRecursively() }
    }
    fun isEncrypted(file: File): Boolean = ZipFile(file).use { require(it.isValidZipFile) { "To nie jest prawidłowy ZIP." }; it.isEncrypted }
    fun read(file: File, password: CharArray? = null, progress: (Float) -> Unit = {}): VerifiedBackup {
        require(file.length() in 1..MAX_ARCHIVE_BYTES) { "Nieprawidłowy rozmiar archiwum." }
        val staging = Files.createTempDirectory(requireNotNull(file.absoluteFile.parentFile).toPath(), "verified-").toFile()
        try {
        return ZipFile(file, password).use { zip ->
            require(zip.isValidZipFile && !zip.isSplitArchive) { "Nieprawidłowe lub dzielone archiwum ZIP." }
            val headers = zip.fileHeaders
            require(headers.size in 2..MAX_ENTRIES) { "Nieprawidłowa liczba plików." }
            val names = headers.map { it.fileName }
            require(names.distinct().size == names.size) { "Powtórzone nazwy plików." }
            require(names.all { it == "manifest.json" || it == "data.json" || it.matches(Regex("covers/[a-f0-9]{64}\\.jpg")) }) {
                "Archiwum zawiera niedozwolone ścieżki."
            }
            require(names.containsAll(listOf("manifest.json", "data.json"))) { "Brak manifestu lub danych." }
            require(headers.all { !it.isDirectory && it.uncompressedSize >= 0 } &&
                headers.sumOf { it.uncompressedSize } <= MAX_EXPANDED_BYTES) { "Archiwum jest zbyt duże." }
            if (zip.isEncrypted) require(password != null && password.isNotEmpty()) { "Ta kopia wymaga hasła." }
            var expanded = 0L
            fun readEntry(name: String, limit: Long): File {
                val header = headers.first { it.fileName == name }
                require(header.uncompressedSize <= limit) { "Plik jest zbyt duży: $name" }
                val target = File(staging, name)
                check(target.canonicalPath.startsWith(staging.canonicalPath + File.separator))
                target.parentFile?.mkdirs()
                zip.getInputStream(header).use { input ->
                    target.outputStream().use { out ->
                        val buffer = ByteArray(8192); var count = 0L
                        while (true) {
                            val n = input.read(buffer); if (n < 0) break
                            count += n; expanded += n
                            require(count <= limit && expanded <= MAX_EXPANDED_BYTES) { "Przekroczono limit rozpakowania." }
                            out.write(buffer, 0, n)
                        }
                        require(count == header.uncompressedSize) { "Nieprawidłowy rozmiar pliku." }
                    }
                }
                return target
            }
            val manifest = json.decodeFromString<BackupManifest>(readEntry("manifest.json", 2L * 1024 * 1024).readText())
            require(manifest.format == "CrooLyyTimer" && manifest.version == 1) { "Nieobsługiwana wersja kopii." }
            require(manifest.files.keys == names.filter { it != "manifest.json" }.toSet()) { "Niekompletny manifest." }
            val entries = manifest.files.keys.mapIndexed { index, name ->
                val source = readEntry(name, if (name == "data.json") MAX_DATA_BYTES else 10L * 1024 * 1024)
                val expected = manifest.files.getValue(name)
                require(source.length() == expected.size && sha256File(source) == expected.sha256) { "Uszkodzony plik: $name" }
                progress((index + 1f) / manifest.files.size)
                name to source
            }.toMap()
            val snapshot = json.decodeFromString<LibrarySnapshot>(entries.getValue("data.json").readText())
            validate(snapshot)
            val covers = entries.filterKeys { it.startsWith("covers/") }.mapKeys { it.key.removePrefix("covers/") }
            require(snapshot.books.mapNotNull { it.cover }.toSet() == covers.keys) { "Niekompletne okładki." }
            covers.forEach { (name, source) -> require(sha256File(source) + ".jpg" == name) { "Uszkodzona okładka." } }
            VerifiedBackup(snapshot, covers, staging)
        }
        } catch (e: Exception) { staging.deleteRecursively(); throw e }
    }
    fun validate(s: LibrarySnapshot) {
        fun uuid(id: String) = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
        fun unique(ids: List<String>) = ids.size == ids.distinct().size && ids.all { uuid(it) }
        require(s.books.size <= 10_000 && s.timers.size <= 100_000 && s.sessions.size <= 500_000 && s.generalSessions.size <= 500_000)
        require(unique(s.books.map { it.id }) && unique(s.timers.map { it.id }) &&
            unique(s.sessions.map { it.id }) && unique(s.generalSessions.map { it.id })) { "Nieprawidłowe identyfikatory." }
        val books = s.books.associateBy { it.id }; val timers = s.timers.associateBy { it.id }
        s.books.forEach {
            require(it.title.isNotBlank() && it.title.length <= 300 && it.author.length <= 300 && it.currentPage >= 0 &&
                (it.totalPages == null || it.totalPages > 0) && (it.cover == null || it.cover.matches(Regex("[a-f0-9]{64}\\.jpg")))) { "Nieprawidłowa książka." }
        }
        s.timers.forEach { require(it.bookId in books && it.name.length <= 150) { "Nieprawidłowe przypisanie timera." } }
        s.sessions.forEach {
            require(it.bookId in books && timers[it.timerId]?.bookId == it.bookId && it.durationMs in 0..315_360_000_000L &&
                (it.startPage == null || it.startPage >= 0) && (it.endPage == null || it.endPage >= 0)) { "Nieprawidłowa sesja." }
        }
        s.generalSessions.forEach { require(it.durationMs in 0..315_360_000_000L) { "Nieprawidłowy pomiar." } }
        require(s.settings.id == 1 && s.settings.theme in listOf("light", "dark", "system")) { "Nieprawidłowe ustawienia." }
        s.active?.let {
            require(it.id == 1 && uuid(it.sessionId) && !it.running && it.accumulatedMs in 0..315_360_000_000L &&
                (it.startPage == null || it.startPage >= 0) &&
                ((it.bookId == null && it.timerId == null) || (it.bookId in books && timers[it.timerId]?.bookId == it.bookId)) &&
                s.sessions.none { session -> session.id == it.sessionId } && s.generalSessions.none { session -> session.id == it.sessionId }) { "Nieprawidłowa aktywna sesja." }
        }
    }
}
