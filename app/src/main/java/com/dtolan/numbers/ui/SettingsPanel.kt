package com.dtolan.numbers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.Category
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.logic.PracticeSettings

@Composable
fun SettingsPanel(
    language: LanguagePack,
    settings: PracticeSettings,
    onCategoryToggle: (Category) -> Unit,
    onRangesChange: (min: Int, max: Int, decimals: Int, yearMin: Int, yearMax: Int) -> Unit,
) {
    // Collapsed by default so the question card stays the focus; the header summarizes what's on.
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(language.ui.settingsTitle, style = MaterialTheme.typography.titleMedium)
                    if (!expanded) {
                        Text(
                            Category.entries.filter { it in settings.enabledCategories }
                                .joinToString(" · ") { language.categoryLabels.getValue(it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            if (!expanded) return@Column

            Text(language.ui.categoriesLabel, style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Category.entries.toList()) { category ->
                    FilterChip(
                        selected = category in settings.enabledCategories,
                        onClick = { onCategoryToggle(category) },
                        label = { Text(language.categoryLabels.getValue(category)) },
                    )
                }
            }

            RangeFields(language, settings, onRangesChange)
        }
    }
}

/** Language dropdown; [compact] shows just the flag, for the in-mode top bar. */
@Composable
fun LanguagePicker(language: LanguagePack, onLanguageChange: (String) -> Unit, compact: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    Row {
        if (compact) {
            TextButton(onClick = { expanded = true }) {
                Text(language.flag, style = MaterialTheme.typography.titleLarge)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = language.ui.languageLabel)
            }
        } else {
            ElevatedButton(onClick = { expanded = true }) {
                Text("${language.flag} ${language.displayName}")
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.wrapContentSize()) {
            Languages.ALL.forEach { pack ->
                DropdownMenuItem(
                    text = { Text("${pack.flag} ${pack.displayName}") },
                    onClick = {
                        expanded = false
                        onLanguageChange(pack.code)
                    },
                )
            }
        }
    }
}

@Composable
private fun RangeFields(
    language: LanguagePack,
    settings: PracticeSettings,
    onRangesChange: (min: Int, max: Int, decimals: Int, yearMin: Int, yearMax: Int) -> Unit,
) {
    var minText by remember(settings.minVal) { mutableStateOf(settings.minVal.toString()) }
    var maxText by remember(settings.maxVal) { mutableStateOf(settings.maxVal.toString()) }
    var decimalsText by remember(settings.decimalPlaces) { mutableStateOf(settings.decimalPlaces.toString()) }
    var yearMinText by remember(settings.yearMin) { mutableStateOf(settings.yearMin.toString()) }
    var yearMaxText by remember(settings.yearMax) { mutableStateOf(settings.yearMax.toString()) }

    fun push() {
        onRangesChange(
            minText.toIntOrNull() ?: settings.minVal,
            maxText.toIntOrNull() ?: settings.maxVal,
            decimalsText.toIntOrNull() ?: settings.decimalPlaces,
            yearMinText.toIntOrNull() ?: settings.yearMin,
            yearMaxText.toIntOrNull() ?: settings.yearMax,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = minText,
            onValueChange = { minText = it; push() },
            label = { Text(language.ui.minLabel) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = maxText,
            onValueChange = { maxText = it; push() },
            label = { Text(language.ui.maxLabel) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = decimalsText,
            onValueChange = { decimalsText = it; push() },
            label = { Text(language.ui.decimalsLabel) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = yearMinText,
            onValueChange = { yearMinText = it; push() },
            label = { Text(language.ui.yearMinLabel) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = yearMaxText,
            onValueChange = { yearMaxText = it; push() },
            label = { Text(language.ui.yearMaxLabel) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
}
