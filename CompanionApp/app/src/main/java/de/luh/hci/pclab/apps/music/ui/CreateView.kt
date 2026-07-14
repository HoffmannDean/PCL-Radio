package de.luh.hci.pclab.apps.music.ui

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.luh.hci.pclab.apps.music.model.Album
import de.luh.hci.pclab.apps.music.model.Song
import kotlinx.coroutines.launch
import java.util.jar.Manifest
import kotlin.Long


@Composable
fun CreateContent(
    songList: List<Song>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onFabClick: () -> Unit,
    onHomeClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    val filteredSong = songList.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    val sortedSongs = filteredSong.sortedByDescending { it.mediaStoreId in selectedIds }
    val coroutineScope = rememberCoroutineScope()
    val darkBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val listState = rememberLazyListState()

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(darkBg)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onHomeClick) {
                        Icon(Icons.Filled.Home, contentDescription = "Home")
                    }
                    IconButton(onClick = onAlbumsClick) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = "Albums")
                    }
                    IconButton(onClick = onSongsClick) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Songs")
                    }
                    IconButton(onClick = onPlayClick) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
                }

                FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Add, contentDescription = "Album erstellen", modifier = Modifier.size(28.dp))
                        Text("${selectedIds.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
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
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create a Playlist!\nAll Stored Songs:",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
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
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.simpleVerticalScrollbar(listState)
                    .weight(1f)
                    .simpleVerticalScrollbar(listState)
            ) {
                items(sortedSongs, key = { "${it.id}-${it.mediaStoreId}" }) { song ->
                    SongRowSelectable(
                        song = song,
                        selected = song.mediaStoreId in selectedIds,
                        onToggle = { onToggle(song.mediaStoreId) }
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateView(
    album: Album?,
    onHomeClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
    onPlayClick: () -> Unit,
    viewModel: CreateViewModel = viewModel(factory = CreateViewModel.provideFactory(album))
) {
    val permissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    val preselected by viewModel.preselectedIds.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(preselected) {
        if (preselected.isNotEmpty()) selectedIds = preselected
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.loaddeviceSongs()
        }
    }

    val songs by viewModel.deviceSongs.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    if (permissionState.status.isGranted) {
        CreateContent(
            songList = songs,
            selectedIds = selectedIds,
            onToggle = { id ->
                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
            },
            onFabClick = { showDialog = true },
            onHomeClick = onHomeClick,
            onAlbumsClick = onAlbumsClick,
            onSongsClick = onSongsClick,
            onPlayClick = onPlayClick
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Permission required to access songs")
        }
    }

    if (showDialog) {
        CreateDialog(
            editingAlbum = album,
            selectedCount = selectedIds.size,
            onDismiss = { showDialog = false },
            onSave = { name, artist ->
                val selected = songs.filter { it.mediaStoreId in selectedIds }
                viewModel.createAlbum(name, artist, selected, album)
                selectedIds = emptySet()
                showDialog = false
            }
        )
    }
}


@Composable
fun SongRowSelectable(
    song: Song,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.inversePrimary
            )
            .clickable(
                onClick = {
                    onToggle()
                }
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                val minutes = song.durationMs / 60000
                val seconds = (song.durationMs % 60000) / 1000
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }

        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() })

    }
}



@Composable
fun CreateDialog(
    editingAlbum: Album? = null,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, artist: String) -> Unit
) {
    var name by remember { mutableStateOf(editingAlbum?.name ?: "") }
    var artist by remember { mutableStateOf(editingAlbum?.artistAl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingAlbum != null) "Edit Album!" else "Create a New Album!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$selectedCount Selected Songs", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist(s)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Return") }
        },
        confirmButton = {
            Button(onClick = { onSave(name, artist) }, enabled = name.isNotBlank()) { Text("Save") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CreateContentPreview() {
    CreateContent(
        songList = listOf(
            Song(id = 1, title = "Sample X", artist = "Ex1", album = "Sample 1", albumId = 1, mediaStoreId = 101, uri = Uri.EMPTY, durationMs = 210000, sizeBytes = 3500000, mimeType = "audio/mpeg", dateAdded = 1700000000),
            Song(id = 2, title = "Sample Y", artist = "Ex2", album = "Sample 1", albumId = 1, mediaStoreId = 102, uri = Uri.EMPTY, durationMs = 185000, sizeBytes = 3100000, mimeType = "audio/mpeg", dateAdded = 1700000100),
            Song(id = 3, title = "Sample Z", artist = "Ex2", album = "Sample 3", albumId = 3, mediaStoreId = 103, uri = Uri.EMPTY, durationMs = 240000, sizeBytes = 4000000, mimeType = "audio/mpeg", dateAdded = 1700000200),
            Song(id = 4, title = "Sample W", artist = "Ex3", album = "Sample 2", albumId = 2, mediaStoreId = 104, uri = Uri.EMPTY, durationMs = 195000, sizeBytes = 3300000, mimeType = "audio/mpeg", dateAdded = 1700000300)),
        selectedIds = setOf(),
        onToggle = {},
        onFabClick = {},
        onHomeClick = {},
        onAlbumsClick = {},
        onSongsClick = {},
        onPlayClick = {}
    )
}
