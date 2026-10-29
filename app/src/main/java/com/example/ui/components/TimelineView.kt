package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TimelineView(
    project: VideoProject,
    currentPositionMs: Long,
    selectedClipId: String?,
    selectedTextId: String?,
    onSeekTo: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onSelectEffect: (String) -> Unit,
    onAddClipClick: () -> Unit,
    onAddAudioClick: () -> Unit,
    onAddTextClick: () -> Unit,
    onOpenTransitions: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Scaling: 1000ms = 70.dp
    val pxPerSec = 70.dp
    val totalDurationMs = project.totalDurationMs.coerceAtLeast(3000L)
    val totalWidth = (totalDurationMs / 1000f) * 70
    val scrollState = rememberScrollState()

    // Sync scroll with playhead position during playback
    LaunchedEffect(currentPositionMs) {
        val targetPx = ((currentPositionMs / 1000f) * 70 * 2.5f).roundToInt()
        // Keep scrubber comfortably in view
        if (targetPx > scrollState.value + 600 || targetPx < scrollState.value - 100) {
            scrollState.animateScrollTo(targetPx.coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(1.dp, BorderSubtle)
            .testTag("timeline_view")
    ) {
        // Timeline Header: Time ruler with second marks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(DarkCard)
                .border(width = 0.5.dp, color = BorderSubtle)
        ) {
            // Horizontal ruler markers
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalSeconds = (totalDurationMs / 1000).toInt() + 2
                for (sec in 0..totalSeconds) {
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = formatSecondsToRuler(sec),
                                color = TextTertiary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(6.dp)
                                    .background(BorderSubtle)
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Multi-Track Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(ObsidianBlack)
        ) {
            // Scrollable tracks
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val clickedSec = offset.x / 70f / 2.5f // scale factor
                            val clickedMs = (clickedSec * 1000).toLong()
                            onSeekTo(clickedMs)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. MAIN VIDEO TRACK
                Row(
                    modifier = Modifier
                        .height(68.dp)
                        .testTag("track_video"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Track header label
                    Surface(
                        color = VideoTrackBg,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, VideoTrackColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Movie,
                                contentDescription = "Video",
                                tint = VideoTrackColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("VIDEO", color = VideoTrackColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Video clips list
                    project.videoClips.forEachIndexed { index, clip ->
                        val isSelected = clip.id == selectedClipId
                        val clipDurSec = clip.effectiveDurationMs / 1000f
                        val clipWidth = (clipDurSec * 70).coerceAtLeast(50f).dp

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Clip Box
                            Box(
                                modifier = Modifier
                                    .width(clipWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) VideoTrackColor.copy(alpha = 0.25f)
                                        else DarkCardElevated
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else BorderSubtle,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectClip(clip.id) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = clip.title,
                                            color = PureWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        if (clip.speed != 1.0f) {
                                            Surface(
                                                color = BrightAmber.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(2.dp)
                                            ) {
                                                Text(
                                                    text = "${clip.speed}x",
                                                    color = BrightAmber,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format("%.1fs", clipDurSec),
                                            color = TextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        // Mini simulated filmstrip frames
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(3) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 10.dp, height = 8.dp)
                                                        .background(VideoTrackColor.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                                                )
                                            }
                                        }
                                    }
                                }

                                // Yellow trim handle bars on selection
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .width(4.dp)
                                            .background(NeonCyan, RoundedCornerShape(2.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(4.dp)
                                            .background(NeonCyan, RoundedCornerShape(2.dp))
                                    )
                                }
                            }

                            // Transition chip between clips
                            if (index < project.videoClips.size - 1) {
                                val nextClip = project.videoClips[index + 1]
                                Surface(
                                    color = if (nextClip.transitionIn != TransitionType.NONE) NeonCyanDark else DarkCard,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, if (nextClip.transitionIn != TransitionType.NONE) NeonCyan else BorderSubtle),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { onOpenTransitions(nextClip.id) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Bolt,
                                            contentDescription = "Transition",
                                            tint = if (nextClip.transitionIn != TransitionType.NONE) ObsidianBlack else TextSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add clip button at end of video track
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .size(width = 44.dp, height = 68.dp)
                            .clickable(onClick = onAddClipClick)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add Media", tint = PureWhite, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // 2. AUDIO TRACK
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("track_audio"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Track label
                    Surface(
                        color = AudioTrackBg,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, AudioTrackColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = "Audio", tint = AudioTrackColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("BGM", color = AudioTrackColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Audio clips
                    project.audioTracks.forEach { audio ->
                        val audioSec = audio.durationMs / 1000f
                        val audioWidth = (audioSec * 70).coerceAtLeast(60f).dp

                        Surface(
                            color = AudioTrackBg,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, AudioTrackColor.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .width(audioWidth)
                                .fillMaxHeight()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = audio.name,
                                    color = ElectricMint,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                // Simulated audio waveform bars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    audio.waveformPoints.take(8).forEach { heightRatio ->
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height((18 * heightRatio).dp)
                                                .background(ElectricMint, RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Add Audio button
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .height(34.dp)
                            .padding(horizontal = 8.dp)
                            .clickable(onClick = onAddAudioClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Audio", tint = ElectricMint, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Audio", color = ElectricMint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 3. TEXT & SUBTITLES TRACK
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("track_text"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Track label
                    Surface(
                        color = TextTrackBg,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, TextTrackColor.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Title, contentDescription = "Text", tint = TextTrackColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("TEXT", color = TextTrackColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Text overlays
                    project.textOverlays.forEach { textOverlay ->
                        val textDurSec = (textOverlay.endMs - textOverlay.startMs) / 1000f
                        val textWidth = (textDurSec * 70).coerceAtLeast(50f).dp
                        val isSelected = textOverlay.id == selectedTextId

                        Surface(
                            color = if (isSelected) TextTrackColor.copy(alpha = 0.35f) else TextTrackBg,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) BrightAmber else TextTrackColor.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .width(textWidth)
                                .fillMaxHeight()
                                .clickable { onSelectText(textOverlay.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = textOverlay.text,
                                    color = BrightAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Add text button
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .height(30.dp)
                            .padding(horizontal = 8.dp)
                            .clickable(onClick = onAddTextClick)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Text", tint = BrightAmber, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Text", color = BrightAmber, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 4. EFFECTS & FILTERS TRACK
                if (project.effectOverlays.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("track_effects"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = EffectTrackBg,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, EffectTrackColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .width(52.dp)
                                .fillMaxHeight()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Effects", tint = EffectTrackColor, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("FX", color = EffectTrackColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        project.effectOverlays.forEach { effect ->
                            val effDurSec = (effect.endMs - effect.startMs) / 1000f
                            val effWidth = (effDurSec * 70).coerceAtLeast(40f).dp

                            Surface(
                                color = EffectTrackBg,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, EffectTrackColor.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .width(effWidth)
                                    .fillMaxHeight()
                                    .clickable { onSelectEffect(effect.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = effect.effectType.displayName,
                                        color = ElectricPink,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Central / Scrubbing Playhead Needle (Fixed vertical line)
            // Visual indicator with diamond head
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.Center)
                    .background(PureWhite)
            )
            // Diamond playhead indicator on top
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopCenter)
                    .background(PureWhite, CircleShape)
                    .border(2.dp, NeonCyan, CircleShape)
            )
        }
    }
}

private fun formatSecondsToRuler(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}
