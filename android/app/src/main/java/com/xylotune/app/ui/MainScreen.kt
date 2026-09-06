package com.xylotune.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xylotune.app.audio.AudioEngine
import com.xylotune.app.data.DriveStatus
import com.xylotune.app.data.DriveSyncEngine
import com.xylotune.app.data.PadState
import com.xylotune.app.data.SongRepository
import com.xylotune.app.data.buildTape
import com.xylotune.app.data.loadSoundPref
import com.xylotune.app.data.rememberDriveAuthManager
import com.xylotune.app.data.saveSoundPref
import com.xylotune.app.model.Song
import com.xylotune.app.player.LiveCapture
import com.xylotune.app.player.Playback
import com.xylotune.app.player.PracticeEngine
import com.xylotune.app.ui.dialogs.ActionDialogHost
import com.xylotune.app.ui.dialogs.DialogHost
import com.xylotune.app.ui.link.LinkView
import com.xylotune.app.ui.roll.PaperRoll
import com.xylotune.app.ui.sheet.PaperSheet
import com.xylotune.app.ui.composer.CursorToolbar
import com.xylotune.app.ui.xylophone.XylophoneBoard
import kotlinx.coroutines.launch

private enum class AppMode { Compose, Practice, Play, Paused }

/**
 * The paper-roll redesign's single screen: one toolbar, one board, and the four views the
 * lyrics button cycles through (Roll/Sheet/Link/Keys) — replacing the old Play/Sheet tab
 * split and its pull-up drawers (see MainActivity for the theme/full-screen half of the
 * redesign, and the mockup's "one red box, three jobs" note for why [AppMode] exists at
 * all: composing, practising and playing each give the reading head a different meaning,
 * and gate whether a bar strike writes into the pad or not).
 */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dialogHost = remember { DialogHost() }

    var material by remember { mutableStateOf(loadSoundPref(context)) }
    val pad = remember { PadState() }
    val liveCapture = remember { LiveCapture(pad, scope) }
    val playback = remember {
        Playback(pad = pad, scope = scope, onPlayNote = { noteIndex, _ -> AudioEngine.playNote(noteIndex, material) })
            .also { p -> pad.onBeforeEdit = { p.finish(); liveCapture.stop() } }
    }
    val practice = remember { PracticeEngine(pad) }

    val driveAuth = rememberDriveAuthManager()
    val driveSync = remember { DriveSyncEngine(context, driveAuth) }

    var savedSongs by remember { mutableStateOf(SongRepository.loadSongs(context)) }
    var currentSongName by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(AppMode.Compose) }
    var view by remember { mutableStateOf(LyricsView.Roll) }
    var settingsOpen by remember { mutableStateOf(false) }
    var musicOpen by remember { mutableStateOf(false) }
    var linkLineIndex by remember { mutableIntStateOf(0) }
    var armedNoteIndex by remember { mutableStateOf<Int?>(null) }

    // Mirrors the web's silent-reconnect-on-load effect: only ever fires once, never shows
    // any UI — if the grant no longer holds, this just leaves the button in Disconnected
    // state for the user to reconnect by hand (see DriveAuthManager.trySilent).
    LaunchedEffect(Unit) {
        driveSync.trySilentReconnect { updated -> savedSongs = updated }
    }

    fun closePanels() {
        settingsOpen = false
        musicOpen = false
    }

    fun startPracticeIfPossible() {
        practice.start()
        mode = if (practice.active) AppMode.Practice else AppMode.Compose
    }

    fun loadSong(name: String) {
        val song = savedSongs[name] ?: return
        playback.finish()
        liveCapture.stop()
        pad.loadSong(song.lines, song.bpm, song.meter)
        currentSongName = name
        linkLineIndex = 0
        armedNoteIndex = null
        startPracticeIfPossible()
    }

    fun newSong() {
        playback.finish()
        liveCapture.stop()
        pad.newSong()
        currentSongName = null
        mode = AppMode.Compose
        linkLineIndex = 0
        armedNoteIndex = null
    }

    fun resetTransport() {
        playback.finish()
        liveCapture.stop()
        mode = if (currentSongName != null) AppMode.Practice else AppMode.Compose
        if (mode == AppMode.Practice) startPracticeIfPossible()
    }

    val isReady by AudioEngine.isReady.collectAsState()
    val tape = buildTape(pad.lines)
    val totalTicks = tape.sumOf { it.ticks }.toDouble()

    fun cursorTick(): Double {
        val exact = tape.firstOrNull { it.li == pad.cursor.line && it.ni == pad.cursor.pos }
        if (exact != null) return exact.startTick.toDouble()
        val lastOnLine = tape.lastOrNull { it.li == pad.cursor.line }
        return lastOnLine?.let { (it.startTick + it.ticks).toDouble() } ?: totalTicks
    }

    val practiceTapeIndex = practice.playingPos?.let { pos -> tape.indexOfFirst { it.li == pos.li && it.ni == pos.ni } }

    val (headTick, highlightIndex, animated) = when (mode) {
        AppMode.Compose -> if (liveCapture.recording) Triple(liveCapture.currentTick, null, false) else Triple(cursorTick(), null, true)
        AppMode.Practice -> Triple(practiceTapeIndex?.let { tape[it].startTick.toDouble() } ?: 0.0, practiceTapeIndex, true)
        AppMode.Play -> Triple(playback.currentTick, playback.currentIndex, false)
        AppMode.Paused -> Triple(playback.currentTick, playback.currentIndex, true)
    }

    fun lyricWordFor(ev: com.xylotune.app.data.TapeEvent): String {
        val line = pad.lines.getOrNull(ev.li) ?: return ""
        val tag = com.xylotune.app.data.collectLineTags(line.notes).find { it.noteIdx == ev.ni } ?: return ""
        return line.lyric.substring(tag.start.coerceIn(0, line.lyric.length), tag.end.coerceIn(0, line.lyric.length))
    }

    fun onStrike(index: Int) {
        if (isReady) AudioEngine.playNote(index, material)
        when (mode) {
            AppMode.Practice -> practice.strike(index)
            AppMode.Compose -> if (view == LyricsView.Roll || view == LyricsView.Keys) liveCapture.punch(index)
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppToolbar(
                songTitle = currentSongName,
                view = view,
                playing = mode == AppMode.Play,
                playEnabled = pad.hasAnyNotes(),
                resetEnabled = pad.hasAnyNotes(),
                settingsOpen = settingsOpen,
                musicOpen = musicOpen,
                onSettingsClick = { val next = !settingsOpen; closePanels(); settingsOpen = next },
                onMusicClick = { val next = !musicOpen; closePanels(); musicOpen = next },
                onViewCycle = {
                    view = when (view) {
                        LyricsView.Roll -> LyricsView.Sheet
                        LyricsView.Sheet -> LyricsView.Link
                        LyricsView.Link -> LyricsView.Keys
                        LyricsView.Keys -> LyricsView.Roll
                    }
                    if (view != LyricsView.Roll && view != LyricsView.Keys) liveCapture.stop()
                    armedNoteIndex = null
                    closePanels()
                },
                onReset = { resetTransport(); closePanels() },
                onPlayPause = {
                    liveCapture.stop()
                    if (pad.hasAnyNotes()) {
                        if (mode == AppMode.Play) {
                            playback.pause()
                            mode = AppMode.Paused
                        } else {
                            playback.startOrResume()
                            mode = AppMode.Play
                        }
                    }
                    closePanels()
                },
                onMaterialToggle = {
                    material = if (material == com.xylotune.app.audio.SoundMaterial.Metal) com.xylotune.app.audio.SoundMaterial.Wood else com.xylotune.app.audio.SoundMaterial.Metal
                    saveSoundPref(context, material)
                    closePanels()
                },
            )

            Box(modifier = Modifier.weight(1f)) {
                when (view) {
                    LyricsView.Roll, LyricsView.Keys -> Column(modifier = Modifier.fillMaxSize()) {
                        if (view == LyricsView.Roll) {
                            PaperRoll(
                                tape = tape,
                                meter = pad.meter,
                                material = material,
                                headTick = headTick,
                                highlightIndex = highlightIndex,
                                animated = animated,
                                lyricWordFor = ::lyricWordFor,
                            )
                        }
                        XylophoneBoard(
                            material = material,
                            targetIndex = if (mode == AppMode.Practice) practice.targetNoteIndex else null,
                            onStrike = ::onStrike,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    LyricsView.Sheet -> {
                        PaperSheet(
                            lines = pad.lines,
                            meter = pad.meter,
                            material = material,
                            cursorLine = pad.cursor.line,
                            cursorPos = pad.cursor.pos,
                            hasAnyNotes = pad.hasAnyNotes(),
                            onNoteClick = { li, ni -> if (mode == AppMode.Compose) pad.placeCursor(li, ni + 1) },
                            onLyricChange = { li, text -> if (mode == AppMode.Compose) pad.setLyricText(li, text) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    LyricsView.Link -> {
                        LinkView(
                            lines = pad.lines,
                            material = material,
                            lineIndex = linkLineIndex,
                            onLineIndexChange = { linkLineIndex = it },
                            armedNoteIndex = armedNoteIndex,
                            onArmNote = { armedNoteIndex = it },
                            onLinkWord = { noteIdx, start, end ->
                                pad.linkLyric(linkLineIndex, noteIdx, noteIdx, start, end)
                                armedNoteIndex = null
                            },
                            onUnlinkWord = { noteIdx -> pad.unlinkLyric(linkLineIndex, noteIdx) },
                            onStrike = { i -> if (isReady) AudioEngine.playNote(i, material) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            if (view == LyricsView.Sheet) {
                CursorToolbar(
                    onMoveLeft = { if (mode == AppMode.Compose) pad.moveCursor(com.xylotune.app.data.CursorDirection.Left) },
                    onMoveRight = { if (mode == AppMode.Compose) pad.moveCursor(com.xylotune.app.data.CursorDirection.Right) },
                    onBackspace = { if (mode == AppMode.Compose) pad.backspace() },
                    onForwardDelete = { if (mode == AppMode.Compose) pad.forwardDelete() },
                    onHardDelete = { if (mode == AppMode.Compose) pad.hardDelete() },
                    onAddHold = { if (mode == AppMode.Compose) pad.addHold() },
                    onNewLine = { if (mode == AppMode.Compose) pad.newLine() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        if (musicOpen) {
            MusicMenu(
                songNames = savedSongs.keys.toList(),
                currentSongName = currentSongName,
                onSelectSong = { name -> loadSong(name); closePanels() },
                onNewSong = { newSong(); closePanels() },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 56.dp, start = 10.dp),
            )
        }
        if (settingsOpen) {
            SettingsSheet(
                bpm = pad.bpm,
                meter = pad.meter,
                speedPercent = playback.speedPercent,
                speedEnabled = mode != AppMode.Practice,
                canDelete = currentSongName != null,
                onBpmChange = { pad.changeBpm(it) },
                onMeterCycle = {
                    val options = listOf(listOf(4, 4), listOf(3, 4), listOf(6, 8))
                    val i = options.indexOfFirst { it == pad.meter }
                    pad.changeMeter(options[(i + 1).mod(options.size)])
                },
                onSpeedChange = { playback.speedPercent = it },
                onSave = { scope.launch { saveSong(pad, dialogHost, driveSync, currentSongName, savedSongs, { savedSongs = it }, { currentSongName = it }, context) } },
                onDelete = { scope.launch { deleteSong(pad, ::newSong, dialogHost, driveSync, currentSongName, savedSongs, { savedSongs = it }, { currentSongName = it }, context) } },
                driveStatus = driveSync.status,
                onDriveClick = { scope.launch { onDriveButtonClick(driveSync, dialogHost) { updated -> savedSongs = updated } } },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 10.dp),
            )
        }
        ActionDialogHost(dialogHost)
    }
}

// Ported from index.html's onDriveButtonClick (lines ~1553-1564): connected/error means a
// tap disconnects (after confirming); anything else means a tap tries to connect.
private suspend fun onDriveButtonClick(driveSync: DriveSyncEngine, dialogHost: DialogHost, onSongsUpdated: (Map<String, Song>) -> Unit) {
    if (driveSync.status == DriveStatus.Connected || driveSync.status == DriveStatus.Error) {
        val ok = dialogHost.askConfirm("Disconnect Google Drive? Your songs stay saved on this device.", confirmLabel = "Disconnect")
        if (ok) driveSync.disconnect()
        return
    }
    driveSync.connect(onSongsUpdated)
}

// Ported from index.html's saveSong() (lines ~1440-1458): prompt for a name, confirm
// overwrite if it collides with a different existing song, persist locally, then push to
// Drive (fire-and-forget, matching the web's own pushToDrive call).
private suspend fun saveSong(
    pad: PadState,
    dialogHost: DialogHost,
    driveSync: DriveSyncEngine,
    currentSongName: String?,
    savedSongs: Map<String, Song>,
    setSavedSongs: (Map<String, Song>) -> Unit,
    setCurrentSongName: (String?) -> Unit,
    context: Context,
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
    val next = savedSongs + (trimmed to Song(lines = pad.toModel(), updatedAt = System.currentTimeMillis(), bpm = pad.bpm, meter = pad.meter))
    setSavedSongs(next)
    SongRepository.saveSongs(context, next)
    val tombstones = SongRepository.loadTombstones(context)
    val nextTombstones = if (tombstones.containsKey(trimmed)) tombstones - trimmed else tombstones
    if (nextTombstones !== tombstones) SongRepository.saveTombstones(context, nextTombstones)
    setCurrentSongName(trimmed)
    driveSync.push(next, nextTombstones)
}

// Ported from index.html's deleteSong() (lines ~1459-1477): confirm, persist a tombstone
// (not just remove the key), clear the pad back to empty, then push to Drive.
private suspend fun deleteSong(
    pad: PadState,
    onCleared: () -> Unit,
    dialogHost: DialogHost,
    driveSync: DriveSyncEngine,
    currentSongName: String?,
    savedSongs: Map<String, Song>,
    setSavedSongs: (Map<String, Song>) -> Unit,
    setCurrentSongName: (String?) -> Unit,
    context: Context,
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
    onCleared()
    driveSync.push(next, tombstones)
}
