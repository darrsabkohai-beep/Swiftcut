package com.example.model

enum class TextAnimation(val displayName: String) {
    NONE("None"),
    TYPEWRITER("Typewriter"),
    BOUNCE("Bounce"),
    POP_IN("Pop In"),
    NEON_FLICKER("Neon Flicker"),
    FADE_SLIDE("Fade Slide")
}

data class TextOverlay(
    val id: String,
    val text: String,
    val fontStyle: String = "Bold Sans",
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long? = 0xAA000000,
    val strokeColor: Long? = null,
    val strokeWidth: Float = 0f,
    val animationType: TextAnimation = TextAnimation.POP_IN,
    val positionX: Float = 0.5f, // Normalized 0f - 1f (center = 0.5f)
    val positionY: Float = 0.75f, // Typically subtitles or titles
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val startMs: Long = 0L,
    val endMs: Long = 3000L
)
