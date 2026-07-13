package de.luh.hci.pclab.navigation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.luh.hci.pclab.apps.casino.ui.CasinoView
import de.luh.hci.pclab.apps.selection.ui.SelectionView
import de.luh.hci.pclab.connectivity.ui.ConnectivityStatusBar
import de.luh.hci.pclab.connectivity.ui.ConnectivityView
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.radio.data.ConnectionState
import de.luh.hci.pclab.radio.ui.DeviceViewModel
import kotlinx.serialization.Serializable
import androidx.navigation.toRoute
import de.luh.hci.pclab.apps.music.ui.SongsView
import de.luh.hci.pclab.apps.music.ui.AlbumsView
import de.luh.hci.pclab.apps.music.ui.CreateView
import de.luh.hci.pclab.apps.music.ui.PlayView
import de.luh.hci.pclab.apps.music.ui.PlayViewModel
import kotlinx.serialization.json.Json
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.core.content.ContextCompat

@Serializable
object Connectivity

@Serializable
object AppSelection

@Serializable
object MusicApp


@Serializable
data class AlbumDetail(
    val albumJson: String,
)

@Serializable
object CreateAlbum
@Serializable
data class EditAlbum(val albumJson: String)
@Serializable
data class Songs(val albumJson: String? = null)
@Serializable
data class Play(
    val songId: Long? = null
    )
@Serializable
object CasinoApp

@RequiresApi(Build.VERSION_CODES.S)
private val btPermissions = arrayOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun rememberBluetoothPermission(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) onGranted()
    }
    return {
        val allGranted = btPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) onGranted() else launcher.launch(btPermissions)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun Navigation(
    deviceViewModel: DeviceViewModel = viewModel(factory = DeviceViewModel.Factory),
    playerViewModel: PlayViewModel = viewModel(factory = PlayViewModel.Factory)
) {
    val navController = rememberNavController()
    val connectedDevice by deviceViewModel.device.collectAsStateWithLifecycle()
    val connectionState by deviceViewModel.connectionState.collectAsStateWithLifecycle()
    val availableDevices by deviceViewModel.availableDevices.collectAsStateWithLifecycle()
    val counter by deviceViewModel.counter.collectAsStateWithLifecycle()

    val requestScan = rememberBluetoothPermission {
        deviceViewModel.searchAvailableDevices()
    }

    LaunchedEffect(Unit) {
        requestScan()
    }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                // Only move to Selection if we are currently on the Connectivity screen
                if (navController.currentDestination?.hasRoute<Connectivity>() == true) {
                    navController.navigate(AppSelection) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            ConnectionState.DISCONNECTED -> {
                if (navController.currentDestination?.hasRoute<Connectivity>() == false) {
                    navController.navigate(Connectivity) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            ConnectionState.CONNECTING -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ConnectivityStatusBar(
                connectedDevice = connectedDevice,
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Connectivity,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable<Connectivity> {
                ConnectivityView(
                    devices = availableDevices,
                    connectionState = connectionState,
                    onDeviceSelected = { deviceInfo ->
                        deviceViewModel.connect(deviceInfo)
                    }
                )
            }

            composable<AppSelection> {
                SelectionView(
                    coinCount = counter,
                    onAppSelected = { app ->
                        when (app.id) {
                            "casino" -> navController.navigate(CasinoApp)
                            "music" -> navController.navigate(MusicApp)
                        }
                    }
                )
            }

            composable<CasinoApp> {
                CasinoView(
                    coinCount = counter,
                    onSubmit = {
                        println("Submitted: $it")
                    },
                    onWin = { deviceViewModel.dispense(counter) }
                )
            }

            composable<CreateAlbum> {
                CreateView(
                    album = null,
                    onHomeClick = { navController.navigate(AppSelection) },
                    onAlbumsClick = { navController.navigate(MusicApp) },
                    onSongsClick = { navController.navigate(Songs(null)) },
                    onPlayClick = {
                        val id = playerViewModel.currentSong.value?.id
                        navController.navigate(Play(id))
                    }
                )
            }

            composable<MusicApp> {
                AlbumsView(
                    onAlbumClick = { album: Album ->
                        navController.navigate(Songs(Json.encodeToString(album)))
                    },
                    onHomeClick = { navController.navigate(AppSelection) },
                    onCreateClick = { navController.navigate(CreateAlbum) },
                    onSongsClick = { navController.navigate(Songs(null)) },
                    onEditClick = { album ->
                        navController.navigate(EditAlbum(Json.encodeToString(album)))
                    },
                    onPlayClick = {
                        val id = playerViewModel.currentSong.value?.id
                        navController.navigate(Play(id))
                    })
                }
            composable<EditAlbum> { backStackEntry ->
                val route = backStackEntry.toRoute<EditAlbum>()
                val album = Json.decodeFromString<Album>(route.albumJson)
                CreateView(
                    album = album,
                    onHomeClick = { navController.navigate(AppSelection) },
                    onAlbumsClick = { navController.navigate(MusicApp) },
                    onSongsClick = { navController.navigate(Songs(null)) },
                    onPlayClick = {
                        val id = playerViewModel.currentSong.value?.id
                        navController.navigate(Play(id))
                    }
                )
            }

            composable<Songs> { backStackEntry ->
                val route = backStackEntry.toRoute<Songs>()
                val album = route.albumJson?.let { Json.decodeFromString<Album>(it) }
                SongsView(
                    album = album,
                    onSongClick = { song -> navController.navigate(Play(song.id)) },
                    onHomeClick = { navController.navigate(AppSelection) },
                    onCreateClick = { navController.navigate(CreateAlbum) },
                    onAlbumsClick = { navController.navigate(MusicApp) },
                    onPlayClick = {
                        val id = playerViewModel.currentSong.value?.id
                        navController.navigate(Play(id))
                    }
                )
            }
            composable<Play> { backStackEntry ->
                val route = backStackEntry.toRoute<Play>()
                PlayView(
                    songId = route.songId,
                    onHomeClick = { navController.navigate(AppSelection) },
                    onCreateClick = { navController.navigate(CreateAlbum) },
                    onAlbumsClick = { navController.navigate(MusicApp) },
                    onSongsClick = { navController.navigate(Songs(null)) },
                )
            }

        }
    }
}
