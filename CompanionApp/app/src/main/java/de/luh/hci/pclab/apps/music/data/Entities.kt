package de.luh.hci.pclab.apps.music.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "songs",
    foreignKeys = [ForeignKey(
        entity = AlbumEntity::class,
        parentColumns = ["id"],
        childColumns = ["albumId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("albumId")]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: Long,
    val mediaStoreId: Long,       // MediaStore._ID
    val uri: String,              // content:// URI
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val dateAdded: Long           // epoch seconds from MediaStore
)
