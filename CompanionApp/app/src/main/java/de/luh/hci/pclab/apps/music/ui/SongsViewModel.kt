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

class SongsViewModel(
    private val repo: DatabaseRepository,
    val album: Album?
) : ViewModel() {

    val songs: StateFlow<List<Song>> = (if (album == null) repo.getAllSongs() else repo.getSongsForAlbum(album.id))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /*fun addSong(song: Song) = viewModelScope.launch {
        repo.addSongToAlbum(song.copy(albumId = album?.id ?: 0L))
    }*/

    fun removeSong(song: Song) = viewModelScope.launch { repo.removeSongFromAlbum(song) }

    companion object {
        fun provideFactory(album: Album?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RadioApp)
                SongsViewModel(application.dbRepo, album)
            }
        }
    }
}