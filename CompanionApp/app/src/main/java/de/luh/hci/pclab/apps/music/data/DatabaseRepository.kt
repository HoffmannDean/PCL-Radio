package de.luh.hci.pclab.apps.music.data

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import de.luh.hci.pclab.apps.music.model.toDomain
import de.luh.hci.pclab.apps.music.model.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DatabaseRepository(
    private val albumDao: AlbumDao,
    private val songDao: SongDao,
    private val contentResolver: ContentResolver
) {
    val albums: Flow<List<Album>> = albumDao.getAllAlbums().map {
        list -> list.map { it.toDomain() }
    }

    suspend fun createAlbum(name: String): Long =
        albumDao.insert(AlbumEntity(name = name))

    suspend fun deleteAlbum(album: AlbumEntity) =
        albumDao.delete(album)

    fun getSongsForAlbum(albumId: Long): Flow<List<Song>> =
        songDao.getSongsForAlbum(albumId).map { list -> list.map { it.toDomain() } }

    suspend fun addSongToAlbum(song: Song) =
        songDao.insert(song.toEntity())

    suspend fun removeSongFromAlbum(song: Song) =
        songDao.delete(song.toEntity())



    suspend fun queryDeviceSongs(albumId: Long? = null): List<Song> = withContext(Dispatchers.IO) {
        val alreadyAdded = if (albumId != null) songDao.getMediaStoreIdsForAlbum(albumId).toSet() else emptySet()
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED
        )

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val msId = cursor.getLong(idCol)
                if (msId in alreadyAdded) continue   // skip already-added songs
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, msId
                )
                songs.add(Song(
                    albumId      = albumId ?: 0L,
                    mediaStoreId = msId,
                    uri          = uri,
                    title        = cursor.getString(titleCol) ?: "Unknown",
                    artist       = cursor.getString(artistCol) ?: "Unknown Artist",
                    album        = cursor.getString(albumCol) ?: "Unknown Album",
                    durationMs   = cursor.getLong(durCol),
                    sizeBytes    = cursor.getLong(sizeCol),
                    mimeType     = cursor.getString(mimeCol) ?: "audio/*",
                    dateAdded    = cursor.getLong(dateCol)
                ))
            }
        }
        songs
    }
}