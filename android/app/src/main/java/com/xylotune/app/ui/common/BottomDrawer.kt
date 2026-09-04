package com.xylotune.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.AccentTeal
import com.xylotune.app.ui.theme.BorderSubtle
import com.xylotune.app.ui.theme.SurfaceControlHover
import com.xylotune.app.ui.theme.SurfaceElevated

private val HANDLE_HEIGHT = 28.dp

/**
 * The pull-tab for [DrawerPanel] — sound/transport/speed on the Play tab, cursor tools and
 * lyric mode on the Sheet tab, both tucked away so the keys/lines above keep the screen
 * (see the plan's Option B layout). In-flow at a fixed height, unlike the panel it opens.
 */
@Composable
fun DrawerHandle(open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HANDLE_HEIGHT)
            .clickable(onClick = onToggle)
            .semantics { contentDescription = if (open) "Hide controls" else "Show controls" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (open) AccentTeal else SurfaceControlHover),
        )
    }
}

/** Slides up from the bottom, overlaying whatever's behind it — call inside a [Box]. */
@Composable
fun DrawerPanel(open: Boolean, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    AnimatedVisibility(
        visible = open,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(SurfaceElevated)
                .border(1.dp, BorderSubtle, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
