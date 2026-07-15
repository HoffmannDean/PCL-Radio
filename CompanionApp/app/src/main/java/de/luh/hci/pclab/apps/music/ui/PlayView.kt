package de.luh.hci.pclab.apps.music.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel

@Composable
fun PlayView(
    songId: Long?,
    albumId: Long?,
    onHomeClick: () -> Unit,
    onCreateClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
    viewModel: PlayViewModel = viewModel(factory = PlayViewModel.provideFactory(songId, albumId))
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val albums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val albumName = albums.firstOrNull { it.id == song?.albumId }?.name ?: ""

    PlayContent(
        song = song,
        albumName = albumName,
        albums = albums,
        isPlaying = isPlaying,
        currentPosition = position,
        onSeekTo = { fraction -> viewModel.seekTo((fraction * (song?.durationMs ?: 0L)).toLong()) },
        onPlayPauseClick = { viewModel.playPause() },
        onNextClick = { viewModel.next() },
        onPreviousClick = { viewModel.previous() },
        onRemoveClick = { viewModel.remove() },
        onAddToAlbum = { album -> song?.let {viewModel.addSongToAlbum(it, album) }},
        onHomeClick = onHomeClick,
        onCreateClick = onCreateClick,
        onAlbumsClick = onAlbumsClick,
        onSongsClick = onSongsClick,
    )
}
@Composable
fun PlayContent(
    song: Song?,
    albums: List<Album>,
    albumName: String,
    isPlaying: Boolean,
    currentPosition: Long,
    onSeekTo: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onAddToAlbum: (Album) -> Unit,
    onHomeClick: () -> Unit,
    onCreateClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
) {
    val darkBg = MaterialTheme.colorScheme.surfaceContainerHighest
    var showPlaylistPopup by remember { mutableStateOf(false) }

    if (showPlaylistPopup) {
        AddToPlaylistDialog(
            albums = albums,
            onAlbumClick = { album ->
                onAddToAlbum(album)
                showPlaylistPopup = false
            },
            onDismiss = { showPlaylistPopup = false }
        )
    }

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
                            //.navigationBarsPadding()
                            .padding(top = 8.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly

                ) {
                    IconButton(onClick = onHomeClick) {
                        Icon(Icons.Filled.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Filled.AddBox, contentDescription = "Create")
                    }
                    IconButton(onClick = onAlbumsClick) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = "Albums")
                    }
                    IconButton(onClick = onSongsClick) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Songs")
                    }
                }
            }
        }
    ) { innerPadding ->
        val title = song?.title ?: "No song selected"
        val artist = song?.artist ?: ""
        val durationMs = song?.durationMs ?: 0L
        val minutes = currentPosition / 60000
        val seconds = (currentPosition % 60000) / 1000
        val totalMin = durationMs / 60000
        val totalSec = (durationMs % 60000) / 1000
        val progress = if (durationMs > 0) currentPosition.toFloat() / durationMs else 0f
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .background(darkBg)
            )

            //HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(0.3f))
                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth(0.9f),
                    contentAlignment = Alignment.Center
                ) {
                    var titleSize by remember(title) { mutableStateOf(32.sp) }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = titleSize,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        onTextLayout = { result ->
                            if (result.hasVisualOverflow && titleSize > 16.sp) {
                                titleSize *= 0.95f
                            }
                        }
                    )
                }
                Text("Artist: ${artist}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Album: ${albumName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.weight(0.3f))

                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .clip(RoundedCornerShape(16.dp))
                            .background(darkBg)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.9f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.weight(0.5f))

                if (song != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (song.albumId != 0L) {
                            IconButton(
                                onClick = onRemoveClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Cancel, contentDescription = "Remove", modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Spacer(Modifier.size(32.dp))
                        }
                        IconButton(
                            onClick = { showPlaylistPopup = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.AddCircleOutline, contentDescription = "Add", modifier = Modifier.size(20.dp))
                        }
                    }

                    Slider(
                        value = progress,
                        onValueChange = onSeekTo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("%02d:%02d".format(minutes, seconds), style = MaterialTheme.typography.bodySmall)
                        Text("%02d:%02d".format(totalMin, totalSec), style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(15.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(23.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPreviousClick) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(40.dp))
                        }
                        IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(64.dp)) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = onNextClick) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(40.dp))
                        }
                    }
                }

                Spacer(Modifier.weight(0.2f))
        }}

    }
}

@Composable
fun AddToPlaylistDialog(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = albums.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Switch to Playlist:", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Cancel, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(filtered) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text(album.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayContentPreview() {
    PlayContent(
        song = Song(id = 1, title = "Sample X", artist = "Ex1", album = "Sample 1", albumId = 1, mediaStoreId = 101, uri = Uri.EMPTY, durationMs = 210000, sizeBytes = 3500000, mimeType = "audio/mpeg", dateAdded = 1700000000),
        albums = emptyList(),
        albumName = "Test",
        isPlaying = true,
        currentPosition = 85000,
        onSeekTo = {},
        onPlayPauseClick = {},
        onNextClick = {},
        onPreviousClick = {},
        onRemoveClick = {},
        onAddToAlbum = {},
        onHomeClick = {},
        onCreateClick = {},
        onAlbumsClick = {},
    ) {}
}