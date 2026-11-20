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
import java.io.File

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
        val song = Song(name = "Демо-песня", status = SongStatus.QUEUED)
        val storage = FileStorage(file)
        storage.add(song)
        storage.save()

        val restored = FileStorage(file)
        restored.load()
        check(restored.songs == listOf(song))
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
