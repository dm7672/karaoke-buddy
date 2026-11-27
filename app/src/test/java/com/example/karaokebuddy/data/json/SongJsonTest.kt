package com.example.karaokebuddy.data.json

import androidx.compose.ui.graphics.Color
import com.example.karaokebuddy.data.storage.FileStorage
import com.example.karaokebuddy.domain.model.LyricTiming
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

class SongJsonTest {
    @Test
    fun songSurvivesJsonFileRoundTrip() {
        val original = Song(
            name = "Кино — Группа крови",
            status = SongStatus.READY,
            color = Color.Red,
            sourceUrl = URI.create("https://www.youtube.com/watch?v=example"),
            instrumentalPath = File("audio/instrumental.wav"),
            vocalPath = File("audio/vocal.wav"),
            lyricsText = "Пример\nтекста",
            lyricTimings = listOf(
                LyricTiming(0, 6, 1000, 500.milliseconds),
                LyricTiming(7, 13, 1500, 500.milliseconds),
            ),
            isFavorite = true,
        )

        val file = File.createTempFile("songs-", ".json")
        try {
            val storage = FileStorage(file)
            storage.add(original)
            storage.save()

            val restoredStorage = FileStorage(file)
            restoredStorage.load()

            assertEquals(original, restoredStorage.songs.single())
        } finally {
            file.delete()
        }
    }
}
