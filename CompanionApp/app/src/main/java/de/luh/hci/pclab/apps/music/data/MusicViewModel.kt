package de.luh.hci.pclab.apps.music.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.luh.hci.pclab.RadioApp
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.radio.data.DeviceViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicViewModel : ViewModel() {
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    fun loadMusic() {

    }

    fun createAlbum(name: String) {

    }

    fun addSong() {

    }

    companion object {
        val Factory : ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MusicViewModel()
            }
        }
    }
}