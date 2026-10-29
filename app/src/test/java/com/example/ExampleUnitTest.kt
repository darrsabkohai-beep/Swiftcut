package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun videoClip_effectiveDuration_calculatesCorrectlyWithSpeed() {
        val clip = VideoClip(
            id = "clip_1",
            title = "Test Clip",
            sourceDurationMs = 6000L,
            startTrimMs = 1000L,
            endTrimMs = 5000L, // Trimmed span = 4000L
            speed = 2.0f
        )
        // 4000L / 2.0f = 2000L
        assertEquals(2000L, clip.effectiveDurationMs)
    }

    @Test
    fun videoClip_slowMotion_extendsEffectiveDuration() {
        val clip = VideoClip(
            id = "clip_2",
            title = "Slow Mo Clip",
            sourceDurationMs = 5000L,
            startTrimMs = 0L,
            endTrimMs = 3000L,
            speed = 0.5f
        )
        // 3000L / 0.5f = 6000L
        assertEquals(6000L, clip.effectiveDurationMs)
    }

    @Test
    fun videoProject_computesTotalDurationAccurately() {
        val project = VideoProject(
            id = "p1",
            name = "Test Project",
            videoClips = listOf(
                VideoClip("c1", "Part 1", sourceDurationMs = 3000L, startTrimMs = 0L, endTrimMs = 3000L),
                VideoClip("c2", "Part 2", sourceDurationMs = 4000L, startTrimMs = 0L, endTrimMs = 4000L)
            ),
            audioTracks = listOf(
                AudioClip("a1", "Beat", startAtProjectMs = 0L, durationMs = 8000L)
            )
        )
        // Max between video (3000+4000 = 7000) and audio (8000) is 8000
        assertEquals(8000L, project.totalDurationMs)
    }
}
