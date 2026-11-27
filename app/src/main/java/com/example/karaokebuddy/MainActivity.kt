package com.example.karaokebuddy

import android.os.Bundle
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
