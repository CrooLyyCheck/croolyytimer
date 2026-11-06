package pl.croolyy.timer

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.croolyy.timer.data.*
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = CrooLyyApp::class, qualifiers = "w393dp-h850dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repository get() = (compose.activity.application as CrooLyyApp).repository
    private fun waitFor(condition: suspend () -> Boolean) = compose.waitUntil(15000) { runBlocking { condition() } }
    private fun screenshot(name: String) {
        val directory = File(System.getProperty("croolyy.screenshots")).apply { mkdirs() }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun nativeScreensReadFromZeroAndOneSaveHistoryAndSwitchTheme() {
        compose.onNodeWithText("Przejdź do ustawień").assertExists()
        screenshot("01-timer-locked")
        compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Dodaj pierwszą książkę").performScrollTo().performClick()
        compose.onNodeWithText("Tytuł książki *").performTextInput("Sztuka uważnego życia")
        compose.onNodeWithText("Autor (opcjonalnie)").performTextInput("Anna Zielińska")
        compose.onNodeWithText("Liczba stron (opcjonalnie)").performTextInput("320")
        compose.onNodeWithText("Zapisz książkę").performScrollTo().performClick()
        waitFor { repository.dao.allBooks().size == 1 }
        screenshot("02-library-light")
        compose.onNodeWithText("Sztuka uważnego życia").performClick()
        screenshot("03-book-detail")
        compose.onNodeWithText("Rozpocznij czytanie").performScrollTo().performClick()
        compose.onNodeWithText("Numer strony").performTextClearance()
        compose.onNodeWithText("Numer strony").performTextInput("0")
        compose.onNodeWithText("Rozpocznij", useUnmergedTree = true).performClick()
        waitFor { repository.dao.active()?.startPage == 0 }
        screenshot("04-reading-timer")
        compose.onNodeWithText("Pauza").performScrollTo().performClick()
        waitFor { repository.dao.active()?.running == false }
        compose.onNodeWithText("Wznów").performScrollTo().performClick()
        waitFor { repository.dao.active()?.running == true }
        compose.onNodeWithText("Zakończ").performScrollTo().performClick()
        compose.onNodeWithText("Numer strony").performTextClearance()
        compose.onNodeWithText("Numer strony").performTextInput("1")
        compose.onNodeWithText("Zapisz sesję").performClick()
        waitFor { repository.dao.allSessions().size == 1 }
        assertEquals(0, runBlocking { repository.dao.allSessions().single().startPage })
        assertEquals(1, runBlocking { repository.dao.allSessions().single().endPage })
        compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Sztuka uważnego życia").performClick()
        compose.onNodeWithText("Rozpocznij czytanie").performScrollTo().performClick()
        compose.onNodeWithText("Numer strony").performTextClearance()
        compose.onNodeWithText("Numer strony").performTextInput("1")
        compose.onNodeWithText("Rozpocznij", useUnmergedTree = true).performClick()
        waitFor { repository.dao.active()?.startPage == 1 }
        compose.onNodeWithText("Zakończ").performScrollTo().performClick()
        compose.onNodeWithText("Zapisz sesję").performClick()
        waitFor { repository.dao.allSessions().size == 2 }
        compose.onNodeWithText("Ustawienia", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("Wymagaj numeru strony przed rozpoczęciem timera").performScrollTo().performClick()
        waitFor { repository.dao.settings()?.askBefore == false }
        compose.onNodeWithTag("Wymagaj numeru strony po zakończeniu timera").assertIsOn()
        compose.onNodeWithText("Ciemny").performScrollTo().performClick()
        waitFor { repository.dao.settings()?.theme == "dark" }
        compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
        screenshot("05-library-dark")
    }
}
