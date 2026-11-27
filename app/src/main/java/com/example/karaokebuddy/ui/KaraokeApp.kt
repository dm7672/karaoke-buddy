package com.example.karaokebuddy.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.karaokebuddy.data.network.ProcessingApi
import com.example.karaokebuddy.data.storage.FileStorage
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

private const val LIBRARY = "library"
private const val EDIT_SONG = "song?uid={uid}"

@Composable
internal fun KaraokeApp() {
    val context = LocalContext.current
    val storage = remember { FileStorage(context) }
    val api = remember { ProcessingApi() }
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val messages = remember { SnackbarHostState() }
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    var libraryLoaded by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf(ServerStatus.CHECKING) }
    var processingSong by remember { mutableStateOf<UUID?>(null) }

    fun reloadSongs() {
        songs = storage.songs + storage.archivedSongs
    }

    fun save(song: Song): Boolean {
        if (!libraryLoaded) return false
        val previous = songs.firstOrNull { it.uid == song.uid }
        storage.add(song)
        return try {
            storage.save()
            reloadSongs()
            true
        } catch (_: IOException) {
            if (previous == null) storage.remove(song.uid) else storage.add(previous)
            scope.launch { messages.showSnackbar("Не удалось сохранить песню") }
            false
        }
    }

    fun remove(song: Song) {
        storage.remove(song.uid)
        try {
            storage.save()
            reloadSongs()
        } catch (_: IOException) {
            storage.add(song)
            scope.launch { messages.showSnackbar("Не удалось удалить песню") }
        }
    }

    suspend fun checkServer() {
        server = ServerStatus.CHECKING
        server = try {
            if (api.availableWorkers() > 0) ServerStatus.AVAILABLE else ServerStatus.NO_WORKERS
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ServerStatus.UNAVAILABLE
        }
    }

    fun process(draft: Song, language: String) {
        if (processingSong != null) {
            scope.launch { messages.showSnackbar("Дождитесь текущего запроса") }
            return
        }
        val existing = songs.firstOrNull { it.uid == draft.uid && it.processingJobId != null }
        if (existing == null && !save(draft)) return
        processingSong = draft.uid
        scope.launch {
            try {
                val updated = if (existing == null) api.submit(draft, language) else api.update(existing)
                if (songs.any { it.uid == draft.uid }) save(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                messages.showSnackbar(e.message ?: "Не удалось получить ответ сервера")
            } finally {
                processingSong = null
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            storage.load()
            reloadSongs()
            libraryLoaded = true
        } catch (_: Exception) {
            messages.showSnackbar("Не удалось открыть библиотеку")
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(messages) }) { padding ->
        NavHost(nav, LIBRARY, Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable(LIBRARY) {
                LaunchedEffect(Unit) { checkServer() }
                SongLibraryScreen(
                    songs = songs,
                    server = server,
                    onRefresh = { scope.launch { checkServer() } },
                    onAdd = {
                        if (libraryLoaded) nav.navigate("song") { launchSingleTop = true }
                        else scope.launch { messages.showSnackbar("Библиотека недоступна") }
                    },
                    onEdit = { nav.navigate("song?uid=${it.uid}") { launchSingleTop = true } },
                    onRemove = ::remove,
                )
            }
            composable(
                route = EDIT_SONG,
                arguments = listOf(navArgument("uid") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val uid = rememberSaveable {
                    entry.arguments?.getString("uid") ?: UUID.randomUUID().toString()
                }
                val existing = songs.firstOrNull { it.uid.toString() == uid }
                val song = existing ?: remember(uid) { Song(uid = UUID.fromString(uid), name = "", status = SongStatus.QUEUED) }
                EditSongScreen(
                    song = song,
                    isNew = existing == null,
                    isProcessing = processingSong == song.uid,
                    onBack = { nav.popBackStack() },
                    onSave = { if (save(it)) nav.popBackStack() },
                    onProcess = ::process,
                )
            }
        }
    }
}
