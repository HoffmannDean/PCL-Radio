package de.luh.hci.pclab.apps.music.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import de.luh.hci.pclab.apps.music.model.Album

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AlbumDetailView(
    album: Album, viewModel: AlbumDetailViewModel = viewModel(
        factory = AlbumDetailViewModel.provideFactory(album)
    ), playerViewModel: PlayerViewModel
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    // we need to request permission to read external storage
    val permission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    rememberPermissionState(permission)

    // TODO: add possibility to add songs to album (maybe a FloatingActionButton + BottomSheet?)

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(songs, key = { it.id }) { song ->
            // TODO: extract to composable and improve
            ListItem(
                headlineContent = {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
            HorizontalDivider()
        }
    }
}
