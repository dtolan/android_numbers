package com.dtolan.numbers.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import com.dtolan.numbers.data.LanguagePack

/**
 * The written-out form of a whole-number answer ("78" → "soixante-dix-huit"), shown after answering
 * so the learner sees how the number they heard is spelled. Draws nothing for anything that isn't
 * a plain digit string (money, dates, phone numbers, times).
 */
@Composable
fun NumberWordsLine(language: LanguagePack, answer: String?, modifier: Modifier = Modifier) {
    val value = answer?.takeIf { it.matches(PLAIN_DIGITS) }?.toIntOrNull() ?: return
    Text(
        language.spellNumber(value),
        style = MaterialTheme.typography.titleMedium,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

private val PLAIN_DIGITS = Regex("\\d{1,9}")
