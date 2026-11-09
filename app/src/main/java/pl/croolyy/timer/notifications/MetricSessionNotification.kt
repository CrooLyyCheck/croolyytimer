package pl.croolyy.timer.notifications

import android.app.Notification
import android.content.Context
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.Instant
import pl.croolyy.timer.domain.TimeMark

/** System-rendered body chronometer, also used as the Live Update's status chip value. */
@RequiresApi(37)
internal object MetricSessionNotification {
    fun apply(
        context: Context,
        notification: Notification,
        name: String,
        elapsed: Long,
        running: Boolean,
        mark: TimeMark,
    ): Notification {
        val format = Notification.Metric.TimeDifference.FORMAT_CHRONOMETER
        val startElapsed = mark.elapsed - elapsed
        val value = when {
            !running -> Notification.Metric.TimeDifference.forPausedStopwatch(Duration.ofMillis(elapsed), format)
            startElapsed >= 0 -> Notification.Metric.TimeDifference.forStopwatch(startElapsed, format)
            // A resumed session may include more saved time than this boot's entire uptime.
            else -> Notification.Metric.TimeDifference.forStopwatch(Instant.ofEpochMilli(mark.wall - elapsed), format)
        }
        val style = Notification.MetricStyle()
            .addMetric(Notification.Metric(value, if (running) "Pomiar" else "Wstrzymano"))
            .setCriticalMetric(0)
        return Notification.Builder.recoverBuilder(context, notification)
            .setStyle(style)
            .setSubText(name)
            // MetricStyle owns the large body time and the chip; hide the duplicate header clock.
            .setShowWhen(false)
            .setUsesChronometer(false)
            .build()
    }
}
