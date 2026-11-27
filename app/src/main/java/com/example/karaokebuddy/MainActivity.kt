package com.example.karaokebuddy

import android.os.Bundle
import android.os.Debug
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.karaokebuddy.data.storage.FileStorage
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import com.example.karaokebuddy.ui.theme.KaraokeBuddyTheme
import timber.log.Timber
import java.io.File
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (BuildConfig.DEBUG) {
            demonstrateProfilerProblems()
            demonstrateFileStorage()
        }
        setContent {
            KaraokeBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun demonstrateFileStorage() {
        val file = File(filesDir, "hw2-demo.json")
        var now = Instant.parse("2026-11-20T12:00:00Z")
        val expiresAt = now.plusSeconds(1)
        val song = Song(name = "Демо-песня", status = SongStatus.QUEUED, expiresAt = expiresAt)
        val storage = FileStorage(file) { now }
        storage.add(song)
        storage.save()

        val restored = FileStorage(file) { now }
        restored.load()
        check(restored.songs == listOf(song))

        now = expiresAt
        check(restored.songs.isEmpty())
        check(restored.archivedSongs == listOf(song))
        Timber.i("archive: %d active, %d archived", restored.songs.size, restored.archivedSongs.size)
    }

    private fun demonstrateProfilerProblems() {
        if (intent.getBooleanExtra("hw2_leak", false)) {
            retainedActivities.add(this)
        } else {
            retainedActivities.clear()
        }
        if (intent.getBooleanExtra("hw2_delay", false)) {
            demonstrateSlowStartup()
        }
    }

    private fun demonstrateSlowStartup() {
        val end = Debug.threadCpuTimeNanos() + 800_000_000_0
        var checksum = 1
        do {
            for (number in 0 until 100_000) {
                checksum = (checksum xor number) * 31
            }
        } while (Debug.threadCpuTimeNanos() < end)
        Timber.d("profiler CPU demo checksum: %d", checksum)
    }

    private companion object {
        val retainedActivities = mutableListOf<MainActivity>()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KaraokeBuddyTheme {
        Greeting("Android")
    }
}
