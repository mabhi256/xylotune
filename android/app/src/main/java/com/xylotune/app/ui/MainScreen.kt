package com.xylotune.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.xylotune.app.audio.AudioEngine
import com.xylotune.app.data.PadState
import com.xylotune.app.data.SongRepository
import com.xylotune.app.data.loadSoundPref
import com.xylotune.app.data.saveSoundPref
import com.xylotune.app.model.Song
import com.xylotune.app.player.Playback
import com.xylotune.app.ui.composer.PadScreen
import com.xylotune.app.ui.composer.PlayTab
import com.xylotune.app.ui.dialogs.ActionDialogHost
import com.xylotune.app.ui.dialogs.DialogHost
import kotlinx.coroutines.launch

private enum class Tab { Play, Sheet }

/** Top app bar + Play/Sheet bottom tabs, hoisting the PadState/Playback both tabs share. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogHost = remember { DialogHost() }

    var material by remember { mutableStateOf(loadSoundPref(context)) }
    val pad = remember { PadState() }
    val playback = remember {
        Playback(pad = pad, scope = scope, onPlayNote = { noteIndex, _ -> AudioEngine.playNote(noteIndex, material) })
            .also { p -> pad.onBeforeEdit = { p.finish() } }
    }

    var savedSongs by remember { mutableStateOf(SongRepository.loadSongs(context)) }
    var currentSongName by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(Tab.Play) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Xylotune") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Play,
                    onClick = { tab = Tab.Play },
                    icon = { Text("🎵") },
                    label = { Text("Play") },
                )
                NavigationBarItem(
                    selected = tab == Tab.Sheet,
                    onClick = { tab = Tab.Sheet },
                    icon = { Text("📄") },
                    label = { Text("Sheet") },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (tab) {
                Tab.Play -> PlayTab(
                    pad = pad,
                    playback = playback,
                    material = material,
                    onMaterialChange = {
                        material = it
                        saveSoundPref(context, it)
                    },
                    songNames = savedSongs.keys.toList(),
                    currentSongName = currentSongName,
                    onSelectSong = { name ->
                        savedSongs[name]?.let { song ->
                            playback.finish()
                            pad.replaceAll(song.lines)
                            currentSongName = name
                        }
                    },
                    onSave = {
                        scope.launch {
                            saveSong(pad, dialogHost, currentSongName, savedSongs, { savedSongs = it }, { currentSongName = it }, context)
                        }
                    },
                )
                Tab.Sheet -> PadScreen(
                    pad = pad,
                    material = material,
                    playback = playback,
                    canDeleteSong = currentSongName != null,
                    onDeleteSong = {
                        scope.launch {
                            deleteSong(pad, playback, dialogHost, currentSongName, savedSongs, { savedSongs = it }, { currentSongName = it }, context)
                        }
                    },
                )
            }
        }
        ActionDialogHost(dialogHost)
    }
}

// Ported from index.html's saveSong() (lines ~1440-1458): prompt for a name, confirm
// overwrite if it collides with a different existing song, persist locally.
private suspend fun saveSong(
    pad: PadState,
    dialogHost: DialogHost,
    currentSongName: String?,
    savedSongs: Map<String, Song>,
    setSavedSongs: (Map<String, Song>) -> Unit,
    setCurrentSongName: (String?) -> Unit,
    context: android.content.Context,
) {
    if (!pad.hasAnyNotes()) {
        dialogHost.showAlert("Play or write some notes before saving a song.")
        return
    }
    val name = dialogHost.askPrompt("Song name:", currentSongName ?: "") ?: return
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    if (savedSongs.containsKey(trimmed) && trimmed != currentSongName) {
        val ok = dialogHost.askConfirm("\"$trimmed\" already exists. Overwrite it?", confirmLabel = "Overwrite")
        if (!ok) return
    }
    val next = savedSongs + (trimmed to Song(lines = pad.toModel(), updatedAt = System.currentTimeMillis()))
    setSavedSongs(next)
    SongRepository.saveSongs(context, next)
    val tombstones = SongRepository.loadTombstones(context)
    if (tombstones.containsKey(trimmed)) {
        SongRepository.saveTombstones(context, tombstones - trimmed)
    }
    setCurrentSongName(trimmed)
}

// Ported from index.html's deleteSong() (lines ~1459-1477): confirm, persist a tombstone
// (not just remove the key), clear the pad back to empty.
private suspend fun deleteSong(
    pad: PadState,
    playback: Playback,
    dialogHost: DialogHost,
    currentSongName: String?,
    savedSongs: Map<String, Song>,
    setSavedSongs: (Map<String, Song>) -> Unit,
    setCurrentSongName: (String?) -> Unit,
    context: android.content.Context,
) {
    val name = currentSongName ?: return
    val ok = dialogHost.askConfirm("Delete \"$name\"?", confirmLabel = "Delete", danger = true)
    if (!ok) return
    val next = savedSongs - name
    setSavedSongs(next)
    SongRepository.saveSongs(context, next)
    val tombstones = SongRepository.loadTombstones(context) + (name to System.currentTimeMillis())
    SongRepository.saveTombstones(context, tombstones)
    setCurrentSongName(null)
    playback.finish()
    pad.replaceAll(emptyList())
}
