package de.luh.hci.pclab

import android.app.Application
import de.luh.hci.pclab.apps.music.data.AppDatabase
import de.luh.hci.pclab.apps.music.data.DatabaseRepository
import de.luh.hci.pclab.apps.music.data.MusicPlayerRepository
import de.luh.hci.pclab.radio.data.Esp32Repository

import de.luh.hci.pclab.radio.data.BluetoothEsp32Repository
import de.luh.hci.pclab.radio.data.Esp32RepositoryDummy

class RadioApp : Application() {

    private val useDummyRepo = true // Toggle this for debugging

    val esp32Repo: Esp32Repository by lazy {
        if (useDummyRepo) {
            Esp32RepositoryDummy(this)
        } else {
            BluetoothEsp32Repository(this)
        }
    }

    val dbRepo: DatabaseRepository by lazy {
        val db = AppDatabase.getInstance(this)
        DatabaseRepository(db.albumDao(), db.songDao(), contentResolver)
    }

    val playerRepo: MusicPlayerRepository by lazy {
        MusicPlayerRepository(this)
    }
}