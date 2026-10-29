package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt
import java.util.UUID

@Composable
fun ContextualToolPanel(
    activePanel: ActiveToolPanel,
    project: VideoProject,
    selectedClipId: String?,
    selectedTextId: String?,
    onClosePanel: () -> Unit,
    onSplitClip: () -> Unit,
    onSetClipSpeed: (String, Float) -> Unit,
    onRotateClip: (String) -> Unit,
    onMirrorClip: (String) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onSetTransition: (String, TransitionType) -> Unit,
    onAddAudio: (AudioClip) -> Unit,
    onAddText: (TextOverlay) -> Unit,
    onUpdateText: (TextOverlay) -> Unit,
    onDeleteText: (String) -> Unit,
    onGenerateCaptions: () -> Unit,
    onAddEffect: (EffectOverlay) -> Unit,
    onAddSticker: (StickerOverlay) -> Unit,
    onSetFilter: (VideoFilter, Float) -> Unit,
    onSetAdjustments: (ColorAdjustments) -> Unit,
    onSetAspectRatio: (AspectRatio) -> Unit,
    onSetCanvasBg: (CanvasBgType) -> Unit,
    musicCatalog: List<AudioClip>,
    sfxCatalog: List<AudioClip>,
    stickerCatalog: List<Pair<String, String>>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("contextual_tool_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header bar of panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = when (activePanel) {
                    is ActiveToolPanel.EditClip -> "Edit Clip"
                    ActiveToolPanel.SpeedControl -> "Speed Control"
                    ActiveToolPanel.AudioLibrary -> "Audio Library"
                    ActiveToolPanel.VolumeControl -> "Volume"
                    ActiveToolPanel.AddText -> "Add Text"
                    is ActiveToolPanel.EditText -> "Edit Text"
                    ActiveToolPanel.AutoCaptions -> "Auto Captions"
                    ActiveToolPanel.Filters -> "Filters & Color"
                    ActiveToolPanel.AdjustColor -> "Color Adjustments"
                    ActiveToolPanel.Effects -> "Video Effects"
                    ActiveToolPanel.Transitions -> "Transitions"
                    ActiveToolPanel.Stickers -> "Stickers"
                    ActiveToolPanel.CanvasSettings -> "Canvas & Ratio"
                    else -> "Tools"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(NeonCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onClosePanel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body depending on panel
            when (activePanel) {
                is ActiveToolPanel.EditClip -> {
                    val clip = project.videoClips.find { it.id == activePanel.clipId }
                        ?: project.videoClips.firstOrNull()

                    if (clip != null) {
                        EditClipPanel(
                            clip = clip,
                            onSplit = onSplitClip,
                            onSpeedChange = { onSetClipSpeed(clip.id, it) },
                            onRotate = { onRotateClip(clip.id) },
                            onMirror = { onMirrorClip(clip.id) },
                            onDuplicate = { onDuplicateClip(clip.id) },
                            onDelete = { onDeleteClip(clip.id) }
                        )
                    }
                }

                ActiveToolPanel.AudioLibrary -> {
                    AudioLibraryPanel(
                        musicCatalog = musicCatalog,
                        sfxCatalog = sfxCatalog,
                        currentPositionMs = currentPositionMs,
                        onAddAudio = onAddAudio
                    )
                }

                ActiveToolPanel.AddText, is ActiveToolPanel.EditText -> {
                    val existing = if (activePanel is ActiveToolPanel.EditText) {
                        project.textOverlays.find { it.id == activePanel.textId }
                    } else null

                    TextEditorPanel(
                        existing = existing,
                        currentPositionMs = currentPositionMs,
                        onSave = {
                            if (existing != null) onUpdateText(it) else onAddText(it)
                            onClosePanel()
                        },
                        onDelete = {
                            if (existing != null) onDeleteText(existing.id)
                            onClosePanel()
                        }
                    )
                }

                ActiveToolPanel.AutoCaptions -> {
                    AutoCaptionsPanel(
                        onGenerate = {
                            onGenerateCaptions()
                            onClosePanel()
                        }
                    )
                }

                ActiveToolPanel.Filters -> {
                    FiltersPanel(
                        currentFilter = project.filter,
                        currentIntensity = project.filterIntensity,
                        adjustments = project.adjustments,
                        onSetFilter = onSetFilter,
                        onSetAdjustments = onSetAdjustments
                    )
                }

                ActiveToolPanel.Effects -> {
                    EffectsPanel(
                        currentPositionMs = currentPositionMs,
                        onAddEffect = {
                            onAddEffect(it)
                            onClosePanel()
                        }
                    )
                }

                ActiveToolPanel.Transitions -> {
                    val targetClip = project.videoClips.find { it.id == selectedClipId }
                        ?: project.videoClips.getOrNull(1)

                    TransitionsPanel(
                        currentTransition = targetClip?.transitionIn ?: TransitionType.NONE,
                        onSelectTransition = { tr ->
                            targetClip?.let { onSetTransition(it.id, tr) }
                            onClosePanel()
                        }
                    )
                }

                ActiveToolPanel.Stickers -> {
                    StickersPanel(
                        stickerCatalog = stickerCatalog,
                        currentPositionMs = currentPositionMs,
                        onAddSticker = {
                            onAddSticker(it)
                            onClosePanel()
                        }
                    )
                }

                ActiveToolPanel.CanvasSettings -> {
                    CanvasSettingsPanel(
                        currentRatio = project.aspectRatio,
                        currentBg = project.canvasBgType,
                        onSetRatio = onSetAspectRatio,
                        onSetBg = onSetCanvasBg
                    )
                }

                else -> {}
            }
        }
    }
}

// 1. Bottom Main Tool Bar (Default horizontal scroll of tools)
@Composable
fun MainEditorToolbar(
    onOpenPanel: (ActiveToolPanel) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_editor_toolbar")
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                ToolButton(
                    icon = Icons.Default.ContentCut,
                    label = "Edit",
                    onClick = { onOpenPanel(ActiveToolPanel.EditClip("")) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.MusicNote,
                    label = "Audio",
                    color = ElectricMint,
                    onClick = { onOpenPanel(ActiveToolPanel.AudioLibrary) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.Title,
                    label = "Text",
                    color = BrightAmber,
                    onClick = { onOpenPanel(ActiveToolPanel.AddText) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.Subtitles,
                    label = "Auto Captions",
                    color = BrightAmber,
                    onClick = { onOpenPanel(ActiveToolPanel.AutoCaptions) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.ColorLens,
                    label = "Filters",
                    color = NeonCyan,
                    onClick = { onOpenPanel(ActiveToolPanel.Filters) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "Effects",
                    color = ElectricPink,
                    onClick = { onOpenPanel(ActiveToolPanel.Effects) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.Bolt,
                    label = "Transitions",
                    color = NeonCyan,
                    onClick = { onOpenPanel(ActiveToolPanel.Transitions) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.EmojiEmotions,
                    label = "Stickers",
                    color = BrightAmber,
                    onClick = { onOpenPanel(ActiveToolPanel.Stickers) }
                )
            }
            item {
                ToolButton(
                    icon = Icons.Default.AspectRatio,
                    label = "Canvas",
                    onClick = { onOpenPanel(ActiveToolPanel.CanvasSettings) }
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    color: Color = PureWhite,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("tool_btn_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(DarkCardElevated, CircleShape)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Subpanel: Edit Clip
@Composable
private fun EditClipPanel(
    clip: VideoClip,
    onSplit: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onRotate: () -> Unit,
    onMirror: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Speed Control
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Speed Multiplier", color = TextSecondary, fontSize = 12.sp)
                Text(
                    "${clip.speed}x ${if (clip.speed < 1f) "(Slow-Mo)" else if (clip.speed > 1f) "(Fast)" else "(Normal)"}",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Slider(
                value = clip.speed,
                onValueChange = { onSpeedChange((it * 10).roundToInt() / 10f) },
                valueRange = 0.2f..4.0f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = DarkCard
                )
            )

            // Speed chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { spd ->
                    FilterChip(
                        selected = clip.speed == spd,
                        onClick = { onSpeedChange(spd) },
                        label = { Text("${spd}x", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = ObsidianBlack,
                            containerColor = DarkCardElevated,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        }

        // Actions row (Rotate, Mirror, Duplicate, Split, Delete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onRotate,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = PureWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rotate 90°", fontSize = 11.sp, color = PureWhite)
            }

            OutlinedButton(
                onClick = onMirror,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(Icons.Default.Flip, contentDescription = "Mirror", tint = PureWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (clip.isMirrored) "Mirrored" else "Mirror", fontSize = 11.sp, color = PureWhite)
            }

            OutlinedButton(
                onClick = onDuplicate,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = PureWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Duplicate", fontSize = 11.sp, color = PureWhite)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSplit,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyanDark, contentColor = PureWhite),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = "Split", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Split Clip Here")
            }

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = RadiantRed.copy(alpha = 0.25f), contentColor = RadiantRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Clip")
            }
        }
    }
}

// Subpanel: Audio Library
@Composable
private fun AudioLibraryPanel(
    musicCatalog: List<AudioClip>,
    sfxCatalog: List<AudioClip>,
    currentPositionMs: Long,
    onAddAudio: (AudioClip) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCard,
            contentColor = ElectricMint
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Trending Music") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Sound Effects (SFX)") })
        }

        Spacer(modifier = Modifier.height(10.dp))

        val list = if (selectedTab == 0) musicCatalog else sfxCatalog

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            list.forEach { item ->
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AudioTrackBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.type == AudioType.BGM) Icons.Default.MusicNote else Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = ElectricMint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(item.name, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${item.artist} • ${(item.durationMs / 1000f)}s", color = TextSecondary, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                onAddAudio(
                                    item.copy(
                                        id = UUID.randomUUID().toString(),
                                        startAtProjectMs = currentPositionMs
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricMint, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Subpanel: Text Editor
@Composable
private fun TextEditorPanel(
    existing: TextOverlay?,
    currentPositionMs: Long,
    onSave: (TextOverlay) -> Unit,
    onDelete: () -> Unit
) {
    var textInput by remember { mutableStateOf(existing?.text ?: "Enter text here...") }
    var selectedColor by remember { mutableLongStateOf(existing?.textColor ?: 0xFFFFFFFF) }
    var selectedAnim by remember { mutableStateOf(existing?.animationType ?: TextAnimation.POP_IN) }
    var fontStyle by remember { mutableStateOf(existing?.fontStyle ?: "Bold Sans") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Caption or Title") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrightAmber,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_input_field")
        )

        // Color selector
        Text("Text Color", color = TextSecondary, fontSize = 12.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                0xFFFFFFFF to "White",
                0xFF00E5FF to "Cyan",
                0xFFF59E0B to "Gold",
                0xFFEC4899 to "Pink",
                0xFF10B981 to "Mint",
                0xFFEF4444 to "Red"
            ).forEach { (colorVal, _) ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(colorVal), CircleShape)
                        .border(
                            width = if (selectedColor == colorVal) 3.dp else 1.dp,
                            color = if (selectedColor == colorVal) PureWhite else BorderSubtle,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = colorVal }
                )
            }
        }

        // Animation presets
        Text("Entry Animation", color = TextSecondary, fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TextAnimation.values()) { anim ->
                FilterChip(
                    selected = selectedAnim == anim,
                    onClick = { selectedAnim = anim },
                    label = { Text(anim.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrightAmber,
                        selectedLabelColor = ObsidianBlack,
                        containerColor = DarkCardElevated,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        // Save & Delete buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (existing != null) {
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RadiantRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete")
                }
            }

            Button(
                onClick = {
                    val overlay = existing?.copy(
                        text = textInput,
                        textColor = selectedColor,
                        animationType = selectedAnim,
                        fontStyle = fontStyle
                    ) ?: TextOverlay(
                        id = UUID.randomUUID().toString(),
                        text = textInput,
                        textColor = selectedColor,
                        animationType = selectedAnim,
                        fontStyle = fontStyle,
                        startMs = currentPositionMs,
                        endMs = currentPositionMs + 3000L
                    )
                    onSave(overlay)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrightAmber, contentColor = ObsidianBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply Text", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Subpanel: Auto Captions
@Composable
private fun AutoCaptionsPanel(
    onGenerate: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = BrightAmber,
            modifier = Modifier.size(44.dp)
        )
        Text(
            "AI Auto-Captions",
            color = PureWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Transcribe voice track into viral synchronized subtitle animations automatically.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Button(
            onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(containerColor = BrightAmber, contentColor = ObsidianBlack),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate Viral Captions", fontWeight = FontWeight.Bold)
        }
    }
}

// Subpanel: Filters
@Composable
private fun FiltersPanel(
    currentFilter: VideoFilter,
    currentIntensity: Float,
    adjustments: ColorAdjustments,
    onSetFilter: (VideoFilter, Float) -> Unit,
    onSetAdjustments: (ColorAdjustments) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = DarkCard,
            contentColor = NeonCyan
        ) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Filter Presets") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Color Adjust") })
        }

        if (tabIndex == 0) {
            // Filter presets list
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(VideoFilter.values()) { f ->
                    val isSelected = currentFilter == f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSetFilter(f, currentIntensity) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (f.tintColor != null) Color(f.tintColor).copy(alpha = 0.5f)
                                    else DarkCardElevated
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else BorderSubtle,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = f.displayName.take(2).uppercase(),
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = f.displayName,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (currentFilter != VideoFilter.ORIGINAL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Filter Intensity", color = TextSecondary, fontSize = 12.sp)
                    Text("${(currentIntensity * 100).toInt()}%", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = currentIntensity,
                    onValueChange = { onSetFilter(currentFilter, it) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }
        } else {
            // Adjustments
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdjustmentSlider(
                    label = "Brightness",
                    value = adjustments.brightness,
                    range = -0.5f..0.5f,
                    onValueChange = { onSetAdjustments(adjustments.copy(brightness = it)) }
                )
                AdjustmentSlider(
                    label = "Vignette Shadow",
                    value = adjustments.vignette,
                    range = 0f..1f,
                    onValueChange = { onSetAdjustments(adjustments.copy(vignette = it)) }
                )
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(String.format("%.1f", value), color = PureWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
        )
    }
}

// Subpanel: Effects
@Composable
private fun EffectsPanel(
    currentPositionMs: Long,
    onAddEffect: (EffectOverlay) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Trending Visual Effects", color = TextSecondary, fontSize = 12.sp)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(EffectType.values()) { eff ->
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .width(130.dp)
                        .clickable {
                            onAddEffect(
                                EffectOverlay(
                                    id = UUID.randomUUID().toString(),
                                    effectType = eff,
                                    startMs = currentPositionMs,
                                    endMs = currentPositionMs + 2500L
                                )
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(EffectTrackBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(eff.displayName, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(eff.description, color = TextTertiary, fontSize = 9.sp, maxLines = 2)
                    }
                }
            }
        }
    }
}

// Subpanel: Transitions
@Composable
private fun TransitionsPanel(
    currentTransition: TransitionType,
    onSelectTransition: (TransitionType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Select Clip Transition", color = TextSecondary, fontSize = 12.sp)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(TransitionType.values()) { tr ->
                val isSelected = currentTransition == tr
                Surface(
                    color = if (isSelected) NeonCyanDark else DarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) NeonCyan else BorderSubtle),
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { onSelectTransition(tr) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isSelected) PureWhite else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            tr.displayName,
                            color = if (isSelected) PureWhite else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// Subpanel: Stickers
@Composable
private fun StickersPanel(
    stickerCatalog: List<Pair<String, String>>,
    currentPositionMs: Long,
    onAddSticker: (StickerOverlay) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tap sticker to place on canvas", color = TextSecondary, fontSize = 12.sp)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(stickerCatalog) { (symbol, label) ->
                Surface(
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            onAddSticker(
                                StickerOverlay(
                                    id = UUID.randomUUID().toString(),
                                    symbol = symbol,
                                    label = label,
                                    positionX = 0.5f,
                                    positionY = 0.45f,
                                    startMs = currentPositionMs,
                                    endMs = currentPositionMs + 3000L
                                )
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(symbol, fontSize = 24.sp)
                        Text(label, color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// Subpanel: Canvas Settings
@Composable
private fun CanvasSettingsPanel(
    currentRatio: AspectRatio,
    currentBg: CanvasBgType,
    onSetRatio: (AspectRatio) -> Unit,
    onSetBg: (CanvasBgType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Aspect Ratio", color = TextSecondary, fontSize = 12.sp)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AspectRatio.values()) { ratio ->
                val isSelected = currentRatio == ratio
                FilterChip(
                    selected = isSelected,
                    onClick = { onSetRatio(ratio) },
                    label = { Text("${ratio.displayName} (${ratio.hint})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = ObsidianBlack,
                        containerColor = DarkCardElevated,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        Text("Canvas Background", color = TextSecondary, fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(CanvasBgType.values()) { bg ->
                val isSelected = currentBg == bg
                FilterChip(
                    selected = isSelected,
                    onClick = { onSetBg(bg) },
                    label = { Text(bg.name.replace("_", " "), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = ObsidianBlack,
                        containerColor = DarkCardElevated,
                        labelColor = TextPrimary
                    )
                )
            }
        }
    }
}
