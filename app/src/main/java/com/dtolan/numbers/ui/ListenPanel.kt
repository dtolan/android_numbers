package com.dtolan.numbers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.listen.ListeningMode
import com.dtolan.numbers.listen.ListeningState

private fun modeLabel(language: LanguagePack, mode: ListeningMode): String = when (mode) {
    ListeningMode.SPACED_REPETITION -> language.ui.listenModeSpacedLabel
    ListeningMode.SEQUENTIAL -> language.ui.listenModeSequentialLabel
    ListeningMode.RANDOM_SHUFFLE -> language.ui.listenModeShuffleLabel
    ListeningMode.ALWAYS_CONFIRM -> language.ui.listenModeAlwaysConfirmLabel
}

/**
 * The passive "listen while doing something else" screen. Deliberately simple: pick a stage,
 * tap Start, then this can be ignored entirely — [ListeningService][com.dtolan.numbers.listen.ListeningService]
 * keeps talking with the screen off, controllable from its notification. The "confirm in"
 * language is a global preference set from the Settings sheet (⚙), shown here read-only so it's
 * clear what will play, without duplicating the picker on every screen that might use it.
 */
@Composable
fun ListenPanel(
    language: LanguagePack,
    initialStageIndex: Int,
    listeningState: ListeningState,
    confirmLanguageCode: String?,
    onStart: (stageIndex: Int, mode: ListeningMode) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    var stageIndex by remember(language.code) {
        mutableIntStateOf(initialStageIndex.coerceIn(0, (language.learnStages.size - 1).coerceAtLeast(0)))
    }
    var mode by remember { mutableStateOf(ListeningMode.SPACED_REPETITION) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    val isThisLanguageRunning = listeningState.isRunning && !listeningState.isCount && listeningState.languageCode == language.code
    val effectiveConfirmCode = confirmLanguageCode?.takeIf { it != language.code }
    val confirmLabel = effectiveConfirmCode?.let { "${Languages.byCode(it).flag} ${Languages.byCode(it).displayName}" }
        ?: language.ui.listenConfirmOff
    val displayedMode = if (isThisLanguageRunning) listeningState.mode else mode

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(language.ui.listenModeLabel, style = MaterialTheme.typography.titleMedium)
            Text(language.ui.listenHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(language.learnStages) { index, stage ->
                    val selected = index == stageIndex
                    Surface(
                        onClick = { stageIndex = index },
                        enabled = !isThisLanguageRunning,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.width(96.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stage.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }

            Text(language.ui.listenModePickerLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                OutlinedButton(onClick = { modeMenuExpanded = true }, enabled = !isThisLanguageRunning) {
                    Text(modeLabel(language, displayedMode))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = modeMenuExpanded, onDismissRequest = { modeMenuExpanded = false }, modifier = Modifier.wrapContentSize()) {
                    ListeningMode.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(modeLabel(language, candidate)) },
                            onClick = { modeMenuExpanded = false; mode = candidate },
                        )
                    }
                }
            }

            Text(
                "${language.ui.listenConfirmLabel} $confirmLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isThisLanguageRunning) {
                Text(
                    if (listeningState.isPaused) language.ui.listenPausedStatus else language.ui.listenRunningStatus,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                listeningState.currentValue?.let { value ->
                    Text("${language.flag} $value", style = MaterialTheme.typography.headlineMedium)
                    Text(language.spellNumber(value), style = MaterialTheme.typography.titleMedium)
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
                Button(onClick = { onStart(stageIndex, mode) }, modifier = Modifier.fillMaxWidth()) {
                    Text(language.ui.listenStartButton)
                }
            }
        }
    }
}
