package pl.croolyy.timer

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.croolyy.timer.data.*
import java.io.File

/** Uses actual native screens and disposable fixtures, never bundled sample user data. */
@RunWith(AndroidJUnit4::class)
class ProductScreenshotsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun captureLightDarkLibraryBookAndActiveSession() {
        val app = compose.activity.application as CrooLyyApp
        val empty = LibrarySnapshot(emptyList(), emptyList(), emptyList(), emptyList(), AppSettings())
        runBlocking { app.repository.replace(empty) }
        fun capture(name: String) {
            compose.waitForIdle()
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val directory = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            capture("01-timer-locked")
            val first = Book(id = "31f55804-e420-4bba-87b7-502d9e4b1848", title = "Sztuka uważnego życia", author = "Anna Zielińska", totalPages = 320, currentPage = 126)
            val second = Book(id = "70471a98-f49f-4c8b-abd2-026967e3ae93", title = "Między drzewami", author = "Michał Nowak", totalPages = 248, currentPage = 43)
            runBlocking {
                app.repository.saveBook(second); app.repository.saveBook(first)
                app.repository.addTimer(first.id, "Wieczorne czytanie")
                val timer = app.repository.dao.allTimers().first { it.bookId == first.id }
                app.repository.dao.putSession(ReadingSession(bookId = first.id, timerId = timer.id,
                    startedAt = System.currentTimeMillis() - 7_200_000, endedAt = System.currentTimeMillis() - 5_400_000,
                    durationMs = 1_800_000, startPage = 100, endPage = 126))
            }
            compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
            compose.onNodeWithText(first.title).assertExists()
            capture("02-library-light")
            compose.onNodeWithText(first.title).performClick()
            capture("03-book-detail")
            compose.onNodeWithText("Czytaj teraz").performScrollTo().performClick()
            compose.onNodeWithText("Rozpocznij", useUnmergedTree = true).performClick()
            compose.waitUntil(10000) { runBlocking { app.repository.dao.active() != null } }
            runBlocking {
                app.repository.dao.active()?.let { app.repository.dao.putActive(it.copy(accumulatedMs = 1_574_000)) }
            }
            capture("04-reading-timer")
            compose.onNodeWithText("Ustawienia", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Ciemny").performScrollTo().performClick()
            compose.waitUntil(10000) { runBlocking { app.repository.dao.settings()?.theme == "dark" } }
            compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
            capture("05-library-dark")
        } finally { runBlocking { app.repository.replace(empty) } }
    }
}
