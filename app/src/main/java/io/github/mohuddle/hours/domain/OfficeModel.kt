package io.github.mohuddle.hours.domain

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

data class Hour(
    val id: String,
    val latin: String,
    val name: String,
    val shortName: String,
    val traditional: String,
    val defaultTime: String,
    val invitatory: String,
    val hymn: String,
    val prayer: String,
    val psalm: String,
    val psalmText: String,
    val enabled: Boolean = true,
    val time: String = defaultTime,
    val minutes: Int = 0,
    val tomorrow: Boolean = false,
)

data class ScheduleState(
    val hours: List<Hour>,
    val all: List<Hour>,
    val current: Hour?,
    val next: Hour?,
    val nowMinutes: Int,
)

data class VerseEntry(
    val reference: String,
    val text: String,
)

data class VerseCatalog(
    val translation: String = "",
    val verses: List<VerseEntry> = emptyList(),
)

data class VerseRecord(
    val reference: String,
    val text: String,
    val translation: String,
    val translationId: String,
)

data class CacheState(
    val date: String = "",
    val translation: String = "",
    val versePosition: Int = -1,
    val reference: String = "",
    val text: String = "",
    val lastNotified: Map<String, String> = emptyMap(),
)

data class LiturgicalDay(
    val date: String,
    val weekday: String,
    val season: String,
    val seasonKey: String,
    val weekNumber: Int?,
    val feast: String?,
    val spoken: String,
)

data class ChapterEntry(
    val reference: String? = null,
    val text: String? = null,
)

data class RespondEntry(
    val respond: String? = null,
    val verse: String? = null,
)

data class CollectEntry(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val hours: List<String>? = null,
    val weekdays: List<String>? = null,
    val seasons: List<String>? = null,
    val week: Int? = null,
)

data class MemorialEntry(
    val id: String = "",
    val title: String = "",
    val text: String = "",
    val weekdays: List<String>? = null,
)

data class OfficeBook(
    val chapters: Map<String, Map<String, ChapterEntry>> = emptyMap(),
    val responds: Map<String, Map<String, RespondEntry>> = emptyMap(),
    val collects: List<CollectEntry> = emptyList(),
    val memorials: List<MemorialEntry> = emptyList(),
)

data class OfficeSection(
    val label: String,
    val body: String,
)

data class BuiltOffice(
    val spoken: String,
    val heading: String,
    val feast: String?,
    val seasonKey: String,
    val weekNumber: Int?,
    val weekday: String,
    val hourId: String,
    val hourName: String,
    val hourShortName: String,
    val chapter: ChapterEntry?,
    val respond: RespondEntry?,
    val collect: CollectEntry?,
    val memorial: MemorialEntry?,
    val sections: List<OfficeSection>,
)

object OfficeModel {
    const val MAX_CACHE_BYTES = 65536
    const val MAX_REFERENCE = 200
    const val MAX_VERSE = 4096
    const val MAX_NOTIFY = 1500

    private val HOUR_IDS = listOf("morning", "prime", "terce", "sext", "none", "evening")
    private val DATE_RE = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    val HOURS: List<Hour> = listOf(
        Hour(
            id = "morning",
            latin = "Laudes",
            name = "Morning Prayer",
            shortName = "Lauds",
            traditional = "Dawn",
            defaultTime = "06:00",
            invitatory = "O Lord, open my lips. And my mouth shall proclaim your praise.",
            hymn = "The night has passed and the day lies open before us; let us pray with one heart and mind.",
            prayer = "Father, we praise you with this morning offering. Make us faithful to your word, and bring us to the glory of the resurrection. Through Christ our Lord. Amen.",
            psalm = "Psalm 5:3",
            psalmText = "O Lord, in the morning you hear my voice; in the morning I plead my case to you, and watch.",
        ),
        Hour(
            id = "prime",
            latin = "Prima",
            name = "Prime",
            shortName = "Prime",
            traditional = "First Hour",
            defaultTime = "07:00",
            invitatory = "O God, come to my assistance. O Lord, make haste to help me.",
            hymn = "Now that the daylight fills the sky, we lift our hearts to God on high, that he, in all we do or say, would keep us free from harm today.",
            prayer = "Lord God, king of heaven and earth, guide and sanctify, rule and govern our hearts and bodies this day in the ways of your commandments. Through Christ our Lord. Amen.",
            psalm = "Psalm 119:147-148",
            psalmText = "I rise before dawn and cry for help; I put my hope in your words. My eyes are awake before each watch of the night, that I may meditate on your promise.",
        ),
        Hour(
            id = "terce",
            latin = "Tertia",
            name = "Terce",
            shortName = "Terce",
            traditional = "Third Hour",
            defaultTime = "09:00",
            invitatory = "O God, come to my assistance. O Lord, make haste to help me.",
            hymn = "Come, Holy Spirit, fill the hearts of your faithful, and kindle in them the fire of your love.",
            prayer = "Lord God, you sent the Holy Spirit upon the apostles at the third hour. Grant us a share in that gift, that we may bear witness to your name. Through Christ our Lord. Amen.",
            psalm = "Acts 2:15-17",
            psalmText = "These people are not drunk, as you suppose, for it is only nine o’clock in the morning. No, this is what was spoken through the prophet Joel: In the last days it will be, God declares, that I will pour out my Spirit upon all flesh.",
        ),
        Hour(
            id = "sext",
            latin = "Sexta",
            name = "Sext",
            shortName = "Sext",
            traditional = "Sixth Hour",
            defaultTime = "12:00",
            invitatory = "O God, come to my assistance. O Lord, make haste to help me.",
            hymn = "At noon you hung upon the Cross, O Christ; grant that we may take up our cross and follow you.",
            prayer = "Almighty Father, you gave your Son to die for us at the sixth hour. Keep us steadfast in his Passion, that we may share in the glory of his resurrection. Through Christ our Lord. Amen.",
            psalm = "John 19:14-16",
            psalmText = "Now it was the day of Preparation for the Passover; and it was about noon. He said to the Jews, Here is your King! They cried out, Away with him! Crucify him!",
        ),
        Hour(
            id = "none",
            latin = "Nona",
            name = "None",
            shortName = "None",
            traditional = "Ninth Hour",
            defaultTime = "15:00",
            invitatory = "O God, come to my assistance. O Lord, make haste to help me.",
            hymn = "At the ninth hour you commended your spirit to the Father; teach us to live and die in your peace.",
            prayer = "Lord Jesus Christ, at the ninth hour you yielded up your spirit. By your death, take away the death of our sin, and grant us the life that never ends. Amen.",
            psalm = "Luke 23:44-46",
            psalmText = "It was now about noon, and darkness came over the whole land until three in the afternoon. Then Jesus, crying with a loud voice, said, Father, into your hands I commend my spirit.",
        ),
        Hour(
            id = "evening",
            latin = "Vesperae",
            name = "Evening Prayer",
            shortName = "Vespers",
            traditional = "Sunset",
            defaultTime = "18:00",
            invitatory = "O God, come to my assistance. O Lord, make haste to help me.",
            hymn = "Let my prayer be counted as incense before you, and the lifting up of my hands as an evening sacrifice.",
            prayer = "Stay with us, Lord, for it is evening and the day is almost over. Kindle in our hearts the hope of the resurrection, and keep us in your peace this night. Through Christ our Lord. Amen.",
            psalm = "Luke 1:46-47",
            psalmText = "My soul magnifies the Lord, and my spirit rejoices in God my Savior.",
        ),
    )

    val VERSES: List<String> = listOf(
        "Genesis 1:1", "Genesis 1:27", "Genesis 50:20", "Exodus 14:14", "Exodus 33:14",
        "Deuteronomy 6:4-5", "Deuteronomy 31:6", "Joshua 1:9", "Joshua 24:15", "Ruth 1:16",
        "1 Samuel 16:7", "1 Chronicles 16:34", "2 Chronicles 7:14", "Nehemiah 8:10", "Job 19:25",
        "Psalm 1:1-2", "Psalm 4:8", "Psalm 5:3", "Psalm 16:11", "Psalm 18:2", "Psalm 19:14",
        "Psalm 23:1-3", "Psalm 23:4", "Psalm 27:1", "Psalm 27:4", "Psalm 34:8", "Psalm 37:5",
        "Psalm 42:1", "Psalm 46:1", "Psalm 46:10", "Psalm 51:10", "Psalm 63:1", "Psalm 90:12",
        "Psalm 91:1-2", "Psalm 95:1-3", "Psalm 100:1-3", "Psalm 103:1-2", "Psalm 118:24",
        "Psalm 119:105", "Psalm 121:1-2", "Psalm 122:1", "Psalm 127:1", "Psalm 130:1-2",
        "Psalm 139:23-24", "Psalm 145:18", "Psalm 150:6", "Proverbs 3:5-6", "Proverbs 9:10",
        "Proverbs 16:9", "Ecclesiastes 3:1", "Isaiah 9:6", "Isaiah 26:3", "Isaiah 40:31",
        "Isaiah 41:10", "Isaiah 43:1-2", "Isaiah 53:5", "Isaiah 55:6-7", "Jeremiah 29:11",
        "Lamentations 3:22-23", "Ezekiel 36:26", "Micah 6:8", "Habakkuk 3:17-18", "Zephaniah 3:17",
        "Matthew 5:3-4", "Matthew 5:9", "Matthew 5:14-16", "Matthew 5:44", "Matthew 6:9-13",
        "Matthew 6:33", "Matthew 7:7", "Matthew 11:28-30", "Matthew 16:24", "Matthew 22:37-39",
        "Matthew 28:19-20", "Mark 8:34", "Mark 10:45", "Mark 12:30-31", "Luke 1:38",
        "Luke 1:46-49", "Luke 6:31", "Luke 6:36", "Luke 9:23", "Luke 11:9", "Luke 12:32",
        "Luke 22:42", "John 1:1", "John 1:14", "John 3:16", "John 6:35", "John 8:12",
        "John 8:32", "John 10:11", "John 11:25-26", "John 13:34-35", "John 14:6", "John 14:27",
        "John 15:5", "John 15:12", "John 16:33", "Acts 1:8", "Acts 2:42", "Romans 5:8",
        "Romans 8:28", "Romans 8:38-39", "Romans 12:1-2", "Romans 12:12", "Romans 15:13",
        "1 Corinthians 10:13", "1 Corinthians 13:4-7", "1 Corinthians 13:13", "1 Corinthians 15:57",
        "2 Corinthians 4:16-18", "2 Corinthians 5:17", "2 Corinthians 12:9", "Galatians 2:20",
        "Galatians 5:22-23", "Galatians 6:9", "Ephesians 2:8-9", "Ephesians 3:20", "Ephesians 4:32",
        "Ephesians 6:10-11", "Philippians 1:6", "Philippians 2:3-4", "Philippians 4:4-7",
        "Philippians 4:8", "Philippians 4:13", "Colossians 3:12-14", "Colossians 3:16",
        "Colossians 3:23", "1 Thessalonians 5:16-18", "2 Timothy 1:7", "2 Timothy 4:7",
        "Hebrews 4:16", "Hebrews 11:1", "Hebrews 12:1-2", "Hebrews 13:8", "James 1:5",
        "James 1:17", "James 1:22", "James 4:7-8", "1 Peter 5:6-7", "1 John 1:9",
        "1 John 4:7-8", "1 John 4:16", "1 John 4:19", "Revelation 3:20", "Revelation 21:4",
    )

    fun pad2(n: Int): String = if (n < 10) "0$n" else n.toString()

    fun isoDate(date: LocalDateTime): String =
        "${date.year}-${pad2(date.monthValue)}-${pad2(date.dayOfMonth)}"

    fun minutesOfDay(date: LocalDateTime): Int = date.hour * 60 + date.minute

    fun parseHm(value: String?, fallback: String?): String {
        val fb = fallback ?: "06:00"
        val m = Regex("^(\\d{1,2}):(\\d{2})$").find((value ?: "").trim()) ?: return fb
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (h > 23 || min > 59) return fb
        return "${pad2(h)}:${pad2(min)}"
    }

    fun minutesFromHm(value: String?): Int {
        val parsed = parseHm(value, "00:00")
        val parts = parsed.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    fun formatUntil(fromMin: Int, toMin: Int, tomorrow: Boolean): String {
        var delta = toMin - fromMin
        if (tomorrow) delta += 24 * 60
        if (delta < 0) delta += 24 * 60
        if (delta == 0) return "now"
        if (delta == 1) return "in 1 min"
        if (delta < 60) return "in $delta min"
        val h = delta / 60
        val m = delta % 60
        if (m == 0) return if (h == 1) "in 1 hour" else "in $h hours"
        return "in ${h}h ${m}m"
    }

    fun boolSetting(value: Any?, fallback: Boolean): Boolean {
        if (value == null) return fallback
        if (value == true || value == "true") return true
        if (value == false || value == "false") return false
        return fallback
    }

    fun nextPosition(position: Int, length: Int, style: String?, salt: String?): Int {
        if (length <= 0) return -1
        if (style.toString() != "random") {
            return (position + 1 + length) % length
        }
        var hash = 0L
        for (ch in (salt ?: "")) {
            hash = (hash * 31 + ch.code) and 0xFFFFFFFFL
        }
        return (hash % length).toInt()
    }

    fun cleanVerseText(raw: String?): String {
        val text = (raw ?: "").replace(Regex("\\s+"), " ").trim()
        return if (text.length <= MAX_VERSE) text else text.substring(0, MAX_VERSE)
    }

    fun plainText(value: String?, limit: Int = MAX_NOTIFY): String {
        var text = (value ?: "").replace(Regex("[\\u0000-\\u001F\\u007F-\\u009F]"), "")
        text = text.replace("&", "").replace("<", "").replace(">", "")
        return if (text.length <= limit) text else text.substring(0, limit)
    }

    fun catalogVerses(catalog: VerseCatalog): List<VerseEntry> = catalog.verses

    fun verseFromCatalog(catalog: VerseCatalog, position: Int): VerseRecord? {
        val verses = catalogVerses(catalog)
        if (verses.isEmpty()) return null
        val idx = maxOf(0, position) % verses.size
        val item = verses[idx]
        val text = cleanVerseText(item.text)
        if (text.isEmpty()) return null
        val reference = plainText(item.reference.ifEmpty { verseForPosition(idx) }, MAX_REFERENCE)
        return VerseRecord(
            reference = reference,
            text = text,
            translation = "Berean Standard Bible",
            translationId = "bsb",
        )
    }

    fun featuredHour(schedule: ScheduleState?): Hour? {
        if (schedule == null) return null
        return schedule.current ?: schedule.next
    }

    fun heroMeta(hour: Hour?): String {
        if (hour == null) return ""
        return "${hour.shortName} · ${hour.traditional} · ${hour.time}"
    }

    fun hourById(id: String): Hour? = HOURS.firstOrNull { it.id == id }

    fun resolvedHours(settings: Map<String, Any?> = emptyMap()): List<Hour> {
        return HOURS.map { hour ->
            val enabled = boolSetting(settings[hour.id + "Enabled"], true)
            val time = parseHm(settings[hour.id + "Time"] as? String, hour.defaultTime)
            hour.copy(
                enabled = enabled,
                time = time,
                minutes = minutesFromHm(time),
                tomorrow = false,
            )
        }
    }

    fun enabledHours(settings: Map<String, Any?> = emptyMap()): List<Hour> =
        resolvedHours(settings).filter { it.enabled }

    fun scheduleState(now: LocalDateTime, settings: Map<String, Any?> = emptyMap()): ScheduleState {
        val hours = enabledHours(settings).sortedBy { it.minutes }
        val nowMin = minutesOfDay(now)
        var current: Hour? = null
        var next: Hour? = null
        for (hour in hours) {
            if (hour.minutes <= nowMin) current = hour
            else if (next == null) next = hour
        }
        if (next == null && hours.isNotEmpty()) next = hours[0].copy(tomorrow = true)
        return ScheduleState(
            hours = hours,
            all = resolvedHours(settings),
            current = current,
            next = next,
            nowMinutes = nowMin,
        )
    }

    fun isCurrentWindow(hour: Hour?, nowMin: Int, nextHour: Hour?, windowMinutes: Int = 20): Boolean {
        if (hour == null || hour.tomorrow) return false
        if (nowMin < hour.minutes) return false
        var end = hour.minutes + windowMinutes
        if (nextHour != null && !nextHour.tomorrow && nextHour.minutes > hour.minutes) {
            end = minOf(end, nextHour.minutes)
        }
        return nowMin < end
    }

    fun dueNotifications(
        now: LocalDateTime,
        settings: Map<String, Any?> = emptyMap(),
        lastNotified: Map<String, String> = emptyMap(),
        graceMinutes: Int = 5,
    ): List<Hour> {
        val today = isoDate(now)
        val nowMin = minutesOfDay(now)
        val due = mutableListOf<Hour>()
        for (hour in enabledHours(settings)) {
            if (lastNotified[hour.id] == today) continue
            val delta = nowMin - hour.minutes
            if (delta in 0..graceMinutes) due.add(hour)
        }
        return due
    }

    fun parseCache(raw: String?): CacheState? {
        val source = raw ?: ""
        if (source.length > MAX_CACHE_BYTES) return null
        val trimmed = source.trim()
        if (trimmed.isEmpty()) return CacheState()
        return try {
            val parsed = JsonParser.parseString(trimmed)
            if (!parsed.isJsonObject) return null
            val data = parsed.asJsonObject
            val notified = mutableMapOf<String, String>()
            val incoming = data.get("last_notified")
            if (incoming != null && incoming.isJsonObject) {
                for (id in HOUR_IDS) {
                    val stamp = incoming.asJsonObject.get(id) ?: continue
                    if (stamp.isJsonPrimitive && stamp.asJsonPrimitive.isString) {
                        val value = stamp.asString
                        if (DATE_RE.matches(value)) notified[id] = value
                    }
                }
            }
            val positionEl = data.get("verse_position")
            val position: Double = when {
                positionEl == null || positionEl.isJsonNull -> -1.0
                positionEl.isJsonPrimitive && positionEl.asJsonPrimitive.isNumber ->
                    positionEl.asDouble
                else -> return null
            }
            if (position.isNaN() || position.isInfinite() || position < -1 || position > 10000) return null
            val dateEl = data.get("date")
            val date = if (dateEl != null && dateEl.isJsonPrimitive && dateEl.asJsonPrimitive.isString &&
                DATE_RE.matches(dateEl.asString)
            ) dateEl.asString else ""
            val translationEl = data.get("translation")
            val translation = if (translationEl != null && translationEl.isJsonPrimitive &&
                translationEl.asString == "bsb"
            ) "bsb" else ""
            val reference = if (data.has("reference") && data.get("reference").isJsonPrimitive) {
                data.get("reference").asString
            } else ""
            val text = if (data.has("text") && data.get("text").isJsonPrimitive) {
                data.get("text").asString
            } else ""
            CacheState(
                date = date,
                translation = translation,
                versePosition = position.toInt(),
                reference = plainText(reference, MAX_REFERENCE),
                text = cleanVerseText(text),
                lastNotified = notified,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun serializeCache(state: CacheState): String {
        val payload = linkedMapOf(
            "date" to (state.date),
            "translation" to state.translation,
            "verse_position" to state.versePosition,
            "reference" to plainText(state.reference, MAX_REFERENCE),
            "text" to cleanVerseText(state.text),
            "last_notified" to state.lastNotified,
        )
        return gson.toJson(payload) + "\n"
    }

    fun notificationTitle(hour: Hour?): String {
        if (hour == null) return "Liturgy of the Hours"
        return plainText("${hour.name} — ${hour.traditional}", 120)
    }

    fun hourNotificationTitles(): List<String> = HOURS.map { notificationTitle(it) }

    fun notificationBody(hour: Hour?, verseReference: String?): String {
        val lines = mutableListOf<String>()
        if (hour != null) {
            if (hour.hymn.isNotEmpty()) lines.add(hour.hymn)
            if (hour.invitatory.isNotEmpty()) lines.add(hour.invitatory)
        }
        if (!verseReference.isNullOrEmpty()) lines.add("Today’s Scripture: $verseReference")
        return plainText(lines.joinToString("\n"), MAX_NOTIFY)
    }

    fun verseForPosition(position: Int): String {
        if (VERSES.isEmpty()) return ""
        val idx = maxOf(0, position) % VERSES.size
        return VERSES[idx]
    }

    // Anonymous Gregorian computus. Month from the algorithm is 1-based.
    fun easterDate(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val monthDay = h + l - 7 * m + 114
        val month = monthDay / 31
        val day = (monthDay % 31) + 1
        return LocalDate.of(year, month, day)
    }

    fun liturgicalDay(now: LocalDateTime): LiturgicalDay {
        val d = now.toLocalDate()
        val year = d.year
        val notes = yearAnchors(year)
        val christmas = LocalDate.of(year, 12, 25)
        val epiphany = LocalDate.of(year, 1, 6)
        val feast = RED_LETTER["${d.monthValue}-${d.dayOfMonth}"]
        val weekday = weekdayName(d)
        var season = "the Christian year"
        var seasonKey = "trinity"
        var weekNumber: Int? = null
        fun cmp(a: LocalDate, b: LocalDate): Int = daysBetween(b, a)

        if (cmp(d, christmas) == 0) {
            season = "Christmas Day"
            seasonKey = "christmas"
        } else if (cmp(d, LocalDate.of(year, 12, 24)) == 0) {
            season = "Christmas Eve"
            seasonKey = "christmas"
        } else if (cmp(d, LocalDate.of(year, 12, 26)) >= 0 || cmp(d, LocalDate.of(year, 1, 5)) <= 0) {
            season = "Christmastide"
            seasonKey = "christmas"
        } else if (cmp(d, epiphany) == 0) {
            season = "the Epiphany of our Lord"
            seasonKey = "epiphany"
            weekNumber = 0
        } else if (cmp(d, epiphany) > 0 && cmp(d, notes.ash) < 0) {
            var firstEpiphany = addDays(epiphany, (7 - jsDay(epiphany)) % 7)
            if (cmp(firstEpiphany, epiphany) <= 0) firstEpiphany = addDays(firstEpiphany, 7)
            val nEp = nthSundayAfter(addDays(firstEpiphany, -7), d)
            seasonKey = "epiphany"
            weekNumber = maxOf(nEp, 1)
            season = if (jsDay(d) == 0) {
                "the ${ordinal(nEp)} Sunday after Epiphany"
            } else {
                "the week following the ${ordinal(maxOf(nEp, 1))} Sunday after Epiphany"
            }
        } else if (cmp(d, notes.ash) == 0) {
            season = "Ash Wednesday"
            seasonKey = "lent"
            weekNumber = 0
        } else if (cmp(d, notes.ash) > 0 && cmp(d, notes.palm) < 0) {
            var firstLent = addDays(notes.ash, (7 - jsDay(notes.ash)) % 7)
            if (cmp(firstLent, notes.ash) == 0) firstLent = addDays(firstLent, 7)
            seasonKey = "lent"
            if (cmp(d, firstLent) < 0) {
                season = "the week of Ash Wednesday"
                weekNumber = 0
            } else if (jsDay(d) == 0) {
                val lentSunday = Math.floorDiv(daysBetween(firstLent, d), 7) + 1
                weekNumber = lentSunday
                season = "the ${ordinal(lentSunday)} Sunday in Lent"
            } else {
                val lentWeek = maxOf(nthSundayAfter(firstLent, d), 1)
                weekNumber = lentWeek
                season = "the week following the ${ordinal(lentWeek)} Sunday in Lent"
            }
        } else if (cmp(d, notes.palm) == 0) {
            season = "Palm Sunday"
            seasonKey = "holyweek"
        } else if (cmp(d, notes.palm) > 0 && cmp(d, notes.easter) < 0) {
            season = "Holy Week"
            seasonKey = "holyweek"
        } else if (cmp(d, notes.easter) == 0) {
            season = "Easter Day"
            seasonKey = "easter"
            weekNumber = 0
        } else if (cmp(d, notes.easter) > 0 && cmp(d, notes.ascension) < 0) {
            val nEaster = nthSundayAfter(notes.easter, d)
            seasonKey = "easter"
            weekNumber = nEaster
            season = if (jsDay(d) == 0) {
                "the ${ordinal(nEaster)} Sunday after Easter"
            } else if (nEaster != 0) {
                "the week following the ${ordinal(maxOf(nEaster, 1))} Sunday after Easter"
            } else {
                "Easter Week"
            }
        } else if (cmp(d, notes.ascension) == 0) {
            season = "Ascension Day"
            seasonKey = "ascension"
        } else if (cmp(d, notes.ascension) > 0 && cmp(d, notes.pentecost) < 0) {
            season = "the week following Ascension Day"
            seasonKey = "ascension"
        } else if (cmp(d, notes.pentecost) == 0) {
            season = "Whitsunday, the Feast of Pentecost"
            seasonKey = "pentecost"
        } else if (cmp(d, notes.pentecost) > 0 && cmp(d, notes.trinity) < 0) {
            season = "the week following Whitsunday"
            seasonKey = "pentecost"
        } else if (cmp(d, notes.trinity) == 0) {
            season = "Trinity Sunday"
            seasonKey = "trinity"
            weekNumber = 0
        } else if (cmp(d, notes.trinity) > 0 && cmp(d, notes.advent) < 0) {
            val nTrin = nthSundayAfter(notes.trinity, d)
            seasonKey = "trinity"
            weekNumber = nTrin
            season = if (jsDay(d) == 0) {
                "the ${ordinal(nTrin)} Sunday after Trinity"
            } else {
                "the week following the ${ordinal(maxOf(nTrin, 1))} Sunday after Trinity"
            }
        } else if (cmp(d, notes.advent) >= 0 && cmp(d, christmas) < 0) {
            val nAdv = Math.floorDiv(daysBetween(notes.advent, d), 7) + 1
            seasonKey = "advent"
            weekNumber = nAdv
            season = if (jsDay(d) == 0) {
                "the ${ordinal(nAdv)} Sunday in Advent"
            } else {
                "the week following the ${ordinal(nAdv)} Sunday in Advent"
            }
        }

        val spoken = if (
            season == "Holy Week" || season == "Easter Week" || season == "Christmastide" ||
            season.startsWith("the week")
        ) {
            "$weekday in $season"
        } else if (
            season.contains("Day") || season.startsWith("Ash") ||
            season.startsWith("Whitsunday") || season.contains("Eve")
        ) {
            if (jsDay(d) == 0 || season.contains("Day")) season else "$weekday, $season"
        } else if (jsDay(d) == 0) {
            season
        } else {
            "$weekday in $season"
        }

        return LiturgicalDay(
            date = isoDate(d.atStartOfDay()),
            weekday = weekday,
            season = season,
            seasonKey = seasonKey,
            weekNumber = weekNumber,
            feast = feast,
            spoken = spoken,
        )
    }

    fun officeNotificationTitle(): String = "The Office"

    fun pluginNotificationTitles(): List<String> =
        hourNotificationTitles() + officeNotificationTitle()

    fun buildOffice(now: LocalDateTime, hour: Hour?, book: OfficeBook?, salt: String?): BuiltOffice {
        val day = liturgicalDay(now)
        val resolved = hour ?: hourById("morning")!!
        val chapter = lookupHourEntry(book?.chapters, day.seasonKey, resolved.id)
        val respond = lookupHourEntry(book?.responds, day.seasonKey, resolved.id)
        val collect = pickCollect(book, day, resolved, salt)
        val memorial = pickMemorial(book, day)
        var heading = day.spoken
        if (day.feast != null) heading += ", ${day.feast}"
        val sections = mutableListOf<OfficeSection>()
        if (chapter != null) {
            val reference = chapter.reference
            val text = chapter.text ?: ""
            val body = if (!reference.isNullOrEmpty()) "$reference\n$text" else text
            sections.add(OfficeSection(label = "The Chapter", body = body))
        }
        if (respond != null) {
            val lines = listOf(
                respond.respond,
                respond.verse,
                "Glory be to the Father, and to the Son, and to the Holy Spirit.",
            ).filter { !it.isNullOrEmpty() }
            sections.add(OfficeSection(label = "The Short Respond", body = lines.joinToString("\n")))
        }
        if (collect != null) {
            sections.add(OfficeSection(label = "Collect", body = collect.text))
        }
        if (memorial != null) {
            val title = memorial.title
            val text = memorial.text
            val body = if (title.isNotEmpty()) "$title\n$text" else text
            sections.add(OfficeSection(label = "Memorial Collect", body = body))
        }
        return BuiltOffice(
            spoken = day.spoken,
            heading = heading,
            feast = day.feast,
            seasonKey = day.seasonKey,
            weekNumber = day.weekNumber,
            weekday = day.weekday,
            hourId = resolved.id,
            hourName = resolved.name,
            hourShortName = resolved.shortName,
            chapter = chapter,
            respond = respond,
            collect = collect,
            memorial = memorial,
            sections = sections,
        )
    }

    fun officeNotificationBody(office: BuiltOffice?): String {
        if (office == null) return "The Office is not ready yet."
        val lines = mutableListOf(office.heading)
        if (office.hourName.isNotEmpty()) lines.add(office.hourName)
        for (section in office.sections) {
            lines.add("")
            lines.add(section.label.uppercase(Locale.ROOT))
            lines.add(section.body)
        }
        return plainText(lines.joinToString("\n"), MAX_NOTIFY)
    }

    private data class YearAnchors(
        val easter: LocalDate,
        val ash: LocalDate,
        val palm: LocalDate,
        val ascension: LocalDate,
        val pentecost: LocalDate,
        val trinity: LocalDate,
        val advent: LocalDate,
    )

    private val ORDINALS = listOf(
        "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh",
        "Eighth", "Ninth", "Tenth", "Eleventh", "Twelfth", "Thirteenth",
        "Fourteenth", "Fifteenth", "Sixteenth", "Seventeenth", "Eighteenth",
        "Nineteenth", "Twentieth", "Twenty-first", "Twenty-second",
        "Twenty-third", "Twenty-fourth", "Twenty-fifth", "Twenty-sixth",
        "Twenty-seventh",
    )

    private val WEEKDAYS = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
    )

    private val RED_LETTER = mapOf(
        "1-1" to "the Circumcision of Christ",
        "1-6" to "the Epiphany of our Lord",
        "1-25" to "the Conversion of Saint Paul",
        "2-2" to "the Presentation of Christ in the Temple",
        "2-24" to "Saint Matthias the Apostle",
        "3-25" to "the Annunciation of the Blessed Virgin Mary",
        "4-25" to "Saint Mark the Evangelist",
        "6-11" to "Saint Barnabas the Apostle",
        "6-24" to "the Nativity of Saint John the Baptist",
        "6-29" to "Saint Peter the Apostle",
        "7-25" to "Saint James the Apostle",
        "8-6" to "the Transfiguration of our Lord",
        "8-15" to "the feast of Saint Mary the Virgin",
        "8-24" to "Saint Bartholomew the Apostle",
        "9-21" to "Saint Matthew the Apostle",
        "9-29" to "Saint Michael and All Angels",
        "10-18" to "Saint Luke the Evangelist",
        "10-28" to "Saint Simon and Saint Jude, Apostles",
        "11-1" to "All Saints",
        "11-30" to "Saint Andrew the Apostle",
        "12-21" to "Saint Thomas the Apostle",
        "12-25" to "the Nativity of our Lord",
        "12-26" to "Saint Stephen, Deacon and Martyr",
        "12-27" to "Saint John the Apostle and Evangelist",
        "12-28" to "the Holy Innocents",
    )

    private val YEAR_NOTES = mapOf(
        2026 to YearAnchors(
            easter = LocalDate.of(2026, 4, 5),
            ash = LocalDate.of(2026, 2, 18),
            palm = LocalDate.of(2026, 3, 29),
            ascension = LocalDate.of(2026, 5, 14),
            pentecost = LocalDate.of(2026, 5, 24),
            trinity = LocalDate.of(2026, 5, 31),
            advent = LocalDate.of(2026, 11, 29),
        ),
    )

    private fun jsDay(d: LocalDate): Int = d.dayOfWeek.value % 7

    private fun addDays(d: LocalDate, n: Int): LocalDate = d.plusDays(n.toLong())

    private fun daysBetween(a: LocalDate, b: LocalDate): Int =
        ChronoUnit.DAYS.between(a, b).toInt()

    private fun sundayOnOrBefore(d: LocalDate): LocalDate = addDays(d, -jsDay(d))

    private fun nthSundayAfter(anchor: LocalDate, d: LocalDate): Int =
        Math.floorDiv(daysBetween(anchor, sundayOnOrBefore(d)), 7)

    private fun weekdayName(d: LocalDate): String = WEEKDAYS[(jsDay(d) + 6) % 7]

    private fun ordinal(n: Int): String = ORDINALS.getOrElse(n) { "undefined" }

    private fun adventSunday(year: Int): LocalDate {
        val start = LocalDate.of(year, 11, 27)
        for (i in 0 until 7) {
            val candidate = addDays(start, i)
            if (jsDay(candidate) == 0) return candidate
        }
        return start
    }

    private fun yearAnchors(year: Int): YearAnchors {
        YEAR_NOTES[year]?.let { return it }
        val easter = easterDate(year)
        return YearAnchors(
            easter = easter,
            ash = addDays(easter, -46),
            palm = addDays(easter, -7),
            ascension = addDays(easter, 39),
            pentecost = addDays(easter, 49),
            trinity = addDays(easter, 56),
            advent = adventSunday(year),
        )
    }

    private fun <T> lookupHourEntry(
        maps: Map<String, Map<String, T>>?,
        seasonKey: String,
        hourId: String,
    ): T? {
        if (maps == null) return null
        val season = maps[seasonKey] ?: maps["trinity"] ?: emptyMap()
        season[hourId]?.let { return it }
        season["default"]?.let { return it }
        val trinity = maps["trinity"] ?: return null
        return trinity[hourId] ?: trinity["default"]
    }

    private fun collectMatches(collect: CollectEntry, day: LiturgicalDay, hourId: String): Boolean {
        if (!collect.hours.isNullOrEmpty() && hourId !in collect.hours) return false
        if (!collect.weekdays.isNullOrEmpty() && day.weekday !in collect.weekdays) return false
        if (!collect.seasons.isNullOrEmpty() && day.seasonKey !in collect.seasons) return false
        if (collect.week != null && collect.week != day.weekNumber) return false
        return true
    }

    private fun pickCollect(
        book: OfficeBook?,
        day: LiturgicalDay,
        hour: Hour,
        salt: String?,
    ): CollectEntry? {
        val pool = mutableListOf<CollectEntry>()
        for (item in book?.collects ?: emptyList()) {
            if (collectMatches(item, day, hour.id)) pool.add(item)
        }
        if (hour.prayer.isNotEmpty()) {
            pool.add(
                CollectEntry(
                    id = "${hour.id}-hour",
                    title = hour.name,
                    text = hour.prayer,
                ),
            )
        }
        if (pool.isEmpty()) return null
        val key = (if (salt.isNullOrEmpty()) day.date else salt) + hour.id + "collect"
        return pool[nextPosition(-1, pool.size, "random", key)]
    }

    private fun pickMemorial(book: OfficeBook?, day: LiturgicalDay): MemorialEntry? {
        var fallback: MemorialEntry? = null
        for (item in book?.memorials ?: emptyList()) {
            if (!item.weekdays.isNullOrEmpty() && day.weekday in item.weekdays) return item
            if (item.weekdays.isNullOrEmpty()) fallback = item
        }
        return fallback
    }
}
