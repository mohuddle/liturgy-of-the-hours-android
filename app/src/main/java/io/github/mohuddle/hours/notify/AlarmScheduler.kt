package io.github.mohuddle.hours.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mohuddle.hours.MainActivity
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.data.hoursStore
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.domain.OfficeModel
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ScheduledAlarm(
    val hourId: String,
    val at: LocalDateTime,
    val date: String,
)

interface AlarmClock {
    fun cancel(hourId: String)
    fun setAlarmClock(hourId: String, at: LocalDateTime, date: String)
}

class AlarmScheduler(private val clock: AlarmClock) {
    fun reschedule(now: LocalDateTime, snapshot: HoursSnapshot) {
        for (hour in OfficeModel.HOURS) {
            clock.cancel(hour.id)
        }
        if (!snapshot.notificationsEnabled) return
        for (hour in snapshot.hours.filter { it.enabled }) {
            val next = nextOccurrence(now, hour)
            clock.setAlarmClock(next.hourId, next.at, next.date)
        }
    }

    companion object {
        const val EXTRA_HOUR_ID = "hourId"
        const val EXTRA_DATE = "date"
        const val ACTION_HOUR = "io.github.mohuddle.hours.HOUR_ALARM"

        private val SYSTEM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )

        fun nextOccurrence(now: LocalDateTime, hour: Hour): ScheduledAlarm {
            val minutes = OfficeModel.minutesFromHm(hour.time)
            val todayAt = now.toLocalDate().atTime(minutes / 60, minutes % 60)
            val at = if (!now.isBefore(todayAt)) todayAt.plusDays(1) else todayAt
            return ScheduledAlarm(hour.id, at, OfficeModel.isoDate(at))
        }

        fun handleSystemEvent(
            action: String?,
            scheduler: AlarmScheduler,
            now: LocalDateTime,
            snapshot: HoursSnapshot,
        ): Boolean {
            if (action !in SYSTEM_ACTIONS) return false
            scheduler.reschedule(now, snapshot)
            return true
        }

        suspend fun rescheduleFrom(context: Context, now: LocalDateTime = LocalDateTime.now()) {
            val snapshot = hoursStore(context).load(now)
            AlarmScheduler(AndroidAlarmClock(context)).reschedule(now, snapshot)
        }
    }
}

class AndroidAlarmClock(private val context: Context) : AlarmClock {
    private val manager = context.getSystemService(AlarmManager::class.java)

    override fun cancel(hourId: String) {
        manager.cancel(pendingIntent(hourId, ""))
    }

    override fun setAlarmClock(hourId: String, at: LocalDateTime, date: String) {
        if (android.os.Build.VERSION.SDK_INT >= 31 && !manager.canScheduleExactAlarms()) return
        val trigger = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val operation = pendingIntent(hourId, date)
        val show = PendingIntent.getActivity(
            context,
            requestCode(hourId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), operation)
    }

    private fun pendingIntent(hourId: String, date: String): PendingIntent {
        val intent = Intent(context, HourReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_HOUR
            putExtra(AlarmScheduler.EXTRA_HOUR_ID, hourId)
            putExtra(AlarmScheduler.EXTRA_DATE, date)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(hourId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun requestCode(hourId: String): Int {
        val index = OfficeModel.HOURS.indexOfFirst { it.id == hourId }
        return if (index >= 0) index + 1 else hourId.hashCode()
    }
}

internal fun BroadcastReceiver.rescheduleAlarmsAsync(context: Context, action: String?) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val now = LocalDateTime.now()
            val snapshot = hoursStore(context).load(now)
            AlarmScheduler.handleSystemEvent(
                action,
                AlarmScheduler(AndroidAlarmClock(context)),
                now,
                snapshot,
            )
        } finally {
            pending.finish()
        }
    }
}
