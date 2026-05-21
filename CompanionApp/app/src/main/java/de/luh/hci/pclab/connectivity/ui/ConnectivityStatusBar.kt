package de.luh.hci.pclab.connectivity.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConnectivityStatusBar(
    connectedDevice: Device?,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (connectedDevice != null) {
        Color(0xFF4CAF50) // Green
    } else {
        MaterialTheme.colorScheme.error
    }
    
    val statusText = if (connectedDevice != null) {
        "Connected to: ${connectedDevice.name}"
    } else {
        "Not Connected"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            color = if (connectedDevice != null) Color.White else MaterialTheme.colorScheme.onError,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
