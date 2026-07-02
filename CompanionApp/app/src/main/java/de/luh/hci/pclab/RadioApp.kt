package de.luh.hci.pclab

import android.app.Application
import de.luh.hci.pclab.apps.music.data.AppDatabase
import de.luh.hci.pclab.apps.music.data.DatabaseRepository
import de.luh.hci.pclab.apps.music.data.MusicPlayerRepository
import de.luh.hci.pclab.radio.data.Esp32Repository

class RadioApp : Application() {

    val esp32Repo: Esp32Repository by lazy {
        Esp32Repository(this)
    }

    val dbRepo: DatabaseRepository by lazy {
        val db = AppDatabase.getInstance(this)
        DatabaseRepository(db.albumDao(), db.songDao(), contentResolver)
    }

    val playerRepo: MusicPlayerRepository by lazy {
        MusicPlayerRepository(this)
    }
}