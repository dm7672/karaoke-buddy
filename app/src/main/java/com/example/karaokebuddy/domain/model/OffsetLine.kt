package com.example.karaokebuddy.domain.model

import kotlin.time.Duration

data class OffsetLine(
    val startMs: Long,
    val duration: Duration,
    val words: List<TimedText>,
)
