package com.dtolan.numbers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.listen.ListeningState

/**
 * A slim bar, like a music app's mini-player, shown at the bottom of every screen while the
 * background Listen drill or Count session runs — so it can be paused or stopped without going
 * back to its own screen. Tapping the bar opens that screen.
 */
@Composable
fun NowPlayingBar(
    state: ListeningState,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val lang = Languages.byCode(state.languageCode)
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, tonalElevation = 3.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(if (state.isCount) "🔁" else "🎧", style = MaterialTheme.typography.titleMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${lang.flag} ${state.stageTitle}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        state.isPaused -> lang.ui.listenPausedStatus
                        state.currentValue != null -> "${state.currentValue} · ${state.spell(lang, state.currentValue)}"
                        else -> "…"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.isPaused) {
                TextButton(onClick = onResume) { Text(lang.ui.listenResumeButton) }
            } else {
                TextButton(onClick = onPause) { Text(lang.ui.listenPauseButton) }
            }
            TextButton(onClick = onStop) { Text(lang.ui.listenStopButton) }
        }
    }
}
