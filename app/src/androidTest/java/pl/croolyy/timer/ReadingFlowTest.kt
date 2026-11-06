package pl.croolyy.timer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repository get() = (compose.activity.application as CrooLyyApp).repository
    private fun waitFor(condition: suspend () -> Boolean) = compose.waitUntil(15000) { runBlocking { condition() } }

    @Test fun firstRunLockedAndTwoBookTimersCanReadFromPagesZeroAndOne() {
        compose.onNodeWithText("Przejdź do ustawień").assertExists()
        compose.onNodeWithText("Książki", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Dodaj pierwszą książkę").performScrollTo().performClick()
        compose.onNodeWithText("Tytuł książki *").performTextInput("Książka testowa")
        compose.onNodeWithText("Autor (opcjonalnie)").performTextInput("Testowa Autorka")
        compose.onNodeWithText("Liczba stron (opcjonalnie)").performTextInput("100")
        compose.onNodeWithText("Zapisz książkę").performScrollTo().performClick()
        waitFor { repository.dao.allBooks().isNotEmpty() }
        compose.onNodeWithText("Książka testowa").performClick()
        compose.onNodeWithContentDescription("Dodaj timer").performScrollTo().performClick()
        compose.onNodeWithText("Nazwa (opcjonalnie)").performTextInput("Wieczór")
        compose.onNodeWithText("Zapisz", useUnmergedTree = true).performClick()
        waitFor { repository.dao.allTimers().size == 2 }
        compose.onAllNodesWithText("Rozpocznij czytanie")[0].performScrollTo().performClick()
        compose.onNodeWithText("Numer strony").performTextClearance()
        compose.onNodeWithText("Numer strony").performTextInput("0")
        compose.onNodeWithText("Rozpocznij", useUnmergedTree = true).performClick()
        waitFor { repository.dao.active()?.startPage == 0 }
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
        compose.onNodeWithText("Książka testowa").performClick()
        compose.onAllNodesWithText("Rozpocznij czytanie")[0].performScrollTo().performClick()
        compose.onNodeWithText("Numer strony").performTextClearance()
        compose.onNodeWithText("Numer strony").performTextInput("1")
        compose.onNodeWithText("Rozpocznij", useUnmergedTree = true).performClick()
        waitFor { repository.dao.active()?.startPage == 1 }
        compose.activityRule.scenario.recreate()
        waitFor { repository.dao.active()?.startPage == 1 }
        compose.onNodeWithText("Zakończ").performScrollTo().performClick()
        compose.onNodeWithText("Zapisz sesję").performClick()
        waitFor { repository.dao.allSessions().size == 2 }
        compose.onNodeWithText("Ustawienia", useUnmergedTree = true).performClick()
        val before = "Wymagaj numeru strony przed rozpoczęciem timera"
        val after = "Wymagaj numeru strony po zakończeniu timera"
        compose.onNodeWithTag(before).performScrollTo().assertIsOn().performClick()
        waitFor { repository.dao.settings()?.askBefore == false }
        compose.onNodeWithTag(after).assertIsOn()
        compose.onNodeWithTag(before).assertIsOff()
    }
}
