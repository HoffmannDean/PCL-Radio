package de.luh.hci.pclab.apps.casino.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.luh.hci.pclab.ui.theme.AppTheme
import de.luh.hci.pclab.ui.theme.casinoFontFamily
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

    val canPlay = !spinning && coinCount > 0 && coinCount > lastPlayedCounter

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        JackpotCard(coinCount)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "CASINO", style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = casinoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 5.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedCard(
                border = BorderStroke(4.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                RadioSlider(
                    options = options,
                    selected = sliderValue,
                    onSelect = { sliderValue = it },
                    animatedIndex = if (spinning) knobPos.value else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                )
            }

            if (result == null) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Text(
                    text = "Choose a target range.", modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Button(
                enabled = canPlay, onClick = {
                    val pick = sliderValue
                    val target = options.indices.random()
                    lastPlayedCounter = coinCount
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
                }, modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    if (result == null) "Start" else "Play Again",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun JackpotCard(jackpot: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ), modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (jackpot > 0) {
                Text(
                    text = "JACKPOT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Paid,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = jackpot.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            } else {
                Text(
                    "INSERT A COIN TO PLAY!", color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/** Sweeps back and forth across the range with decaying amplitude, then settles on [target]. */
private suspend fun spinToTarget(
    knob: Animatable<Float, *>,
    target: Int,
    lastIndex: Int,
) {
    listOf(0f to lastIndex.toFloat()) // full range first
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
            coinCount = 12, onSubmit = {
                println("Submitted: $it")
            })
    }
}
