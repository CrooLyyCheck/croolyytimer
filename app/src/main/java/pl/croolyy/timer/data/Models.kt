package pl.croolyy.timer.data

import androidx.room.*
import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String = newId(),
    val title: String,
    val author: String = "",
    val totalPages: Int? = null,
    val currentPage: Int = 0,
    val cover: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val finished: Boolean = false,
)

@Serializable
@Entity(tableName = "timers", foreignKeys = [ForeignKey(
    entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("bookId")])
data class BookTimer(@PrimaryKey val id: String = newId(), val bookId: String, val name: String = "Mój czas z książką")

@Serializable
@Entity(tableName = "reading_sessions", foreignKeys = [
    ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = BookTimer::class, parentColumns = ["id"], childColumns = ["timerId"], onDelete = ForeignKey.CASCADE),
], indices = [Index("bookId"), Index("timerId")])
data class ReadingSession(
    @PrimaryKey val id: String = newId(), val bookId: String, val timerId: String,
    val startedAt: Long, val endedAt: Long, val durationMs: Long,
    val startPage: Int? = null, val endPage: Int? = null,
) {
    val pages: Int? get() = if (startPage != null && endPage != null && endPage >= startPage) endPage - startPage else null
}

@Serializable
@Entity(tableName = "general_sessions")
data class GeneralSession(@PrimaryKey val id: String = newId(), val startedAt: Long, val endedAt: Long, val durationMs: Long)

/** Exactly one row (id = 1), shared by both timer types. Every transition is transactional. */
@Serializable
@Entity(tableName = "active_session")
data class ActiveSession(
    @PrimaryKey val id: Int = 1,
    val sessionId: String = newId(),
    val bookId: String? = null, val timerId: String? = null,
    val startedAt: Long,
    val segmentElapsed: Long,
    val boot: Int,
    val accumulatedMs: Long = 0,
    val checkpointElapsed: Long,
    val running: Boolean = true,
    val startPage: Int? = null,
    val recoveredAfterReboot: Boolean = false,
)

@Serializable
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val askBefore: Boolean = true,
    val askAfter: Boolean = true,
    val theme: String = "system",
    val keepScreenOn: Boolean = false,
    val encryptBackup: Boolean = false,
)

@Serializable
data class LibrarySnapshot(
    val books: List<Book>, val timers: List<BookTimer>, val sessions: List<ReadingSession>,
    val generalSessions: List<GeneralSession>, val settings: AppSettings,
    val active: ActiveSession? = null,
)

data class LibraryState(
    val books: List<Book> = emptyList(), val timers: List<BookTimer> = emptyList(),
    val sessions: List<ReadingSession> = emptyList(), val generalSessions: List<GeneralSession> = emptyList(),
    val settings: AppSettings = AppSettings(), val active: ActiveSession? = null,
)
