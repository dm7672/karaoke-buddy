package com.example.karaokebuddy.data.storage

import com.example.karaokebuddy.data.json.json
import com.example.karaokebuddy.data.json.parse
import com.example.karaokebuddy.domain.model.Song
import org.json.JSONArray
import java.io.File
import java.util.UUID

class FileStorage(private val file: File) {
    private val items = linkedMapOf<UUID, Song>()

    val songs: List<Song>
        get() = items.values.toList()

    // Песня с таким же uid заменяет предыдущую запись.
    fun add(song: Song) {
        items[song.uid] = song
    }

    fun remove(uid: UUID) {
        items.remove(uid)
    }

    fun save() {
        val json = JSONArray()
        for (song in items.values) {
            json.put(song.json)
        }
        file.writeText(json.toString(2))
    }

    fun load() {
        if (!file.exists()) {
            items.clear()
            return
        }

        val json = JSONArray(file.readText())
        val loaded = linkedMapOf<UUID, Song>()
        for (index in 0 until json.length()) {
            val entry = json.optJSONObject(index) ?: continue
            val song = parse(entry) ?: continue
            loaded[song.uid] = song
        }

        items.clear()
        items.putAll(loaded)
    }
}
