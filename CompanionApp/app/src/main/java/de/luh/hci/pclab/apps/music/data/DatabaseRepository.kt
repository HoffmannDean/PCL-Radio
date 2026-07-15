package de.luh.hci.pclab.apps.music.data

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import de.luh.hci.pclab.apps.music.model.toDomain
import de.luh.hci.pclab.apps.music.model.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatabaseRepository(
    private val albumDao: AlbumDao,
    private val songDao: SongDao,
    private val contentResolver: ContentResolver
) {
    val albums: Flow<List<Album>> = albumDao.getAllAlbums().map {
        list -> list.map { it.toDomain() }
    }

    fun getAllAlbums(): Flow<List<Album>> = albums

    suspend fun createAlbum(name: String, artistAl: String, songs: List<Song>): Long {
        val albumId = albumDao.insert(AlbumEntity(
            name = name,
            artistAl = artistAl,
            durationMs = songs.sumOf { it.durationMs },
            songCount = songs.size
        ))

        songs.forEach { song ->
            if (song.albumId != 0L) {
                songDao.update(song.copy(albumId = albumId, album = name).toEntity())}
            else{
                songDao.insert(song.copy(albumId = albumId, album=name).toEntity())
            }
        }

        return albumId
    }

    suspend fun deleteAlbum(album: AlbumEntity) {
        val songs = songDao.getSongsForAlbum(album.id).first()
        songs.forEach { song ->
            songDao.delete(song)
        }
        albumDao.delete(album)
    }


    fun getSongsForAlbum(albumId: Long): Flow<List<Song>> =
        songDao.getSongsForAlbum(albumId).map { list -> list.map { it.toDomain() } }

    suspend fun addSongToAlbum(song: Song, album: Album) {
        val songInDb = songDao.getSongById(song.id)
        if (songInDb != null) {
            // Already in DB, update albumId
            songDao.update(songInDb.copy(albumId = album.id))
        } else {
            // New from device, insert
            songDao.insert(song.copy(albumId = album.id).toEntity())
        }
        
        val a = albumDao.getAlbumById(album.id) ?: return
        albumDao.updateAlbum(
            a.copy(
                durationMs = a.durationMs + song.durationMs,
                songCount = a.songCount + 1
            )
        )
    }

    suspend fun removeSongFromAlbum(song: Song?) {
        if (song == null) return
        val albumId = song.albumId
        if (albumId == 0L) return

        songDao.delete(song.toEntity())

        val album = albumDao.getAlbumById(albumId) ?: return
        albumDao.updateAlbum(
            album.copy(
                durationMs = (album.durationMs - song.durationMs).coerceAtLeast(0L),
                songCount = if (album.songCount > 0) album.songCount - 1 else 0
            )
        )
    }



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

    fun getAllSongs(): Flow<List<Song>> =
        songDao.getAllSongs().map { list -> list.map { it.toDomain() } }
}