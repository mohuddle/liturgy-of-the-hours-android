package io.github.mohuddle.hours.ui

import io.github.mohuddle.hours.data.HoursStore
import io.github.mohuddle.hours.domain.OfficeModel
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

class OfficeViewTest {
    private lateinit var dir: File
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        dir = File(System.getProperty("java.io.tmpdir"), "office-view-${UUID.randomUUID()}")
        dir.mkdirs()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
        dir.deleteRecursively()
    }

    @Test
    fun noneOnTrinityFridayHasHeadingAndFourSections() {
        val book = loadBook()
        val state = officeViewState(
            now = LocalDateTime.of(2026, 8, 21, 15, 5),
            hourId = "none",
            hours = OfficeModel.resolvedHours(),
            book = book,
        )
        assertEquals(
            "Friday in the week following the Eleventh Sunday after Trinity",
            state.heading,
        )
        assertEquals("None", state.hourName)
        assertEquals(
            listOf("The Chapter", "The Short Respond", "Collect", "Memorial Collect"),
            state.sections.map { it.label },
        )
        assertTrue(state.sections[0].body.startsWith("1 Corinthians 6:20"))
        assertNull(state.error)
    }

    @Test
    fun sextOpensSextOffice() {
        val book = loadBook()
        val state = officeViewState(
            now = LocalDateTime.of(2026, 8, 21, 12, 5),
            hourId = "sext",
            hours = OfficeModel.resolvedHours(),
            book = book,
        )
        assertEquals("Sext", state.hourName)
        assertEquals("sext", state.hourId)
        assertTrue(state.sections[0].body.startsWith("Galatians 6:2"))
        assertEquals("office/sext", HoursRoutes.office("sext"))
    }

    @Test
    fun missingBookShowsNotReady() {
        val state = officeViewState(
            now = LocalDateTime.of(2026, 8, 21, 15, 5),
            hourId = "none",
            hours = OfficeModel.resolvedHours(),
            book = null,
        )
        assertEquals("The Office is not ready yet.", state.error)
        assertTrue(state.sections.isEmpty())
    }

    @Test
    fun bellUsesFeaturedHour() {
        val now = LocalDateTime.of(2026, 8, 21, 9, 5)
        val hours = OfficeModel.resolvedHours()
        assertEquals("terce", featuredOfficeHourId(now, hours))
        assertEquals("hours", HoursRoutes.HOURS)
        assertEquals("settings", HoursRoutes.SETTINGS)
        assertEquals("office/{hourId}", HoursRoutes.OFFICE)
    }

    private fun loadBook() = runBlocking {
        val file = File(dir, "${UUID.randomUUID()}.preferences_pb")
        val store = HoursStore(
            HoursStore.newDataStore(file, scope),
            { name ->
                javaClass.classLoader!!.getResourceAsStream(name)!!
                    .bufferedReader().use { it.readText() }
            },
        )
        store.load(LocalDateTime.of(2026, 8, 21, 15, 5)).officeBook
    }
}
