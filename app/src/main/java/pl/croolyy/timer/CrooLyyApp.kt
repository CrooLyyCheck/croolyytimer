package pl.croolyy.timer

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pl.croolyy.timer.backup.BackupManager
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.AndroidClock
import pl.croolyy.timer.security.AccessManager

class CrooLyyApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val clock by lazy { AndroidClock(this) }
    val database by lazy { CrooDatabase.open(this) }
    val repository by lazy { LibraryRepository(database, clock) }
    val covers by lazy { CoverStore(this) }
    val access by lazy { AccessManager(this, clock) }
    val backup by lazy { BackupManager(this, repository, covers) }
}
