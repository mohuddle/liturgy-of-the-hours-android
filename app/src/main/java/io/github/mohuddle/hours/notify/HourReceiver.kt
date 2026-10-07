package io.github.mohuddle.hours.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mohuddle.hours.data.hoursStore
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HourReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = LocalDateTime.now()
                val store = hoursStore(context)
                val snapshot = store.load(now)
                when (
                    val decision = HourFire.handle(
                        now = now,
                        hourId = intent.getStringExtra(AlarmScheduler.EXTRA_HOUR_ID),
                        date = intent.getStringExtra(AlarmScheduler.EXTRA_DATE),
                        snapshot = snapshot,
                    )
                ) {
                    is FireDecision.Post -> {
                        AndroidHourPoster(context).post(decision.notification)
                        store.setLastNotified(decision.lastNotified)
                        if (decision.reschedule) {
                            AlarmScheduler(AndroidAlarmClock(context)).reschedule(now, store.load(now))
                        }
                    }
                    is FireDecision.Drop -> Unit
                }
            } finally {
                pending.finish()
            }
        }
    }
}
