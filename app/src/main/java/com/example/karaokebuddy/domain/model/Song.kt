package com.example.karaokebuddy.domain.model

import androidx.compose.ui.graphics.Color
import java.util.UUID
data class Song(
    val uid: UUID = UUID.randomUUID(),
    val name: String,
    val status: SongStatus,
    val color: Color = Color.White,
    val sourceUrl: String? = null,
    val instrumentalPath: String? = null,
    val vocalPath: String? = null,
    val timedLyrics: TimedLyrics? = null,
    val isFavorite: Boolean = false,
)
