package io.github.mohuddle.hours.domain

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class OfficeModelTest {
    @Test
    fun hoursTableMatchesPlugin() {
        assertTrue(OfficeModel.VERSES.size >= 120)
        assertTrue(OfficeModel.VERSES.size <= 250)
        assertEquals(6, OfficeModel.HOURS.size)
        assertEquals(
            listOf("morning", "prime", "terce", "sext", "none", "evening"),
            OfficeModel.HOURS.map { it.id },
        )
        assertEquals("Third Hour", OfficeModel.hourById("terce")!!.traditional)
        assertEquals("Nona", OfficeModel.hourById("none")!!.latin)
        assertEquals("Vespers", OfficeModel.hourById("evening")!!.shortName)
    }

    @Test
    fun clockHelpersMatchPlugin() {
        assertEquals("2026-01-05", OfficeModel.isoDate(LocalDateTime.of(2026, 1, 5, 0, 0)))
        assertEquals("09:00", OfficeModel.parseHm("9:00", "06:00"))
        assertEquals("06:00", OfficeModel.parseHm("24:00", "06:00"))
        assertEquals("07:00", OfficeModel.parseHm("nope", "07:00"))
        assertEquals(360, OfficeModel.minutesFromHm("06:00"))
        assertEquals(900, OfficeModel.minutesFromHm("15:00"))
        assertEquals(570, OfficeModel.minutesOfDay(LocalDateTime.of(2026, 8, 19, 9, 30)))
        assertEquals("now", OfficeModel.formatUntil(540, 540, false))
        assertEquals("in 15 min", OfficeModel.formatUntil(540, 555, false))
        assertEquals("in 1 hour", OfficeModel.formatUntil(540, 600, false))
        assertTrue(OfficeModel.formatUntil(18 * 60, 6 * 60, true).contains("in "))
    }

    @Test
    fun verseRotationAndSettings() {
        assertEquals(0, OfficeModel.nextPosition(-1, 3, "sequential", "x"))
        assertEquals(0, OfficeModel.nextPosition(2, 3, "sequential", "x"))
        assertEquals(
            OfficeModel.nextPosition(4, 9, "random", "2026-01-01v"),
            OfficeModel.nextPosition(-1, 9, "random", "2026-01-01v"),
        )
        assertEquals(true, OfficeModel.boolSetting(null, true))
        assertEquals(false, OfficeModel.boolSetting(false, true))
    }

    @Test
    fun catalogVersesMatchBundledJson() {
        val raw = javaClass.classLoader!!.getResourceAsStream("verses.json")!!
            .bufferedReader()
            .use { it.readText() }
        val json = Gson().fromJson(raw, JsonObject::class.java)
        assertEquals("BSB", json.get("translation").asString)
        val verses = json.getAsJsonArray("verses").map { el ->
            val obj = el.asJsonObject
            VerseEntry(obj.get("reference").asString, obj.get("text").asString)
        }
        val catalog = VerseCatalog("BSB", verses)
        assertEquals(OfficeModel.VERSES.size, catalog.verses.size)
        assertEquals(OfficeModel.VERSES, catalog.verses.map { it.reference })
        val john = OfficeModel.verseFromCatalog(catalog, OfficeModel.VERSES.indexOf("John 14:27"))!!
        assertEquals("John 14:27", john.reference)
        assertTrue(john.text.contains("Peace I leave with you"))
        assertEquals("bsb", john.translationId)
        assertNull(OfficeModel.verseFromCatalog(VerseCatalog(verses = emptyList()), 0))
    }

    @Test
    fun scheduleHighlightsCurrentAndNextHour() {
        val hours = OfficeModel.resolvedHours(mapOf("primeEnabled" to false, "terceTime" to "9:15"))
        assertEquals(false, hours.first { it.id == "prime" }.enabled)
        assertEquals("09:15", hours.first { it.id == "terce" }.time)
        assertEquals(5, OfficeModel.enabledHours(mapOf("primeEnabled" to false)).size)

        val schedule = OfficeModel.scheduleState(LocalDateTime.of(2026, 8, 19, 10, 0), emptyMap())
        assertEquals("terce", schedule.current!!.id)
        assertEquals("sext", schedule.next!!.id)
        assertEquals("terce", OfficeModel.featuredHour(schedule)!!.id)
        assertEquals("Terce · Third Hour · 09:00", OfficeModel.heroMeta(schedule.current))
        assertFalse(
            OfficeModel.isCurrentWindow(schedule.current, schedule.nowMinutes, schedule.next, 20),
        )

        val terceSchedule = OfficeModel.scheduleState(LocalDateTime.of(2026, 8, 19, 9, 5), emptyMap())
        assertTrue(
            OfficeModel.isCurrentWindow(
                terceSchedule.current,
                terceSchedule.nowMinutes,
                terceSchedule.next,
                20,
            ),
        )
    }

    @Test
    fun dueNotificationsHonorGraceAndLastNotified() {
        val due = OfficeModel.dueNotifications(
            LocalDateTime.of(2026, 8, 19, 9, 2),
            emptyMap(),
            emptyMap(),
            5,
        )
        assertEquals(1, due.size)
        assertEquals("terce", due[0].id)
        assertEquals(
            0,
            OfficeModel.dueNotifications(
                LocalDateTime.of(2026, 8, 19, 9, 2),
                emptyMap(),
                mapOf("terce" to "2026-08-19"),
                5,
            ).size,
        )
        assertEquals(
            0,
            OfficeModel.dueNotifications(
                LocalDateTime.of(2026, 8, 19, 10, 0),
                emptyMap(),
                emptyMap(),
                5,
            ).size,
        )
    }

    @Test
    fun notificationsCacheAndAfterEvening() {
        assertEquals("None — Ninth Hour", OfficeModel.notificationTitle(OfficeModel.hourById("none")!!))
        assertTrue(
            OfficeModel.notificationBody(OfficeModel.hourById("morning")!!, "John 3:16")
                .contains("Today’s Scripture: John 3:16"),
        )
        assertEquals("a img src=x  b", OfficeModel.plainText("a <img src=x> & b", 80))
        assertEquals(OfficeModel.parseCache(""), OfficeModel.parseCache("   "))
        assertNull(OfficeModel.parseCache("{not json"))
        val cached = OfficeModel.parseCache(
            """{"date":"2026-08-19","translation":"bsb","verse_position":2,"reference":"John 3:16","text":"For God so loved the world","last_notified":{"terce":"2026-08-19","evil":"nope"}}""",
        )!!
        assertEquals("John 3:16", cached.reference)
        assertEquals(mapOf("terce" to "2026-08-19"), cached.lastNotified)

        val hourTitles = OfficeModel.hourNotificationTitles()
        assertEquals(6, hourTitles.size)
        assertEquals(6, hourTitles.toSet().size)
        assertTrue(hourTitles.contains("Terce — Third Hour"))

        val bell = listOf(
            File("src/main/res/raw/church_bell.ogg"),
            File("app/src/main/res/raw/church_bell.ogg"),
        ).firstOrNull { it.exists() }
        assertNotNull("bundled church bell", bell)

        assertEquals(OfficeModel.VERSES[0], OfficeModel.verseForPosition(0))
        assertEquals(OfficeModel.VERSES[0], OfficeModel.verseForPosition(OfficeModel.VERSES.size))

        val afterEvening = OfficeModel.scheduleState(LocalDateTime.of(2026, 8, 19, 22, 0), emptyMap())
        assertEquals("morning", afterEvening.next!!.id)
        assertTrue(afterEvening.next.tomorrow)
        assertEquals("evening", OfficeModel.featuredHour(afterEvening)!!.id)
    }

    @Test
    fun liturgicalDayAndOfficeAssembly() {
        val easter = OfficeModel.easterDate(2026)
        assertEquals(2026, easter.year)
        assertEquals(4, easter.monthValue)
        assertEquals(5, easter.dayOfMonth)

        val fridayTrinity = OfficeModel.liturgicalDay(LocalDateTime.of(2026, 8, 21, 15, 5))
        assertEquals(
            "Friday in the week following the Eleventh Sunday after Trinity",
            fridayTrinity.spoken,
        )
        assertEquals("trinity", fridayTrinity.seasonKey)
        assertEquals(11, fridayTrinity.weekNumber)
        assertEquals("Friday", fridayTrinity.weekday)
        assertEquals(
            "Trinity Sunday",
            OfficeModel.liturgicalDay(LocalDateTime.of(2026, 5, 31, 0, 0)).spoken,
        )
        assertEquals(
            "Easter Day",
            OfficeModel.liturgicalDay(LocalDateTime.of(2026, 4, 5, 0, 0)).spoken,
        )
        assertEquals(
            "the First Sunday in Advent",
            OfficeModel.liturgicalDay(LocalDateTime.of(2026, 11, 29, 0, 0)).spoken,
        )
        assertEquals(
            "the Eleventh Sunday after Trinity",
            OfficeModel.liturgicalDay(LocalDateTime.of(2026, 8, 16, 0, 0)).spoken,
        )

        val book = loadOfficeBook()
        assertTrue(book.chapters["trinity"]!!["none"]!!.reference!!.isNotEmpty())
        assertTrue(book.collects.any { it.id == "trinity-11" })

        val built = OfficeModel.buildOffice(
            LocalDateTime.of(2026, 8, 21, 15, 5),
            OfficeModel.hourById("none"),
            book,
            "2026-08-21",
        )
        assertEquals(
            "Friday in the week following the Eleventh Sunday after Trinity",
            built.heading,
        )
        assertEquals("None", built.hourShortName)
        assertEquals("1 Corinthians 6:20", built.chapter!!.reference)
        assertTrue(built.chapter.text!!.contains("bought at a price"))
        assertTrue(built.respond!!.respond!!.contains("Buy us back"))
        assertEquals("cross", built.memorial!!.id)
        assertTrue(built.collect!!.text.contains("Amen"))
        assertEquals(4, built.sections.size)
        assertEquals("The Office", OfficeModel.officeNotificationTitle())
        assertTrue(OfficeModel.officeNotificationBody(built).contains("THE CHAPTER"))
        assertTrue(OfficeModel.pluginNotificationTitles().contains("The Office"))
    }

    private fun loadOfficeBook(): OfficeBook {
        val raw = javaClass.classLoader!!.getResourceAsStream("office.json")!!
            .bufferedReader()
            .use { it.readText() }
        val json = Gson().fromJson(raw, JsonObject::class.java)

        fun stringList(obj: JsonObject, key: String): List<String>? {
            val value = obj.get(key) ?: return null
            if (!value.isJsonArray) return null
            return value.asJsonArray.map { it.asString }
        }

        fun week(obj: JsonObject): Int? {
            val value = obj.get("week") ?: return null
            if (value.isJsonNull || !value.isJsonPrimitive) return null
            return value.asInt
        }

        val chapters = json.getAsJsonObject("chapters").entrySet().associate { (season, hours) ->
            season to hours.asJsonObject.entrySet().associate { (hourId, entry) ->
                val item = entry.asJsonObject
                hourId to ChapterEntry(
                    reference = item.get("reference")?.asString,
                    text = item.get("text")?.asString,
                )
            }
        }
        val responds = json.getAsJsonObject("responds").entrySet().associate { (season, hours) ->
            season to hours.asJsonObject.entrySet().associate { (hourId, entry) ->
                val item = entry.asJsonObject
                hourId to RespondEntry(
                    respond = item.get("respond")?.asString,
                    verse = item.get("verse")?.asString,
                )
            }
        }
        val collects = json.getAsJsonArray("collects").map { element ->
            val item = element.asJsonObject
            CollectEntry(
                id = item.get("id")?.asString ?: "",
                title = item.get("title")?.asString ?: "",
                text = item.get("text")?.asString ?: "",
                hours = stringList(item, "hours"),
                weekdays = stringList(item, "weekdays"),
                seasons = stringList(item, "seasons"),
                week = week(item),
            )
        }
        val memorials = json.getAsJsonArray("memorials").map { element ->
            val item = element.asJsonObject
            MemorialEntry(
                id = item.get("id")?.asString ?: "",
                title = item.get("title")?.asString ?: "",
                text = item.get("text")?.asString ?: "",
                weekdays = stringList(item, "weekdays"),
            )
        }
        return OfficeBook(
            chapters = chapters,
            responds = responds,
            collects = collects,
            memorials = memorials,
        )
    }
}
