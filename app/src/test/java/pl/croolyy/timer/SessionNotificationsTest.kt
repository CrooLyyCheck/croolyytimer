package pl.croolyy.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.TimeMark
import pl.croolyy.timer.notifications.SessionNotifications

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35, 36], application = CrooLyyApp::class)
class SessionNotificationsTest {
    @Test fun runningUsesSystemCountUpAndPausedShowsFixedTime() {
        val app = ApplicationProvider.getApplicationContext<CrooLyyApp>()
        val mark = TimeMark(1_800_000_000_000, 80_000, 1)
        val active = ActiveSession(startedAt = mark.wall - 70_000, segmentElapsed = 20_000, checkpointElapsed = 80_000, boot = 1, accumulatedMs = 10_000)
        val running = app.notifications.build(active, "Książka", "Wieczorne czytanie", mark)
        assertTrue(NotificationCompat.getUsesChronometer(running))
        assertFalse(running.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(mark.wall - 70_000, running.`when`)
        assertTrue(running.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals(Build.VERSION.SDK_INT >= 36, NotificationCompat.isRequestPromotedOngoing(running))
        assertEquals("Pauza", running.actions[0].title)
        val paused = app.notifications.build(active.copy(running = false, accumulatedMs = 70_000), "Książka", "Czytanie", mark)
        assertFalse(NotificationCompat.getUsesChronometer(paused))
        assertFalse(NotificationCompat.isRequestPromotedOngoing(paused))
        assertEquals("Wstrzymano · 00:01:10", paused.extras.getString(Notification.EXTRA_TEXT))
        assertEquals("Wznów", paused.actions[0].title)
        val other = app.notifications.build(active.copy(sessionId = newId()), "Inna", "Czytanie", mark)
        assertNotEquals(running.actions[0].actionIntent, other.actions[0].actionIntent)
    }
    @Test fun dismissalIsRespectedForTheSessionAndANewSessionCanNotify() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<CrooLyyApp>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = app.getSystemService(NotificationManager::class.java)
        val book = Book(title = "Biblioteka")
        app.repository.saveBook(book)
        val timer = app.repository.dao.allTimers().single()
        app.repository.start(timer.id, 0)
        val first = app.repository.dao.active()!!.sessionId
        app.notifications.refresh()
        assertNotNull(shadowOf(manager).getNotification(SessionNotifications.ID))
        app.notifications.dismiss(first); app.notifications.refresh()
        assertNull(shadowOf(manager).getNotification(SessionNotifications.ID))
        app.repository.discard(); app.repository.start(timer.id, 0); app.notifications.refresh()
        assertNotNull(shadowOf(manager).getNotification(SessionNotifications.ID))
        Unit
    }
}
