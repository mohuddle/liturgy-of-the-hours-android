package io.github.mohuddle.hours.notify

import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.domain.OfficeModel
import io.github.mohuddle.hours.domain.VerseCatalog
import io.github.mohuddle.hours.domain.VerseEntry
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HourReceiverTest {
    private val now = LocalDateTime.of(2026, 8, 19, 9, 0)
    private val today = "2026-08-19"
    @Test
    fun happyPathPostsOngoingNotificationAndMarksNotified() {
        val decision = HourFire.handle(
            now = now,
            hourId = "terce",
            date = today,
            snapshot = snapshot(),
        )
        val post = decision as FireDecision.Post
        assertEquals("terce", post.notification.hourId)
        assertEquals("Terce — Third Hour", post.notification.title)
        assertTrue(post.notification.body.contains("Come, Holy Spirit"))
        assertTrue(post.notification.body.contains("Today’s Scripture: Genesis 1:1"))
        assertTrue(post.notification.ongoing)
        assertEquals(HourFire.CHANNEL_ID, post.notification.channelId)
        assertEquals(today, post.lastNotified["terce"])
        assertTrue(post.reschedule)
    }

    @Test
    fun staleDateIsDropped() {
        val decision = HourFire.handle(now, "terce", "2026-08-18", snapshot())
        assertEquals("stale", (decision as FireDecision.Drop).reason)
    }

    @Test
    fun alreadyNotifiedIsDropped() {
        val decision = HourFire.handle(
            now,
            "terce",
            today,
            snapshot(lastNotified = mapOf("terce" to today)),
        )
        assertEquals("already-notified", (decision as FireDecision.Drop).reason)
    }

    @Test
    fun disabledHourIsDropped() {
        val hours = OfficeModel.resolvedHours().map {
            if (it.id == "terce") it.copy(enabled = false) else it
        }
        val decision = HourFire.handle(now, "terce", today, snapshot(hours = hours))
        assertEquals("disabled", (decision as FireDecision.Drop).reason)
    }

    @Test
    fun remindersOffIsDropped() {
        val decision = HourFire.handle(
            now,
            "terce",
            today,
            snapshot(notificationsEnabled = false),
        )
        assertEquals("reminders-off", (decision as FireDecision.Drop).reason)
    }

    @Test
    fun permissionCtasShowWhenGrantsMissing() {
        assertEquals(
            PermissionCtas(allowNotifications = true, allowExactAlarms = true),
            permissionCtas(notificationsGranted = false, exactAlarmsGranted = false),
        )
        assertEquals(
            PermissionCtas(allowNotifications = false, allowExactAlarms = false),
            permissionCtas(notificationsGranted = true, exactAlarmsGranted = true),
        )
    }

    private fun snapshot(
        hours: List<Hour> = OfficeModel.resolvedHours(),
        notificationsEnabled: Boolean = true,
        lastNotified: Map<String, String> = emptyMap(),
    ) = HoursSnapshot(
        notificationsEnabled = notificationsEnabled,
        hours = hours,
        verse = OfficeModel.verseFromCatalog(
            VerseCatalog(
                translation = "BSB",
                verses = listOf(VerseEntry("Genesis 1:1", "In the beginning")),
            ),
            0,
        ),
        verseError = null,
        officeBook = null,
        officeError = null,
        lastNotified = lastNotified,
        versePosition = 0,
        cachedDate = today,
    )
}
