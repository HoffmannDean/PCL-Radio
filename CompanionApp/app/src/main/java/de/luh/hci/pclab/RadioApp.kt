package de.luh.hci.pclab

import android.app.Application
import de.luh.hci.pclab.radio.data.Esp32Repository

class RadioApp : Application() {

    val esp32Repo: Esp32Repository by lazy {
        Esp32Repository(this)
    }
}