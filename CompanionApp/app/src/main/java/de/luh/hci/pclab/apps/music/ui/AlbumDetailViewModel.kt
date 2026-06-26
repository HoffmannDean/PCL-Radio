package de.luh.hci.pclab.apps.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.luh.hci.pclab.RadioApp
import de.luh.hci.pclab.apps.music.data.DatabaseRepository
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val repo: DatabaseRepository,
    val album: Album
) : ViewModel() {

    val songs = repo.getSongsForAlbum(album.id).stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5000),
        emptyList()
    )

    private val _storedSongs = MutableStateFlow<List<Song>>(emptyList())
    val storedSongs: StateFlow<List<Song>> = _storedSongs

    init {
        loadStoredSongs()
    }

    fun loadStoredSongs() = viewModelScope.launch {
        _storedSongs.value = repo.queryDeviceSongs(album.id)
    }

    fun addSong(song: Song) = viewModelScope.launch {
        repo.addSongToAlbum(song.copy(albumId = album.id))
        loadStoredSongs() // refresh picker to remove added song
    }

    fun removeSong(song: Song) = viewModelScope.launch { repo.removeSongFromAlbum(song) }

    companion object {
        fun provideFactory(album: Album): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as RadioApp)
                AlbumDetailViewModel(application.dbRepo, album)
            }
        }
    }
}