package com.example.karaokebuddy.domain.model

import androidx.compose.ui.graphics.Color
import java.util.UUID
data class Song(
    val uid: UUID = UUID.randomUUID(),
    val name: String,
    val status: SongStatus,
    val color: Color = Color.White,
    val sourceUrl: String? = null,
    val isFavorite: Boolean = false,
)
