package de.luh.hci.pclab.apps.radio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.luh.hci.pclab.radio.data.Esp32Repository
import de.luh.hci.pclab.ui.theme.AppTheme
import kotlin.math.roundToInt

/**
 * Amplifier controls: shows the current battery charge, lets the user switch
 * between Bluetooth (DAC) and analog radio, and adjust the volume.
 *
 * @param batteryLevel charge in percent (0..100)
 * @param source current source ([Esp32Repository.SOURCE_DAC] or [Esp32Repository.SOURCE_RADIO])
 * @param volume current attenuation (0 = loudest, 64 = quietest)
 */
@Composable
fun RadioControlView(
    batteryLevel: Int,
    source: Int,
    volume: Int,
    onSelectSource: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Radio",
            style = MaterialTheme.typography.headlineMedium,
        )

        BatteryCard(batteryLevel)
        SourceSelector(source, onSelectSource)
        VolumeControl(volume, onVolumeChange)
    }
}

@Composable
private fun BatteryCard(level: Int) {
    val tint = when {
        level <= 15 -> MaterialTheme.colorScheme.error
        level <= 40 -> Color(0xFFF9A825) // amber
        else -> Color(0xFF4CAF50)        // green
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Battery",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$level%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SourceSelector(source: Int, onSelectSource: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Source",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = source == Esp32Repository.SOURCE_DAC,
                onClick = { onSelectSource(Esp32Repository.SOURCE_DAC) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
            ) { Text("Bluetooth") }
            SegmentedButton(
                selected = source == Esp32Repository.SOURCE_RADIO,
                onClick = { onSelectSource(Esp32Repository.SOURCE_RADIO) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.Radio, contentDescription = null) },
            ) { Text("Radio") }
        }
    }
}

@Composable
private fun VolumeControl(volume: Int, onVolumeChange: (Int) -> Unit) {
    // The device works in attenuation (0 = loud, 64 = quiet); the slider shows
    // loudness (right = louder), so we invert when reading and writing.
    val maxAtt = Esp32Repository.VOLUME_MUTE.toFloat()

    // Local slider position for smooth dragging; resync when the device value changes.
    var loudness by remember(volume) {
        mutableFloatStateOf(maxAtt - volume.coerceIn(0, Esp32Repository.VOLUME_MUTE))
    }
    val percent = (loudness / maxAtt * 100f).roundToInt()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Volume", style = MaterialTheme.typography.titleMedium)
            Text(text = "$percent%", style = MaterialTheme.typography.titleMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null)
            Slider(
                value = loudness,
                onValueChange = { loudness = it },
                valueRange = 0f..maxAtt,
                onValueChangeFinished = {
                    onVolumeChange((maxAtt - loudness).roundToInt())
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RadioControlViewPreview() {
    AppTheme(dynamicColor = false) {
        RadioControlView(
            batteryLevel = 72,
            source = Esp32Repository.SOURCE_RADIO,
            volume = 20,
            onSelectSource = {},
            onVolumeChange = {},
        )
    }
}
