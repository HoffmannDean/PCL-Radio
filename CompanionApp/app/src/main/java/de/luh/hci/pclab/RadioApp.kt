package de.luh.hci.pclab

import android.app.Application
import de.luh.hci.pclab.apps.music.data.AppDatabase
import de.luh.hci.pclab.apps.music.data.DatabaseRepository
import de.luh.hci.pclab.apps.music.data.MusicPlayerRepository
import de.luh.hci.pclab.radio.data.BleEsp32Repository
import de.luh.hci.pclab.radio.data.DummyEsp32Repository
import de.luh.hci.pclab.radio.data.Esp32Repository

class RadioApp : Application() {

    val esp32Repo: Esp32Repository by lazy {
        val useDummy = false // Change to false for real hardware
        if (useDummy) {
            DummyEsp32Repository()
        } else {
            BleEsp32Repository(this)
        }
    }

    val dbRepo: DatabaseRepository by lazy {
        val db = AppDatabase.getInstance(this)
        DatabaseRepository(db.albumDao(), db.songDao(), contentResolver)
    }

    val playerRepo: MusicPlayerRepository by lazy {
        val db = AppDatabase.getInstance(this)
        MusicPlayerRepository(this, db.songDao())
    }
}