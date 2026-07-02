package de.luh.hci.pclab.apps.casino.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.luh.hci.pclab.ui.theme.errorLight
import kotlin.math.abs

@Composable
fun RadioSlider(
    options: Array<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    animatedIndex: Float? = null,   // when set, knob follows this and input is locked
) {
    val tickSize = 10.dp
    val density = LocalDensity.current
    val locked = animatedIndex != null

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            val totalWidth = maxWidth
            val padH = maxWidth / options.size / 2
            val usable = totalWidth - padH * 2
            val step = usable / (options.size - 1)

            fun xForIndex(i: Float) = padH + step * i
            fun xForIndex(i: Int) = xForIndex(i.toFloat())

            // Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(3.dp)
                    )
            )

            // Ticks
            options.indices.forEach { i ->
                val x = xForIndex(i)
                val isSelected = i == selected
                Box(
                    modifier = Modifier
                        .offset(x = x - tickSize / 2)
                        .size(tickSize)
                        .align(Alignment.CenterStart)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .then(
                            if (locked) Modifier
                            else Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onSelect(i) }
                        )
                        .visible(!isSelected)
                )
            }

            // highlight only when not animating (based on discrete selection)
            if (!locked) {
                val highlightLeft = if (selected == 0) 0.dp
                else (xForIndex(selected) + xForIndex(selected - 1)) / 2
                val highlightRight = if (selected == options.lastIndex) totalWidth
                else (xForIndex(selected) + xForIndex(selected + 1)) / 2

                Box(
                    modifier = Modifier
                        .offset(x = highlightLeft)
                        .width(highlightRight - highlightLeft)
                        .height(6.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            MaterialTheme.colorScheme.inverseOnSurface,
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            val knobX = if (animatedIndex != null) xForIndex(animatedIndex)
            else xForIndex(selected)

            SliderKnob(
                xPos = knobX,
                enabled = !locked,
                onSelect = { currentX ->
                    val newIndex = options.indices.minByOrNull { i ->
                        val tx = with(density) { xForIndex(i).toPx() }
                        abs(currentX - tx)
                    } ?: selected
                    onSelect(newIndex)
                }
            )
        }

        // Labels (unchanged)
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i == selected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}


@Composable
fun BoxWithConstraintsScope.SliderKnob(
    xPos: Dp,
    width: Dp = 30.dp,
    height: Dp = 44.dp,
    color: Color = errorLight,
    enabled: Boolean = true,
    onSelect: (Float) -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset(x = xPos - width / 2)
            .size(width, height)
            .align(Alignment.CenterStart)
            .then(
                if (!enabled) Modifier
                else Modifier.draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> dragX += delta },
                    onDragStarted = { dragX = 0f },
                    onDragStopped = {
                        val currentX = with(density) { xPos.toPx() } + dragX
                        onSelect(currentX)
                        dragX = 0f
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(height)
                .background(color)
        )
    }
}