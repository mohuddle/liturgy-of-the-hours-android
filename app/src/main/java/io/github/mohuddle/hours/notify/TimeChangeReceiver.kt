package io.github.mohuddle.hours.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        rescheduleAlarmsAsync(context, intent.action)
    }
}
