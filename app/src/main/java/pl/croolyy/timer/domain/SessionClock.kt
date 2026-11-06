package pl.croolyy.timer.domain

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import pl.croolyy.timer.data.ActiveSession

data class TimeMark(val wall: Long, val elapsed: Long, val boot: Int)
fun interface AppClock { fun now(): TimeMark }
class AndroidClock(private val context: Context) : AppClock {
    override fun now() = TimeMark(System.currentTimeMillis(), SystemClock.elapsedRealtime(),
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1))
}

object SessionClock {
    fun sameBoot(active: ActiveSession, now: TimeMark) = active.boot == now.boot && now.elapsed >= active.segmentElapsed
    fun duration(active: ActiveSession, now: TimeMark): Long = active.accumulatedMs + when {
        !active.running -> 0L
        sameBoot(active, now) -> (now.elapsed - active.segmentElapsed).coerceAtLeast(0)
        else -> (active.checkpointElapsed - active.segmentElapsed).coerceAtLeast(0)
    }
    fun recover(active: ActiveSession, now: TimeMark): ActiveSession =
        if (!sameBoot(active, now)) active.copy(
            accumulatedMs = duration(active, now), running = false, boot = now.boot,
            segmentElapsed = now.elapsed, checkpointElapsed = now.elapsed, recoveredAfterReboot = true,
        ) else active
}

fun validPage(text: String, optional: Boolean = false): Int? {
    if (optional && text.isBlank()) return null
    return text.toIntOrNull()?.takeIf { it >= 0 }
        ?: throw IllegalArgumentException("Wpisz numer strony: 0 lub większy.")
}
