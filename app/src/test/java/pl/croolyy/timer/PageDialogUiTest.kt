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
import pl.croolyy.timer.ui.CrooTheme
import pl.croolyy.timer.ui.PageDialog
import androidx.compose.runtime.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageDialogUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun zeroCanBeConfirmedInTheActualPageDialog() {
        var result: Int? = null
        compose.setContent { CrooTheme("light") { PageDialog(true, 0, onDismiss = {}) { page, _ -> result = page } } }
        compose.onNodeWithText("Numer strony").assertExists()
        compose.onNodeWithText("Rozpocznij").performClick()
        compose.runOnIdle { assertEquals(0, result) }
    }
    @Test fun oneCanBeConfirmedInTheActualPageDialog() {
        var result: Int? = null
        compose.setContent { CrooTheme("light") { PageDialog(true, 1, onDismiss = {}) { page, _ -> result = page } } }
        compose.onNodeWithText("Rozpocznij").performClick()
        compose.runOnIdle { assertEquals(1, result) }
    }
    @Test fun backwardsPageNeedsExplicitAcknowledgement() {
        var result: Pair<Int, Boolean>? = null
        compose.setContent { CrooTheme("light") { PageDialog(false, 5, 10, onDismiss = {}) { page, acknowledged -> result = page to acknowledged } } }
        compose.onNodeWithText("Zapisz sesję").performClick()
        compose.runOnIdle { assertNull(result) }
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onNodeWithText("Zapisz sesję").performClick()
        compose.runOnIdle { assertEquals(5 to true, result) }
    }
    @Test fun dialogOpenedFromAButtonAcceptsZero() {
        androidx.test.espresso.IdlingPolicies.setMasterPolicyTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        var result: Int? = null
        compose.setContent {
            var show by remember { mutableStateOf(false) }
            CrooTheme("light") {
                Button({ show = true }) { Text("Otwórz") }
                if (show) PageDialog(true, 0, onDismiss = { show = false }) { page, _ -> result = page }
            }
        }
        compose.onNodeWithText("Otwórz").performClick()
        compose.onNodeWithText("Rozpocznij").performClick()
        compose.runOnIdle { assertEquals(0, result) }
    }
}
