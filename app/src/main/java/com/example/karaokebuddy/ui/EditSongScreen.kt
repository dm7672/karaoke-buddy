package com.example.karaokebuddy.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.karaokebuddy.domain.model.Song
import com.example.karaokebuddy.domain.model.SongDifficulty
import com.example.karaokebuddy.domain.model.SongStatus
import com.example.karaokebuddy.ui.theme.KaraokeBuddyTheme
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val presets = listOf(
    "Белый" to Color.White,
    "Лиловый" to Color(0xFFC9B3FF),
    "Мятный" to Color(0xFFA5DAC7),
    "Персиковый" to Color(0xFFFFCB97),
)

@Composable
internal fun EditSongScreen(
    song: Song,
    isNew: Boolean,
    isProcessing: Boolean,
    onBack: () -> Unit,
    onSave: (Song) -> Unit,
    onProcess: (Song, String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var name by rememberSaveable { mutableStateOf(song.name) }
    var link by rememberSaveable { mutableStateOf(song.sourceUrl?.toString().orEmpty()) }
    var date by rememberSaveable {
        mutableStateOf(song.expiresAt?.atZone(zone)?.minusNanos(1)?.toLocalDate()?.toString().orEmpty())
    }
    var archived by rememberSaveable { mutableStateOf(song.expiresAt?.let { it <= Instant.now() } == true) }
    var color by rememberSaveable { mutableStateOf(song.color.toArgb()) }
    var difficulty by rememberSaveable { mutableStateOf(song.difficulty.name) }
    var language by rememberSaveable { mutableStateOf("ru") }
    val sourceUrl = runCatching { URI(link.trim()) }.getOrNull()?.takeIf {
        it.host != null && (it.scheme.equals("https", ignoreCase = true) || it.scheme.equals("http", ignoreCase = true))
    }
    val invalidLink = link.isNotBlank() && sourceUrl == null
    val canSave = name.isNotBlank() && !invalidLink

    fun editedSong(): Song {
        val expiration = date.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it).plusDays(1).atStartOfDay(zone).toInstant() }
        val now = Instant.now()
        val expiresAt = if (archived) song.expiresAt?.takeIf { it <= now } ?: now else expiration
        return song.copy(
            name = name.trim(),
            sourceUrl = sourceUrl,
            color = Color(color),
            expiresAt = expiresAt,
            difficulty = SongDifficulty.valueOf(difficulty),
        )
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Назад") }
                Text(if (isNew) "Новая песня" else "Редактирование", style = MaterialTheme.typography.headlineSmall)
            }
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().background(
                        if (archived) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        MaterialTheme.shapes.extraSmall,
                    ),
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Ссылка на песню") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = invalidLink,
                    supportingText = if (invalidLink) ({ Text("Введи ссылку с http:// или https://") }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                ArchiveFields(date, archived, onDate = { date = it }, onArchive = {
                    archived = it
                    if (!it && date.isNotEmpty() && LocalDate.parse(date) < LocalDate.now()) date = ""
                })
                ColorFields(color) { color = it }
                DifficultyFields(difficulty) { difficulty = it }
                Button(onClick = { onSave(editedSong()) }, enabled = canSave && !isProcessing) { Text("Сохранить") }
                HorizontalDivider()
                ProcessingResult(
                    song = song,
                    language = language,
                    onLanguage = { language = it },
                    isProcessing = isProcessing,
                    enabled = song.processingJobId != null || canSave,
                    onProcess = { onProcess(editedSong(), language) },
                )
            }
        }
    }
}

@Composable
private fun ArchiveFields(date: String, archived: Boolean, onDate: (String) -> Unit, onArchive: (Boolean) -> Unit) {
    val context = LocalContext.current
    val day = date.takeIf { it.isNotEmpty() }?.let(LocalDate::parse)
    Text("Дата автоархивации", style = MaterialTheme.typography.titleSmall)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = {
            val initial = day ?: LocalDate.now()
            DatePickerDialog(context, { _, year, month, selectedDay ->
                onDate(LocalDate.of(year, month + 1, selectedDay).toString())
            }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
        }) { Text(day?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Выбрать дату") }
        if (day != null) TextButton(onClick = { onDate("") }) { Text("Сбросить") }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = archived, onCheckedChange = onArchive)
        Text("В архиве")
    }
}

@Composable
private fun ColorFields(color: Int, onSelect: (Int) -> Unit) {
    Text("Цвет карточки", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((label, preset) in presets) {
            val selectedColor = color == preset.toArgb()
            Surface(
                onClick = { onSelect(preset.toArgb()) },
                modifier = Modifier.size(42.dp).semantics { contentDescription = label; selected = selectedColor },
                color = preset,
                contentColor = Color.Black,
                shape = RectangleShape,
                border = BorderStroke(1.dp, Color.Black),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selectedColor) Text("✓")
                }
            }
        }
    }
}

@Composable
private fun DifficultyFields(current: String, onSelected: (String) -> Unit) {
    Text("Сложность исполнения", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (difficulty in SongDifficulty.entries) {
            val label = when (difficulty) {
                SongDifficulty.EASY -> "Легко"
                SongDifficulty.NORMAL -> "Обычно"
                SongDifficulty.HARD -> "Сложно"
            }
            FilterChip(selected = current == difficulty.name, onClick = { onSelected(difficulty.name) }, label = { Text(label) })
        }
    }
}

@Composable
private fun ProcessingResult(
    song: Song,
    language: String,
    onLanguage: (String) -> Unit,
    isProcessing: Boolean,
    enabled: Boolean,
    onProcess: () -> Unit,
) {
    Text("Результат обработки", style = MaterialTheme.typography.titleMedium)
    Text(if (isProcessing) "Запрос к серверу…" else song.processingLabel())
    if (song.processingJobId == null) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = language == "ru", onClick = { onLanguage("ru") }, label = { Text("Русский") })
            FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("Английский") })
        }
    }
    if (song.status != SongStatus.READY) {
        TextButton(onClick = onProcess, enabled = enabled && !isProcessing) {
            Text(if (song.processingJobId == null) "Отправить на обработку" else "Обновить результат")
        }
    }
    val lines = song.lyricLines()
    if (song.status == SongStatus.READY && lines.isEmpty()) Text("Сервер вернул результат без текста")
    for (line in lines) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            line.startMs?.let { ms ->
                val time = String.format(Locale.ROOT, "%02d:%02d.%d", ms / 60000, ms / 1000 % 60, ms / 100 % 10)
                Text(time, color = MaterialTheme.colorScheme.primary)
            }
            Text(line.text, Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditSongPreview() {
    KaraokeBuddyTheme {
        EditSongScreen(Song(name = "Ночной поезд", status = SongStatus.QUEUED), false, false, {}, {}, { _, _ -> })
    }
}
