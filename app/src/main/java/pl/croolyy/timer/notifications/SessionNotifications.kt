package pl.croolyy.timer.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pl.croolyy.timer.CrooLyyApp
import pl.croolyy.timer.MainActivity
import pl.croolyy.timer.R
import pl.croolyy.timer.data.*
import pl.croolyy.timer.domain.*
import pl.croolyy.timer.ui.duration

class SessionNotifications(private val app: CrooLyyApp) {
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val preferences = app.getSharedPreferences("session-notifications", Context.MODE_PRIVATE)
    private var observer: Job? = null
    private var dismissed: String?
        get() = preferences.getString("dismissed-session", null)
        set(value) { preferences.edit().putString("dismissed-session", value).apply() }

    fun start() {
        if (observer != null) return
        channel()
        observer = app.scope.launch {
            app.repository.state.distinctUntilChanged { old, new ->
                old.active?.copy(checkpointElapsed = 0) == new.active?.copy(checkpointElapsed = 0) &&
                    old.books == new.books && old.timers == new.timers
            }.collect { publish(it) }
        }
    }
    suspend fun refresh() { channel(); publish(app.repository.state.first()) }
    fun dismiss(sessionId: String) { dismissed = sessionId; manager.cancel(ID) }
    private fun channel() {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Aktywny stoper", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Czas aktywnej sesji i sterowanie pomiarem"
            setSound(null, null); enableVibration(false); setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        })
    }
    fun build(active: ActiveSession, title: String, name: String, mark: TimeMark): Notification {
        val elapsed = SessionClock.duration(active, mark)
        val effectiveRunning = active.running && SessionClock.sameBoot(active, mark)
        val open = PendingIntent.getActivity(app, 10, Intent(app, MainActivity::class.java).apply {
            action = OPEN; flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(action: String, request: Int) = PendingIntent.getBroadcast(app, request, Intent(app, SessionActionReceiver::class.java).apply {
            this.action = action; putExtra(SESSION_ID, active.sessionId)
            data = android.net.Uri.parse("croolyytimer://session/${active.sessionId}/$action")
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(R.drawable.ic_stopwatch)
            .setContentTitle(title)
            .setContentText(if (effectiveRunning) name else "Wstrzymano · ${duration(elapsed)}")
            .setCategory("stopwatch")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setDeleteIntent(action(DISMISS, 11))
            .setWhen(mark.wall - elapsed)
            .setShowWhen(effectiveRunning)
            .setUsesChronometer(effectiveRunning)
            .setChronometerCountDown(false)
            .addAction(R.drawable.ic_stopwatch, if (effectiveRunning) "Pauza" else "Wznów", action(TOGGLE, 12))
            .addAction(R.drawable.ic_stopwatch, "Otwórz", open)
            .apply {
                if (Build.VERSION.SDK_INT >= 36) {
                    setRequestPromotedOngoing(effectiveRunning)
                    if (!effectiveRunning) setShortCriticalText("Pauza")
                }
            }.build()
        return if (Build.VERSION.SDK_INT >= 37) {
            MetricSessionNotification.apply(app, notification, name, elapsed, effectiveRunning, mark)
        } else notification
    }
    private fun publish(state: LibraryState) {
        val active = state.active
        if (active == null || active.sessionId == dismissed || !manager.areNotificationsEnabled() ||
            (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
            manager.cancel(ID); return
        }
        val title = state.books.find { it.id == active.bookId }?.title ?: "Stoper"
        val name = state.timers.find { it.id == active.timerId }?.name ?: "Pomiar czasu"
        try { manager.notify(ID, build(active, title, name, app.clock.now())) }
        catch (_: SecurityException) { /* The user can revoke notification access at any time. */ }
    }
    companion object {
        const val ID = 2401
        const val CHANNEL = "active-reading"
        const val SESSION_ID = "session-id"
        const val OPEN = "pl.croolyy.timer.OPEN_SESSION"
        const val TOGGLE = "pl.croolyy.timer.TOGGLE_SESSION"
        const val DISMISS = "pl.croolyy.timer.DISMISS_SESSION"
    }
}

class SessionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(SessionNotifications.SESSION_ID) ?: return
        val app = context.applicationContext as CrooLyyApp
        if (intent.action == SessionNotifications.DISMISS) { app.notifications.dismiss(id); return }
        if (intent.action != SessionNotifications.TOGGLE) return
        val pending = goAsync()
        app.scope.launch {
            try {
                val active = app.repository.dao.active() ?: return@launch
                if (active.sessionId != id) return@launch
                app.access.refresh()
                app.repository.toggleFromNotification(id, app.access.state.value.unlocked)
            } catch (_: Exception) {
                // Keep the persisted session intact if a background action cannot complete.
            } finally {
                try { app.notifications.refresh() } finally { pending.finish() }
            }
        }
    }
}
