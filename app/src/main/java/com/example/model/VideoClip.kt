package com.example.model

enum class StockTheme {
    CYBERPUNK,
    SUNSET_WAVES,
    NEON_DRIFT,
    RETRO_FILM,
    AESTHETIC_CAFE,
    MOUNTAIN_ROADTRIP
}

enum class TransitionType(val displayName: String) {
    NONE("None"),
    DISSOLVE("Dissolve"),
    FLASH_WHITE("Flash White"),
    FADE_BLACK("Fade Black"),
    WHIP_PAN("Whip Pan"),
    ZOOM_IN("Zoom In"),
    GLITCH("Glitch")
}

data class VideoClip(
    val id: String,
    val title: String,
    val uri: String? = null,
    val stockTheme: StockTheme? = null,
    val sourceDurationMs: Long = 5000L,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = 5000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val rotation: Float = 0f,
    val isMirrored: Boolean = false,
    val isReversed: Boolean = false,
    val transitionIn: TransitionType = TransitionType.NONE,
    val transitionDurationMs: Long = 500L
) {
    val effectiveDurationMs: Long
        get() {
            val trimmed = (endTrimMs - startTrimMs).coerceAtLeast(200L)
            return (trimmed / speed.coerceIn(0.2f, 10f)).toLong()
        }
}
