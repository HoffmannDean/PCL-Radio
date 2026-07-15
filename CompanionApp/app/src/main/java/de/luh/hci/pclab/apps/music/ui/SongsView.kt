package de.luh.hci.pclab.apps.music.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.AddBox
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


//TODO: button um bei Album View den Filter auszustellen und alle songs anzeigen zu lassen
//TODO: song aus album entfernen button
//Todo: das angezeigt Albumtitel ist nicht identisch mit dem Album aus der App=> überschreiben?
@Composable

fun SongsContent(
    album: Album?,
    songList: List<Song>,
    onSongClick: (Song) -> Unit,
    onHomeClick: () -> Unit,
    onCreateClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSong = songList.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    val darkBg = MaterialTheme.colorScheme.surfaceContainerHighest
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
                IconButton(onClick = onAlbumsClick) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = "Albums")
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
                Column {
                    Text(
                        text = if (album?.id != null) "All Songs of ${album?.name}" else "All Songs",
                        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                    if (album?.id != null) {
                        Text(
                            text = album?.artistAl ?: "",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${album?.songCount} Songs",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.simpleVerticalScrollbar(listState)
                    .weight(1f)
                    .simpleVerticalScrollbar(listState)
            ) {
                items(filteredSong, key = { "${it.id}-${it.mediaStoreId}" }) { song ->
                    SongRow(song = song, onClick = { onSongClick(song) })
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SongsView(
    album: Album?,
    onSongClick: (Song) -> Unit,
    onHomeClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onCreateClick: () -> Unit,
    onPlayClick: () -> Unit,
    viewModel: SongsViewModel = viewModel(factory = SongsViewModel.provideFactory(album))
) {
    /*val permissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.loaddeviceSongs()
        }
    }*/

    val songs by viewModel.songs.collectAsStateWithLifecycle()

    SongsContent(
            album = album,
            songList = songs,
            onSongClick = onSongClick,
            onHomeClick = onHomeClick,
            onAlbumsClick = onAlbumsClick,
            onCreateClick = onCreateClick,
            onPlayClick = onPlayClick
        )

}

@Composable
private fun SongRow(
    song: Song,
    onClick: () -> Unit
) {
    val darkBg = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.inversePrimary)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1/4 der Breite: Cover (Platzhalter)
        /*AlbumCover(
            modifier = Modifier
                .weight(0.25f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )*/

        // 3/4 der Breite: Titel + Interpret
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))
                val minutes = song.durationMs / 60000
                val seconds = (song.durationMs % 60000) / 1000

                //TODO: hier nur album anzeigen, wenn alle songs angezeigt werden
                Text(
                    text = "${song.album} - %02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }

        }
    }
}

//TODO: ggf zentralisieren, da auch in AlbumsView relevant

@Composable
fun Modifier.simpleVerticalScrollbar(state: LazyListState): Modifier {
    val col = MaterialTheme.colorScheme.inversePrimary
    val targetAlpha = if (state.isScrollInProgress) 0.5f else 0.3f
    val alpha by animateFloatAsState(targetAlpha, animationSpec = tween(300))

    return this.drawWithContent {
        drawContent()
        val first = state.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@drawWithContent
        val total = state.layoutInfo.totalItemsCount
        if (total == 0) return@drawWithContent

        val barHeight = (size.height * state.layoutInfo.visibleItemsInfo.size) / total
        val offset = (size.height * first.index) / total

        drawRoundRect(
            color = col,
            topLeft = Offset(size.width - 6.dp.toPx(), offset),
            size = Size(4.dp.toPx(), barHeight),
            cornerRadius = CornerRadius(2.dp.toPx()),
            alpha = alpha
        )
    }
}


/*@Composable
private fun SongCover(modifier: Modifier = Modifier) {
    // Platzhalter. Für echtes Cover: Coil AsyncImage(model = song.uri, ...) verwenden.
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}*/


@Preview(showBackground = true)
@Composable
fun SongsContentPreview() {
    SongsContent(
        album = null,
        songList = listOf(
            Song(id = 1, title = "Sample X", artist = "Ex1", album = "Sample 1", albumId = 1, mediaStoreId = 101, uri = Uri.EMPTY, durationMs = 210000, sizeBytes = 3500000, mimeType = "audio/mpeg", dateAdded = 1700000000),
            Song(id = 2, title = "Sample Y", artist = "Ex2", album = "Sample 1", albumId = 1, mediaStoreId = 102, uri = Uri.EMPTY, durationMs = 185000, sizeBytes = 3100000, mimeType = "audio/mpeg", dateAdded = 1700000100),
            Song(id = 3, title = "Sample Z", artist = "Ex2", album = "Sample 3", albumId = 3, mediaStoreId = 103, uri = Uri.EMPTY, durationMs = 240000, sizeBytes = 4000000, mimeType = "audio/mpeg", dateAdded = 1700000200),
            Song(id = 4, title = "Sample W", artist = "Ex3", album = "Sample 2", albumId = 2, mediaStoreId = 104, uri = Uri.EMPTY, durationMs = 195000, sizeBytes = 3300000, mimeType = "audio/mpeg", dateAdded = 1700000300)),
        onSongClick = {},
        onHomeClick = {},
        onAlbumsClick = {},
        onCreateClick = {},
        onPlayClick = {}
    )
}