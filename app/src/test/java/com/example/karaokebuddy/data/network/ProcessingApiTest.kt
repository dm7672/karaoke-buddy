package com.example.karaokebuddy.data.network

import com.example.karaokebuddy.domain.model.LyricTiming
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ProcessingApiTest {
    @Test
    fun responseKeepsRepeatedWordsAndLineOnlyTimings() {
        val result = JSONObject("""
            {"lines":[
              {"text":"да да", "startMs":1000, "words":[
                {"text":"да", "startMs":1000, "endMs":1200},
                {"text":"да", "startMs":1300, "endMs":1500}
              ]},
              {"text":"нет", "startMs":2000}
            ]}
        """.trimIndent())

        val (text, timings) = readLyrics(result)

        assertEquals("да да\nнет", text)
        assertEquals(listOf(
            LyricTiming(0, 2, 1000, 200.milliseconds),
            LyricTiming(3, 5, 1300, 200.milliseconds),
            LyricTiming(6, 9, 2000, Duration.ZERO),
        ), timings)
    }
}
