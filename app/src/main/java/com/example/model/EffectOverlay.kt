package com.example.model

enum class EffectType(val displayName: String, val description: String) {
    GLITCH_RGB("RGB Glitch", "Cyberpunk chromatic color split"),
    FILM_BURN("Film Burn", "Vintage warm analog light leak"),
    SPARKLES("Neon Sparkles", "Shimmering glow particle accents"),
    VIGNETTE_PULSE("Dark Vignette", "Dramatic edge shadow"),
    RETRO_VHS("VHS 90s", "Classic retro tape scanlines"),
    ZOOM_BLUR("Impact Zoom", "Dynamic motion kinetic burst")
}

data class EffectOverlay(
    val id: String,
    val effectType: EffectType,
    val startMs: Long = 0L,
    val endMs: Long = 2500L,
    val intensity: Float = 0.8f
)

data class StickerOverlay(
    val id: String,
    val symbol: String,
    val label: String,
    val category: String = "Trending",
    val positionX: Float = 0.5f,
    val positionY: Float = 0.4f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val startMs: Long = 0L,
    val endMs: Long = 3000L
)
