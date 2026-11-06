package com.example.karaokebuddy.data.json

import androidx.compose.ui.graphics.Color
import com.example.karaokebuddy.data.storage.FileStorage
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SongJsonTest {
    @Test
    fun songSurvivesJsonFileRoundTrip() {
        val original = Song(
            name = "Кино — Группа крови",
            status = SongStatus.READY,
            color = Color.Red,
            sourceUrl = "https://www.youtube.com/watch?v=example",
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
