package io.github.mohuddle.hours.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.mohuddle.hours.domain.ChapterEntry
import io.github.mohuddle.hours.domain.CollectEntry
import io.github.mohuddle.hours.domain.Hour
import io.github.mohuddle.hours.domain.MemorialEntry
import io.github.mohuddle.hours.domain.OfficeBook
import io.github.mohuddle.hours.domain.OfficeModel
import io.github.mohuddle.hours.domain.RespondEntry
import io.github.mohuddle.hours.domain.VerseCatalog
import io.github.mohuddle.hours.domain.VerseEntry
import io.github.mohuddle.hours.domain.VerseRecord
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first

private val Context.hoursDataStore: DataStore<Preferences> by preferencesDataStore(name = "hours")

fun hoursStore(context: Context): HoursStore = HoursStore(
    dataStore = context.hoursDataStore,
    readAsset = { name ->
        context.assets.open(name).bufferedReader().use { it.readText() }
    },
)

data class HoursSnapshot(
    val notificationsEnabled: Boolean,
    val hours: List<Hour>,
    val verse: VerseRecord?,
    val verseError: String?,
    val officeBook: OfficeBook?,
    val officeError: String?,
    val lastNotified: Map<String, String>,
    val versePosition: Int,
    val cachedDate: String,
)

class HoursStore(
    private val dataStore: DataStore<Preferences>,
    private val readAsset: (String) -> String,
) {
    suspend fun load(now: LocalDateTime = LocalDateTime.now()): HoursSnapshot {
        val prefs = dataStore.data.first()
        val settings = settingsFrom(prefs)
        val today = OfficeModel.isoDate(now)
        val catalog = readAssetSafely("verses.json")?.let { parseVerseCatalog(it) }
        val officeBook = readAssetSafely("office.json")?.let { parseOfficeBook(it) }

        var position = prefs[Keys.versePosition] ?: -1
        var cachedDate = prefs[Keys.cachedDate] ?: ""
        var reference = prefs[Keys.verseReference] ?: ""
        var text = prefs[Keys.verseText] ?: ""
        var verse: VerseRecord? = null
        var verseError: String? = if (catalog == null) SCRIPTURE_ERROR else null

        if (catalog != null && catalog.verses.isNotEmpty()) {
            val alreadyToday = cachedDate == today && position >= 0 && text.isNotEmpty()
            if (!alreadyToday) {
                position = OfficeModel.nextPosition(
                    position,
                    catalog.verses.size,
                    "sequential",
                    today + "v",
                )
                val record = OfficeModel.verseFromCatalog(catalog, position)
                if (record == null) {
                    verseError = NO_SCRIPTURE_ERROR
                } else {
                    reference = record.reference
                    text = record.text
                    cachedDate = today
                    verse = record
                    verseError = null
                    dataStore.edit { mutable ->
                        mutable[Keys.versePosition] = position
                        mutable[Keys.cachedDate] = cachedDate
                        mutable[Keys.verseReference] = reference
                        mutable[Keys.verseText] = text
                    }
                }
            } else {
                verse = VerseRecord(
                    reference = reference,
                    text = text,
                    translation = "Berean Standard Bible",
                    translationId = "bsb",
                )
            }
        }

        return HoursSnapshot(
            notificationsEnabled = OfficeModel.boolSetting(settings["notificationsEnabled"], true),
            hours = OfficeModel.resolvedHours(settings),
            verse = verse,
            verseError = verseError,
            officeBook = officeBook,
            officeError = if (officeBook == null) OFFICE_ERROR else null,
            lastNotified = decodeLastNotified(prefs[Keys.lastNotified]),
            versePosition = position,
            cachedDate = cachedDate,
        )
    }

    suspend fun setLastNotified(lastNotified: Map<String, String>) {
        val encoded = encodeLastNotified(lastNotified)
        dataStore.edit { mutable ->
            mutable[Keys.lastNotified] = encoded
        }
    }

    private fun settingsFrom(prefs: Preferences): Map<String, Any?> {
        val settings = linkedMapOf<String, Any?>()
        prefs[Keys.notificationsEnabled]?.let { settings["notificationsEnabled"] = it }
        for (hour in OfficeModel.HOURS) {
            prefs[Keys.enabled(hour.id)]?.let { settings["${hour.id}Enabled"] = it }
            prefs[Keys.time(hour.id)]?.let { settings["${hour.id}Time"] = it }
        }
        return settings
    }

    private fun readAssetSafely(name: String): String? = try {
        readAsset(name)
    } catch (_: Exception) {
        null
    }

    private object Keys {
        val notificationsEnabled = booleanPreferencesKey("notificationsEnabled")
        val versePosition = intPreferencesKey("versePosition")
        val cachedDate = stringPreferencesKey("cachedDate")
        val verseReference = stringPreferencesKey("verseReference")
        val verseText = stringPreferencesKey("verseText")
        val lastNotified = stringPreferencesKey("lastNotified")

        fun enabled(id: String) = booleanPreferencesKey("${id}Enabled")
        fun time(id: String) = stringPreferencesKey("${id}Time")
    }

    companion object {
        const val SCRIPTURE_ERROR = "Couldn\u2019t load today\u2019s Scripture."
        const val NO_SCRIPTURE_ERROR = "No Scripture for today."
        const val OFFICE_ERROR = "Couldn\u2019t read office data."

        fun newDataStore(
            file: java.io.File,
            scope: kotlinx.coroutines.CoroutineScope,
        ): DataStore<Preferences> = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = scope,
            produceFile = { file },
        )
    }
}

private fun parseVerseCatalog(raw: String): VerseCatalog? {
    return try {
        val parsed = JsonParser.parseString(raw)
        if (!parsed.isJsonObject) return null
        val versesEl = parsed.asJsonObject.get("verses") ?: return null
        if (!versesEl.isJsonArray) return null
        val verses = mutableListOf<VerseEntry>()
        for (element in versesEl.asJsonArray) {
            if (!element.isJsonObject) return null
            val item = element.asJsonObject
            val reference = item.get("reference")?.takeIf { it.isJsonPrimitive }?.asString ?: return null
            val text = item.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            verses.add(VerseEntry(reference = reference, text = text))
        }
        val translation = parsed.asJsonObject.get("translation")
            ?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        VerseCatalog(translation = translation, verses = verses)
    } catch (_: Exception) {
        null
    }
}

private fun parseOfficeBook(raw: String): OfficeBook? {
    return try {
        val parsed = JsonParser.parseString(raw)
        if (!parsed.isJsonObject) return null
        val json = parsed.asJsonObject
        OfficeBook(
            chapters = parseChapters(json.getAsJsonObject("chapters")),
            responds = parseResponds(json.getAsJsonObject("responds")),
            collects = parseCollects(json),
            memorials = parseMemorials(json),
        )
    } catch (_: Exception) {
        null
    }
}

private fun stringList(obj: JsonObject, key: String): List<String>? {
    val value = obj.get(key) ?: return null
    if (!value.isJsonArray) return null
    return value.asJsonArray.map { it.asString }
}

private fun weekNumber(obj: JsonObject): Int? {
    val value = obj.get("week") ?: return null
    if (value.isJsonNull || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) return null
    return value.asInt
}

private fun parseChapters(obj: JsonObject?): Map<String, Map<String, ChapterEntry>> {
    if (obj == null) return emptyMap()
    return obj.entrySet().associate { (season, hours) ->
        season to hours.asJsonObject.entrySet().associate { (hourId, entry) ->
            val item = entry.asJsonObject
            hourId to ChapterEntry(
                reference = item.get("reference")?.takeIf { it.isJsonPrimitive }?.asString,
                text = item.get("text")?.takeIf { it.isJsonPrimitive }?.asString,
            )
        }
    }
}

private fun parseResponds(obj: JsonObject?): Map<String, Map<String, RespondEntry>> {
    if (obj == null) return emptyMap()
    return obj.entrySet().associate { (season, hours) ->
        season to hours.asJsonObject.entrySet().associate { (hourId, entry) ->
            val item = entry.asJsonObject
            hourId to RespondEntry(
                respond = item.get("respond")?.takeIf { it.isJsonPrimitive }?.asString,
                verse = item.get("verse")?.takeIf { it.isJsonPrimitive }?.asString,
            )
        }
    }
}

private fun parseCollects(json: JsonObject): List<CollectEntry> {
    val array = json.getAsJsonArray("collects") ?: return emptyList()
    return array.map { element ->
        val item = element.asJsonObject
        CollectEntry(
            id = item.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
            title = item.get("title")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
            text = item.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
            hours = stringList(item, "hours"),
            weekdays = stringList(item, "weekdays"),
            seasons = stringList(item, "seasons"),
            week = weekNumber(item),
        )
    }
}

private fun parseMemorials(json: JsonObject): List<MemorialEntry> {
    val array = json.getAsJsonArray("memorials") ?: return emptyList()
    return array.map { element ->
        val item = element.asJsonObject
        MemorialEntry(
            id = item.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
            title = item.get("title")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
            text = item.get("text")?.takeIf { it.isJsonPrimitive }?.asString ?: "",
            weekdays = stringList(item, "weekdays"),
        )
    }
}

private fun decodeLastNotified(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    val cache = OfficeModel.parseCache("""{"last_notified":$raw}""") ?: return emptyMap()
    return cache.lastNotified
}

private fun encodeLastNotified(map: Map<String, String>): String {
    val obj = JsonObject()
    for (hour in OfficeModel.HOURS) {
        val date = map[hour.id] ?: continue
        val parsed = OfficeModel.parseCache("""{"last_notified":{"${hour.id}":"$date"}}""")
        if (parsed?.lastNotified?.get(hour.id) != date) continue
        obj.addProperty(hour.id, date)
    }
    return obj.toString()
}
