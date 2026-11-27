package com.example.karaokebuddy.domain.model

import androidx.compose.ui.graphics.Color
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.UUID

data class Song(
    val uid: UUID = UUID.randomUUID(),
    val name: String,
    val status: SongStatus,
    val color: Color = Color.White,
    val sourceUrl: URI? = null,
    val instrumentalPath: File? = null,
    val vocalPath: File? = null,
    val lyricsText: String? = null,
    val lyricTimings: List<LyricTiming> = emptyList(),
    val isFavorite: Boolean = false,
    val expiresAt: Instant? = null,
) {
    fun lyricLines(): List<LyricLine> {
        val text = lyricsText ?: return emptyList()
        if (text.isEmpty()) return emptyList()

        val lines = mutableListOf<LyricLine>()
        var lineStart = 0
        for (line in text.lineSequence()) {
            val lineEnd = lineStart + line.length
            val timings = mutableListOf<LyricTiming>()
            for (timing in lyricTimings) {
                if (timing.startIndex < lineStart || timing.endIndex > lineEnd ||
                    timing.startIndex >= timing.endIndex
                ) continue

                timings.add(timing.copy(
                    startIndex = timing.startIndex - lineStart,
                    endIndex = timing.endIndex - lineStart,
                ))
            }
            lines.add(LyricLine(line, timings))
            lineStart = lineEnd + if (text.startsWith("\r\n", lineEnd)) 2 else 1
        }
        return lines
    }

    companion object
}
