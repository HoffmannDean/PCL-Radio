package de.luh.hci.pclab.apps.music.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Dao
import de.luh.hci.pclab.apps.music.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import de.luh.hci.pclab.apps.music.data.SongDao
import de.luh.hci.pclab.apps.music.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MusicPlayerRepository(context: Context, private val songDao: SongDao) {

    private val player = ExoPlayer.Builder(context).build()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var currentPlaylist: List<Song> = emptyList()
    private var currentIndex: Int = 0

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)?.toDomain()

    fun togglePause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stop() {
        player.stop()
        _currentSong.value = null
    }

    fun release() = player.release()

    fun currentPosition(): Long = player.currentPosition

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    /** Song abspielen, Queue unangetastet lassen. */
    private fun playSong(song: Song) {
        _currentSong.value = song
        player.setMediaItem(MediaItem.fromUri(song.uri))
        player.prepare()
        player.play()
    }

    /** Song abspielen und Queue aus dem Flow setzen. */
    suspend fun play(song: Song, playlistFlow: Flow<List<Song>>) {
        val playlist = playlistFlow.first()
        currentPlaylist = if (playlist.any { it.id == song.id }) playlist else listOf(song)
        currentIndex = currentPlaylist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playSong(song)
    }

    /** Queue neu laden, z.B. nach dem Entfernen eines Songs. */
    suspend fun refreshPlaylist(playlistFlow: Flow<List<Song>>) {
        val newList = playlistFlow.first()
        val currentId = _currentSong.value?.id

        currentPlaylist = newList

        if (newList.isEmpty()) {
            currentIndex = 0
            player.stop()
            player.clearMediaItems()
            _currentSong.value = null      // ⇒ UI zeigt "No song selected"
            return
        }

        val idx = newList.indexOfFirst { it.id == currentId }
        if (idx >= 0) {
            currentIndex = idx             // aktueller Song noch da: läuft weiter
        } else {
            currentIndex = currentIndex.coerceIn(0, newList.lastIndex)
            playSong(newList[currentIndex])  // Song ist raus: nächsten anspielen
        }
    }

    fun next() {
        if (currentPlaylist.isEmpty()) return
        currentIndex = (currentIndex + 1) % currentPlaylist.size
        playSong(currentPlaylist[currentIndex])
    }

    fun previous() {
        if (currentPlaylist.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) currentPlaylist.size - 1 else currentIndex - 1
        playSong(currentPlaylist[currentIndex])
    }
}
