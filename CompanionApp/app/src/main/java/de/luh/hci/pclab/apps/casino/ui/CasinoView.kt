package de.luh.hci.pclab.apps.casino.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.luh.hci.pclab.ui.theme.AppTheme
import de.luh.hci.pclab.radio.data.DeviceViewModel

@Composable
fun CasinoView(
    counter: Int,
    modifier: Modifier = Modifier,
    onSubmit: (Int) -> Unit
) {
    var sliderValue by remember { mutableIntStateOf(3) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    )  {
        Text(
            text = "Casino App",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Current Jackpot: $counter")
            Text(
                text = "Choose a target range:",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedCard(
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                RadioSlider(
                    options = arrayOf("1", "2", "3", "4", "5"),
                    selected = sliderValue,
                    onSelect = { sliderValue = it },
                    modifier = Modifier.fillMaxWidth().padding(all = 16.dp)
                )
            }
        }
        Button(
            onClick = { onSubmit(sliderValue) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text("Start")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CasinoViewPreview() {
    AppTheme(
        dynamicColor = false,
    ) {
        CasinoView(
            counter = 123,
            onSubmit = {
                println("Submitted: $it")
            })
    }
}
