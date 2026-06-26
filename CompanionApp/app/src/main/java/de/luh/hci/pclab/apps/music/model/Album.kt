package de.luh.hci.pclab.apps.music.model

import de.luh.hci.pclab.apps.music.data.AlbumEntity
import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)

fun AlbumEntity.toDomain(songCount: Int = 0) = Album(
    id = id,
    name = name,
    createdAt = createdAt,
    songCount = songCount
)

fun Album.toEntity() = AlbumEntity(
    id = id,
    name = name,
    createdAt = createdAt
)
