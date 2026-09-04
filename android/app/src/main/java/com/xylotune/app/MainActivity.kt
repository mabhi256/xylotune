package com.xylotune.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.AudioEngine
import com.xylotune.app.data.loadSoundPref
import com.xylotune.app.data.saveSoundPref
import com.xylotune.app.ui.theme.XylotuneTheme
import com.xylotune.app.ui.xylophone.SoundToggle
import com.xylotune.app.ui.xylophone.XylophoneBoard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XylotuneTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    InstrumentScreen()
                }
            }
        }
    }
}

// Placeholder top-level screen for M1: a fully playable instrument, no composer yet.
// MainScreen (top app bar + Play/Sheet tabs) replaces this in M2.
@Composable
private fun InstrumentScreen() {
    val context = LocalContext.current
    val isReady by AudioEngine.isReady.collectAsState()
    var material by remember { mutableStateOf(loadSoundPref(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // enableEdgeToEdge() draws the background behind the system status/navigation
            // bars for a borderless look, but actual content — the bars themselves — must
            // stay clear of them, or (as on this landscape layout) the last bar's edge
            // renders right under the on-screen nav buttons.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        XylophoneBoard(
            material = material,
            targetIndex = null,
            onStrike = { index -> if (isReady) AudioEngine.playNote(index, material) },
        )
        SoundToggle(
            material = material,
            onChange = {
                material = it
                saveSoundPref(context, it)
            },
        )
    }
}
