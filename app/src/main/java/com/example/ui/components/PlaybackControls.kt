package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PlaybackControls(
    currentPositionMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onTogglePlay: () -> Unit,
    onStepFrame: (Boolean) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(DarkCard)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Timecode display
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTimecode(currentPositionMs),
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " / ${formatTimecode(totalDurationMs)}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Center Playback and Nudge controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nudge backward 1 frame
            IconButton(
                onClick = { onStepFrame(false) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("step_backward_button")
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Step Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Big Play / Pause button
            FilledIconButton(
                onClick = onTogglePlay,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = NeonCyan,
                    contentColor = ObsidianBlack
                ),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Nudge forward 1 frame
            IconButton(
                onClick = { onStepFrame(true) },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("step_forward_button")
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Step Forward",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Action tools: Split at playhead + Undo / Redo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Split button
            Button(
                onClick = onSplitAtPlayhead,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCardElevated,
                    contentColor = PureWhite
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("split_button")
            ) {
                Icon(
                    Icons.Default.ContentCut,
                    contentDescription = "Split",
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Split", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            // Undo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("undo_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) PureWhite else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Redo
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("redo_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) PureWhite else TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    val f = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", m, s, f)
}
