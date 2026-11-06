package pl.croolyy.timer.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.combine
import pl.croolyy.timer.domain.AppClock
import pl.croolyy.timer.domain.SessionClock

class LibraryRepository(val db: CrooDatabase, private val clock: AppClock) {
    val dao = db.dao()
    val state = combine(
        combine(dao.books(), dao.timers(), dao.sessions()) { b, t, s -> Triple(b, t, s) },
        dao.general(), dao.settingsFlow(), dao.activeFlow(),
    ) { library, general, settings, active ->
        LibraryState(library.first, library.second, library.third, general, settings ?: AppSettings(), active)
    }

    suspend fun saveBook(book: Book) = db.withTransaction {
        require(book.title.isNotBlank()) { "Podaj tytuł książki." }
        require(book.title.length <= 300 && book.author.length <= 300) { "Tytuł i autor mogą mieć do 300 znaków." }
        require(book.currentPage >= 0 && (book.totalPages == null || book.totalPages > 0)) { "Nieprawidłowa liczba stron." }
        val isNew = dao.book(book.id) == null
        dao.putBook(book.copy(title = book.title.trim(), author = book.author.trim()))
        if (isNew) dao.putTimer(BookTimer(bookId = book.id))
    }
    suspend fun addTimer(bookId: String, name: String) = db.withTransaction {
        require(dao.book(bookId) != null) { "Nie znaleziono książki." }
        require(name.length <= 150) { "Nazwa timera może mieć do 150 znaków." }
        dao.putTimer(BookTimer(bookId = bookId, name = name.trim().ifBlank { "Mój czas z książką" }))
    }
    suspend fun renameTimer(timer: BookTimer, name: String) {
        require(name.isNotBlank() && name.length <= 150) { "Podaj nazwę do 150 znaków." }
        dao.putTimer(timer.copy(name = name.trim()))
    }
    suspend fun deleteBook(id: String) = db.withTransaction {
        require(dao.active()?.bookId != id) { "Najpierw zakończ aktywną sesję tej książki." }
        dao.deleteBook(id)
    }
    suspend fun deleteTimer(id: String) = db.withTransaction {
        require(dao.active()?.timerId != id) { "Najpierw zakończ aktywną sesję tego timera." }
        dao.deleteTimer(id)
    }
    suspend fun settings(settings: AppSettings) { dao.putSettings(settings) }

    suspend fun start(timerId: String?, page: Int?) = db.withTransaction {
        require(dao.active() == null) { "Masz już aktywną sesję. Najpierw ją zakończ." }
        require(page == null || page >= 0) { "Numer strony nie może być ujemny." }
        val timer = timerId?.let { requireNotNull(dao.timer(it)) { "Nie znaleziono timera." } }
        if (timer != null && (dao.settings() ?: AppSettings()).askBefore) require(page != null) { "Podaj stronę początkową." }
        val mark = clock.now()
        dao.putActive(ActiveSession(bookId = timer?.bookId, timerId = timer?.id, startedAt = mark.wall,
            segmentElapsed = mark.elapsed, checkpointElapsed = mark.elapsed, boot = mark.boot, startPage = page))
        if (timer != null && page != null) dao.book(timer.bookId)?.let { dao.putBook(it.copy(currentPage = page)) }
    }
    suspend fun checkpoint() = db.withTransaction {
        dao.active()?.let { old ->
            val mark = clock.now()
            val active = SessionClock.recover(old, mark)
            dao.putActive(if (active.running) active.copy(checkpointElapsed = mark.elapsed) else active)
        }
    }
    suspend fun pause() = db.withTransaction {
        dao.active()?.let { old ->
            val mark = clock.now()
            val a = SessionClock.recover(old, mark)
            dao.putActive(a.copy(accumulatedMs = SessionClock.duration(a, mark), running = false,
                segmentElapsed = mark.elapsed, checkpointElapsed = mark.elapsed))
        }
    }
    suspend fun resume() = db.withTransaction {
        dao.active()?.let { old ->
            val mark = clock.now()
            val active = SessionClock.recover(old, mark)
            if (!active.running) dao.putActive(active.copy(running = true, boot = mark.boot,
                segmentElapsed = mark.elapsed, checkpointElapsed = mark.elapsed))
        }
    }
    suspend fun finish(endPage: Int?, acknowledgeBackwards: Boolean = false) = db.withTransaction {
        val a = requireNotNull(dao.active()) { "Brak aktywnej sesji." }
        require(endPage == null || endPage >= 0) { "Numer strony nie może być ujemny." }
        if (a.bookId != null && (dao.settings() ?: AppSettings()).askAfter) require(endPage != null) { "Podaj stronę końcową." }
        require(acknowledgeBackwards || endPage == null || a.startPage == null || endPage >= a.startPage) {
            "Końcowa strona jest mniejsza od początkowej. Potwierdź ponowne czytanie fragmentu."
        }
        val mark = clock.now()
        val duration = SessionClock.duration(a, mark)
        if (a.bookId != null && a.timerId != null) {
            dao.putSession(ReadingSession(a.sessionId, a.bookId, a.timerId, a.startedAt, mark.wall, duration, a.startPage, endPage))
            if (endPage != null) dao.book(a.bookId)?.let { dao.putBook(it.copy(currentPage = endPage)) }
        } else dao.putGeneral(GeneralSession(a.sessionId, a.startedAt, mark.wall, duration))
        dao.clearActive()
    }
    suspend fun discard() { dao.clearActive() }
    suspend fun editSession(session: ReadingSession) = db.withTransaction {
        require(session.startPage == null || session.startPage >= 0)
        require(session.endPage == null || session.endPage >= 0)
        require(session.durationMs >= 0)
        dao.putSession(session)
        val latest = dao.allSessions().filter { it.bookId == session.bookId }.maxByOrNull { it.endedAt }
        if (latest?.id == session.id && session.endPage != null) dao.book(session.bookId)?.let {
            dao.putBook(it.copy(currentPage = session.endPage))
        }
    }
    suspend fun snapshot(): LibrarySnapshot = db.withTransaction {
        val mark = clock.now()
        val active = dao.active()?.let { it.copy(accumulatedMs = SessionClock.duration(it, mark),
            running = false, segmentElapsed = mark.elapsed, checkpointElapsed = mark.elapsed,
            boot = mark.boot, recoveredAfterReboot = true) }
        LibrarySnapshot(dao.allBooks(), dao.allTimers(), dao.allSessions(), dao.allGeneral(), dao.settings() ?: AppSettings(), active)
    }
    suspend fun replace(snapshot: LibrarySnapshot) = db.withTransaction {
        dao.clearActive(); dao.clearSessions(); dao.clearTimers(); dao.clearBooks(); dao.clearGeneral()
        snapshot.books.forEach { dao.putBook(it) }
        snapshot.timers.forEach { dao.putTimer(it) }
        snapshot.sessions.forEach { dao.putSession(it) }
        snapshot.generalSessions.forEach { dao.putGeneral(it) }
        dao.putSettings(snapshot.settings)
        val mark = clock.now()
        snapshot.active?.let { dao.putActive(it.copy(running = false, boot = mark.boot,
            segmentElapsed = mark.elapsed, checkpointElapsed = mark.elapsed, recoveredAfterReboot = true)) }
    }
}
