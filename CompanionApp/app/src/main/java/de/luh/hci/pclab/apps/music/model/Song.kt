package de.luh.hci.pclab.apps.music.model

import android.net.Uri
import de.luh.hci.pclab.apps.music.data.SongEntity
import androidx.core.net.toUri

data class Song(
    val id: Long = 0,
    val albumId: Long = 0,
    val mediaStoreId: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val dateAdded: Long
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val sizeMb: String get() = "%.1f MB".format(sizeBytes / 1_048_576f)
}

fun SongEntity.toDomain() = Song(
    id = id,
    albumId = albumId,
    mediaStoreId = mediaStoreId,
    uri = uri.toUri(),
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    dateAdded = dateAdded
)

fun Song.toEntity() = SongEntity(
    id = id,
    albumId = albumId,
    mediaStoreId = mediaStoreId,
    uri = uri.toString(),
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    dateAdded = dateAdded
)




