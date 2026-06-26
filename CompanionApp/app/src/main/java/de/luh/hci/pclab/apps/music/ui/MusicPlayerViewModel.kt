package de.luh.hci.pclab.apps.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.luh.hci.pclab.RadioApp
import de.luh.hci.pclab.apps.music.data.MusicPlayerRepository
import de.luh.hci.pclab.apps.music.model.Song


class PlayerViewModel(private val repo: MusicPlayerRepository) : ViewModel() {

    val currentSong = repo.currentSong
    val isPlaying = repo.isPlaying

    fun play(song: Song) = repo.play(song)
    fun togglePause() = repo.togglePause()
    fun stop() = repo.stop()

    override fun onCleared() {
        repo.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY] as RadioApp)
                PlayerViewModel(application.playerRepo)
            }
        }
    }
}
