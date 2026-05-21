package de.luh.hci.pclab.connectivity.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class Device(val name: String, val address: String)

@Composable
fun ConnectivityView(
    devices: List<Device>,
    onDeviceSelected: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDevice by remember { mutableStateOf<Device?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Connect to a Device",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Please select a device from the list below:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (devices.isEmpty()) {
            Text(
                text = "No devices found.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 32.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(devices) { device ->
                    DeviceListItem(
                        device = device,
                        selected = device == selectedDevice,
                        onClick = {
                            if (device == selectedDevice) {
                                selectedDevice = null;
                            } else {
                                selectedDevice = device
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        Button(
            onClick = { selectedDevice?.let { onDeviceSelected(it) } },
            enabled = selectedDevice != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Connect")
        }
    }
}

@Composable
fun DeviceListItem(
    device: Device,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(device.name) },
        supportingContent = { Text(device.address) },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
}

@Preview(showBackground = true)
@Composable
fun ConnectivityViewPreview() {
    val dummyDevices = listOf(
        Device("ESP32-Sensor-01", "00:1A:7D:DA:71:13"),
        Device("ESP32-Sensor-02", "00:1A:7D:DA:71:14"),
        Device("ESP32-Sensor-03", "00:1A:7D:DA:71:15"),
    )
    ConnectivityView(
        devices = dummyDevices,
        onDeviceSelected = {}
    )
}
