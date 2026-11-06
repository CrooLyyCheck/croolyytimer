package pl.croolyy.timer

import org.junit.Assert.*
import org.junit.Test
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.ReadingAnalytics
import java.time.*

class ReadingAnalyticsTest {
    private val zone = ZoneId.of("Europe/Warsaw")
    private fun session(date: String, minutes: Long = 30, start: Int? = null, end: Int? = null, book: String = "a"): ReadingSession {
        val instant = LocalDateTime.parse(date).atZone(zone).toInstant().toEpochMilli()
        return ReadingSession(bookId = book, timerId = "timer-$book", startedAt = instant, endedAt = instant + minutes * 60_000, durationMs = minutes * 60_000, startPage = start, endPage = end)
    }
    @Test fun localStartDateOwnsTheSessionEvenAcrossMidnightAndDaylightSaving() {
        val before = session("2026-03-28T23:45", 60)
        val after = session("2026-03-29T03:15", 20)
        val analytics = ReadingAnalytics(listOf(after, before), zone)
        assertEquals(3_600_000L, analytics.day(LocalDate.parse("2026-03-28")).durationMs)
        assertEquals(listOf(after), analytics.day(LocalDate.parse("2026-03-29")).sessions)
        assertEquals(2, analytics.daysWithSessions)
    }
    @Test fun paceIsWeightedAndDoesNotInventMissingOrBackwardsPageCounts() {
        val analytics = ReadingAnalytics(listOf(
            session("2026-11-01T10:00", 10, 0, 10),
            session("2026-11-02T10:00", 90, 10, 40),
            session("2026-11-03T10:00", 30, null, 50),
            session("2026-11-04T10:00", 30, 50, 20),
            session("2026-11-05T10:00", 10, 20, 20),
            session("2026-11-06T10:00", 0, 20, 30),
        ), zone)
        assertEquals(150_000.0, analytics.millisPerPage!!, 0.001)
        assertEquals(24.0, analytics.pagesPerHour!!, 0.001)
        assertEquals(50, analytics.knownPages)
        assertNull(analytics.day(LocalDate.parse("2026-11-03")).pages)
        assertEquals(15_000_000L, analytics.estimatedRemainingMs(Book(title = "Test", totalPages = 150, currentPage = 50)))
        assertEquals(0L, analytics.estimatedRemainingMs(Book(title = "Ponowne wydanie", totalPages = 100, currentPage = 120)))
        assertNull(analytics.estimatedRemainingMs(Book(title = "Bez liczby stron")))
    }
    @Test fun streaksCountDatesOnceAndCanContinueFromYesterday() {
        val analytics = ReadingAnalytics(listOf(
            session("2026-10-31T10:00"), session("2026-11-01T10:00"), session("2026-11-01T18:00"),
            session("2026-11-02T10:00"), session("2026-11-05T10:00"),
        ), zone)
        assertEquals(3, analytics.longestStreak())
        assertEquals(3, analytics.currentStreak(LocalDate.parse("2026-11-03")))
        assertEquals(0, analytics.currentStreak(LocalDate.parse("2026-11-04")))
        assertEquals(1, analytics.currentStreak(LocalDate.parse("2026-11-06")))
    }
    @Test fun emptyAndLeapMonthsHaveEveryDateAndNoFabricatedMetrics() {
        val analytics = ReadingAnalytics(emptyList(), zone)
        assertEquals(29, analytics.month(YearMonth.of(2024, 2)).size)
        assertTrue(analytics.month(YearMonth.of(2024, 2)).all { it.intensity == 0 })
        assertEquals(LocalDate.parse("2026-10-31"), analytics.recentDays(LocalDate.parse("2026-11-06"), 7).first().date)
        assertNull(analytics.averageMs); assertNull(analytics.pagesPerHour); assertNull(analytics.knownPages)
        assertEquals(0, analytics.currentStreak(LocalDate.now()))
    }
}
