package com.example.musicactivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicInfo(
    val song: String = "Nothing playing",
    val artist: String = "",
    val app: String = "",
    val artwork: String? = null
)

object MusicState {

    private val _musicInfo = MutableStateFlow(
        MusicInfo()
    )

    val musicInfo = _musicInfo.asStateFlow()


    private val _sharingEnabled = MutableStateFlow(true)

    val sharingEnabled = _sharingEnabled.asStateFlow()


    fun update(
        song: String,
        artist: String,
        app: String,
        artwork: String? = null
    ) {
        _musicInfo.value = MusicInfo(
            song = song,
            artist = artist,
            app = app,
            artwork = artwork
        )
    }


    fun setSharingEnabled(enabled: Boolean) {
        _sharingEnabled.value = enabled
    }
}