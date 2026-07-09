package de.luh.hci.pclab.apps.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Scaffold
import kotlin.Unit


@Composable
fun MusicHomeContent(
    onAlbumsClick: () -> Unit,
    onSongsClick: () -> Unit,
    onPlayClick: () -> Unit,
    onExitClick: () -> Unit
) {
    val tileColors = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFAB47BC),
        Color(0xFF66BB6A), Color(0xFFFFCA28), Color(0xFFFF7043),
        Color(0xFF26C6DA), Color(0xFFEC407A), Color(0xFF8D6E63),
        Color(0xFF5C6BC0), Color(0xFFD4E157), Color(0xFF29B6F6),
        Color(0xFFFF8A65), Color(0xFF9CCC65), Color(0xFFBA68C8),
        Color(0xFF78909C), Color(0xFFFFA726), Color(0xFF4DB6AC),
        Color(0xFFE57373), Color(0xFF7986CB), Color(0xFFAED581),
        Color(0xFF4DD0E1), Color(0xFFFFB74D), Color(0xFFF06292),
    )

    val darkBg = MaterialTheme.colorScheme.surfaceContainerHighest

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(darkBg)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onExitClick) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Exit")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hintergrund: kleine bunte Kacheln
            val columns = 6
            val rows = 12
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until rows) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0 until columns) {
                            val color = tileColors[(row * columns + col) % tileColors.size]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Album,
                                    contentDescription = null,
                                    tint = color.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Vordergrund
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Was willst du\nheute hören?",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Kachel: Alben
                HomeTile(
                    title = "Alben",
                    icon = Icons.Filled.LibraryMusic,
                    color = Color(0xFF42A5F5),
                    onClick = onAlbumsClick
                )
                Spacer(Modifier.height(16.dp))

                // Kachel: Songs
                HomeTile(
                    title = "Songs",
                    icon = Icons.Filled.MusicNote,
                    color = Color(0xFF66BB6A),
                    onClick = onSongsClick
                )
                Spacer(Modifier.height(16.dp))

                // Kachel: Play
                HomeTile(
                    title = "Zuletzt gespielt",
                    icon = Icons.Filled.PlayArrow,
                    color = Color(0xFFAB47BC),
                    onClick = onPlayClick
                )
            }
        }
    }
}

@Composable
private fun HomeTile(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
    }
}