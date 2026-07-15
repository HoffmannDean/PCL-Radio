package de.luh.hci.pclab.apps.music.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.selection.ui.SelectionView
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import kotlin.Unit

//Shows all albums and allows search of title. Click on album opens list of songs of this album.

@Composable
fun AlbumsContent(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onEditClick: (Album) -> Unit,
    onDeleteClick: (Album) -> Unit,
    onHomeClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSongsClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredAlbums = albums.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    val darkBg = MaterialTheme.colorScheme.surfaceContainer
    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(darkBg)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onHomeClick) {
                        Icon(Icons.Filled.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Filled.AddBox, contentDescription = "Create")
                    }
                    IconButton(onClick = onSongsClick) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Songs")
                    }
                    IconButton(onClick = onPlayClick) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
               Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(darkBg)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "All Playlists",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top=8.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth().padding(
                        top = 12.dp, bottom = 15.dp, start = 20.dp, end = 20.dp,
                    ),
                    singleLine = true
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.simpleVerticalScrollbar(listState)
                    .weight(1f)
                    .simpleVerticalScrollbar(listState)
            ) {
                items(filteredAlbums, key = { it.id }) { album ->
                    AlbumRow(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        onLongClick = { selectedAlbum = album }
                    )
                }
            }}
    }
    //Editing Pop-Up
    selectedAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = { selectedAlbum = null },
            title = { Text(album.name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedAlbum = null
                            onEditClick(album)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit") }

                    Button(
                        onClick = {
                            selectedAlbum = null
                            onDeleteClick(album)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedAlbum = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AlbumsView(
    onAlbumClick: (Album) -> Unit,
    onEditClick: (Album) -> Unit,
    onHomeClick: () -> Unit,
    onCreateClick: () -> Unit,
    onSongsClick: () -> Unit,
    onPlayClick: () -> Unit,
    viewModel: AlbumsViewModel = viewModel(factory = AlbumsViewModel.Factory)
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    AlbumsContent(
        albums = albums,
        onAlbumClick = onAlbumClick,
        onEditClick = onEditClick,
        onDeleteClick = { viewModel.deleteAlbum(it) },
        onHomeClick = onHomeClick,
        onCreateClick = onCreateClick,
        onSongsClick = onSongsClick,
        onPlayClick = onPlayClick
    )
}

@Composable
private fun AlbumRow(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val darkBg = MaterialTheme.colorScheme.surfaceContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(darkBg)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = album.artistAl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                val minutes = album.durationMs / 60000
                val seconds = (album.durationMs % 60000) / 1000

                Text(
                    text = "${album.songCount} Songs - %02d:%02d Min".format(minutes, seconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }

        }
    }
}



@Preview(showBackground = true)
@Composable
fun AlbumsContentPreview() {
    AlbumsContent(
        albums = listOf(Album(id = 1, name = "Sample 1", durationMs = 3400), Album(id = 2, name = "Sample 2", durationMs = 3400), Album(id = 3, name = "Sample 3", durationMs = 3400), Album(id = 4, name = "Sample 4", durationMs = 3400)),
        onAlbumClick = {},
        onEditClick = {},
        onDeleteClick = {},
        onHomeClick = {},
        onCreateClick = {},
        onSongsClick = {},
        onPlayClick = {}
    )
}