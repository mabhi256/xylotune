package com.xylotune.app.ui.composer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.AccentAmber
import com.xylotune.app.ui.theme.TextMuted
import com.xylotune.app.ui.theme.TextPrimary

/**
 * Lyric text editing, mobile's replacement for the web's hand-rolled contentEditable caret
 * and drag-select: a native text field, so cursor placement, selection handles, and the
 * keyboard are the OS's job again. The amber dashed outline matches index.html's own
 * `.lyric-editable.editing` affordance color. Committing on every keystroke (not just blur)
 * mirrors the web's handleInput, which strips a line's tags the moment its text changes.
 */
@Composable
fun LyricEditField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        placeholder = { Text("Add lyrics…", color = TextMuted) },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = AccentAmber,
            unfocusedBorderColor = AccentAmber.copy(alpha = 0.5f),
            cursorColor = AccentAmber,
        ),
    )
}
