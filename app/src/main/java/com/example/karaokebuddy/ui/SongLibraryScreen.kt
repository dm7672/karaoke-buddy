package com.example.karaokebuddy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongStatus
import kotlinx.coroutines.launch
import java.time.Instant

internal enum class ServerStatus(val label: String) {
    CHECKING("Проверяем сервер…"),
    AVAILABLE("Обработка доступна"),
    NO_WORKERS("Сервер на связи · нет исполнителей"),
    UNAVAILABLE("Сервер недоступен"),
}

@Composable
internal fun SongLibraryScreen(
    songs: List<Song>,
    server: ServerStatus,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Song) -> Unit,
    onRemove: (Song) -> Unit,
) {
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = onAdd) { Text("＋ Песня", Modifier.padding(horizontal = 16.dp)) }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text("Библиотека", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    server.label,
                    Modifier.weight(1f),
                    color = if (server == ServerStatus.UNAVAILABLE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onRefresh, enabled = server != ServerStatus.CHECKING) { Text("Обновить") }
            }
            if (songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("В библиотеке пока нет песен")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(songs, key = { it.uid.toString() }) { song ->
                        SongRow(song, onEdit = { onEdit(song) }, onRemove = { onRemove(song) })
                    }
                    item { Box(Modifier.padding(bottom = 80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onEdit: () -> Unit, onRemove: () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    TextButton(onClick = { scope.launch { state.reset() } }) { Text("Отмена") }
                    TextButton(onClick = onRemove) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
    ) {
        Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (song.color != Color.White) Text("●", color = song.color)
                    Text(song.name, style = MaterialTheme.typography.titleMedium)
                }
                if (song.expiresAt?.let { it <= Instant.now() } == true) Text("В архиве")
                Text(
                    song.processingLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (song.status == SongStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (song.status == SongStatus.PROCESSING) {
                    val progress = song.processingProgress
                    if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { progress.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

internal fun Song.processingLabel(): String = when (status) {
    SongStatus.QUEUED -> if (processingJobId == null) "Не отправлена" else "В очереди"
    SongStatus.PROCESSING -> processingProgress?.let { "Обработка · $it%" } ?: "Обработка"
    SongStatus.READY -> "Готово"
    SongStatus.ERROR -> processingMessage ?: "Ошибка обработки"
}
