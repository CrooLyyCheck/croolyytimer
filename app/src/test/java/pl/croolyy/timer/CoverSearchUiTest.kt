package pl.croolyy.timer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.croolyy.timer.data.*
import pl.croolyy.timer.ui.*
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w393dp-h850dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverSearchUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun searchAndChooseDownloadsTheSelectedLargeCover() {
        val image = ByteArrayOutputStream().use { output ->
            Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(36, 77, 62)) }
                .compress(Bitmap.CompressFormat.JPEG, 90, output)
            output.toByteArray()
        }
        val client = OpenLibraryClient(compose.activity.cacheDir) { url, _ ->
            if (url.contains("search.json")) """{"docs":[
                {"title":"Mały Książę","author_name":["Antoine de Saint-Exupéry"],"cover_i":123,"first_publish_year":1943},
                {"title":"Mały Książę — inne wydanie","cover_i":456,"first_publish_year":2016}
            ]}""".toByteArray() else image
        }
        var chosen: File? = null
        compose.setContent { CrooTheme("light") { CoverSearchDialog("Mały Książę", client, {}, { chosen = it }) } }
        compose.onNodeWithText("Szukaj okładek").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("cover-candidate-123").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("cover-candidate-456").assertExists()
        compose.onNodeWithTag("cover-candidate-123").performClick()
        compose.waitUntil(15_000) { chosen != null }
        val downloaded = requireNotNull(chosen)
        assertTrue(downloaded.name.endsWith("-L.jpg"))
        assertArrayEquals(image, downloaded.readBytes())
    }
}
