package de.luh.hci.pclab.apps.music.ui

import android.util.SparseBooleanArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.luh.hci.pclab.RadioApp
import de.luh.hci.pclab.apps.music.data.DatabaseRepository
import de.luh.hci.pclab.apps.music.data.MusicPlayerRepository
import de.luh.hci.pclab.apps.music.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayViewModel(
    private val repo: MusicPlayerRepository,
    private val dbRepo: DatabaseRepository,
    private val songId: Long?
) : ViewModel() {

    val currentSong: StateFlow<Song?> = repo.currentSong

    val isPlaying: StateFlow<Boolean> = repo.isPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    init {
        viewModelScope.launch {
            if (songId != null) {
                val song = repo.getSongById(songId)
                if (song != null && repo.currentSong.value?.id != songId) {
                    val albumSongs = if (song.albumId != 0L) {
                        dbRepo.getSongsForAlbum(song.albumId)
                    } else {
                        null // Or a flow of all device songs if available
                    }
                    repo.play(song, albumSongs)
                }
            }
            while (true) {
                _position.value = repo.currentPosition()
                delay(500)
            }
        }
    }

    fun playPause() = repo.togglePause()
    fun next() = repo.next()
    fun previous() = repo.previous()
    fun seekTo(positionMs: Long) = repo.seekTo(positionMs)
    fun remove() {
        viewModelScope.launch {
            currentSong.value?.let { dbRepo.removeSongFromAlbum(it) }
            repo.next() //TODO: bei letztem Song in der letzte wechseln zu Default Ansciht
        }
    }
    companion object {
        fun provideFactory(songId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RadioApp
                PlayViewModel(app.playerRepo, app.dbRepo, songId)
            }
        }
    }
}