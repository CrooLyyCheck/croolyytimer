package pl.croolyy.timer

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.croolyy.timer.backup.*
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.*
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = android.app.Application::class)
class RepositoryBackupTest {
    private lateinit var db: CrooDatabase
    private lateinit var repository: LibraryRepository
    private lateinit var context: Context
    private var mark = TimeMark(1_700_000_000_000, 1000, 1)
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CrooDatabase::class.java).build()
        repository = LibraryRepository(db, AppClock { mark })
    }
    @After fun close() { db.close() }

    @Test fun fullRoundTripRestoresBooksTimersSessionsSettingsAndCovers() = runBlocking {
        val covers = CoverStore(context)
        val bytes = ByteArrayOutputStream().use { out ->
            Bitmap.createBitmap(12, 18, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, out); out.toByteArray()
        }
        val cover = sha256(bytes) + ".jpg"
        covers.install(cover, bytes)
        val first = Book(title = "Pierwsza książka", author = "Autorka", totalPages = 200, cover = cover)
        val second = Book(title = "Druga książka", cover = cover)
        repository.saveBook(first); repository.saveBook(second)
        repository.addTimer(first.id, "Wieczorne czytanie")
        val timers = repository.dao.allTimers()
        val a = timers.first { it.bookId == first.id }
        val b = timers.last { it.bookId == first.id }
        repository.start(a.id, 0); mark = mark.copy(elapsed = 61000); repository.finish(1)
        repository.start(b.id, 1); mark = mark.copy(elapsed = 121000); repository.finish(10)
        repository.settings(AppSettings(askBefore = false, askAfter = true, theme = "dark", keepScreenOn = true, encryptBackup = true))
        repository.start(null, null); mark = mark.copy(elapsed = 124000); repository.finish(null)
        val expected = repository.snapshot()
        val file = File.createTempFile("roundtrip", ".zip", context.cacheDir)
        val backup = BackupManager(context, repository, covers)
        backup.archive.create(file, expected, covers::file)
        repository.replace(LibrarySnapshot(emptyList(), emptyList(), emptyList(), emptyList(), AppSettings()))
        covers.file(cover).delete()
        assertTrue(repository.dao.allBooks().isEmpty())
        backup.restore(file, null) {}
        assertEquals(expected, repository.snapshot())
        assertEquals(2, repository.dao.allBooks().size)
        assertEquals(3, repository.dao.allTimers().size)
        assertEquals(2, repository.dao.allSessions().size)
        assertEquals(1, repository.dao.allGeneral().size)
        assertArrayEquals(bytes, covers.file(cover).readBytes())
        file.delete()
        Unit
    }
    @Test fun aes256RequiresCorrectPasswordAndCorruptionPreservesExistingDatabase() = runBlocking {
        repository.saveBook(Book(title = "Zachowaj mnie"))
        val snapshot = repository.snapshot()
        val file = File.createTempFile("encrypted", ".zip", context.cacheDir)
        val backup = BackupManager(context, repository, CoverStore(context))
        backup.archive.create(file, snapshot, { error("No covers") }, "test-password-123".toCharArray())
        assertTrue(backup.archive.isEncrypted(file))
        assertTrue(runCatching { backup.restore(file, "wrong".toCharArray()) {} }.isFailure)
        assertEquals(snapshot, repository.snapshot())
        backup.archive.read(file, "test-password-123".toCharArray()).use { assertEquals(snapshot, it.snapshot) }
        file.writeBytes(byteArrayOf(1, 2, 3))
        assertTrue(runCatching { backup.restore(file, null) {} }.isFailure)
        assertEquals(snapshot, repository.snapshot())
    }
    @Test fun archiveRejectsZipSlipAndBrokenRelations() = runBlocking {
        val archive = BackupArchive()
        val file = File.createTempFile("zipslip", ".zip", context.cacheDir)
        ZipOutputStream(file.outputStream()).use {
            it.putNextEntry(ZipEntry("../escape")); it.write("bad".toByteArray()); it.closeEntry()
            it.putNextEntry(ZipEntry("manifest.json")); it.write("{}".toByteArray()); it.closeEntry()
        }
        assertTrue(runCatching { archive.read(file) }.isFailure)
        val invalid = LibrarySnapshot(emptyList(), listOf(BookTimer(bookId = newId())), emptyList(), emptyList(), AppSettings())
        assertTrue(runCatching { archive.validate(invalid) }.isFailure)
    }
    @Test fun validZipWithTamperedDataFailsChecksumBeforeReplacingLibrary() = runBlocking {
        repository.saveBook(Book(title = "Oryginalna biblioteka"))
        val snapshot = repository.snapshot()
        val archive = File.createTempFile("tampered-", ".zip", context.cacheDir)
        val manager = BackupManager(context, repository, CoverStore(context))
        manager.archive.create(archive, snapshot, { error("No covers") })
        net.lingala.zip4j.ZipFile(archive).use { zip ->
            val params = net.lingala.zip4j.model.ZipParameters().apply { fileNameInZip = "data.json" }
            "{}".byteInputStream().use { zip.addStream(it, params) }
        }
        assertTrue(runCatching { manager.restore(archive, null) {} }.isFailure)
        assertEquals(snapshot, repository.snapshot())
    }
    @Test fun onlyOneTimerCanRunAndPauseResumeCountsExactly() = runBlocking {
        val book = Book(title = "Czas")
        repository.saveBook(book)
        val timer = repository.dao.allTimers().single()
        repository.start(timer.id, 0)
        assertTrue(runCatching { repository.start(timer.id, 1) }.isFailure)
        mark = mark.copy(elapsed = 11000); repository.pause()
        mark = mark.copy(elapsed = 99000); repository.resume()
        mark = mark.copy(elapsed = 104000); repository.finish(1)
        assertEquals(15000, repository.dao.allSessions().single().durationMs)
    }
    @Test fun startAndFinishRequirementsAreIndependentAndUnknownPagesStayNull() = runBlocking {
        val book = Book(title = "Niezależne pytania")
        repository.saveBook(book); val timer = repository.dao.allTimers().single()
        for ((before, after) in listOf(true to true, false to false, true to false, false to true)) {
            repository.settings(AppSettings(askBefore = before, askAfter = after))
            if (before) assertTrue(runCatching { repository.start(timer.id, null) }.isFailure)
            repository.start(timer.id, if (before) 0 else null)
            if (after) assertTrue(runCatching { repository.finish(null) }.isFailure)
            repository.finish(if (after) 1 else null)
            val saved = repository.dao.allSessions().last()
            assertEquals(if (before) 0 else null, saved.startPage)
            assertEquals(if (after) 1 else null, saved.endPage)
        }
    }
    @Test fun persistedActiveSessionSurvivesRepositoryRecreationAndRebootIsPaused() = runBlocking {
        repository.start(null, null)
        mark = mark.copy(elapsed = 11000); repository.checkpoint()
        val recreated = LibraryRepository(db, AppClock { mark })
        mark = mark.copy(elapsed = 16000)
        assertEquals(15000, SessionClock.duration(recreated.dao.active()!!, mark))
        mark = mark.copy(elapsed = 100, boot = 2)
        recreated.checkpoint()
        assertFalse(recreated.dao.active()!!.running)
        assertEquals(10000, recreated.dao.active()!!.accumulatedMs)
    }
}
