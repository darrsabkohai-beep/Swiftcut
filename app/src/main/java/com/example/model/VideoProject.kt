package com.example.model

enum class AspectRatio(val displayName: String, val ratioWidth: Float, val ratioHeight: Float, val hint: String) {
    RATIO_9_16("9:16", 9f, 16f, "TikTok / Reels / Shorts"),
    RATIO_16_9("16:9", 16f, 9f, "YouTube / Cinema"),
    RATIO_1_1("1:1", 1f, 1f, "Instagram Square"),
    RATIO_4_5("4:5", 4f, 5f, "Instagram Feed"),
    RATIO_21_9("21:9", 21f, 9f, "Anamorphic Movie")
}

enum class VideoFilter(val displayName: String, val tintColor: Long?) {
    ORIGINAL("Original", null),
    CYBERPUNK("Cyberpunk", 0xFF00E5FF),
    GOLDEN_HOUR("Golden Hour", 0xFFF59E0B),
    EMERALD_GLOW("Emerald", 0xFF10B981),
    FILM_NOIR("B&W Noir", 0xFF64748B),
    RETRO_VHS("Retro VHS", 0xFFEC4899),
    WARM_SUNSET("Sunset", 0xFFF97316),
    COLD_GLACIER("Glacier", 0xFF38BDF8)
}

enum class CanvasBgType {
    BLUR,
    BLACK,
    GRADIENT_CYBER,
    GRADIENT_SUNSET,
    DEEP_VIOLET
}

data class ColorAdjustments(
    val brightness: Float = 0f, // -1f to +1f
    val contrast: Float = 1f,   // 0.5f to 2f
    val saturation: Float = 1f, // 0f to 2f
    val warmth: Float = 0f,     // -1f to +1f
    val vignette: Float = 0f,   // 0f to 1f
    val sharpen: Float = 0f     // 0f to 1f
)

data class VideoProject(
    val id: String,
    val name: String,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
    val resolution: String = "1080p FHD",
    val fps: Int = 30,
    val videoClips: List<VideoClip> = emptyList(),
    val audioTracks: List<AudioClip> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val effectOverlays: List<EffectOverlay> = emptyList(),
    val stickers: List<StickerOverlay> = emptyList(),
    val filter: VideoFilter = VideoFilter.ORIGINAL,
    val filterIntensity: Float = 0.8f,
    val adjustments: ColorAdjustments = ColorAdjustments(),
    val canvasBgType: CanvasBgType = CanvasBgType.BLUR,
    val lastModified: Long = System.currentTimeMillis()
) {
    val totalDurationMs: Long
        get() {
            val videoDuration = videoClips.sumOf { it.effectiveDurationMs }
            val audioDuration = audioTracks.maxOfOrNull { it.startAtProjectMs + it.durationMs } ?: 0L
            val textDuration = textOverlays.maxOfOrNull { it.endMs } ?: 0L
            val effectDuration = effectOverlays.maxOfOrNull { it.endMs } ?: 0L
            return maxOf(videoDuration, audioDuration, textDuration, effectDuration).coerceAtLeast(1000L)
        }
}
