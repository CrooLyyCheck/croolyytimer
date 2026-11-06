package pl.croolyy.timer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = android.app.Application::class)
class PersistenceTest {
    @Test fun closeAndReopenDatabaseKeepsActiveSessionAndIndependentPreferences() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "durability-${newId()}.db"
        var mark = TimeMark(1_700_000_000_000, 500, 1)
        fun open() = Room.databaseBuilder(context, CrooDatabase::class.java, name).addMigrations(CrooDatabase.MIGRATION_1_2).build()
        var database = open()
        try {
            val repository = LibraryRepository(database, AppClock { mark })
            repository.saveBook(Book(title = "Trwała biblioteka"))
            repository.settings(AppSettings(askBefore = false, askAfter = true, theme = "dark"))
            repository.start(repository.dao.allTimers().single().id, null)
            val id = repository.dao.active()!!.sessionId
            mark = mark.copy(elapsed = 5500); repository.checkpoint()
            database.close(); database = open()
            val recovered = LibraryRepository(database, AppClock { mark })
            mark = mark.copy(elapsed = 10500)
            assertEquals(id, recovered.dao.active()!!.sessionId)
            assertEquals(10000, SessionClock.duration(recovered.dao.active()!!, mark))
            assertEquals(AppSettings(askBefore = false, askAfter = true, theme = "dark"), recovered.dao.settings())
            recovered.finish(1)
            assertEquals(1, recovered.dao.allSessions().size)
            assertNull(recovered.dao.active())
        } finally { database.close(); context.deleteDatabase(name) }
    }
    @Test @Config(sdk = [26]) fun selectedImageIsCopiedAndSurvivesSourceRemoval() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File.createTempFile("gallery-", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        val covers = CoverStore(context)
        val imported = covers.import(Uri.fromFile(source))
        source.delete()
        assertTrue(covers.file(imported).isFile)
        assertEquals(imported, sha256File(covers.file(imported)) + ".jpg")
        val oversized = File.createTempFile("oversized-", ".jpg", context.cacheDir)
        java.io.RandomAccessFile(oversized, "rw").use { it.setLength(30L * 1024 * 1024 + 1) }
        assertTrue(runCatching { covers.import(Uri.fromFile(oversized)) }.isFailure)
        oversized.delete()
        assertTrue(covers.file(imported).isFile)
    }
    @Test @Config(sdk = [26]) fun downloadedInternetCoverSurvivesRemovalOfTheNetworkCache() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = java.io.ByteArrayOutputStream().use { output ->
            Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, output)
            output.toByteArray()
        }
        val client = OpenLibraryClient(context.cacheDir) { _, _ -> bytes }
        val downloaded = client.image(CoverCandidate(9988, "Książka", "Autor", null), large = true)
        val covers = CoverStore(context)
        val imported = covers.import(Uri.fromFile(downloaded))
        downloaded.delete()
        assertTrue(covers.file(imported).isFile)
        assertEquals(imported, sha256File(covers.file(imported)) + ".jpg")
    }
}
