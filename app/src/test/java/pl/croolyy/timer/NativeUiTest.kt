package pl.croolyy.timer

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.After
import kotlinx.coroutines.cancel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.AppClock
import pl.croolyy.timer.domain.TimeMark
import pl.croolyy.timer.ui.AppViewModel
import pl.croolyy.timer.ui.CrooApp
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = UiTestApp::class, qualifiers = "w393dp-h850dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val repository get() = (compose.activity.application as CrooLyyApp).repository
    @After fun closeAppResources() {
        compose.runOnIdle {
            compose.activity.viewModelStore.clear()
            (compose.activity.application as CrooLyyApp).scope.cancel()
        }
        (compose.activity.application as CrooLyyApp).database.close()
    }
    private fun waitFor(condition: suspend () -> Boolean) {
        try { compose.waitUntil(15000) { runBlocking { condition() } } }
        catch (e: Throwable) { throw AssertionError(runBlocking {
            "active=${repository.dao.active()}, sessions=${repository.dao.allSessions()}, settings=${repository.dao.settings()}"
        }, e) }
    }
    private fun screenshot(name: String) {
        val directory = File(requireNotNull(System.getProperty("croolyy.screenshots"))).apply { mkdirs() }
        compose.runOnIdle {
            val root = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Test fun nativeLibraryTimerHistoryAndThemeWithPagePromptsDisabled() {
        compose.setContent { CrooApp(androidx.lifecycle.viewmodel.compose.viewModel<AppViewModel>(), {}, {}) }
        compose.onNodeWithText("Przejdź do ustawień").assertExists()
        screenshot("01-timer-locked")
        compose.onNodeWithText("Ustawienia", useUnmergedTree = true).performClick()
        val before = "Wymagaj numeru strony przed rozpoczęciem stopera"
        val after = "Wymagaj numeru strony po zakończeniu stopera"
        compose.onNodeWithTag(before).performScrollTo().assertIsOn().performClick()
        waitFor { repository.dao.settings()?.askBefore == false }
        compose.onNodeWithTag(after).performScrollTo().assertIsOn().performClick()
        waitFor { repository.dao.settings()?.askAfter == false }
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
        compose.onNodeWithText("Czytaj teraz").performClick()
        waitFor { repository.dao.active() != null }
        screenshot("04-reading-timer")
        compose.onNodeWithText("Pauza").performScrollTo().performClick()
        waitFor { repository.dao.active()?.running == false }
        compose.onNodeWithText("Wznów").performScrollTo().performClick()
        waitFor { repository.dao.active()?.running == true }
        compose.onNodeWithText("Zakończ").performScrollTo().performClick()
        waitFor { repository.dao.allSessions().size == 1 }
        assertNull(runBlocking { repository.dao.allSessions().single().startPage })
        assertNull(runBlocking { repository.dao.allSessions().single().endPage })
        org.robolectric.shadows.ShadowLooper.idleMainLooper(6, java.util.concurrent.TimeUnit.SECONDS)
        compose.mainClock.advanceTimeBy(6000)
        compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Sztuka uważnego życia").performClick()
        compose.onNodeWithText("Czytaj teraz").performClick()
        waitFor { repository.dao.active() != null }
        compose.onNodeWithText("Zakończ").performScrollTo().performClick()
        waitFor { repository.dao.allSessions().size == 2 }
        org.robolectric.shadows.ShadowLooper.idleMainLooper(6, java.util.concurrent.TimeUnit.SECONDS)
        compose.mainClock.advanceTimeBy(6000)
        compose.onNodeWithText("Ustawienia", useUnmergedTree = true).performClick()
        compose.onNodeWithTag(after).performScrollTo().performClick()
        waitFor { repository.dao.settings()?.askAfter == true }
        compose.onNodeWithTag(before).assertIsOff()
        compose.onNodeWithText("Ciemny").performScrollTo().performClick()
        waitFor { repository.dao.settings()?.theme == "dark" }
        compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
        screenshot("05-library-dark")
        compose.onNodeWithText("Ustawienia", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("about-card").performScrollTo().assertExists()
        screenshot("09-settings-about")
    }
}

/** A deterministic dependency for UI tests; production always uses AndroidClock. */
class UiTestApp : CrooLyyApp() {
    override val clock = AppClock { TimeMark(1_793_952_000_000, 1_000, 1) }
}
