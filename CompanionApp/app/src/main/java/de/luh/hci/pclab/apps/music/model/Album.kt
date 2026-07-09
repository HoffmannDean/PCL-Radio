package de.luh.hci.pclab.apps.music.model

import de.luh.hci.pclab.apps.music.data.AlbumEntity
import kotlinx.serialization.Serializable
import kotlin.Long

@Serializable
data class Album(
    val id: Long = 0,
    val name: String,
    val artistAl: String = "",
    var durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    var songCount: Int = 0
)

fun AlbumEntity.toDomain() = Album(
    id = id,
    name = name,
    artistAl = artistAl,
    durationMs = durationMs,
    createdAt = createdAt,
    songCount = songCount
)

fun Album.toEntity() = AlbumEntity(
    id = id,
    name = name,
    artistAl = artistAl,
    durationMs = durationMs,
    createdAt = createdAt,
    songCount = songCount
)
