package com.dtolan.numbers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.GameScoreStore
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.viewmodel.AppMode

private data class Tile(
    val mode: AppMode,
    val icon: String,
    val title: String,
    val subtitle: String,
    val bestScore: Int?,
)

/**
 * The landing screen: tiles grouped into sections (train, passive listening, understand, games),
 * so every mode stays one tap away while related modes sit together. Tapping a tile opens that
 * mode full-screen with a compact top bar (see NumbersApp). A new mode is one more Tile in the
 * section it belongs to.
 */
@Composable
fun HomeScreen(language: LanguagePack, onSelect: (AppMode) -> Unit) {
    val context = LocalContext.current
    val scoreStore = remember { GameScoreStore(context) }
    val bingoBest by scoreStore.bestScore(GameScoreStore.GAME_BINGO, language.code).collectAsState(initial = null)
    val sequenceBest by scoreStore.bestScore(GameScoreStore.GAME_SEQUENCE, language.code).collectAsState(initial = null)
    val dictationBest by scoreStore.bestScore(GameScoreStore.GAME_DICTATION, language.code).collectAsState(initial = null)
    val simonBest by scoreStore.bestScore(GameScoreStore.GAME_SIMON, language.code).collectAsState(initial = null)
    val storiesBest by scoreStore.storiesUnlockedTotal(language.code).collectAsState(initial = null)

    val ui = language.ui
    val sections = listOf(
        ui.homeSectionTrain to listOf(
            Tile(AppMode.LEARN, "⚡", ui.learnModeLabel, ui.learnModeSubtitle, null),
            Tile(AppMode.PRACTICE, "⌨️", ui.practiceModeLabel, ui.practiceModeSubtitle, null),
        ),
        ui.homeSectionListen to listOf(
            Tile(AppMode.LISTEN, "🎧", ui.listenModeLabel, ui.listenModeSubtitle, null),
            Tile(AppMode.COUNT, "🔁", ui.countModeLabel, ui.countModeSubtitle, null),
        ),
        ui.homeSectionUnderstand to listOf(
            Tile(AppMode.STORIES, "📖", ui.storyModeLabel, ui.storyModeSubtitle, storiesBest),
        ),
        language.speak.homeSection to listOf(
            Tile(AppMode.SPEAK, "🎤", language.speak.modeLabel, language.speak.modeSubtitle, null),
        ),
        ui.homeSectionGames to listOf(
            Tile(AppMode.BINGO, "🎯", ui.bingoModeLabel, ui.bingoModeSubtitle, bingoBest),
            Tile(AppMode.SEQUENCE, "🔢", ui.sequenceModeLabel, ui.sequenceModeSubtitle, sequenceBest),
            Tile(AppMode.DICTATION, "✍️", ui.dictationModeLabel, ui.dictationModeSubtitle, dictationBest),
            Tile(AppMode.SIMON, "🧠", ui.simonModeLabel, ui.simonModeSubtitle, simonBest),
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        sections.forEach { (title, tiles) ->
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
            tiles.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                ) {
                    // A lone tile (e.g. Stories) spans the full width.
                    row.forEach { tile ->
                        TileCard(
                            tile = tile,
                            language = language,
                            onClick = { onSelect(tile.mode) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileCard(tile: Tile, language: LanguagePack, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Text(tile.icon, style = MaterialTheme.typography.headlineSmall)
            Text(tile.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (tile.bestScore != null) {
                Text(
                    String.format(language.ui.bestScoreLabel, tile.bestScore),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
