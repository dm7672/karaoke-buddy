package com.example.karaokebuddy.domain.model

import androidx.compose.ui.graphics.Color
import java.io.File
import java.net.URI
import java.util.UUID

data class Song(
    val uid: UUID = UUID.randomUUID(),
    val name: String,
    val status: SongStatus,
    val color: Color = Color.White,
    val sourceUrl: URI? = null,
    val instrumentalPath: File? = null,
    val vocalPath: File? = null,
    val timedLyrics: TimedLyrics? = null,
    val isFavorite: Boolean = false,
)
