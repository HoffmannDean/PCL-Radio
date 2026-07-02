package de.luh.hci.pclab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import de.luh.hci.pclab.navigation.ui.Navigation
import de.luh.hci.pclab.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme(
                dynamicColor = false,
            ) {
                Navigation()
            }
        }
    }
}
