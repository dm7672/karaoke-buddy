package com.example.karaokebuddy.data.json

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

fun parse(json: JSONObject): Song? = try {
    Song(
        uid = UUID.fromString(json.getString("uid")),
        name = json.getString("name"),
        status = SongStatus.valueOf(json.optString("status", SongStatus.QUEUED.name)),
        color = Color(json.optInt("color", Color.White.toArgb())),
        sourceUrl = if (json.isNull("sourceUrl")) null else json.getString("sourceUrl"),
        isFavorite = json.optBoolean("isFavorite", false),
    )
} catch (_: JSONException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

val Song.json: JSONObject
    get() = JSONObject().apply {
        put("uid", uid.toString())
        put("name", name)
        if (status != SongStatus.QUEUED) put("status", status.name)
        if (color != Color.White) put("color", color.toArgb())
        sourceUrl?.let { put("sourceUrl", it) }
        put("isFavorite", isFavorite)
    }
