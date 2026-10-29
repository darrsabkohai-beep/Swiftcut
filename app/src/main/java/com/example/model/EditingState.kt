package com.example.model

sealed interface ActiveToolPanel {
    data object None : ActiveToolPanel
    data class EditClip(val clipId: String) : ActiveToolPanel
    data object SpeedControl : ActiveToolPanel
    data object AudioLibrary : ActiveToolPanel
    data object VolumeControl : ActiveToolPanel
    data object AddText : ActiveToolPanel
    data class EditText(val textId: String) : ActiveToolPanel
    data object AutoCaptions : ActiveToolPanel
    data object Filters : ActiveToolPanel
    data object AdjustColor : ActiveToolPanel
    data object Effects : ActiveToolPanel
    data object Transitions : ActiveToolPanel
    data object Stickers : ActiveToolPanel
    data object CanvasSettings : ActiveToolPanel
    data object ExportModal : ActiveToolPanel
}

enum class TimelineTrackType {
    VIDEO,
    AUDIO,
    TEXT,
    EFFECTS
}
