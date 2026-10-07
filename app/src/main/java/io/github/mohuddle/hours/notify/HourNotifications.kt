package io.github.mohuddle.hours.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.mohuddle.hours.MainActivity
import io.github.mohuddle.hours.R
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.domain.OfficeModel
import java.time.LocalDateTime

data class PostedHour(
    val id: Int,
    val hourId: String,
    val title: String,
    val body: String,
    val ongoing: Boolean,
    val channelId: String,
)

sealed class FireDecision {
    data class Post(
        val notification: PostedHour,
        val lastNotified: Map<String, String>,
        val reschedule: Boolean,
    ) : FireDecision()

    data class Drop(val reason: String) : FireDecision()
}

data class PermissionCtas(
    val allowNotifications: Boolean,
    val allowExactAlarms: Boolean,
)

fun permissionCtas(
    notificationsGranted: Boolean,
    exactAlarmsGranted: Boolean,
) = PermissionCtas(
    allowNotifications = !notificationsGranted,
    allowExactAlarms = !exactAlarmsGranted,
)

object HourFire {
    const val CHANNEL_ID = "hours"
    const val ACTION_OPEN_OFFICE = "io.github.mohuddle.hours.OPEN_OFFICE"

    fun notificationId(hourId: String): Int {
        val index = OfficeModel.HOURS.indexOfFirst { it.id == hourId }
        return if (index >= 0) index + 1 else 0
    }

    fun handle(
        now: LocalDateTime,
        hourId: String?,
        date: String?,
        snapshot: HoursSnapshot,
    ): FireDecision {
        if (hourId.isNullOrBlank() || date.isNullOrBlank()) return FireDecision.Drop("missing")
        val today = OfficeModel.isoDate(now)
        if (date != today) return FireDecision.Drop("stale")
        if (!snapshot.notificationsEnabled) return FireDecision.Drop("reminders-off")
        val hour = snapshot.hours.firstOrNull { it.id == hourId } ?: return FireDecision.Drop("unknown")
        if (!hour.enabled) return FireDecision.Drop("disabled")
        if (snapshot.lastNotified[hourId] == today) return FireDecision.Drop("already-notified")
        return FireDecision.Post(
            notification = PostedHour(
                id = notificationId(hourId),
                hourId = hourId,
                title = OfficeModel.notificationTitle(hour),
                body = OfficeModel.notificationBody(hour, snapshot.verse?.reference),
                ongoing = true,
                channelId = CHANNEL_ID,
            ),
            lastNotified = snapshot.lastNotified + (hourId to today),
            reschedule = true,
        )
    }
}

fun canPostNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 31) return true
    return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}

fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < 31) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

class AndroidHourPoster(private val context: Context) {
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.church_bell}")
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            HourFire.CHANNEL_ID,
            "Hours",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(sound, attrs)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun post(posted: PostedHour) {
        if (!canPostNotifications(context)) return
        ensureChannel()
        val open = Intent(context, MainActivity::class.java).apply {
            action = HourFire.ACTION_OPEN_OFFICE
            putExtra(AlarmScheduler.EXTRA_HOUR_ID, posted.hourId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tap = PendingIntent.getActivity(
            context,
            posted.id,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, posted.channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(posted.title)
            .setContentText(posted.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(posted.body))
            .setOngoing(posted.ongoing)
            .setContentIntent(tap)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(posted.id, notification)
    }

    fun cancelAll() {
        val manager = NotificationManagerCompat.from(context)
        for (hour in OfficeModel.HOURS) {
            manager.cancel(HourFire.notificationId(hour.id))
        }
    }
}
