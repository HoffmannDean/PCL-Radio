package de.luh.hci.pclab.apps.music.model

import android.media.Image
import java.util.Date

data class Song(
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMilliseconds: Long,
    val dateMade: Date,
    val dateAdded: Date,
    val coverImage: Image
)
