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

class MusicPlayerRepository(context: Context, private val songDao: SongDao) {

    private val player = ExoPlayer.Builder(context).build()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
        })
    }

    fun play(song: Song) {
        _currentSong.value = song
        player.setMediaItem(MediaItem.fromUri(song.uri))
        player.prepare()
        player.play()
    }

    suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.toDomain()
    }

    fun togglePause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stop() {
        player.stop()
        _currentSong.value = null
    }

    fun release() {
        player.release()
    }

    fun currentPosition(): Long = player.currentPosition

    fun pause() {
        player.pause()
    }

    fun previous() {
        player.seekToPreviousMediaItem()
    }

    fun next() {
        player.seekToNextMediaItem()
    }
}
