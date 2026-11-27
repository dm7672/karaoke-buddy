package com.example.karaokebuddy.data.network

import com.example.karaokebuddy.BuildConfig
import com.example.karaokebuddy.domain.model.LyricTiming
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val SERVER = "https://karaokebuddy.online"

internal class ProcessingApi {
    suspend fun availableWorkers(): Int = request("/v1/status").getInt("availableWorkers")

    suspend fun submit(song: Song, language: String): Song {
        val source = JSONObject().apply {
            put("type", if (song.sourceUrl == null) "query" else "url")
            put("value", song.sourceUrl?.toString() ?: song.name)
        }
        val body = JSONObject().put("language", language).put("source", source)
        return update(song, request("/v1/jobs", body, song.uid.toString()))
    }

    suspend fun update(song: Song): Song {
        val id = requireNotNull(song.processingJobId)
        return update(song, request("/v1/jobs/$id"))
    }

    private suspend fun update(song: Song, response: JSONObject): Song {
        val id = response.getString("id")
        val status = when (response.getString("status")) {
            "queued", "waiting_for_worker" -> SongStatus.QUEUED
            "running" -> SongStatus.PROCESSING
            "succeeded" -> SongStatus.READY
            "failed", "canceled" -> SongStatus.ERROR
            else -> throw IOException("Неизвестный статус обработки")
        }
        val updated = song.copy(
            status = status,
            processingJobId = id,
            processingProgress = if (response.isNull("progress")) null else response.getInt("progress"),
            processingMessage = response.optString("message").ifBlank { null },
        )
        if (status != SongStatus.READY) return updated
        val result = request("/v1/jobs/$id/result")
        val (text, timings) = readLyrics(result)
        return updated.copy(lyricsText = text, lyricTimings = timings)
    }

    private suspend fun request(path: String, body: JSONObject? = null, key: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = URL(SERVER + path).openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = 5000
                connection.readTimeout = 10000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                if (path != "/v1/status" && BuildConfig.KARAOKE_SERVER_TOKEN.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.KARAOKE_SERVER_TOKEN}")
                }
                if (body != null) {
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connection.setRequestProperty("Idempotency-Key", requireNotNull(key))
                    connection.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val code = connection.responseCode
                if (code !in 200..299) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    val message = error?.let {
                        runCatching { JSONObject(it).getJSONObject("error").getString("message") }.getOrNull()
                    }
                    throw IOException(message ?: if (code == 401) "Сервер требует токен доступа" else "Ошибка сервера HTTP $code")
                }
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }
}

internal fun readLyrics(result: JSONObject): Pair<String, List<LyricTiming>> {
    val lines = result.getJSONArray("lines")
    val text = StringBuilder()
    val timings = mutableListOf<LyricTiming>()
    for (index in 0 until lines.length()) {
        if (index > 0) text.append('\n')
        val line = lines.getJSONObject(index)
        val lineText = line.getString("text")
        val offset = text.length
        text.append(lineText)
        val countBefore = timings.size
        val words = line.optJSONArray("words")
        var cursor = 0
        if (words != null) {
            for (wordIndex in 0 until words.length()) {
                val word = words.getJSONObject(wordIndex)
                val value = word.getString("text")
                val start = lineText.indexOf(value, cursor)
                if (value.isEmpty() || start < 0) continue
                cursor = start + value.length
                val startMs = word.getLong("startMs")
                val endMs = word.getLong("endMs")
                timings.add(LyricTiming(offset + start, offset + cursor, startMs, (endMs - startMs).milliseconds))
            }
        }
        if (lineText.isNotEmpty() && timings.size == countBefore && !line.isNull("startMs")) {
            timings.add(LyricTiming(offset, offset + lineText.length, line.getLong("startMs"), Duration.ZERO))
        }
    }
    return text.toString() to timings
}
