package pl.croolyy.timer.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM books ORDER BY createdAt DESC, id") fun books(): Flow<List<Book>>
    @Query("SELECT * FROM timers ORDER BY rowid") fun timers(): Flow<List<BookTimer>>
    @Query("SELECT * FROM reading_sessions ORDER BY startedAt DESC, id") fun sessions(): Flow<List<ReadingSession>>
    @Query("SELECT * FROM general_sessions ORDER BY startedAt DESC, id") fun general(): Flow<List<GeneralSession>>
    @Query("SELECT * FROM active_session WHERE id = 1") fun activeFlow(): Flow<ActiveSession?>
    @Query("SELECT * FROM app_settings WHERE id = 1") fun settingsFlow(): Flow<AppSettings?>
    @Query("SELECT * FROM books") suspend fun allBooks(): List<Book>
    @Query("SELECT * FROM timers") suspend fun allTimers(): List<BookTimer>
    @Query("SELECT * FROM reading_sessions") suspend fun allSessions(): List<ReadingSession>
    @Query("SELECT * FROM general_sessions") suspend fun allGeneral(): List<GeneralSession>
    @Query("SELECT * FROM active_session WHERE id = 1") suspend fun active(): ActiveSession?
    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun settings(): AppSettings?
    @Query("SELECT * FROM books WHERE id = :id") suspend fun book(id: String): Book?
    @Query("SELECT * FROM timers WHERE id = :id") suspend fun timer(id: String): BookTimer?
    @Upsert suspend fun putBook(book: Book)
    @Upsert suspend fun putTimer(timer: BookTimer)
    @Upsert suspend fun putSession(session: ReadingSession)
    @Upsert suspend fun putGeneral(session: GeneralSession)
    @Upsert suspend fun putActive(active: ActiveSession)
    @Upsert suspend fun putSettings(settings: AppSettings)
    @Query("DELETE FROM active_session") suspend fun clearActive()
    @Query("DELETE FROM reading_sessions") suspend fun clearSessions()
    @Query("DELETE FROM general_sessions") suspend fun clearGeneral()
    @Query("DELETE FROM timers") suspend fun clearTimers()
    @Query("DELETE FROM books") suspend fun clearBooks()
    @Query("DELETE FROM books WHERE id = :id") suspend fun deleteBook(id: String)
    @Query("DELETE FROM timers WHERE id = :id") suspend fun deleteTimer(id: String)
    @Query("DELETE FROM reading_sessions WHERE id = :id") suspend fun deleteSession(id: String)
    @Query("DELETE FROM general_sessions WHERE id = :id") suspend fun deleteGeneral(id: String)
}

@Database(entities = [Book::class, BookTimer::class, ReadingSession::class, GeneralSession::class,
    ActiveSession::class, AppSettings::class], version = 2, exportSchema = true)
abstract class CrooDatabase : RoomDatabase() {
    abstract fun dao(): LibraryDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN finished INTEGER NOT NULL DEFAULT 0")
            }
        }
        fun open(context: Context) = Room.databaseBuilder(context, CrooDatabase::class.java, "croolyytimer.db")
            .addMigrations(MIGRATION_1_2).build()
    }
}
