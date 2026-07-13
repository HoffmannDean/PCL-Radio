package de.luh.hci.pclab.apps.music.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: Long): AlbumEntity?
    @Insert
    suspend fun insert(album: AlbumEntity): Long

    @Delete
    suspend fun delete(album: AlbumEntity)

    @Update
    fun updateAlbum(album: AlbumEntity)
}

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY title ASC")
    fun getSongsForAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntity)

    @Query("UPDATE songs SET albumId = :albumId WHERE id = :songId")
    fun addSongToAlbum(songId: Long, albumId: Long)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT mediaStoreId FROM songs WHERE albumId = :albumId")
    suspend fun getMediaStoreIdsForAlbum(albumId: Long): List<Long>

}