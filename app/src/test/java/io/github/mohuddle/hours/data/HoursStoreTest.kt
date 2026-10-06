package io.github.mohuddle.hours.data

import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HoursStoreTest {
    private lateinit var dir: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "hours-store-${UUID.randomUUID()}")
        dir.mkdirs()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
        dir.deleteRecursively()
    }

    @Test
    fun firstLaunchUsesPluginDefaults() = runBlocking {
        val snapshot = newStore().load(LocalDateTime.of(2026, 8, 19, 9, 0))
        assertTrue(snapshot.notificationsEnabled)
        assertEquals(6, snapshot.hours.size)
        assertTrue(snapshot.hours.all { it.enabled })
        assertEquals(
            listOf("06:00", "07:00", "09:00", "12:00", "15:00", "18:00"),
            snapshot.hours.map { it.time },
        )
        assertEquals(0, snapshot.versePosition)
        assertEquals("2026-08-19", snapshot.cachedDate)
        assertEquals("Genesis 1:1", snapshot.verse!!.reference)
        assertTrue(snapshot.verse.text.contains("In the beginning"))
        assertNull(snapshot.verseError)
        assertEquals(
            "1 Corinthians 6:20",
            snapshot.officeBook!!.chapters["trinity"]!!["none"]!!.reference,
        )
        assertTrue(snapshot.officeBook.collects.any { it.id == "trinity-11" })
        assertNull(snapshot.officeError)
        assertTrue(snapshot.lastNotified.isEmpty())
    }

    @Test
    fun verseAdvancesOncePerLocalDate() = runBlocking {
        val store = newStore()
        val first = store.load(LocalDateTime.of(2026, 8, 19, 9, 0))
        val sameDay = store.load(LocalDateTime.of(2026, 8, 19, 18, 30))
        assertEquals(0, first.versePosition)
        assertEquals(first.verse!!.reference, sameDay.verse!!.reference)
        assertEquals(first.verse.text, sameDay.verse.text)
        assertEquals(0, sameDay.versePosition)

        val nextDay = store.load(LocalDateTime.of(2026, 8, 20, 6, 0))
        assertEquals(1, nextDay.versePosition)
        assertEquals("Genesis 1:27", nextDay.verse!!.reference)
        assertEquals("2026-08-20", nextDay.cachedDate)
    }

    @Test
    fun lastNotifiedRoundTrips() = runBlocking {
        val store = newStore()
        store.load(LocalDateTime.of(2026, 8, 19, 9, 0))
        store.setLastNotified(mapOf("terce" to "2026-08-19", "evil" to "nope"))
        val again = store.load(LocalDateTime.of(2026, 8, 19, 12, 0))
        assertEquals(mapOf("terce" to "2026-08-19"), again.lastNotified)
        assertEquals(0, again.versePosition)
    }

    @Test
    fun corruptJsonDoesNotThrow() = runBlocking {
        val store = newStore { "{not json" }
        val snapshot = store.load(LocalDateTime.of(2026, 8, 19, 9, 0))
        assertEquals("Couldn\u2019t load today\u2019s Scripture.", snapshot.verseError)
        assertNull(snapshot.verse)
        assertEquals("Couldn\u2019t read office data.", snapshot.officeError)
        assertNull(snapshot.officeBook)
        assertEquals(6, snapshot.hours.size)
        assertEquals("06:00", snapshot.hours.first { it.id == "morning" }.time)
        assertTrue(snapshot.notificationsEnabled)
        assertTrue(snapshot.lastNotified.isEmpty())
    }

    private fun bundledAsset(name: String): String {
        val stream = javaClass.classLoader!!.getResourceAsStream(name)
            ?: error("missing asset $name")
        return stream.bufferedReader().use { it.readText() }
    }

    private fun newStore(readAsset: (String) -> String = ::bundledAsset): HoursStore {
        val file = File(dir, "${UUID.randomUUID()}.preferences_pb")
        return HoursStore(HoursStore.newDataStore(file, scope), readAsset)
    }
}
