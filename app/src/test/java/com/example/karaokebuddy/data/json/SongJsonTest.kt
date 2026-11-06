package com.example.karaokebuddy.data.json

import androidx.compose.ui.graphics.Color
import com.example.karaokebuddy.data.storage.FileStorage
import com.example.karaokebuddy.domain.model.OffsetLine
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import com.example.karaokebuddy.domain.model.TimedLyrics
import com.example.karaokebuddy.domain.model.TimedText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class SongJsonTest {
    @Test
    fun songSurvivesJsonFileRoundTrip() {
        val original = Song(
            name = "Кино — Группа крови",
            status = SongStatus.READY,
            color = Color.Red,
            sourceUrl = "https://www.youtube.com/watch?v=example",
            instrumentalPath = "audio/instrumental.wav",
            vocalPath = "audio/vocal.wav",
            timedLyrics = TimedLyrics(listOf(
                OffsetLine(
                    startMs = 1000,
                    duration = 1000.milliseconds,
                    words = listOf(
                        TimedText("Пример", 1000, 500.milliseconds),
                        TimedText("текста", 1500, 500.milliseconds),
                    ),
                ),
            )),
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
