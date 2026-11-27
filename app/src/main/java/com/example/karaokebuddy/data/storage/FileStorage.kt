package com.example.karaokebuddy.data.storage

import android.content.Context
import com.example.karaokebuddy.data.json.songsFromJson
import com.example.karaokebuddy.data.json.songsToJson
import com.example.karaokebuddy.domain.model.Song
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID

class FileStorage internal constructor(
    private val file: File,
    private val now: () -> Instant = { Instant.now() },
) {
    constructor(context: Context) : this(File(context.filesDir, "songs.json"))

    private val items = linkedMapOf<UUID, Song>()

    val songs: List<Song>
        get() = songsByArchiveState(archived = false)

    val archivedSongs: List<Song>
        get() = songsByArchiveState(archived = true)

    private fun songsByArchiveState(archived: Boolean): List<Song> {
        val currentTime = now()
        return items.values.filter { song ->
            val expiresAt = song.expiresAt
            val isArchived = expiresAt != null && expiresAt <= currentTime
            isArchived == archived
        }
    }

    fun add(song: Song) {
        items[song.uid] = song
    }

    fun remove(uid: UUID) {
        items.remove(uid)
    }

    fun save() {
        Timber.i("save: start, %d songs", items.size)
        try {
            file.parentFile?.mkdirs()
            file.writeText(songsToJson(items.values))
            Timber.i("save: done, %d bytes", file.length())
        } catch (e: IOException) {
            Timber.e(e, "save: failed")
            throw e
        }
    }

    fun load() {
        if (!file.exists()) {
            items.clear()
            Timber.w("load: file does not exist, cleared the library")
            return
        }

        val loaded = songsFromJson(file.readText()).associateBy { it.uid }
        items.clear()
        items.putAll(loaded)
        Timber.d("load: %d songs restored", items.size)
    }
}
