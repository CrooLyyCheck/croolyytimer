package pl.croolyy.timer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.croolyy.timer.ui.*
import pl.croolyy.timer.update.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UpdateUiTest {
    @get:Rule val compose = createComposeRule()
    private val release = AppRelease(4, "1.2.1", "v1.2.1", "CrooLyyTimer-1.2.1-debug.apk", "a".repeat(64), 10, 26, setOf("b".repeat(64)))
    @Test fun availableVersionCanBeDownloadedAndActiveReadingBlocksInstallation() {
        var downloaded = false
        compose.setContent { CrooTheme("light") { UpdateCard(UpdateState(release = release), false, false, {}, { downloaded = true }, {}, {}, {}, {}) } }
        compose.onNodeWithText("Nowa wersja 1.2.1").assertExists()
        compose.onNodeWithText("Pobierz aktualizację").performClick()
        assertTrue(downloaded)
    }
    @Test fun preparedUpdateCannotInterruptASession() {
        compose.setContent { CrooTheme("light") { UpdateCard(UpdateState(release = release, ready = true), true, false, {}, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("Zainstaluj aktualizację").assertIsNotEnabled()
        compose.onNodeWithText("Zakończ aktywną sesję, aby zainstalować aktualizację.").assertExists()
    }
}
