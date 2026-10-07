package io.github.mohuddle.hours.notify

import android.content.Intent
import io.github.mohuddle.hours.data.HoursSnapshot
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.domain.OfficeModel
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {
    @Test
    fun sixEnabledHoursSetSixClocks() {
        val clock = FakeAlarmClock()
        val scheduler = AlarmScheduler(clock)
        val now = LocalDateTime.of(2026, 8, 19, 9, 5)
        scheduler.reschedule(now, snapshot(OfficeModel.resolvedHours()))

        assertEquals(OfficeModel.HOURS.map { it.id }, clock.cancelled)
        assertEquals(6, clock.set.size)
        assertEquals(
            listOf("morning", "prime", "terce", "sext", "none", "evening"),
            clock.set.map { it.hourId },
        )
        assertEquals(LocalDateTime.of(2026, 8, 20, 6, 0), clock.at("morning"))
        assertEquals(LocalDateTime.of(2026, 8, 20, 9, 0), clock.at("terce"))
        assertEquals(LocalDateTime.of(2026, 8, 19, 12, 0), clock.at("sext"))
        assertEquals("2026-08-20", clock.date("morning"))
        assertEquals("2026-08-19", clock.date("evening"))
    }

    @Test
    fun disablingTerceLeavesFiveClocks() {
        val clock = FakeAlarmClock()
        val scheduler = AlarmScheduler(clock)
        val hours = OfficeModel.resolvedHours().map {
            if (it.id == "terce") it.copy(enabled = false) else it
        }
        scheduler.reschedule(LocalDateTime.of(2026, 8, 19, 9, 5), snapshot(hours))

        assertEquals(6, clock.cancelled.size)
        assertEquals(5, clock.set.size)
        assertTrue(clock.set.none { it.hourId == "terce" })
    }

    @Test
    fun afterEighteenHundredEveningIsTomorrow() {
        val clock = FakeAlarmClock()
        val scheduler = AlarmScheduler(clock)
        scheduler.reschedule(
            LocalDateTime.of(2026, 8, 19, 18, 0),
            snapshot(OfficeModel.resolvedHours()),
        )

        assertEquals(LocalDateTime.of(2026, 8, 20, 18, 0), clock.at("evening"))
        assertEquals("2026-08-20", clock.date("evening"))
    }

    @Test
    fun remindersOffCancelsAllAndSetsNone() {
        val clock = FakeAlarmClock()
        AlarmScheduler(clock).reschedule(
            LocalDateTime.of(2026, 8, 19, 9, 5),
            snapshot(OfficeModel.resolvedHours(), notificationsEnabled = false),
        )
        assertEquals(6, clock.cancelled.size)
        assertTrue(clock.set.isEmpty())
    }

    @Test
    fun rebootPathInvokesReschedule() {
        val clock = FakeAlarmClock()
        val scheduler = AlarmScheduler(clock)
        val now = LocalDateTime.of(2026, 8, 19, 9, 5)
        val snap = snapshot(OfficeModel.resolvedHours())
        assertTrue(AlarmScheduler.handleSystemEvent(Intent.ACTION_BOOT_COMPLETED, scheduler, now, snap))
        assertEquals(6, clock.set.size)
        clock.set.clear()
        clock.cancelled.clear()
        assertTrue(AlarmScheduler.handleSystemEvent(Intent.ACTION_TIME_CHANGED, scheduler, now, snap))
        assertEquals(6, clock.set.size)
        clock.set.clear()
        assertTrue(AlarmScheduler.handleSystemEvent(Intent.ACTION_TIMEZONE_CHANGED, scheduler, now, snap))
        assertEquals(6, clock.set.size)
        assertEquals(false, AlarmScheduler.handleSystemEvent(Intent.ACTION_POWER_CONNECTED, scheduler, now, snap))
    }

    private fun snapshot(
        hours: List<Hour>,
        notificationsEnabled: Boolean = true,
    ) = HoursSnapshot(
        notificationsEnabled = notificationsEnabled,
        hours = hours,
        verse = null,
        verseError = null,
        officeBook = null,
        officeError = null,
        lastNotified = emptyMap(),
        versePosition = 0,
        cachedDate = "",
    )
}

class FakeAlarmClock : AlarmClock {
    val cancelled = mutableListOf<String>()
    val set = mutableListOf<ScheduledAlarm>()

    override fun cancel(hourId: String) {
        cancelled += hourId
        set.removeAll { it.hourId == hourId }
    }

    override fun setAlarmClock(hourId: String, at: LocalDateTime, date: String) {
        set.removeAll { it.hourId == hourId }
        set += ScheduledAlarm(hourId, at, date)
    }
}

private fun FakeAlarmClock.at(hourId: String): LocalDateTime =
    set.first { it.hourId == hourId }.at

private fun FakeAlarmClock.date(hourId: String): String =
    set.first { it.hourId == hourId }.date
