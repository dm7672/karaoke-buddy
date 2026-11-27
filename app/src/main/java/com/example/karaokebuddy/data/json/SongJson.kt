package com.example.karaokebuddy.data.json

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.karaokebuddy.domain.model.LyricTiming
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID
import kotlin.time.Duration

internal fun songsToJson(songs: Collection<Song>): String {
    val json = JSONArray()
    for (song in songs) {
        json.put(song.json)
    }
    return json.toString(2)
}

internal fun songsFromJson(text: String): List<Song> {
    val json = JSONArray(text)
    val songs = mutableListOf<Song>()
    for (index in 0 until json.length()) {
        val entry = json.optJSONObject(index) ?: continue
        val song = Song.parse(entry) ?: continue
        songs.add(song)
    }
    return songs
}

fun Song.Companion.parse(json: JSONObject): Song? = try {
    val (lyricsText, lyricTimings) = parseLyrics(json)
    Song(
        uid = UUID.fromString(json.getString("uid")),
        name = json.getString("name"),
        status = SongStatus.valueOf(json.optString("status", SongStatus.QUEUED.name)),
        color = Color(json.optInt("color", Color.White.toArgb())),
        sourceUrl = json.optionalNonBlankString("sourceUrl")?.let(URI::create),
        instrumentalPath = json.optionalNonBlankString("instrumentalPath")?.let(::File),
        vocalPath = json.optionalNonBlankString("vocalPath")?.let(::File),
        lyricsText = lyricsText,
        lyricTimings = lyricTimings,
        isFavorite = json.optBoolean("isFavorite", false),
        expiresAt = json.optionalNonBlankString("expiresAt")?.let(Instant::parse),
    )
} catch (_: JSONException) {
    null
} catch (_: IllegalArgumentException) {
    null
} catch (_: DateTimeParseException) {
    null
}

private fun JSONObject.optionalNonBlankString(key: String): String? {
    if (isNull(key)) return null
    val value = get(key)
    require(value is String && value.isNotBlank())
    return value
}

val Song.json: JSONObject
    get() = JSONObject().apply {
        put("uid", uid.toString())
        put("name", name)
        if (status != SongStatus.QUEUED) put("status", status.name)
        if (color != Color.White) put("color", color.toArgb())
        sourceUrl?.let { put("sourceUrl", it.toString()) }
        instrumentalPath?.let { put("instrumentalPath", it.path) }
        vocalPath?.let { put("vocalPath", it.path) }
        lyricsText?.let { put("lyricsText", it) }
        if (lyricTimings.isNotEmpty()) put("lyricTimings", JSONArray(lyricTimings.map { it.json }))
        put("isFavorite", isFavorite)
        expiresAt?.let { put("expiresAt", it.toString()) }
    }

private fun parseLyrics(json: JSONObject): Pair<String?, List<LyricTiming>> {
    val text = if (json.isNull("lyricsText")) null else json.get("lyricsText")
    require(text == null || text is String)

    val timings = mutableListOf<LyricTiming>()
    if (!json.isNull("lyricTimings")) {
        val entries = json.getJSONArray("lyricTimings")
        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            timings.add(LyricTiming(
                startIndex = entry.getInt("startIndex"),
                endIndex = entry.getInt("endIndex"),
                startMs = entry.getLong("startMs"),
                duration = Duration.parse(entry.getString("duration")),
            ))
        }
    }
    return (text as String?) to timings
}

private val LyricTiming.json: JSONObject
    get() = JSONObject().apply {
        put("startIndex", startIndex)
        put("endIndex", endIndex)
        put("startMs", startMs)
        put("duration", duration.toString())
    }
