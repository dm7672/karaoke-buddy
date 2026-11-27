package com.example.karaokebuddy.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Индексы в timings отсчитываются от начала text. */
data class LyricLine(
    val text: String,
    val timings: List<LyricTiming>,
) {
    val startMs: Long? = timings.minOfOrNull { it.startMs }
    val duration: Duration? = startMs?.let { start ->
        timings.maxOf { (it.startMs - start).milliseconds + it.duration }
    }
}
