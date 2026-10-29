package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ExportState {
    data object Idle : ExportState
    data class Rendering(val progress: Float, val currentFrame: Int, val totalFrames: Int, val resolution: String) : ExportState
    data class Finished(val resolution: String, val durationMs: Long, val fileSizeMb: String) : ExportState
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application.applicationContext)

    private val _projects = MutableStateFlow<List<VideoProject>>(emptyList())
    val projects: StateFlow<List<VideoProject>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<VideoProject?>(null)
    val currentProject: StateFlow<VideoProject?> = _currentProject.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _activeToolPanel = MutableStateFlow<ActiveToolPanel>(ActiveToolPanel.None)
    val activeToolPanel: StateFlow<ActiveToolPanel> = _activeToolPanel.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedTextId = MutableStateFlow<String?>(null)
    val selectedTextId: StateFlow<String?> = _selectedTextId.asStateFlow()

    private val _selectedStickerId = MutableStateFlow<String?>(null)
    val selectedStickerId: StateFlow<String?> = _selectedStickerId.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableListOf<VideoProject>()
    private val redoStack = mutableListOf<VideoProject>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null

    init {
        loadProjects()
    }

    fun loadProjects() {
        val list = repository.getAllProjects()
        _projects.value = list
    }

    fun selectProject(projectId: String) {
        val project = repository.getProjectById(projectId) ?: _projects.value.firstOrNull()
        if (project != null) {
            _currentProject.value = project
            _currentPositionMs.value = 0L
            _isPlaying.value = false
            _selectedClipId.value = project.videoClips.firstOrNull()?.id
            _activeToolPanel.value = ActiveToolPanel.None
            undoStack.clear()
            redoStack.clear()
            updateUndoRedoStates()
        }
    }

    fun createNewProject(
        name: String = "New Project",
        aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
        clips: List<VideoClip> = emptyList()
    ): VideoProject {
        val selectedClips = if (clips.isNotEmpty()) clips else listOf(
            VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Neon Drift Opening",
                stockTheme = StockTheme.NEON_DRIFT,
                sourceDurationMs = 5000L,
                startTrimMs = 0L,
                endTrimMs = 5000L
            ),
            VideoClip(
                id = UUID.randomUUID().toString(),
                title = "Cyber City Rush",
                stockTheme = StockTheme.CYBERPUNK,
                sourceDurationMs = 4500L,
                startTrimMs = 0L,
                endTrimMs = 4500L,
                transitionIn = TransitionType.FLASH_WHITE
            )
        )

        val newProject = VideoProject(
            id = UUID.randomUUID().toString(),
            name = name,
            aspectRatio = aspectRatio,
            videoClips = selectedClips,
            audioTracks = listOf(
                AudioClip(
                    id = UUID.randomUUID().toString(),
                    name = "Midnight Cyber Phonk",
                    artist = "CapCut Beats",
                    type = AudioType.BGM,
                    startAtProjectMs = 0L,
                    durationMs = 9500L,
                    volume = 0.9f
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = UUID.randomUUID().toString(),
                    text = "EDIT WITH CAPCUT ✨",
                    fontStyle = "Bold Sans",
                    textColor = 0xFF00E5FF,
                    backgroundColor = 0xAA000000,
                    animationType = TextAnimation.POP_IN,
                    positionX = 0.5f,
                    positionY = 0.3f,
                    startMs = 500L,
                    endMs = 3500L
                )
            ),
            lastModified = System.currentTimeMillis()
        )

        repository.saveProject(newProject)
        loadProjects()
        selectProject(newProject.id)
        return newProject
    }

    fun deleteProject(id: String) {
        repository.deleteProject(id)
        loadProjects()
        if (_currentProject.value?.id == id) {
            _currentProject.value = null
        }
    }

    fun duplicateProject(id: String) {
        val copy = repository.duplicateProject(id)
        loadProjects()
        if (copy != null) {
            selectProject(copy.id)
        }
    }

    fun closeProject() {
        stopPlayback()
        _currentProject.value?.let { repository.saveProject(it) }
        _currentProject.value = null
        loadProjects()
    }

    // Playback control
    fun togglePlayPause() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        val project = _currentProject.value ?: return
        val totalMs = project.totalDurationMs
        if (_currentPositionMs.value >= totalMs) {
            _currentPositionMs.value = 0L
        }
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                delay(33L) // ~30 fps
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                val newPos = _currentPositionMs.value + delta
                if (newPos >= totalMs) {
                    _currentPositionMs.value = 0L // Loop playback
                } else {
                    _currentPositionMs.value = newPos
                }
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun seekTo(positionMs: Long) {
        val project = _currentProject.value ?: return
        val clamped = positionMs.coerceIn(0L, project.totalDurationMs)
        _currentPositionMs.value = clamped
    }

    fun stepFrame(forward: Boolean) {
        val stepMs = 33L // ~1 frame at 30fps
        val target = if (forward) _currentPositionMs.value + stepMs else _currentPositionMs.value - stepMs
        seekTo(target)
    }

    fun openToolPanel(panel: ActiveToolPanel) {
        _activeToolPanel.value = panel
    }

    fun closeToolPanel() {
        _activeToolPanel.value = ActiveToolPanel.None
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
        if (clipId != null) {
            _activeToolPanel.value = ActiveToolPanel.EditClip(clipId)
        }
    }

    fun selectText(textId: String?) {
        _selectedTextId.value = textId
        if (textId != null) {
            _activeToolPanel.value = ActiveToolPanel.EditText(textId)
        }
    }

    fun selectSticker(stickerId: String?) {
        _selectedStickerId.value = stickerId
    }

    // Timeline editing actions
    private fun mutateProject(block: (VideoProject) -> VideoProject) {
        val current = _currentProject.value ?: return
        undoStack.add(current)
        if (undoStack.size > 20) undoStack.removeAt(0)
        redoStack.clear()
        updateUndoRedoStates()

        val updated = block(current).copy(lastModified = System.currentTimeMillis())
        _currentProject.value = updated
        repository.saveProject(updated)
    }

    fun undo() {
        val current = _currentProject.value ?: return
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current)
            _currentProject.value = previous
            repository.saveProject(previous)
            updateUndoRedoStates()
        }
    }

    fun redo() {
        val current = _currentProject.value ?: return
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(current)
            _currentProject.value = next
            repository.saveProject(next)
            updateUndoRedoStates()
        }
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    // Split video clip at playhead
    fun splitAtPlayhead() {
        val project = _currentProject.value ?: return
        val playhead = _currentPositionMs.value

        var runningMs = 0L
        var targetClipIndex = -1
        var offsetInClip = 0L

        for (i in project.videoClips.indices) {
            val clip = project.videoClips[i]
            val clipDuration = clip.effectiveDurationMs
            if (playhead >= runningMs && playhead < runningMs + clipDuration) {
                targetClipIndex = i
                offsetInClip = playhead - runningMs
                break
            }
            runningMs += clipDuration
        }

        if (targetClipIndex != -1) {
            val clip = project.videoClips[targetClipIndex]
            val cutTimeSourceMs = clip.startTrimMs + (offsetInClip * clip.speed).toLong()

            // Don't split if too close to clip edges
            if (cutTimeSourceMs - clip.startTrimMs > 300L && clip.endTrimMs - cutTimeSourceMs > 300L) {
                val firstPart = clip.copy(
                    id = UUID.randomUUID().toString(),
                    endTrimMs = cutTimeSourceMs
                )
                val secondPart = clip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clip.title} (Part 2)",
                    startTrimMs = cutTimeSourceMs,
                    transitionIn = TransitionType.NONE
                )

                mutateProject { p ->
                    val newClips = p.videoClips.toMutableList()
                    newClips.removeAt(targetClipIndex)
                    newClips.add(targetClipIndex, secondPart)
                    newClips.add(targetClipIndex, firstPart)
                    p.copy(videoClips = newClips)
                }
                _selectedClipId.value = secondPart.id
            }
        }
    }

    fun trimClip(clipId: String, newStartTrimMs: Long, newEndTrimMs: Long) {
        mutateProject { p ->
            val updated = p.videoClips.map { c ->
                if (c.id == clipId) {
                    val start = newStartTrimMs.coerceIn(0L, c.sourceDurationMs - 300L)
                    val end = newEndTrimMs.coerceIn(start + 300L, c.sourceDurationMs)
                    c.copy(startTrimMs = start, endTrimMs = end)
                } else c
            }
            p.copy(videoClips = updated)
        }
    }

    fun setClipSpeed(clipId: String, speed: Float) {
        mutateProject { p ->
            val updated = p.videoClips.map { c ->
                if (c.id == clipId) c.copy(speed = speed.coerceIn(0.2f, 10.0f)) else c
            }
            p.copy(videoClips = updated)
        }
    }

    fun rotateClip(clipId: String) {
        mutateProject { p ->
            val updated = p.videoClips.map { c ->
                if (c.id == clipId) c.copy(rotation = (c.rotation + 90f) % 360f) else c
            }
            p.copy(videoClips = updated)
        }
    }

    fun mirrorClip(clipId: String) {
        mutateProject { p ->
            val updated = p.videoClips.map { c ->
                if (c.id == clipId) c.copy(isMirrored = !c.isMirrored) else c
            }
            p.copy(videoClips = updated)
        }
    }

    fun deleteClip(clipId: String) {
        mutateProject { p ->
            val updated = p.videoClips.filterNot { it.id == clipId }
            p.copy(videoClips = updated)
        }
        if (_selectedClipId.value == clipId) {
            _selectedClipId.value = _currentProject.value?.videoClips?.firstOrNull()?.id
            _activeToolPanel.value = ActiveToolPanel.None
        }
    }

    fun duplicateClip(clipId: String) {
        mutateProject { p ->
            val idx = p.videoClips.indexOfFirst { it.id == clipId }
            if (idx != -1) {
                val original = p.videoClips[idx]
                val copy = original.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${original.title} (Copy)"
                )
                val newClips = p.videoClips.toMutableList()
                newClips.add(idx + 1, copy)
                p.copy(videoClips = newClips)
            } else p
        }
    }

    fun setTransition(clipId: String, transitionType: TransitionType) {
        mutateProject { p ->
            val updated = p.videoClips.map { c ->
                if (c.id == clipId) c.copy(transitionIn = transitionType) else c
            }
            p.copy(videoClips = updated)
        }
    }

    fun addVideoClip(clip: VideoClip) {
        mutateProject { p ->
            p.copy(videoClips = p.videoClips + clip)
        }
        _selectedClipId.value = clip.id
    }

    fun addAudioTrack(audioClip: AudioClip) {
        mutateProject { p ->
            p.copy(audioTracks = p.audioTracks + audioClip)
        }
    }

    fun removeAudioTrack(audioId: String) {
        mutateProject { p ->
            p.copy(audioTracks = p.audioTracks.filterNot { it.id == audioId })
        }
    }

    fun setAudioVolume(audioId: String, volume: Float) {
        mutateProject { p ->
            val updated = p.audioTracks.map { a ->
                if (a.id == audioId) a.copy(volume = volume.coerceIn(0f, 2f)) else a
            }
            p.copy(audioTracks = updated)
        }
    }

    fun addTextOverlay(text: TextOverlay) {
        mutateProject { p ->
            p.copy(textOverlays = p.textOverlays + text)
        }
        _selectedTextId.value = text.id
    }

    fun updateTextOverlay(text: TextOverlay) {
        mutateProject { p ->
            val updated = p.textOverlays.map { t ->
                if (t.id == text.id) text else t
            }
            p.copy(textOverlays = updated)
        }
    }

    fun deleteTextOverlay(textId: String) {
        mutateProject { p ->
            p.copy(textOverlays = p.textOverlays.filterNot { it.id == textId })
        }
        if (_selectedTextId.value == textId) {
            _selectedTextId.value = null
            _activeToolPanel.value = ActiveToolPanel.None
        }
    }

    fun generateAutoCaptions() {
        val project = _currentProject.value ?: return
        val captions = mutableListOf<TextOverlay>()
        var offset = 500L
        val templates = listOf(
            "Welcome to the video! 🚀",
            "Notice the smooth transition right here ✨",
            "This beat drop is crazy 🔥",
            "Shot on mobile, edited in CapCut 🎬",
            "Follow for more video editing tips! 💯"
        )
        for (i in templates.indices) {
            if (offset >= project.totalDurationMs) break
            val end = (offset + 2200L).coerceAtMost(project.totalDurationMs)
            captions.add(
                TextOverlay(
                    id = UUID.randomUUID().toString(),
                    text = templates[i],
                    fontStyle = "Bold Sans",
                    textColor = 0xFFFFFFFF,
                    backgroundColor = 0xCC111216,
                    animationType = TextAnimation.POP_IN,
                    positionX = 0.5f,
                    positionY = 0.78f,
                    scale = 0.95f,
                    startMs = offset,
                    endMs = end
                )
            )
            offset = end + 300L
        }

        mutateProject { p ->
            p.copy(textOverlays = p.textOverlays + captions)
        }
    }

    fun addEffect(effect: EffectOverlay) {
        mutateProject { p ->
            p.copy(effectOverlays = p.effectOverlays + effect)
        }
    }

    fun removeEffect(effectId: String) {
        mutateProject { p ->
            p.copy(effectOverlays = p.effectOverlays.filterNot { it.id == effectId })
        }
    }

    fun addSticker(sticker: StickerOverlay) {
        mutateProject { p ->
            p.copy(stickers = p.stickers + sticker)
        }
        _selectedStickerId.value = sticker.id
    }

    fun updateStickerPosition(stickerId: String, x: Float, y: Float) {
        mutateProject { p ->
            val updated = p.stickers.map { s ->
                if (s.id == stickerId) s.copy(positionX = x.coerceIn(0.1f, 0.9f), positionY = y.coerceIn(0.1f, 0.9f)) else s
            }
            p.copy(stickers = updated)
        }
    }

    fun deleteSticker(stickerId: String) {
        mutateProject { p ->
            p.copy(stickers = p.stickers.filterNot { it.id == stickerId })
        }
        if (_selectedStickerId.value == stickerId) {
            _selectedStickerId.value = null
        }
    }

    fun setFilter(filter: VideoFilter, intensity: Float = 0.8f) {
        mutateProject { p ->
            p.copy(filter = filter, filterIntensity = intensity)
        }
    }

    fun setAdjustments(adjustments: ColorAdjustments) {
        mutateProject { p ->
            p.copy(adjustments = adjustments)
        }
    }

    fun setAspectRatio(ratio: AspectRatio) {
        mutateProject { p ->
            p.copy(aspectRatio = ratio)
        }
    }

    fun setCanvasBgType(bgType: CanvasBgType) {
        mutateProject { p ->
            p.copy(canvasBgType = bgType)
        }
    }

    fun updateProjectMeta(name: String, resolution: String, fps: Int) {
        mutateProject { p ->
            p.copy(name = name, resolution = resolution, fps = fps)
        }
    }

    // Export simulation
    fun startExport(resolution: String, fps: Int) {
        val project = _currentProject.value ?: return
        stopPlayback()
        val totalFrames = ((project.totalDurationMs / 1000f) * fps).toInt().coerceAtLeast(30)

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            for (frame in 1..totalFrames) {
                val progress = frame.toFloat() / totalFrames
                _exportState.value = ExportState.Rendering(progress, frame, totalFrames, resolution)
                delay(35L) // Smooth rendering progress
            }
            val estMb = String.format("%.1f MB", (totalFrames * 0.12f).coerceAtLeast(4.5f))
            _exportState.value = ExportState.Finished(resolution, project.totalDurationMs, estMb)
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportState.Idle
    }

    // Catalogs helpers
    fun getMusicCatalog() = repository.getMusicCatalog()
    fun getSoundEffectsCatalog() = repository.getSoundEffectsCatalog()
    fun getStockClips() = repository.getStockClips()
    fun getStickerCatalog() = repository.getStickerCatalog()
}
