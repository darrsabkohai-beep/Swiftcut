package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.VideoProject
import com.example.ui.theme.*
import com.example.viewmodel.ExportState

@Composable
fun ExportDialog(
    project: VideoProject,
    exportState: ExportState,
    onStartExport: (resolution: String, fps: Int) -> Unit,
    onCancelExport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedResolution by remember { mutableStateOf("1080p FHD") }
    var selectedFps by remember { mutableIntStateOf(30) }

    Dialog(onDismissRequest = {
        if (exportState !is ExportState.Rendering) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("export_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (exportState) {
                    ExportState.Idle -> {
                        // Configuration Step
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Export Video",
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resolution selection
                        Text(
                            "Resolution",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("720p HD", "1080p FHD", "4K UHD").forEach { res ->
                                FilterChip(
                                    selected = selectedResolution == res,
                                    onClick = { selectedResolution = res },
                                    label = { Text(res, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = ObsidianBlack,
                                        containerColor = DarkCardElevated,
                                        labelColor = TextPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Frame rate selection
                        Text(
                            "Frame Rate (FPS)",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(24 to "24 (Cinema)", 30 to "30 (Standard)", 60 to "60 (Smooth)").forEach { (fpsVal, label) ->
                                FilterChip(
                                    selected = selectedFps == fpsVal,
                                    onClick = { selectedFps = fpsVal },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = ObsidianBlack,
                                        containerColor = DarkCardElevated,
                                        labelColor = TextPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Estimated size calculation
                        Surface(
                            color = DarkCardElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Estimated File Size", color = TextSecondary, fontSize = 11.sp)
                                    val sizeMb = (project.totalDurationMs / 1000f * 0.15f * if (selectedResolution.contains("4K")) 2.5f else 1.0f)
                                    Text(String.format("%.1f MB", sizeMb), color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("Format: MP4 (H.264)", color = TextTertiary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onStartExport(selectedResolution, selectedFps) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("confirm_export_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export & Render", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    is ExportState.Rendering -> {
                        // Rendering Progress Step
                        Text(
                            "Exporting Video...",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Keep CapCut open while we encode your high quality video",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // Circular progress with percentage inside
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { exportState.progress },
                                modifier = Modifier.size(100.dp),
                                color = NeonCyan,
                                strokeWidth = 8.dp,
                                trackColor = DarkCardElevated
                            )
                            Text(
                                text = "${(exportState.progress * 100).toInt()}%",
                                color = PureWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Frame ${exportState.currentFrame} / ${exportState.totalFrames} • ${exportState.resolution}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = onCancelExport,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = RadiantRed)
                        }
                    }

                    is ExportState.Finished -> {
                        // Export Done Step
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(ElectricMint.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ElectricMint,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Ready to Share!",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Exported in ${exportState.resolution} • ${exportState.fileSizeMb}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                        )

                        // Share button
                        Button(
                            onClick = {
                                shareExportedVideo(context, project.name)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("share_video_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Video", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = PureWhite)
                        }
                    }
                }
            }
        }
    }
}

private fun shareExportedVideo(context: Context, projectName: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out my edit: $projectName")
        putExtra(Intent.EXTRA_TEXT, "I just made a video edit '$projectName' in CapCut Studio!")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Video via"))
}
