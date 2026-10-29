package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    projects: List<VideoProject>,
    stockClips: List<VideoClip>,
    onOpenProject: (String) -> Unit,
    onCreateNewProject: (name: String, ratio: AspectRatio, clips: List<VideoClip>) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewProjectModal by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for picking real videos or photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val clip = VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Device Media Clip",
                uri = uri.toString(),
                sourceDurationMs = 5000L,
                startTrimMs = 0L,
                endTrimMs = 5000L
            )
            onCreateNewProject("Imported Video Edit", AspectRatio.RATIO_9_16, listOf(clip))
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        containerColor = ObsidianBlack,
        topBar = {
            HomeTopBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero "+ New Project" Card
            NewProjectHeroCard(
                onClick = { showNewProjectModal = true }
            )

            // Quick Tools Bar (CapCut style)
            QuickToolsRow(
                onToolClick = { showNewProjectModal = true }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Projects / Templates Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ObsidianBlack,
                contentColor = NeonCyan,
                divider = { HorizontalDivider(color = BorderSubtle) },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Projects (${projects.size})",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Trending Templates",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Drafts / Projects List
                if (projects.isEmpty()) {
                    EmptyProjectsView(onNewProject = { showNewProjectModal = true })
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        projects.forEach { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onOpenProject(project.id) },
                                onDuplicate = { onDuplicateProject(project.id) },
                                onDelete = { onDeleteProject(project.id) }
                            )
                        }
                    }
                }
            } else {
                // Templates list
                TemplatesGridView(
                    onSelectTemplate = { templateName, ratio, clips ->
                        onCreateNewProject(templateName, ratio, clips)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // New Project Dialog (Picker between Gallery / Stock Video / Aspect Ratio)
        if (showNewProjectModal) {
            NewProjectDialog(
                stockClips = stockClips,
                onDismiss = { showNewProjectModal = false },
                onPickGallery = {
                    showNewProjectModal = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onPickStock = { selectedStockClips, selectedRatio, projectName ->
                    showNewProjectModal = false
                    onCreateNewProject(projectName, selectedRatio, selectedStockClips)
                }
            )
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ContentCut,
                    contentDescription = "CapCut Icon",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    "CapCut Studio",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text("Video Editor & Maker", color = TextSecondary, fontSize = 10.sp)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = BrightAmber.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BrightAmber.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = BrightAmber, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PRO", color = BrightAmber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun NewProjectHeroCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = DarkCardElevated,
        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(NeonCyan, ElectricMint))),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = NeonCyan)
            .testTag("new_project_hero_button")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F1E2E),
                            Color(0xFF142436),
                            Color(0xFF1A1D24)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = ObsidianBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "New Project",
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Create reel, shorts, or video edit",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Surface(
                    color = NeonCyanDark.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NeonCyan)
                ) {
                    Text(
                        "Start",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickToolsRow(onToolClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickToolItem(Icons.Default.AutoMode, "AutoCut", NeonCyan, onToolClick)
        QuickToolItem(Icons.Default.Speed, "Speed Ramp", ElectricMint, onToolClick)
        QuickToolItem(Icons.Default.Subtitles, "AI Captions", BrightAmber, onToolClick)
        QuickToolItem(Icons.Default.GraphicEq, "Sound FX", ElectricPink, onToolClick)
    }
}

@Composable
private fun QuickToolItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(DarkCard, CircleShape)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProjectCard(
    project: VideoProject,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail preview box
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 76.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (project.filter) {
                            VideoFilter.CYBERPUNK -> Color(0xFF003049)
                            VideoFilter.GOLDEN_HOUR -> Color(0xFF4A2B0F)
                            VideoFilter.WARM_SUNSET -> Color(0xFF381414)
                            else -> DarkCardElevated
                        }
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(26.dp)
                )

                // Aspect ratio tag
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                ) {
                    Text(
                        project.aspectRatio.displayName,
                        color = PureWhite,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Project Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatDuration(project.totalDurationMs)} • ${project.videoClips.size} clips",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = DarkCardElevated,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            project.resolution,
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (project.filter != VideoFilter.ORIGINAL) {
                        Surface(
                            color = DarkCardElevated,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                project.filter.displayName,
                                color = BrightAmber,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // More Options Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkCardElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Duplicate", color = PureWhite) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PureWhite) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = RadiantRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RadiantRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProjectsView(onNewProject: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("No Drafts Yet", color = PureWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Create your first CapCut project to begin editing", color = TextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNewProject,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
        ) {
            Text("Create Project")
        }
    }
}

@Composable
private fun TemplatesGridView(
    onSelectTemplate: (name: String, ratio: AspectRatio, clips: List<VideoClip>) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val templates = listOf(
            Triple(
                "Viral TikTok Beat Reel 🔥",
                AspectRatio.RATIO_9_16,
                listOf(
                    VideoClip(UUID.randomUUID().toString(), "Neon Speed Run", null, StockTheme.NEON_DRIFT, 3000L, 0L, 3000L, speed = 1.4f),
                    VideoClip(UUID.randomUUID().toString(), "Cyber Impact", null, StockTheme.CYBERPUNK, 3500L, 0L, 3500L, transitionIn = TransitionType.FLASH_WHITE)
                )
            ),
            Triple(
                "Cinematic Travel Vlog 🌴",
                AspectRatio.RATIO_9_16,
                listOf(
                    VideoClip(UUID.randomUUID().toString(), "Sunset Shoreline", null, StockTheme.SUNSET_WAVES, 4000L, 0L, 4000L, speed = 0.8f),
                    VideoClip(UUID.randomUUID().toString(), "Mountain Pass", null, StockTheme.MOUNTAIN_ROADTRIP, 4500L, 0L, 4500L, transitionIn = TransitionType.DISSOLVE)
                )
            ),
            Triple(
                "90s Retro VHS Tape 📼",
                AspectRatio.RATIO_4_5,
                listOf(
                    VideoClip(UUID.randomUUID().toString(), "Tape Glitch", null, StockTheme.RETRO_FILM, 5000L, 0L, 5000L, transitionIn = TransitionType.GLITCH)
                )
            )
        )

        templates.forEach { (title, ratio, clips) ->
            Surface(
                onClick = { onSelectTemplate(title, ratio, clips) },
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(DarkCardElevated, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = ElectricPink, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, color = PureWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("1-Tap Smart Sync • ${ratio.displayName}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { onSelectTemplate(title, ratio, clips) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Use", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewProjectDialog(
    stockClips: List<VideoClip>,
    onDismiss: () -> Unit,
    onPickGallery: () -> Unit,
    onPickStock: (List<VideoClip>, AspectRatio, String) -> Unit
) {
    var projectName by remember { mutableStateOf("My Edit") }
    var selectedRatio by remember { mutableStateOf(AspectRatio.RATIO_9_16) }
    val selectedStockClips = remember { mutableStateListOf<VideoClip>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = {
            Text("Create New Project", color = PureWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Aspect Ratio Selector
                Text("Aspect Ratio", color = TextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AspectRatio.values()) { ratio ->
                        FilterChip(
                            selected = selectedRatio == ratio,
                            onClick = { selectedRatio = ratio },
                            label = { Text(ratio.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = ObsidianBlack,
                                containerColor = DarkCardElevated,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                // Import Options
                Text("Select Starting Media", color = TextSecondary, fontSize = 12.sp)

                // 1. Device Gallery Picker Option
                Surface(
                    onClick = onPickGallery,
                    color = DarkCardElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, NeonCyanDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeonCyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Import from Phone Gallery", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Pick videos or photos from your device", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // 2. Built-in Stock Clips
                Text("Or choose cinematic stock clips:", color = TextTertiary, fontSize = 11.sp)
                stockClips.forEach { clip ->
                    val isChecked = selectedStockClips.any { it.title == clip.title }
                    Surface(
                        onClick = {
                            if (isChecked) {
                                selectedStockClips.removeAll { it.title == clip.title }
                            } else {
                                selectedStockClips.add(clip)
                            }
                        },
                        color = if (isChecked) VideoTrackColor.copy(alpha = 0.2f) else DarkCardElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isChecked) NeonCyan else BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedStockClips.add(clip)
                                        else selectedStockClips.removeAll { it.title == clip.title }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(clip.title, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${clip.sourceDurationMs / 1000}s HD", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clips = if (selectedStockClips.isNotEmpty()) selectedStockClips.toList() else stockClips.take(2)
                    onPickStock(clips, selectedRatio, projectName.ifBlank { "My Edit" })
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
            ) {
                Text("Start Editing", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val sec = ms / 1000
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}
