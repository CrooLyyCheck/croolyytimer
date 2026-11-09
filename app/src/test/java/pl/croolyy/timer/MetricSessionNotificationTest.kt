package pl.croolyy.timer

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.croolyy.timer.data.ActiveSession
import pl.croolyy.timer.domain.TimeMark

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = CrooLyyApp::class)
class MetricSessionNotificationTest {
    private val app get() = ApplicationProvider.getApplicationContext<CrooLyyApp>()
    private val mark = TimeMark(1_800_000_000_000, 80_000, 1)
    private val active = ActiveSession(
        startedAt = mark.wall - 70_000, segmentElapsed = 20_000,
        checkpointElapsed = 80_000, boot = 1, accumulatedMs = 10_000,
    )
    private fun build(session: ActiveSession = active, now: TimeMark = mark) =
        app.notifications.build(session, "Książka", "Wieczorne czytanie", now)
    private fun style(notification: Notification) =
        Notification.Builder.recoverBuilder(app, notification).style as Notification.MetricStyle
    private fun value(notification: Notification) =
        style(notification).metrics.single().value as Notification.Metric.TimeDifference

    @Test fun runningTimeIsTheBodyMetricAndCriticalChipValueWithoutAHeaderClock() {
        val notification = build()
        val style = style(notification)
        val time = value(notification)
        assertTrue(time.isStopwatch)
        assertFalse(time.isTimer)
        assertEquals(10_000L, time.zeroElapsedRealtime)
        assertNull(time.zeroTime)
        assertNull(time.pausedDuration)
        assertEquals(Notification.Metric.TimeDifference.FORMAT_CHRONOMETER, time.format)
        assertEquals(style.metrics.single(), style.criticalMetric)
        assertEquals("Pomiar", style.metrics.single().label)
        assertFalse(NotificationCompat.getShowWhen(notification))
        assertFalse(NotificationCompat.getUsesChronometer(notification))
        assertEquals("Książka", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Wieczorne czytanie", notification.extras.getString(Notification.EXTRA_SUB_TEXT))
        assertTrue(notification.hasPromotableCharacteristics())
        assertNull(notification.contentView)
        assertNull(notification.bigContentView)
        assertEquals(listOf("Pauza", "Otwórz"), notification.actions.map { it.title })
    }

    @Test fun pauseFreezesTheBodyTimeAndResumeExcludesThePause() {
        val paused = active.copy(running = false, accumulatedMs = 70_000)
        val later = mark.copy(wall = mark.wall + 100_000, elapsed = mark.elapsed + 100_000)
        val notification = build(paused, later)
        assertEquals(Duration.ofSeconds(70), value(notification).pausedDuration)
        assertNull(value(notification).zeroElapsedRealtime)
        assertEquals("Wstrzymano", style(notification).metrics.single().label)
        assertFalse(notification.hasPromotableCharacteristics())
        assertEquals("Wznów", notification.actions[0].title)
        val resumed = paused.copy(running = true, segmentElapsed = later.elapsed, checkpointElapsed = later.elapsed)
        val afterResume = later.copy(wall = later.wall + 5_000, elapsed = later.elapsed + 5_000)
        assertEquals(75_000L, afterResume.elapsed - value(build(resumed, afterResume)).zeroElapsedRealtime!!)
    }

    @Test fun changedBootShowsTheSavedTimeAsPausedInsteadOfStartingANewClock() {
        val notification = build(now = mark.copy(boot = 2))
        assertEquals(Duration.ofSeconds(70), value(notification).pausedDuration)
        assertFalse(notification.hasPromotableCharacteristics())
        assertEquals("Wznów", notification.actions[0].title)
    }

    @Test fun savedTimeLongerThanUptimeKeepsTheEntireDuration() {
        val resumed = active.copy(accumulatedMs = 180_000, segmentElapsed = mark.elapsed)
        val time = value(build(resumed))
        assertEquals(Instant.ofEpochMilli(mark.wall - 180_000), time.zeroTime)
        assertNull(time.zeroElapsedRealtime)
        assertNull(time.pausedDuration)
    }
}
