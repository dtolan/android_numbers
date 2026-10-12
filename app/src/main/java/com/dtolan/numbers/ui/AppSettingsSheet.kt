package com.dtolan.numbers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.LanguagePack

/**
 * Global preferences that apply across every mode — Practice, Learn and Listen all read [Float]
 * speech rate from the same place, and Listen reads the confirm language from here too. Deliberately
 * separate from each mode's own inline controls (categories/ranges in Practice, stage pickers in
 * Learn/Listen): those are things you change every round while playing, not durable app settings,
 * so they stay on their own screens. This sheet is the one home for cross-cutting preferences —
 * the place future ones (voice choice, a new game's global option, etc.) should go too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsSheet(
    language: LanguagePack,
    speechRate: Float,
    confirmLanguageCode: String?,
    onRateChange: (Float) -> Unit,
    onConfirmLanguageChange: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(language.ui.settingsTitle, style = MaterialTheme.typography.titleLarge)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${language.ui.rateLabel}: ${"%.1f".format(speechRate)}", style = MaterialTheme.typography.labelMedium)
                Slider(value = speechRate, onValueChange = onRateChange, valueRange = 0.5f..1.3f, steps = 7)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(language.ui.listenConfirmLabel, style = MaterialTheme.typography.labelMedium)
                ConfirmLanguagePicker(
                    language = language,
                    selectedCode = confirmLanguageCode,
                    enabled = true,
                    onSelect = onConfirmLanguageChange,
                )
            }
        }
    }
}
