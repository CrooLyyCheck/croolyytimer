package pl.croolyy.timer

import org.junit.Assert.*
import org.junit.Test
import pl.croolyy.timer.data.ActiveSession
import pl.croolyy.timer.data.ReadingSession
import pl.croolyy.timer.domain.*

class SessionClockTest {
    private val active = ActiveSession(startedAt = 1_000_000, segmentElapsed = 1000, boot = 3, checkpointElapsed = 6000)
    @Test fun monotonicTimeSurvivesWallClockChangesAndProcessRecreation() {
        assertEquals(10_000, SessionClock.duration(active.copy(), TimeMark(0, 11000, 3)))
        assertEquals(10_000, SessionClock.duration(active, TimeMark(Long.MAX_VALUE, 11000, 3)))
    }
    @Test fun pauseDoesNotCountTime() {
        assertEquals(1200, SessionClock.duration(active.copy(running = false, accumulatedMs = 1200), TimeMark(900000, 900000, 3)))
    }
    @Test fun rebootFreezesAtLastCheckpointAndCanBeRecoveredOnlyOnce() {
        val now = TimeMark(2_000_000, 100, 4)
        val recovered = SessionClock.recover(active, now)
        assertFalse(recovered.running)
        assertTrue(recovered.recoveredAfterReboot)
        assertEquals(5000, recovered.accumulatedMs)
        assertEquals(recovered, SessionClock.recover(recovered, now.copy(elapsed = 9000)))
        assertEquals(5000, SessionClock.duration(recovered, now.copy(elapsed = 9000)))
    }
    @Test fun zeroAndOneAreValidStartPages() { assertEquals(0, validPage("0")); assertEquals(1, validPage("1")) }
    @Test fun emptyOptionalIsUnknown() { assertNull(validPage("", true)) }
    @Test(expected = IllegalArgumentException::class) fun negativePageIsRejected() { validPage("-1") }
    @Test fun statisticsDoNotInventMissingOrBackwardsPages() {
        val session = ReadingSession(bookId = "a", timerId = "b", startedAt = 0, endedAt = 1, durationMs = 1)
        assertNull(session.pages)
        assertNull(session.copy(startPage = 20, endPage = 10).pages)
        assertEquals(1, session.copy(startPage = 0, endPage = 1).pages)
    }
}
