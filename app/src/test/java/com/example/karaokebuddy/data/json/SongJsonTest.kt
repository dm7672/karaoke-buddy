package com.example.karaokebuddy.data.json

import androidx.compose.ui.graphics.Color
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SongJsonTest {
    @Test
    fun songSurvivesJsonRoundTrip() {
        val original = Song(
            name = "Кино — Группа крови",
            status = SongStatus.READY,
            color = Color.Red,
            sourceUrl = "https://www.youtube.com/watch?v=example",
            isFavorite = true,
        )

        val jsonText = original.json.toString()
        val restored = parse(JSONObject(jsonText))

        assertEquals(original, restored)
    }
}
