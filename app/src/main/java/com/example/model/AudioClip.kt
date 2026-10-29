package com.example.model

enum class AudioType {
    BGM,
    SOUND_EFFECT,
    VOICEOVER
}

data class AudioClip(
    val id: String,
    val name: String,
    val artist: String = "CapCut Beats",
    val type: AudioType = AudioType.BGM,
    val startAtProjectMs: Long = 0L,
    val durationMs: Long = 5000L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val waveformPoints: List<Float> = listOf(
        0.3f, 0.5f, 0.8f, 0.6f, 0.9f, 0.4f, 0.7f, 1.0f, 0.5f, 0.8f, 0.4f, 0.6f, 0.7f, 0.9f, 0.5f, 0.3f
    )
)
