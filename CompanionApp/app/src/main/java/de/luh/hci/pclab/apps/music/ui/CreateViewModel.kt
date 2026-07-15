package de.luh.hci.pclab.apps.music.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.luh.hci.pclab.RadioApp
import de.luh.hci.pclab.apps.music.data.DatabaseRepository
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import de.luh.hci.pclab.apps.music.model.toEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch

class CreateViewModel(
    private val repo: DatabaseRepository,
    private val album: Album?
) : ViewModel() {
    private val _deviceSongs = MutableStateFlow<List<Song>>(emptyList())
    val deviceSongs: StateFlow<List<Song>> = _deviceSongs
    var albumName by mutableStateOf(album?.name ?: "")
    var albumArtist by mutableStateOf(album?.artistAl ?: "")
    private val _preselectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val preselectedIds: StateFlow<Set<Long>> = _preselectedIds

    init {
        loaddeviceSongs()
        if (album != null) {
            viewModelScope.launch {
                val songs = repo.getSongsForAlbum(album.id).first()
                _preselectedIds.value = songs.map { it.mediaStoreId }.toSet()
            }
        }
    }

    fun loaddeviceSongs() = viewModelScope.launch {
        _deviceSongs.value = repo.queryDeviceSongs(null)
    }

    val songs: StateFlow<List<Song>> = _deviceSongs


    /*fun addSong(song: Song) = viewModelScope.launch {
        repo.addSongToAlbum(song.copy(albumId = album?.id ?: 0L))
        loaddeviceSongs() // refresh picker to remove added song
    }*/

    fun removeSong(song: Song) = viewModelScope.launch { repo.removeSongFromAlbum(song) }

    fun createAlbum(name: String, artist: String, songs: List<Song>, album: Album?) = viewModelScope.launch {
        repo.createAlbum(name, artist, songs)
        if (album != null) {
            repo.deleteAlbum(album.toEntity())
        }
    }

    companion object {
        fun provideFactory(album: Album? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RadioApp
                CreateViewModel(app.dbRepo, album)
            }
        }
    }
}