package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project by viewModel.currentProject.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val activeToolPanel by viewModel.activeToolPanel.collectAsState()
    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val selectedTextId by viewModel.selectedTextId.collectAsState()
    val selectedStickerId by viewModel.selectedStickerId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    var showExportModal by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back button: closes active panel or returns to Home
    BackHandler {
        if (activeToolPanel != ActiveToolPanel.None) {
            viewModel.closeToolPanel()
        } else {
            viewModel.closeProject()
            onBack()
        }
    }

    val currentProj = project
    if (currentProj == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("editor_screen"),
        containerColor = ObsidianBlack,
        topBar = {
            EditorTopBar(
                projectName = currentProj.name,
                aspectRatio = currentProj.aspectRatio,
                resolution = currentProj.resolution,
                onBack = {
                    viewModel.closeProject()
                    onBack()
                },
                onRenameClick = { showRenameDialog = true },
                onExportClick = { showExportModal = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. VIDEO PREVIEW VIEWPORT (Top 42% of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                VideoCanvasPreview(
                    project = currentProj,
                    currentPositionMs = currentPositionMs,
                    isPlaying = isPlaying,
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onSelectText = { viewModel.selectText(it) },
                    onSelectSticker = { viewModel.selectSticker(it) },
                    onMoveSticker = { id, x, y -> viewModel.updateStickerPosition(id, x, y) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. PLAYBACK & SCRUBBER CONTROLS ROW
            PlaybackControls(
                currentPositionMs = currentPositionMs,
                totalDurationMs = currentProj.totalDurationMs,
                isPlaying = isPlaying,
                canUndo = canUndo,
                canRedo = canRedo,
                onTogglePlay = { viewModel.togglePlayPause() },
                onStepFrame = { viewModel.stepFrame(it) },
                onSplitAtPlayhead = { viewModel.splitAtPlayhead() },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() }
            )

            // 3. MULTI-TRACK TIMELINE
            TimelineView(
                project = currentProj,
                currentPositionMs = currentPositionMs,
                selectedClipId = selectedClipId,
                selectedTextId = selectedTextId,
                onSeekTo = { viewModel.seekTo(it) },
                onSelectClip = { viewModel.selectClip(it) },
                onSelectText = { viewModel.selectText(it) },
                onSelectEffect = { viewModel.openToolPanel(ActiveToolPanel.Effects) },
                onAddClipClick = { viewModel.openToolPanel(ActiveToolPanel.EditClip("")) },
                onAddAudioClick = { viewModel.openToolPanel(ActiveToolPanel.AudioLibrary) },
                onAddTextClick = { viewModel.openToolPanel(ActiveToolPanel.AddText) },
                onOpenTransitions = {
                    viewModel.selectClip(it)
                    viewModel.openToolPanel(ActiveToolPanel.Transitions)
                }
            )

            // 4. BOTTOM DOCK / CONTEXTUAL TOOL PANELS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (activeToolPanel != ActiveToolPanel.None) {
                    ContextualToolPanel(
                        activePanel = activeToolPanel,
                        project = currentProj,
                        selectedClipId = selectedClipId,
                        selectedTextId = selectedTextId,
                        onClosePanel = { viewModel.closeToolPanel() },
                        onSplitClip = { viewModel.splitAtPlayhead() },
                        onSetClipSpeed = { id, spd -> viewModel.setClipSpeed(id, spd) },
                        onRotateClip = { viewModel.rotateClip(it) },
                        onMirrorClip = { viewModel.mirrorClip(it) },
                        onDuplicateClip = { viewModel.duplicateClip(it) },
                        onDeleteClip = { viewModel.deleteClip(it) },
                        onSetTransition = { id, tr -> viewModel.setTransition(id, tr) },
                        onAddAudio = { viewModel.addAudioTrack(it) },
                        onAddText = { viewModel.addTextOverlay(it) },
                        onUpdateText = { viewModel.updateTextOverlay(it) },
                        onDeleteText = { viewModel.deleteTextOverlay(it) },
                        onGenerateCaptions = { viewModel.generateAutoCaptions() },
                        onAddEffect = { viewModel.addEffect(it) },
                        onAddSticker = { viewModel.addSticker(it) },
                        onSetFilter = { f, intensity -> viewModel.setFilter(f, intensity) },
                        onSetAdjustments = { viewModel.setAdjustments(it) },
                        onSetAspectRatio = { viewModel.setAspectRatio(it) },
                        onSetCanvasBg = { viewModel.setCanvasBgType(it) },
                        musicCatalog = viewModel.getMusicCatalog(),
                        sfxCatalog = viewModel.getSoundEffectsCatalog(),
                        stickerCatalog = viewModel.getStickerCatalog(),
                        currentPositionMs = currentPositionMs
                    )
                } else {
                    MainEditorToolbar(
                        onOpenPanel = { viewModel.openToolPanel(it) }
                    )
                }
            }
        }

        // Export Dialog
        if (showExportModal) {
            ExportDialog(
                project = currentProj,
                exportState = exportState,
                onStartExport = { res, fps -> viewModel.startExport(res, fps) },
                onCancelExport = { viewModel.cancelExport() },
                onDismiss = {
                    viewModel.cancelExport()
                    showExportModal = false
                }
            )
        }

        // Rename Dialog
        if (showRenameDialog) {
            var newName by remember { mutableStateOf(currentProj.name) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                containerColor = DarkCard,
                title = { Text("Rename Project", color = PureWhite) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateProjectMeta(newName.ifBlank { "Untitled" }, currentProj.resolution, currentProj.fps)
                            showRenameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    projectName: String,
    aspectRatio: AspectRatio,
    resolution: String,
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = projectName,
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${aspectRatio.displayName} • $resolution",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = onRenameClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Export button
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = ObsidianBlack
                ),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("export_button")
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Export",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
