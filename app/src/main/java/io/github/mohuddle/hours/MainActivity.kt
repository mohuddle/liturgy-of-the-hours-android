package io.github.mohuddle.hours

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.data.hoursStore
import io.github.mohuddle.hours.notify.AlarmScheduler
import io.github.mohuddle.hours.notify.AndroidAlarmClock
import io.github.mohuddle.hours.notify.AndroidHourPoster
import io.github.mohuddle.hours.notify.canPostNotifications
import io.github.mohuddle.hours.notify.canScheduleExactAlarms
import io.github.mohuddle.hours.notify.openExactAlarmSettings
import io.github.mohuddle.hours.notify.openNotificationSettings
import io.github.mohuddle.hours.notify.permissionCtas
import io.github.mohuddle.hours.ui.HoursApp
import io.github.mohuddle.hours.ui.theme.HoursTheme
import java.time.LocalDateTime
import androidx.core.util.Consumer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = hoursStore(this)
        val scheduler = AlarmScheduler(AndroidAlarmClock(this))
        val poster = AndroidHourPoster(this)
        setContent {
            HoursTheme {
                var snapshot by remember { mutableStateOf<HoursSnapshot?>(null) }
                var now by remember { mutableStateOf(LocalDateTime.now()) }
                var openOfficeHourId by remember {
                    mutableStateOf(intent.getStringExtra(AlarmScheduler.EXTRA_HOUR_ID))
                }
                var notificationsGranted by remember { mutableStateOf(canPostNotifications(this)) }
                var exactAlarmsGranted by remember { mutableStateOf(canScheduleExactAlarms(this)) }
                DisposableEffect(Unit) {
                    val newIntent = Consumer<Intent> { incoming ->
                        openOfficeHourId = incoming.getStringExtra(AlarmScheduler.EXTRA_HOUR_ID)
                    }
                    addOnNewIntentListener(newIntent)
                    val lifecycleObserver = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            notificationsGranted = canPostNotifications(this@MainActivity)
                            exactAlarmsGranted = canScheduleExactAlarms(this@MainActivity)
                        }
                    }
                    lifecycle.addObserver(lifecycleObserver)
                    onDispose {
                        removeOnNewIntentListener(newIntent)
                        lifecycle.removeObserver(lifecycleObserver)
                    }
                }
                LaunchedEffect(Unit) {
                    now = LocalDateTime.now()
                    val loaded = store.load(now)
                    scheduler.reschedule(now, loaded)
                    snapshot = loaded
                }
                val loaded = snapshot
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (loaded == null) {
                        Box(Modifier.fillMaxSize())
                    } else {
                        HoursApp(
                            snapshot = loaded,
                            now = now,
                            openOfficeHourId = openOfficeHourId,
                            onHoursOpened = { poster.cancelAll() },
                            permissionCtas = permissionCtas(notificationsGranted, exactAlarmsGranted),
                            onAllowNotifications = { openNotificationSettings(this) },
                            onAllowExactAlarms = { openExactAlarmSettings(this) },
                            onSaveSettings = { notificationsEnabled, hours ->
                                store.saveSettings(notificationsEnabled, hours)
                                val updated = store.load(now)
                                scheduler.reschedule(now, updated)
                                updated
                            },
                        )
                    }
                }
            }
        }
    }
}
