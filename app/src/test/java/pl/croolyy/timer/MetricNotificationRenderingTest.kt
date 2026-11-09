package pl.croolyy.timer

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import android.widget.FrameLayout
import androidx.core.view.descendants
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock
import pl.croolyy.timer.data.ActiveSession
import pl.croolyy.timer.domain.TimeMark

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = CrooLyyApp::class, qualifiers = "w393dp-h850dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MetricNotificationRenderingTest {
    @Test fun promotedSystemTemplateRendersALargeBodyChronometer() {
        val app = ApplicationProvider.getApplicationContext<CrooLyyApp>()
        ShadowSystemClock.advanceBy(Duration.ofHours(1))
        val now = TimeMark(System.currentTimeMillis(), SystemClock.elapsedRealtime(), 1)
        val active = ActiveSession(
            startedAt = now.wall - 70_000, segmentElapsed = now.elapsed - 70_000,
            checkpointElapsed = now.elapsed, boot = 1,
        )
        val notification = app.notifications.build(active, "Mały Książę", "Czytanie wieczorem", now)
        // System UI sets this flag after promotion; exercise its public framework layout here.
        notification.flags = notification.flags or Notification.FLAG_PROMOTED_ONGOING
        val parent = FrameLayout(app).apply { setBackgroundColor(Color.WHITE) }
        val body = Notification.Builder.recoverBuilder(app, notification).createBigContentView().apply(app, parent)
        parent.addView(body)
        val width = (393 * app.resources.displayMetrics.density).toInt()
        parent.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        parent.layout(0, 0, width, parent.measuredHeight)
        val clock = parent.descendants.filterIsInstance<Chronometer>().single {
            it.visibility == View.VISIBLE && it.base == now.elapsed - 70_000
        }
        assertTrue("The body time must be large", clock.textSize / app.resources.displayMetrics.scaledDensity >= 24)
        assertTrue("Expected the elapsed time in the body: ${clock.text}", clock.text.toString().matches(Regex("0?1:10")))
        assertTrue(clock.width > 0 && clock.height > 0)
        val directory = File(requireNotNull(System.getProperty("croolyy.screenshots"))).apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(parent.width, parent.height, Bitmap.Config.ARGB_8888)
        parent.draw(Canvas(bitmap))
        File(directory, "11-notification-android17.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
