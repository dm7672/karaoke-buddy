package com.example.karaokebuddy.data.json

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.karaokebuddy.domain.model.OffsetLine
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import com.example.karaokebuddy.domain.model.TimedLyrics
import com.example.karaokebuddy.domain.model.TimedText
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.URI
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
        val song = parse(entry) ?: continue
        songs.add(song)
    }
    return songs
}

fun parse(json: JSONObject): Song? = try {
    Song(
        uid = UUID.fromString(json.getString("uid")),
        name = json.getString("name"),
        status = SongStatus.valueOf(json.optString("status", SongStatus.QUEUED.name)),
        color = Color(json.optInt("color", Color.White.toArgb())),
        sourceUrl = json.optionalNonBlankString("sourceUrl")?.let(URI::create),
        instrumentalPath = json.optionalNonBlankString("instrumentalPath")?.let(::File),
        vocalPath = json.optionalNonBlankString("vocalPath")?.let(::File),
        timedLyrics = if (json.isNull("timedLyrics")) null else parseLyrics(json.getJSONObject("timedLyrics")),
        isFavorite = json.optBoolean("isFavorite", false),
    )
} catch (_: JSONException) {
    null
} catch (_: IllegalArgumentException) {
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
        timedLyrics?.let { put("timedLyrics", it.json) }
        put("isFavorite", isFavorite)
    }

private fun parseLyrics(json: JSONObject): TimedLyrics {
    val lines = json.getJSONArray("offsetLines")
    return TimedLyrics((0 until lines.length()).map { lineIndex ->
        val line = lines.getJSONObject(lineIndex)
        val words = line.getJSONArray("words")
        OffsetLine(
            startMs = line.getLong("startMs"),
            duration = Duration.parse(line.getString("duration")),
            words = (0 until words.length()).map { wordIndex ->
                val word = words.getJSONObject(wordIndex)
                TimedText(
                    text = word.getString("text"),
                    startMs = word.getLong("startMs"),
                    duration = Duration.parse(word.getString("duration")),
                )
            },
        )
    })
}

private val TimedLyrics.json: JSONObject
    get() = JSONObject().apply {
        put("offsetLines", JSONArray(offsetLines.map { it.json }))
    }

private val OffsetLine.json: JSONObject
    get() = JSONObject().apply {
        put("startMs", startMs)
        put("duration", duration.toString())
        put("words", JSONArray(words.map { it.json }))
    }

private val TimedText.json: JSONObject
    get() = JSONObject().apply {
        put("text", text)
        put("startMs", startMs)
        put("duration", duration.toString())
    }
