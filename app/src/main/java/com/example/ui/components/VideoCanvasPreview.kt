package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun VideoCanvasPreview(
    project: VideoProject,
    currentPositionMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSelectText: (String) -> Unit,
    onSelectSticker: (String) -> Unit,
    onMoveSticker: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Find active clip at current playhead position
    var runningDuration = 0L
    var activeClip: VideoClip? = null
    var activeClipLocalMs = 0L
    var transitionProgress = 0f
    var activeTransition: TransitionType = TransitionType.NONE

    for (clip in project.videoClips) {
        val clipDur = clip.effectiveDurationMs
        if (currentPositionMs >= runningDuration && currentPositionMs < runningDuration + clipDur) {
            activeClip = clip
            activeClipLocalMs = currentPositionMs - runningDuration

            // Transition handling at start of clip
            if (activeClipLocalMs < clip.transitionDurationMs && clip.transitionIn != TransitionType.NONE) {
                transitionProgress = 1f - (activeClipLocalMs.toFloat() / clip.transitionDurationMs)
                activeTransition = clip.transitionIn
            }
            break
        }
        runningDuration += clipDur
    }

    if (activeClip == null && project.videoClips.isNotEmpty()) {
        activeClip = project.videoClips.last()
        activeClipLocalMs = activeClip.effectiveDurationMs
    }

    // Active effects at current timecode
    val activeEffects = project.effectOverlays.filter {
        currentPositionMs >= it.startMs && currentPositionMs <= it.endMs
    }

    // Active text overlays
    val visibleTexts = project.textOverlays.filter {
        currentPositionMs >= it.startMs && currentPositionMs <= it.endMs
    }

    // Active stickers
    val visibleStickers = project.stickers.filter {
        currentPositionMs >= it.startMs && currentPositionMs <= it.endMs
    }

    // Background styling behind canvas
    val bgBrush = when (project.canvasBgType) {
        CanvasBgType.BLUR -> Brush.radialGradient(
            colors = listOf(Color(0xFF1F222E), Color(0xFF0C0D10)),
            radius = 800f
        )
        CanvasBgType.BLACK -> Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF000000)))
        CanvasBgType.GRADIENT_CYBER -> Brush.linearGradient(
            listOf(Color(0xFF0F172A), Color(0xFF003049), Color(0xFF00E5FF).copy(alpha = 0.2f))
        )
        CanvasBgType.GRADIENT_SUNSET -> Brush.linearGradient(
            listOf(Color(0xFF2D120B), Color(0xFF4C1D95), Color(0xFF0C0D10))
        )
        CanvasBgType.DEEP_VIOLET -> Brush.linearGradient(
            listOf(Color(0xFF1E1035), Color(0xFF0C0D10))
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .clickable(onClick = onTogglePlay)
            .testTag("video_preview_canvas"),
        contentAlignment = Alignment.Center
    ) {
        // Aspect ratio container
        val ratio = project.aspectRatio
        val targetAspect = ratio.ratioWidth / ratio.ratioHeight

        Box(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxHeight()
                .aspectRatio(targetAspect, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Render video content: either local image/video URI or procedural cinematic generator
            if (activeClip?.uri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(activeClip.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = activeClip.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Procedural cinematic canvas rendering
                val theme = activeClip?.stockTheme ?: StockTheme.CYBERPUNK
                ProceduralCinematicRenderer(
                    theme = theme,
                    timeMs = activeClipLocalMs,
                    filter = project.filter,
                    filterIntensity = project.filterIntensity,
                    adjustments = project.adjustments,
                    isMirrored = activeClip?.isMirrored ?: false,
                    rotation = activeClip?.rotation ?: 0f,
                    activeEffects = activeEffects,
                    transitionType = activeTransition,
                    transitionProgress = transitionProgress,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Visible Stickers overlay
            visibleStickers.forEach { sticker ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(sticker.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = sticker.positionX + (dragAmount.x / size.width.toFloat())
                                val newY = sticker.positionY + (dragAmount.y / size.height.toFloat())
                                onMoveSticker(sticker.id, newX, newY)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = (sticker.positionX * 300).dp,
                                y = (sticker.positionY * 500).dp
                            )
                            .clickable { onSelectSticker(sticker.id) }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = sticker.symbol,
                            fontSize = (32 * sticker.scale).sp,
                            modifier = Modifier.shadow(4.dp, CircleShape)
                        )
                    }
                }
            }

            // Visible Text Subtitles / Titles overlay
            visibleTexts.forEach { textOverlay ->
                val elapsedInText = currentPositionMs - textOverlay.startMs
                val animProgress = (elapsedInText / 400f).coerceIn(0f, 1f)

                // Animation styling
                val displayText = if (textOverlay.animationType == TextAnimation.TYPEWRITER) {
                    val charCount = ((elapsedInText / 80f).toInt()).coerceIn(0, textOverlay.text.length)
                    textOverlay.text.substring(0, charCount)
                } else {
                    textOverlay.text
                }

                val scaleAnim = when (textOverlay.animationType) {
                    TextAnimation.POP_IN -> 0.7f + 0.3f * animProgress
                    TextAnimation.BOUNCE -> 1.0f + 0.2f * sin(elapsedInText / 100f).toFloat().coerceIn(-0.2f, 0.2f)
                    else -> 1.0f
                }

                val alphaAnim = if (textOverlay.animationType == TextAnimation.NEON_FLICKER) {
                    0.7f + 0.3f * sin(elapsedInText / 60f).toFloat().coerceIn(0f, 1f)
                } else 1.0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = androidx.compose.ui.BiasAlignment(
                        horizontalBias = (textOverlay.positionX - 0.5f) * 2f,
                        verticalBias = (textOverlay.positionY - 0.5f) * 2f
                    )
                ) {
                    Surface(
                        color = Color(textOverlay.backgroundColor ?: 0x00000000),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = textOverlay.scale * scaleAnim
                                scaleY = textOverlay.scale * scaleAnim
                                alpha = alphaAnim
                            }
                            .clickable { onSelectText(textOverlay.id) }
                    ) {
                        Text(
                            text = displayText,
                            color = Color(textOverlay.textColor),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (textOverlay.fontStyle.contains("Serif", ignoreCase = true)) FontFamily.Serif
                            else if (textOverlay.fontStyle.contains("Mono", ignoreCase = true)) FontFamily.Monospace
                            else FontFamily.SansSerif,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Aspect Ratio badge in top-left
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "${project.aspectRatio.displayName} • ${project.resolution}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            // CapCut Watermark toggle representation in top right (user can tap or keep)
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(NeonCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CAPCUT",
                        color = PureWhite,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Play/Pause center overlay when paused
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProceduralCinematicRenderer(
    theme: StockTheme,
    timeMs: Long,
    filter: VideoFilter,
    filterIntensity: Float,
    adjustments: ColorAdjustments,
    isMirrored: Boolean,
    rotation: Float,
    activeEffects: List<EffectOverlay>,
    transitionType: TransitionType,
    transitionProgress: Float,
    modifier: Modifier = Modifier
) {
    val sec = timeMs / 1000f

    Canvas(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
                if (isMirrored) scaleX = -1f
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw procedural scene based on theme
        when (theme) {
            StockTheme.CYBERPUNK -> {
                drawCyberpunkScene(w, h, sec)
            }
            StockTheme.NEON_DRIFT -> {
                drawNeonDriftScene(w, h, sec)
            }
            StockTheme.SUNSET_WAVES -> {
                drawSunsetWavesScene(w, h, sec)
            }
            StockTheme.RETRO_FILM -> {
                drawRetroFilmScene(w, h, sec)
            }
            StockTheme.AESTHETIC_CAFE -> {
                drawAestheticCafeScene(w, h, sec)
            }
            StockTheme.MOUNTAIN_ROADTRIP -> {
                drawMountainRoadtripScene(w, h, sec)
            }
        }

        // 2. Apply active video filter tint
        if (filter != VideoFilter.ORIGINAL && filter.tintColor != null) {
            val filterColor = Color(filter.tintColor).copy(alpha = 0.25f * filterIntensity)
            drawRect(color = filterColor, size = size, blendMode = BlendMode.Overlay)
        }

        // 3. Apply adjustments
        // Brightness & Contrast
        if (adjustments.brightness != 0f) {
            val bColor = if (adjustments.brightness > 0) Color.White else Color.Black
            drawRect(color = bColor.copy(alpha = abs(adjustments.brightness) * 0.35f), size = size)
        }
        // Vignette
        if (adjustments.vignette > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = adjustments.vignette * 0.8f)),
                    center = center,
                    radius = max(w, h) * 0.65f
                ),
                size = size
            )
        }

        // 4. Render Active Visual Effects
        for (effect in activeEffects) {
            when (effect.effectType) {
                EffectType.GLITCH_RGB -> {
                    // RGB chromatic aberration displacement
                    val shift = (sin(sec * 40f) * 16f * effect.intensity)
                    drawRect(
                        color = Color.Cyan.copy(alpha = 0.25f * effect.intensity),
                        topLeft = Offset(shift, 0f),
                        size = size,
                        blendMode = BlendMode.Screen
                    )
                    drawRect(
                        color = Color.Magenta.copy(alpha = 0.25f * effect.intensity),
                        topLeft = Offset(-shift, 0f),
                        size = size,
                        blendMode = BlendMode.Screen
                    )
                }
                EffectType.RETRO_VHS -> {
                    // Scanline stripes
                    val scanGap = 8f
                    var y = 0f
                    while (y < h) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.35f * effect.intensity),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 2f
                        )
                        y += scanGap
                    }
                }
                EffectType.FILM_BURN -> {
                    val flareX = (sin(sec * 3f) * 0.5f + 0.5f) * w
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF9E00).copy(alpha = 0.45f * effect.intensity), Color.Transparent),
                            center = Offset(flareX, h * 0.2f),
                            radius = w * 0.5f
                        ),
                        radius = w * 0.5f,
                        center = Offset(flareX, h * 0.2f)
                    )
                }
                EffectType.SPARKLES -> {
                    for (i in 0..12) {
                        val px = ((sin(sec * 2f + i) * 0.5f + 0.5f) * w)
                        val py = ((cos(sec * 3f + i * 2) * 0.5f + 0.5f) * h)
                        drawCircle(
                            color = Color.White.copy(alpha = (sin(sec * 6f + i) * 0.5f + 0.5f) * effect.intensity),
                            radius = (4f + (i % 3) * 3f),
                            center = Offset(px, py)
                        )
                    }
                }
                EffectType.VIGNETTE_PULSE -> {
                    val pulse = (sin(sec * 8f) * 0.2f + 0.8f) * effect.intensity
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = pulse * 0.85f)),
                            radius = max(w, h) * 0.6f
                        ),
                        size = size
                    )
                }
                EffectType.ZOOM_BLUR -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f * effect.intensity),
                        radius = (w * 0.4f * (sin(sec * 10f) * 0.2f + 0.8f)),
                        center = center,
                        style = Stroke(width = 8f)
                    )
                }
            }
        }

        // 5. Render Transition In (if active)
        if (transitionProgress > 0f) {
            when (transitionType) {
                TransitionType.FLASH_WHITE -> {
                    drawRect(color = Color.White.copy(alpha = transitionProgress), size = size)
                }
                TransitionType.FADE_BLACK -> {
                    drawRect(color = Color.Black.copy(alpha = transitionProgress), size = size)
                }
                TransitionType.DISSOLVE -> {
                    drawRect(color = Color(0xFF14151B).copy(alpha = transitionProgress * 0.7f), size = size)
                }
                TransitionType.GLITCH -> {
                    val offsetNoise = sin(transitionProgress * 50f) * 40f
                    drawRect(
                        color = Color(0xFF00E5FF).copy(alpha = transitionProgress * 0.5f),
                        topLeft = Offset(offsetNoise, 0f),
                        size = size
                    )
                }
                else -> {}
            }
        }
    }
}

// Procedural visual scene renderers
private fun DrawScope.drawCyberpunkScene(w: Float, h: Float, sec: Float) {
    // Gradient sky
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F051D), Color(0xFF280B45), Color(0xFF004358)),
            startY = 0f,
            endY = h
        ),
        size = size
    )

    // Glowing synthwave sun
    val sunY = h * 0.42f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF007A), Color(0xFFFF8A00), Color.Transparent),
            center = Offset(w * 0.5f, sunY),
            radius = w * 0.35f
        ),
        radius = w * 0.35f,
        center = Offset(w * 0.5f, sunY)
    )

    // Skyline silhouettes
    val buildingCount = 8
    val bWidth = w / buildingCount
    for (i in 0 until buildingCount) {
        val bH = ((sin(i * 1.7f) * 0.3f + 0.4f) * h * 0.4f)
        drawRect(
            color = Color(0xFF0A0512),
            topLeft = Offset(i * bWidth, h * 0.6f - bH),
            size = Size(bWidth - 2f, bH + h * 0.4f)
        )
        // Neon windows
        for (wRow in 0..4) {
            val winY = h * 0.6f - bH + 20f + wRow * 25f
            if (winY < h * 0.58f) {
                drawRect(
                    color = if ((i + wRow) % 2 == 0) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFFFF007A).copy(alpha = 0.6f),
                    topLeft = Offset(i * bWidth + 6f, winY),
                    size = Size(6f, 6f)
                )
            }
        }
    }

    // Perspective wireframe grid at bottom
    val gridTop = h * 0.6f
    drawRect(color = Color(0xFF05020A), topLeft = Offset(0f, gridTop), size = Size(w, h - gridTop))
    for (gx in 0..10) {
        val xNorm = gx / 10f
        val startX = w * 0.5f + (xNorm - 0.5f) * w * 0.1f
        val endX = w * 0.5f + (xNorm - 0.5f) * w * 1.8f
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.4f),
            start = Offset(startX, gridTop),
            end = Offset(endX, h),
            strokeWidth = 1.5f
        )
    }

    // Moving horizontal grid lines
    val gridOffset = (sec * 60f) % 40f
    var gy = gridTop
    var step = 10f
    while (gy < h) {
        drawLine(
            color = Color(0xFFEC4899).copy(alpha = (gy - gridTop) / (h - gridTop) * 0.6f),
            start = Offset(0f, gy),
            end = Offset(w, gy),
            strokeWidth = 1.5f
        )
        step *= 1.3f
        gy += step + (gridOffset * 0.1f)
    }
}

private fun DrawScope.drawNeonDriftScene(w: Float, h: Float, sec: Float) {
    // Night highway road with glowing light trails
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF080B11), Color(0xFF10192A), Color(0xFF0B101D)),
            startY = 0f,
            endY = h
        ),
        size = size
    )

    // Horizon line
    val horizonY = h * 0.45f
    drawLine(
        color = Color(0xFF00E5FF).copy(alpha = 0.5f),
        start = Offset(0f, horizonY),
        end = Offset(w, horizonY),
        strokeWidth = 2f
    )

    // Vanishing road lines
    val roadLeft = w * 0.1f
    val roadRight = w * 0.9f
    drawLine(
        color = Color(0xFF00E5FF).copy(alpha = 0.7f),
        start = Offset(w * 0.5f, horizonY),
        end = Offset(roadLeft, h),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF00E5FF).copy(alpha = 0.7f),
        start = Offset(w * 0.5f, horizonY),
        end = Offset(roadRight, h),
        strokeWidth = 3f
    )

    // Fast moving dashed center line
    val dashOffset = (sec * 120f) % 60f
    var ly = horizonY + dashOffset
    while (ly < h) {
        val prog = (ly - horizonY) / (h - horizonY)
        drawLine(
            color = Color(0xFFF59E0B).copy(alpha = prog * 0.9f),
            start = Offset(w * 0.5f, ly),
            end = Offset(w * 0.5f, ly + 25f * prog),
            strokeWidth = 4f * prog
        )
        ly += 50f * (prog + 0.3f)
    }

    // Kinetic light trails (red tail lights & cyan headlights)
    val redX = w * 0.55f + sin(sec * 4f) * 40f
    drawCircle(
        color = Color(0xFFEF4444).copy(alpha = 0.9f),
        radius = 12f,
        center = Offset(redX, h * 0.75f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFEF4444).copy(alpha = 0.5f), Color.Transparent),
            center = Offset(redX, h * 0.75f),
            radius = 60f
        ),
        radius = 60f,
        center = Offset(redX, h * 0.75f)
    )
}

private fun DrawScope.drawSunsetWavesScene(w: Float, h: Float, sec: Float) {
    // Warm sunset gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E0E38), Color(0xFF8B2635), Color(0xFFE86A33), Color(0xFFFEE180)),
            startY = 0f,
            endY = h * 0.55f
        ),
        size = Size(w, h * 0.55f)
    )

    // Sun setting
    drawCircle(
        color = Color(0xFFFFF275),
        radius = w * 0.16f,
        center = Offset(w * 0.5f, h * 0.42f)
    )

    // Ocean water
    val waterY = h * 0.55f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE86A33).copy(alpha = 0.8f), Color(0xFF0F3057), Color(0xFF001B3A)),
            startY = waterY,
            endY = h
        ),
        topLeft = Offset(0f, waterY),
        size = Size(w, h - waterY)
    )

    // Animated water reflection waves
    for (i in 0..10) {
        val waveY = waterY + 20f + i * 28f
        val waveOffset = sin(sec * 3f + i * 0.8f) * 20f
        val waveWidth = (w * 0.3f + i * 25f)
        drawLine(
            color = Color(0xFFFFF275).copy(alpha = (1f - (i / 11f)) * 0.7f),
            start = Offset(w * 0.5f - waveWidth * 0.5f + waveOffset, waveY),
            end = Offset(w * 0.5f + waveWidth * 0.5f + waveOffset, waveY),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawRetroFilmScene(w: Float, h: Float, sec: Float) {
    // Sepia / 90s vintage film tape
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF281E15), Color(0xFF4A3728), Color(0xFF1F1710)),
            startY = 0f,
            endY = h
        ),
        size = size
    )

    // Film sprocket holes on sides
    val holeSize = 16f
    var hy = 10f
    while (hy < h) {
        drawRect(color = Color.Black, topLeft = Offset(10f, hy), size = Size(holeSize, holeSize))
        drawRect(color = Color.Black, topLeft = Offset(w - 10f - holeSize, hy), size = Size(holeSize, holeSize))
        hy += holeSize + 14f
    }

    // Moving grain noise scratch line
    val scratchX = (sec * 180f) % w
    drawLine(
        color = Color.White.copy(alpha = 0.3f),
        start = Offset(scratchX, 0f),
        end = Offset(scratchX + 4f, h),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawAestheticCafeScene(w: Float, h: Float, sec: Float) {
    // Warm coffee brew aesthetics
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF261C14), Color(0xFF3D2C1E), Color(0xFF5A422D)),
            startY = 0f,
            endY = h
        ),
        size = size
    )

    // Coffee cup silhouette
    val cupY = h * 0.55f
    drawCircle(
        color = Color(0xFF160E08),
        radius = w * 0.22f,
        center = Offset(w * 0.5f, cupY)
    )
    drawCircle(
        color = Color(0xFF8B5A2B),
        radius = w * 0.18f,
        center = Offset(w * 0.5f, cupY)
    )

    // Animated steam rising
    for (s in 0..2) {
        val steamX = w * 0.45f + s * 25f + sin(sec * 2f + s) * 12f
        val steamY = cupY - 40f - (sec * 30f + s * 40f) % 120f
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = 8f + s * 3f,
            center = Offset(steamX, steamY)
        )
    }
}

private fun DrawScope.drawMountainRoadtripScene(w: Float, h: Float, sec: Float) {
    // Alpine mountain dawn
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1B2A4A), Color(0xFF465E87), Color(0xFFD68060)),
            startY = 0f,
            endY = h * 0.6f
        ),
        size = Size(w, h * 0.6f)
    )

    // Mountain triangles
    val mBaseY = h * 0.6f
    val path1 = Path().apply {
        moveTo(w * 0.1f, mBaseY)
        lineTo(w * 0.45f, mBaseY - h * 0.32f)
        lineTo(w * 0.8f, mBaseY)
        close()
    }
    drawPath(path1, color = Color(0xFF1E2838))

    val path2 = Path().apply {
        moveTo(w * 0.4f, mBaseY)
        lineTo(w * 0.75f, mBaseY - h * 0.25f)
        lineTo(w * 1.1f, mBaseY)
        close()
    }
    drawPath(path2, color = Color(0xFF141C28))

    // Snow caps
    val snow1 = Path().apply {
        moveTo(w * 0.38f, mBaseY - h * 0.25f)
        lineTo(w * 0.45f, mBaseY - h * 0.32f)
        lineTo(w * 0.52f, mBaseY - h * 0.25f)
        close()
    }
    drawPath(snow1, color = Color.White.copy(alpha = 0.85f))

    // Road at bottom
    drawRect(color = Color(0xFF0F1520), topLeft = Offset(0f, mBaseY), size = Size(w, h - mBaseY))
}
