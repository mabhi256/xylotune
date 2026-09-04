package com.xylotune.app.ui.composer

// Mobile's 3-state replacement for index.html's 5-state LYRIC_CYCLE
// (off/on/grouped/link/edit). "Link" isn't its own mode here: with no mouse-drag text
// selection, linking is a tap-note-then-tap-word action available inside Show (see
// LinkToggleButton), not a separate screen state to cycle through.
enum class LyricMode(val label: String) {
    Off("Off"),
    Show("Show"),
    Edit("Edit"),
}
