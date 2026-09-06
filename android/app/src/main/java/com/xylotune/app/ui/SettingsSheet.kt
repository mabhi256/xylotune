package com.xylotune.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xylotune.app.data.DriveStatus
import com.xylotune.app.ui.drive.DriveRow
import com.xylotune.app.ui.theme.AccentDanger
import com.xylotune.app.ui.theme.AppBackground
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.TextPrimary
import com.xylotune.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Tempo, meter, playback speed, and the song actions that used to float as separate HUD
 * chips or live in the pull-up drawer — gathered into one sheet, matching the redesign's
 * "everything is either on the toolbar or in here" rule.
 */
@Composable
fun SettingsSheet(
    bpm: Int,
    meter: List<Int>,
    speedPercent: Int,
    speedEnabled: Boolean,
    canDelete: Boolean,
    onBpmChange: (Int) -> Unit,
    onMeterCycle: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    driveStatus: DriveStatus,
    onDriveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(min = 240.dp, max = 320.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppBackground)
            .border(2.5.dp, TextPrimary, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsRow("Tempo") { Text("$bpm BPM", color = TextPrimary, fontWeight = FontWeight.Bold) }

        var taps by remember { mutableStateOf(emptyList<Long>()) }
        var lastTapNanos by remember { mutableLongStateOf(0L) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton("−", onClick = { onBpmChange(bpm - 5) })
            TapTempoButton(
                modifier = Modifier.weight(1f),
                onTap = {
                    val now = System.nanoTime()
                    taps = if (taps.isNotEmpty() && (now - lastTapNanos) / 1_000_000L > 2500L) listOf(now) else (taps + now).takeLast(9)
                    lastTapNanos = now
                    if (taps.size >= 3) {
                        val gaps = taps.zipWithNext { a, b -> (b - a) / 1_000_000.0 }.sorted()
                        val mid = gaps.size / 2
                        val medianMs = if (gaps.size % 2 == 1) gaps[mid] else (gaps[mid - 1] + gaps[mid]) / 2.0
                        onBpmChange((60000.0 / medianMs).roundToInt())
                    }
                },
            )
            StepButton("+", onClick = { onBpmChange(bpm + 5) })
        }

        SettingsRow("Meter") {
            Text(
                text = "${meter.getOrElse(0) { 4 }}/${meter.getOrElse(1) { 4 }}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .border(2.dp, TextPrimary, RoundedCornerShape(9.dp))
                    .clickable(onClick = onMeterCycle)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }

        val dotSeconds = com.xylotune.app.data.secPerTick(bpm) * com.xylotune.app.data.TICKS_PER_DOT
        SettingsRow("one sixteenth") {
            Text("%.3f s".format(dotSeconds), color = TextSecondary)
        }

        Divider()

        SettingsRow("Speed") { Text("$speedPercent%", color = TextPrimary, fontWeight = FontWeight.Bold) }
        Slider(
            value = speedPercent.toFloat(),
            onValueChange = { onSpeedChange(it.roundToInt()) },
            valueRange = 50f..200f,
            steps = 14,
            enabled = speedEnabled,
        )

        Divider()

        ActionRow("Save song", onClick = onSave)
        ActionRow("Delete song", onClick = onDelete, enabled = canDelete, danger = true)

        Divider()

        DriveRow(status = driveStatus, onClick = onDriveClick)
    }
}

@Composable
private fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextSecondary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(2.dp, BorderSubtle, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun TapTempoButton(onTap: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(2.dp, TextPrimary, RoundedCornerShape(9.dp))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Text("Tap tempo", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false) {
    Text(
        text = label,
        color = if (danger) AccentDanger else TextPrimary,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 9.dp),
    )
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(BorderSubtle))
}
