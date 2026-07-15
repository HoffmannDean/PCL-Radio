package de.luh.hci.pclab.apps.selection.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector
import de.luh.hci.pclab.R

data class AppInfo(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

object Apps {
    val all = listOf(
        AppInfo(
            "music",
            R.string.music_app_name,
            R.string.music_app_description,
            Icons.Default.MusicNote
        ),
        AppInfo(
            "casino",
            R.string.casino_app_name,
            R.string.casino_app_description,
            Icons.Default.Paid
        ),
        AppInfo(
            "radio",
            R.string.radio_app_name,
            R.string.radio_app_description,
            Icons.Default.Radio
        )
    )
}
