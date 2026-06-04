package de.luh.hci.pclab.apps.casino.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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

@Composable
fun CasinoView(
    modifier: Modifier = Modifier
) {
    var sliderValue by remember { mutableIntStateOf(3) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    )  {
        Text(
            text = "Casino App",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {

            Text(
                text = "Select target range:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            RadioSlider(
                options = arrayOf("1", "2", "3", "4", "5"),
                selected = sliderValue,
                onSelect = { sliderValue = it },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { /* Handle start */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                Text("Start")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CasinoViewPreview() {
    CasinoView()
}
