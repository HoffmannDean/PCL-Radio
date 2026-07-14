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
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayViewModel(
    private val repo: MusicPlayerRepository,
    private val dbRepo: DatabaseRepository,
    private val songId: Long?,
    private val albumId: Long?
) : ViewModel() {

    val currentSong: StateFlow<Song?> = repo.currentSong

    val isPlaying: StateFlow<Boolean> = repo.isPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    val allAlbums: StateFlow<List<Album>> = dbRepo.getAllAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (songId != null) {
                val song = repo.getSongById(songId)
                if (song != null && repo.currentSong.value?.id != songId) {
                    repo.play(song, playlistFlow)
                }
            }
        }

        viewModelScope.launch {
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

    private val playlistFlow: Flow<List<Song>>
        get() = if (albumId != null) dbRepo.getSongsForAlbum(albumId) else dbRepo.getAllSongs()

    fun remove() {
        val current = currentSong.value ?: return
        viewModelScope.launch {
            dbRepo.removeSongFromAlbum(current)
            repo.refreshPlaylist(playlistFlow)
        }
    }

    fun addSongToAlbum(song: Song, album: Album) {
        if (song.albumId == album.id) return
        viewModelScope.launch {
            dbRepo.addSongToAlbum(song, album)
            repo.refreshPlaylist(playlistFlow)
        }
    }
    companion object {
        fun provideFactory(songId: Long?, albumId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RadioApp
                PlayViewModel(app.playerRepo, app.dbRepo, songId, albumId)
            }
        }
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RadioApp
                PlayViewModel(app.playerRepo, app.dbRepo, null, null)
            }
        }
    }
}