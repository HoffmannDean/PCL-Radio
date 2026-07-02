package de.luh.hci.pclab.apps.casino.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CasinoView(
    coinCount: Int,
    modifier: Modifier = Modifier,
    onSubmit: (Int) -> Unit,
    onWin: () -> Unit = {},          // does nothing for now
) {
    val options = arrayOf("1", "2", "3", "4", "5")
    var sliderValue by remember { mutableIntStateOf(3) }
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var lastPlayedCounter by remember { mutableIntStateOf(-1) }

    val knobPos = remember { Animatable(sliderValue.toFloat()) }
    val scope = rememberCoroutineScope()

    val canPlay = !spinning && counter > 0 && counter > lastPlayedCounter

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Text(
            text = "Casino App",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Current Jackpot: $coinCount")
            Text(
                text = "Choose a target range:",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedCard(
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                RadioSlider(
                    options = options,
                    selected = sliderValue,
                    onSelect = { sliderValue = it },
                    animatedIndex = if (spinning) knobPos.value else null,
                    modifier = Modifier.fillMaxWidth().padding(all = 16.dp)
                )
            }

            result?.let {
                Text(
                    text = if (it) "You won!" else "You lost!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (it) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }

        Button(
            enabled = canPlay,
            onClick = {
                val pick = sliderValue
                val target = options.indices.random()
                lastPlayedCounter = counter
                result = null
                spinning = true
                scope.launch {
                    knobPos.snapTo(pick.toFloat())
                    spinToTarget(knobPos, target, options.lastIndex)
                    sliderValue = target
                    val won = target == pick
                    result = won
                    spinning = false
                    onSubmit(pick)
                    if (won) onWin()   // caller resets the count
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text(if (result == null) "Start" else "Play Again")
        }
    }
}

/** Sweeps back and forth across the range with decaying amplitude, then settles on [target]. */
private suspend fun spinToTarget(
    knob: Animatable<Float, *>,
    target: Int,
    lastIndex: Int,
) {
    val sweeps = listOf(0f to lastIndex.toFloat()) // full range first
    // a few decaying oscillations around the target
    val swings = listOf(lastIndex * 0.9f, lastIndex * 0.35f, lastIndex * 0.12f)

    // one full sweep to build momentum
    knob.animateTo(lastIndex.toFloat(), tween(500, easing = LinearEasing))
    knob.animateTo(0f, tween(600, easing = LinearEasing))

    // decaying swings centered on target
    swings.forEach { amp ->
        knob.animateTo((target - amp).coerceIn(0f, lastIndex.toFloat()), tween(400))
        knob.animateTo((target + amp).coerceIn(0f, lastIndex.toFloat()), tween(400))
    }
    // settle
    knob.animateTo(target.toFloat(), tween(500, easing = FastOutSlowInEasing))
}

@Preview(showBackground = true)
@Composable
fun CasinoViewPreview() {
    AppTheme(
        dynamicColor = false,
    ) {
        CasinoView(
            coinCount = 123,
            onSubmit = {
                println("Submitted: $it")
            })
    }
}
