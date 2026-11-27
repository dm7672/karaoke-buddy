package com.example.karaokebuddy.domain.model

import kotlin.time.Duration

/** Диапазон [startIndex, endIndex) в тексте, к которому относится отметка. */
data class LyricTiming(
    val startIndex: Int,
    val endIndex: Int,
    val startMs: Long,
    val duration: Duration,
)
