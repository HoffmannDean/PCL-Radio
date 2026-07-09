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

class HomeViewModel(
    private val musicRepo: DatabaseRepository
) : ViewModel() {
    val albums = musicRepo.albums.stateIn(
        viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RadioApp)
                val musicRepo = application.dbRepo
                HomeViewModel(musicRepo)
            }
        }
    }
}