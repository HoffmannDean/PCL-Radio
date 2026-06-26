package de.luh.hci.pclab.apps.music.ui

import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luh.hci.pclab.apps.music.model.Album

// Shows multiple albums and allows updates, add, delete...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsView(
    onAlbumClick: (Album) -> Unit,
    viewModel: AlbumsViewModel = viewModel(factory = AlbumsViewModel.Factory)
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    // TODO: improve and add "add album" button

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums) { album ->
            ListItem(
                headlineContent = { Text(album.name) },
                supportingContent = { Text("${album.songCount} songs") },
                modifier = Modifier.clickable { onAlbumClick(album) })
            HorizontalDivider()
        }
    }
}
