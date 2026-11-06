package com.example.karaokebuddy.domain.model

import kotlin.time.Duration

data class TimedText(
    val text: String,
    val startMs: Long,
    val duration: Duration,
)
