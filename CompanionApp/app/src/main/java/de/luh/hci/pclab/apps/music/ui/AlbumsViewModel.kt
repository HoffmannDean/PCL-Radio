package de.luh.hci.pclab.apps.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.luh.hci.pclab.RadioApp
import de.luh.hci.pclab.apps.music.data.AlbumEntity
import de.luh.hci.pclab.apps.music.data.DatabaseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumsViewModel(
    private val musicRepo: DatabaseRepository
) : ViewModel() {
    val albums = musicRepo.albums.stateIn(
        viewModelScope,
            SharingStarted.Companion.WhileSubscribed(5000),
            emptyList()
        )

    fun loadAlbums() {

    }

    /*fun createAlbum(name: String) {
       viewModelScope.launch {
           musicRepo.createAlbum(name)
       }
    }*/

    fun deleteAlbum(album: AlbumEntity) {
        viewModelScope.launch {
            musicRepo.deleteAlbum(album)
        }
    }

    fun addSong() {

    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as RadioApp)
                val musicRepo = application.dbRepo
                AlbumsViewModel(musicRepo)
            }
        }
    }
}