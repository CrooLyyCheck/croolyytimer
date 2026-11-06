package pl.croolyy.timer.backup

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.croolyy.timer.data.CoverStore
import pl.croolyy.timer.data.LibraryRepository
import java.io.File

private val Context.backupMetadata by preferencesDataStore("backup_metadata")
class BackupManager(private val context: Context, private val repository: LibraryRepository, private val covers: CoverStore) {
    val archive = BackupArchive()
    private val mutex = Mutex()
    private val lastKey = longPreferencesKey("last_backup")
    val lastBackup = context.backupMetadata.data.map { it[lastKey] ?: 0 }
    suspend fun markBackup() { context.backupMetadata.edit { it[lastKey] = System.currentTimeMillis() } }
    suspend fun create(password: CharArray?, progress: (Float) -> Unit): File = mutex.withLock {
        withContext(Dispatchers.IO) {
            val file = File.createTempFile("CrooLyyTimer-", ".zip", context.cacheDir)
            try { archive.create(file, repository.snapshot(), covers::file, password, progress); file }
            catch (e: Exception) { file.delete(); throw e }
        }
    }
    suspend fun export(uri: Uri, password: CharArray?, progress: (Float) -> Unit) {
        val file = create(password, progress)
        try {
            withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri, "wt"))
                .use { out -> file.inputStream().use { it.copyTo(out) } } }
            markBackup()
        } finally { file.delete(); password?.fill('\u0000') }
    }
    suspend fun stage(uri: Uri): File = withContext(Dispatchers.IO) {
        val file = File.createTempFile("restore-", ".zip", context.cacheDir)
        try {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { input -> file.outputStream().use { out ->
                val buffer = ByteArray(8192); var size = 0L
                while (true) { val n = input.read(buffer); if (n < 0) break; size += n
                    require(size <= BackupArchive.MAX_ARCHIVE_BYTES) { "Kopia może mieć najwyżej 256 MB." }; out.write(buffer, 0, n) }
            } }
            archive.isEncrypted(file)
            file
        } catch (e: Exception) { file.delete(); throw e }
    }
    suspend fun restore(file: File, password: CharArray?, progress: (Float) -> Unit) = mutex.withLock {
        try {
            withContext(Dispatchers.IO) {
                archive.read(file, password) { progress(it * .8f) }.use { backup ->
                // Immutable content-addressed covers are installed before the DB commit.
                // Existing files remain valid on any failure or process death.
                backup.covers.forEach { (name, source) -> covers.install(name, source.readBytes()) }
                progress(.9f)
                repository.replace(backup.snapshot)
                progress(1f)
                }
            }
        } finally { password?.fill('\u0000') }
    }
}
