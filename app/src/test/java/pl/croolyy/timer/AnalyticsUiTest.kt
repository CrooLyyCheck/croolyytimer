package pl.croolyy.timer

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.croolyy.timer.data.*
import pl.croolyy.timer.ui.*
import java.io.File
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w393dp-h850dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AnalyticsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val book = Book(id = "first", title = "Sztuka uważnego życia", author = "Anna Zielińska", totalPages = 320, currentPage = 120)
    private val second = Book(id = "second", title = "Mały Książę", author = "Antoine de Saint-Exupéry")
    private val today = LocalDate.now()
    private fun session(id: String, book: Book, date: LocalDate, minutes: Int) : ReadingSession {
        val start = date.atTime(18, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ReadingSession(id, book.id, "timer-${book.id}", start, start + minutes * 60_000, minutes * 60_000L, 10, 30)
    }
    private fun screenshot(name: String) = compose.runOnIdle {
        val directory = File(requireNotNull(System.getProperty("croolyy.screenshots"))).apply { mkdirs() }
        val root = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun heatmapDateShowsItsBooksAndBookFilterUpdatesTheDay() {
        val chosen = today.withDayOfMonth(if (today.dayOfMonth == 1) 2 else 1)
        val sessions = listOf(session("one", book, chosen, 45), session("two", second, chosen, 20)) +
            (0..5).map { session("recent-$it", book, today.minusDays(it.toLong()), 15 + it * 8) }
        val state = LibraryState(books = listOf(book, second), sessions = sessions)
        var opened: String? = null
        compose.setContent { CrooTheme("light") { CalendarScreen(state, { null }, onBook = { opened = it }) } }
        screenshot("06-calendar-heatmap")
        compose.onNodeWithTag("calendar-day-$chosen").performScrollTo().performClick()
        compose.onNodeWithTag("calendar-list").performScrollToNode(hasTestTag("calendar-session-two"))
        compose.onNodeWithTag("calendar-session-two").assertExists().performClick()
        assertEquals(second.id, opened)
        compose.onNodeWithTag("calendar-list").performScrollToIndex(1)
        compose.onNodeWithText(second.title).performClick()
        compose.onNodeWithTag("calendar-list").performScrollToNode(hasTestTag("calendar-session-two"))
        compose.onNodeWithTag("calendar-session-one").assertDoesNotExist()
        compose.onNodeWithTag("calendar-session-two").assertExists()
        screenshot("07-calendar-day")
    }
    @Test fun bookGraphCanSwitchRangeAndOpensTheCalendar() {
        val sessions = (0..12).map { session("stats-$it", book, today.minusDays(it.toLong()), 10 + it * 6) }
        var opened = false
        compose.setContent { CrooTheme("light") { BookStatistics(book, sessions) { opened = true } } }
        compose.onNodeWithTag("reading-chart").assertExists()
        compose.onNodeWithText("30 dni").performClick()
        compose.onNodeWithText("Do końca około", substring = true).assertExists()
        screenshot("08-book-statistics")
        compose.onNodeWithText("Zobacz kalendarz tej książki").performClick()
        assertTrue(opened)
    }
}
