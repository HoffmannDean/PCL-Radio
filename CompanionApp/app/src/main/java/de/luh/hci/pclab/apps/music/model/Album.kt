package de.luh.hci.pclab.apps.music.model

import java.util.Date

data class Album(
    val title: String,
    val dateAdded: Date,
    val songs: List<Song>
)
