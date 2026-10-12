package com.dtolan.numbers.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages

/**
 * Dropdown for picking the Listen-mode "confirm in" language — a global preference, shown both
 * in the Settings sheet and (read-only) referenced from the Listen screen. A dropdown rather than
 * a chip row so it scales cleanly as more languages get added.
 */
@Composable
fun ConfirmLanguagePicker(
    language: LanguagePack,
    selectedCode: String?,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = selectedCode?.let { code ->
        val selected = Languages.byCode(code)
        "${selected.flag} ${selected.displayName}"
    } ?: language.ui.listenConfirmOff

    Row {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) {
            Text(currentLabel)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.wrapContentSize()) {
            DropdownMenuItem(
                text = { Text(language.ui.listenConfirmOff) },
                onClick = { expanded = false; onSelect(null) },
            )
            Languages.ALL.filter { it.code != language.code }.forEach { other ->
                DropdownMenuItem(
                    text = { Text("${other.flag} ${other.displayName}") },
                    onClick = { expanded = false; onSelect(other.code) },
                )
            }
        }
    }
}
