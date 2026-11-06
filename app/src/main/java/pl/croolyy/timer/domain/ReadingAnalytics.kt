package pl.croolyy.timer.domain

import pl.croolyy.timer.data.Book
import pl.croolyy.timer.data.ReadingSession
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToLong

data class ReadingDay(val date: LocalDate, val sessions: List<ReadingSession>) {
    val durationMs: Long get() = sessions.sumOf { it.durationMs }
    val bookCount: Int get() = sessions.map { it.bookId }.distinct().size
    val pages: Int? get() = sessions.mapNotNull { it.pages }.takeIf { it.isNotEmpty() }?.sum()
    val intensity: Int get() = when {
        sessions.isEmpty() -> 0
        durationMs < 15 * 60_000L -> 1
        durationMs < 30 * 60_000L -> 2
        durationMs < 60 * 60_000L -> 3
        else -> 4
    }
}

/** A session belongs to its local starting date. Pauses cannot be reconstructed by day. */
class ReadingAnalytics(val sessions: List<ReadingSession>, val zone: ZoneId = ZoneId.systemDefault()) {
    val byDate: Map<LocalDate, List<ReadingSession>> = sessions.groupBy {
        Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
    }
    val totalMs = sessions.sumOf { it.durationMs }
    val averageMs: Long? = if (sessions.isEmpty()) null else totalMs / sessions.size
    val longestMs: Long? = sessions.maxOfOrNull { it.durationMs }
    val knownPages: Int? = sessions.mapNotNull { it.pages }.takeIf { it.isNotEmpty() }?.sum()
    val daysWithSessions = byDate.size
    private val paced = sessions.filter { (it.pages ?: 0) > 0 && it.durationMs > 0 }
    val millisPerPage: Double? = if (paced.isEmpty()) null else
        paced.sumOf { it.durationMs }.toDouble() / paced.sumOf { it.pages!!.toLong() }
    val pagesPerHour: Double? = millisPerPage?.let { 3_600_000.0 / it }
    fun day(date: LocalDate) = ReadingDay(date, byDate[date].orEmpty().sortedBy { it.startedAt })
    fun recentDays(today: LocalDate, count: Int): List<ReadingDay> {
        require(count in 1..366)
        return (count - 1 downTo 0).map { day(today.minusDays(it.toLong())) }
    }
    fun month(month: YearMonth) = (1..month.lengthOfMonth()).map { day(month.atDay(it)) }
    fun currentStreak(today: LocalDate): Int {
        var date = if (today in byDate) today else today.minusDays(1)
        var count = 0
        while (date in byDate) { count++; date = date.minusDays(1) }
        return count
    }
    fun longestStreak(): Int {
        var previous: LocalDate? = null; var current = 0; var best = 0
        byDate.keys.sorted().forEach { date ->
            current = if (previous?.plusDays(1) == date) current + 1 else 1
            best = maxOf(best, current); previous = date
        }
        return best
    }
    fun estimatedRemainingMs(book: Book): Long? {
        val total = book.totalPages ?: return null
        val pace = millisPerPage ?: return null
        val estimate = (total - book.currentPage).coerceAtLeast(0) * pace
        return estimate.takeIf { it.isFinite() && it <= Long.MAX_VALUE }?.roundToLong()
    }
}
