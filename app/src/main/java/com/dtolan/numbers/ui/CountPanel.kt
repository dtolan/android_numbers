package com.dtolan.numbers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.listen.ListeningState
import com.dtolan.numbers.logic.CountDirection
import kotlin.math.roundToInt

/** Slider scales: everyday counting up to 100, bigger numbers, and a years range. */
private val YEARS_SCALE = 1900..2030
private val SCALES = listOf(0..100, 0..1000, 0..10000, YEARS_SCALE)

/**
 * Passive counting: pick a range on the start/end bar (fine-tune each end with −/+), a direction (up, down, or
 * oscillating up and down)
 * and whether to repeat, then just listen. Runs in [com.dtolan.numbers.listen.ListeningService]
 * like the Listen drill, so it keeps going with the screen off and is controllable from the
 * notification — but speaks the range in order with a short gap and no translation echo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CountPanel(
    language: LanguagePack,
    listeningState: ListeningState,
    onStart: (from: Int, to: Int, direction: CountDirection, loop: Boolean, asYears: Boolean) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    var scaleIndex by rememberSaveable { mutableIntStateOf(0) }
    var from by rememberSaveable { mutableIntStateOf(1) }
    var to by rememberSaveable { mutableIntStateOf(100) }
    var direction by rememberSaveable { mutableStateOf(CountDirection.UP) }
    var loop by rememberSaveable { mutableStateOf(true) }
    val scale = SCALES[scaleIndex]
    val running = listeningState.isRunning && listeningState.isCount

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // While counting, only the progress and controls show — the setup is hidden so
            // Pause/Stop never end up below the fold.
            if (running) {
                Text(
                    if (listeningState.isPaused) language.ui.listenPausedStatus else listeningState.stageTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                listeningState.currentValue?.let { value ->
                    Text(
                        "$value",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    // The spelling under the digits ties what's heard to how it's written.
                    Text(
                        listeningState.spell(language, value),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (listeningState.isPaused) {
                        Button(onClick = onResume, modifier = Modifier.weight(1f)) { Text(language.ui.listenResumeButton) }
                    } else {
                        Button(onClick = onPause, modifier = Modifier.weight(1f)) { Text(language.ui.listenPauseButton) }
                    }
                    OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text(language.ui.listenStopButton) }
                }
            } else {
                Text(language.ui.countHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SCALES.forEachIndexed { index, range ->
                        FilterChip(
                            selected = scaleIndex == index,
                            onClick = {
                                scaleIndex = index
                                from = from.coerceIn(range)
                                to = to.coerceIn(range)
                                if (from == to) { from = range.first; to = range.last }
                            },
                            label = { Text("${range.first}–${range.last}") },
                        )
                    }
                }

                Text(
                    String.format(language.ui.countRangeLabel, from, to),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                RangeSlider(
                    value = from.toFloat()..to.toFloat(),
                    onValueChange = { range ->
                        from = range.start.roundToInt()
                        to = range.endInclusive.roundToInt()
                    },
                    valueRange = scale.first.toFloat()..scale.last.toFloat(),
                )
                // The bar is coarse on big scales, so each end can also be nudged one at a time.
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Stepper(from, onMinus = { from = (from - 1).coerceAtLeast(scale.first) }, onPlus = { from = (from + 1).coerceAtMost(to) })
                    Stepper(to, onMinus = { to = (to - 1).coerceAtLeast(from) }, onPlus = { to = (to + 1).coerceAtMost(scale.last) })
                }

                // Wraps rather than scrolls, so no option is hidden off the edge in longer languages.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        CountDirection.UP to language.ui.countUpLabel,
                        CountDirection.DOWN to language.ui.countDownLabel,
                        CountDirection.UP_DOWN to language.ui.countUpDownLabel,
                    ).forEach { (dir, label) ->
                        FilterChip(selected = direction == dir, onClick = { direction = dir }, label = { Text(label) })
                    }
                }
                FilterChip(selected = loop, onClick = { loop = !loop }, label = { Text(language.ui.countRepeatLabel) })

                Button(onClick = { onStart(from, to, direction, loop, scale == YEARS_SCALE) }, modifier = Modifier.fillMaxWidth()) {
                    Text(language.ui.listenStartButton)
                }
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onMinus) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
