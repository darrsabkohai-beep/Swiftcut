package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("capcut_studio_projects", Context.MODE_PRIVATE)

    fun getAllProjects(): List<VideoProject> {
        val rawJson = prefs.getString("projects_list", null)
        if (rawJson.isNullOrBlank()) {
            val defaults = createStarterProjects()
            saveAllProjects(defaults)
            return defaults
        }
        return try {
            parseProjects(rawJson)
        } catch (e: Exception) {
            createStarterProjects()
        }
    }

    fun getProjectById(id: String): VideoProject? {
        return getAllProjects().find { it.id == id }
    }

    fun saveProject(project: VideoProject) {
        val all = getAllProjects().toMutableList()
        val index = all.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            all[index] = project.copy(lastModified = System.currentTimeMillis())
        } else {
            all.add(0, project.copy(lastModified = System.currentTimeMillis()))
        }
        saveAllProjects(all)
    }

    fun duplicateProject(id: String): VideoProject? {
        val original = getProjectById(id) ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            lastModified = System.currentTimeMillis()
        )
        saveProject(copy)
        return copy
    }

    fun deleteProject(id: String) {
        val all = getAllProjects().filterNot { it.id == id }
        saveAllProjects(all)
    }

    private fun saveAllProjects(projects: List<VideoProject>) {
        val jsonArray = JSONArray()
        projects.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("aspectRatio", p.aspectRatio.name)
            obj.put("resolution", p.resolution)
            obj.put("fps", p.fps)
            obj.put("filter", p.filter.name)
            obj.put("filterIntensity", p.filterIntensity.toDouble())
            obj.put("canvasBgType", p.canvasBgType.name)
            obj.put("lastModified", p.lastModified)

            // Adjustments
            val adj = JSONObject()
            adj.put("brightness", p.adjustments.brightness.toDouble())
            adj.put("contrast", p.adjustments.contrast.toDouble())
            adj.put("saturation", p.adjustments.saturation.toDouble())
            adj.put("warmth", p.adjustments.warmth.toDouble())
            adj.put("vignette", p.adjustments.vignette.toDouble())
            adj.put("sharpen", p.adjustments.sharpen.toDouble())
            obj.put("adjustments", adj)

            // Video clips
            val clipsArr = JSONArray()
            p.videoClips.forEach { c ->
                val cObj = JSONObject()
                cObj.put("id", c.id)
                cObj.put("title", c.title)
                cObj.put("uri", c.uri ?: "")
                cObj.put("stockTheme", c.stockTheme?.name ?: "")
                cObj.put("sourceDurationMs", c.sourceDurationMs)
                cObj.put("startTrimMs", c.startTrimMs)
                cObj.put("endTrimMs", c.endTrimMs)
                cObj.put("speed", c.speed.toDouble())
                cObj.put("volume", c.volume.toDouble())
                cObj.put("rotation", c.rotation.toDouble())
                cObj.put("isMirrored", c.isMirrored)
                cObj.put("isReversed", c.isReversed)
                cObj.put("transitionIn", c.transitionIn.name)
                cObj.put("transitionDurationMs", c.transitionDurationMs)
                clipsArr.put(cObj)
            }
            obj.put("videoClips", clipsArr)

            // Audio tracks
            val audioArr = JSONArray()
            p.audioTracks.forEach { a ->
                val aObj = JSONObject()
                aObj.put("id", a.id)
                aObj.put("name", a.name)
                aObj.put("artist", a.artist)
                aObj.put("type", a.type.name)
                aObj.put("startAtProjectMs", a.startAtProjectMs)
                aObj.put("durationMs", a.durationMs)
                aObj.put("volume", a.volume.toDouble())
                aObj.put("isMuted", a.isMuted)
                audioArr.put(aObj)
            }
            obj.put("audioTracks", audioArr)

            // Text overlays
            val textArr = JSONArray()
            p.textOverlays.forEach { t ->
                val tObj = JSONObject()
                tObj.put("id", t.id)
                tObj.put("text", t.text)
                tObj.put("fontStyle", t.fontStyle)
                tObj.put("textColor", t.textColor)
                tObj.put("backgroundColor", t.backgroundColor ?: 0L)
                tObj.put("animationType", t.animationType.name)
                tObj.put("positionX", t.positionX.toDouble())
                tObj.put("positionY", t.positionY.toDouble())
                tObj.put("scale", t.scale.toDouble())
                tObj.put("rotation", t.rotation.toDouble())
                tObj.put("startMs", t.startMs)
                tObj.put("endMs", t.endMs)
                textArr.put(tObj)
            }
            obj.put("textOverlays", textArr)

            // Effects
            val effArr = JSONArray()
            p.effectOverlays.forEach { e ->
                val eObj = JSONObject()
                eObj.put("id", e.id)
                eObj.put("effectType", e.effectType.name)
                eObj.put("startMs", e.startMs)
                eObj.put("endMs", e.endMs)
                eObj.put("intensity", e.intensity.toDouble())
                effArr.put(eObj)
            }
            obj.put("effectOverlays", effArr)

            // Stickers
            val stArr = JSONArray()
            p.stickers.forEach { s ->
                val sObj = JSONObject()
                sObj.put("id", s.id)
                sObj.put("symbol", s.symbol)
                sObj.put("label", s.label)
                sObj.put("category", s.category)
                sObj.put("positionX", s.positionX.toDouble())
                sObj.put("positionY", s.positionY.toDouble())
                sObj.put("scale", s.scale.toDouble())
                sObj.put("startMs", s.startMs)
                sObj.put("endMs", s.endMs)
                stArr.put(sObj)
            }
            obj.put("stickers", stArr)

            jsonArray.put(obj)
        }
        prefs.edit().putString("projects_list", jsonArray.toString()).apply()
    }

    private fun parseProjects(jsonStr: String): List<VideoProject> {
        val list = mutableListOf<VideoProject>()
        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.getString("id")
            val name = obj.getString("name")
            val aspectRatio = try {
                AspectRatio.valueOf(obj.getString("aspectRatio"))
            } catch (e: Exception) { AspectRatio.RATIO_9_16 }
            val resolution = obj.optString("resolution", "1080p FHD")
            val fps = obj.optInt("fps", 30)
            val filter = try {
                VideoFilter.valueOf(obj.getString("filter"))
            } catch (e: Exception) { VideoFilter.ORIGINAL }
            val filterIntensity = obj.optDouble("filterIntensity", 0.8).toFloat()
            val canvasBgType = try {
                CanvasBgType.valueOf(obj.getString("canvasBgType"))
            } catch (e: Exception) { CanvasBgType.BLUR }
            val lastModified = obj.optLong("lastModified", System.currentTimeMillis())

            val adjObj = obj.optJSONObject("adjustments")
            val adjustments = if (adjObj != null) {
                ColorAdjustments(
                    brightness = adjObj.optDouble("brightness", 0.0).toFloat(),
                    contrast = adjObj.optDouble("contrast", 1.0).toFloat(),
                    saturation = adjObj.optDouble("saturation", 1.0).toFloat(),
                    warmth = adjObj.optDouble("warmth", 0.0).toFloat(),
                    vignette = adjObj.optDouble("vignette", 0.0).toFloat(),
                    sharpen = adjObj.optDouble("sharpen", 0.0).toFloat()
                )
            } else ColorAdjustments()

            // Parse clips
            val clipsArr = obj.optJSONArray("videoClips") ?: JSONArray()
            val clips = mutableListOf<VideoClip>()
            for (j in 0 until clipsArr.length()) {
                val c = clipsArr.getJSONObject(j)
                val stockStr = c.optString("stockTheme", "")
                val stock = if (stockStr.isNotBlank()) {
                    try { StockTheme.valueOf(stockStr) } catch (e: Exception) { null }
                } else null
                val trStr = c.optString("transitionIn", "NONE")
                val tr = try { TransitionType.valueOf(trStr) } catch (e: Exception) { TransitionType.NONE }
                clips.add(
                    VideoClip(
                        id = c.getString("id"),
                        title = c.getString("title"),
                        uri = c.optString("uri").takeIf { it.isNotBlank() },
                        stockTheme = stock,
                        sourceDurationMs = c.optLong("sourceDurationMs", 4000L),
                        startTrimMs = c.optLong("startTrimMs", 0L),
                        endTrimMs = c.optLong("endTrimMs", 4000L),
                        speed = c.optDouble("speed", 1.0).toFloat(),
                        volume = c.optDouble("volume", 1.0).toFloat(),
                        rotation = c.optDouble("rotation", 0.0).toFloat(),
                        isMirrored = c.optBoolean("isMirrored", false),
                        isReversed = c.optBoolean("isReversed", false),
                        transitionIn = tr,
                        transitionDurationMs = c.optLong("transitionDurationMs", 500L)
                    )
                )
            }

            // Parse audio
            val audioArr = obj.optJSONArray("audioTracks") ?: JSONArray()
            val audios = mutableListOf<AudioClip>()
            for (j in 0 until audioArr.length()) {
                val a = audioArr.getJSONObject(j)
                val typeStr = a.optString("type", "BGM")
                val type = try { AudioType.valueOf(typeStr) } catch (e: Exception) { AudioType.BGM }
                audios.add(
                    AudioClip(
                        id = a.getString("id"),
                        name = a.getString("name"),
                        artist = a.optString("artist", "CapCut Beats"),
                        type = type,
                        startAtProjectMs = a.optLong("startAtProjectMs", 0L),
                        durationMs = a.optLong("durationMs", 5000L),
                        volume = a.optDouble("volume", 1.0).toFloat(),
                        isMuted = a.optBoolean("isMuted", false)
                    )
                )
            }

            // Parse text
            val textArr = obj.optJSONArray("textOverlays") ?: JSONArray()
            val texts = mutableListOf<TextOverlay>()
            for (j in 0 until textArr.length()) {
                val t = textArr.getJSONObject(j)
                val animStr = t.optString("animationType", "POP_IN")
                val anim = try { TextAnimation.valueOf(animStr) } catch (e: Exception) { TextAnimation.POP_IN }
                val bgCol = t.optLong("backgroundColor", 0L)
                texts.add(
                    TextOverlay(
                        id = t.getString("id"),
                        text = t.getString("text"),
                        fontStyle = t.optString("fontStyle", "Bold Sans"),
                        textColor = t.optLong("textColor", 0xFFFFFFFF),
                        backgroundColor = if (bgCol != 0L) bgCol else null,
                        animationType = anim,
                        positionX = t.optDouble("positionX", 0.5).toFloat(),
                        positionY = t.optDouble("positionY", 0.75).toFloat(),
                        scale = t.optDouble("scale", 1.0).toFloat(),
                        rotation = t.optDouble("rotation", 0.0).toFloat(),
                        startMs = t.optLong("startMs", 0L),
                        endMs = t.optLong("endMs", 3000L)
                    )
                )
            }

            // Parse effects
            val effArr = obj.optJSONArray("effectOverlays") ?: JSONArray()
            val effs = mutableListOf<EffectOverlay>()
            for (j in 0 until effArr.length()) {
                val e = effArr.getJSONObject(j)
                val effTypeStr = e.optString("effectType", "GLITCH_RGB")
                val effType = try { EffectType.valueOf(effTypeStr) } catch (ex: Exception) { EffectType.GLITCH_RGB }
                effs.add(
                    EffectOverlay(
                        id = e.getString("id"),
                        effectType = effType,
                        startMs = e.optLong("startMs", 0L),
                        endMs = e.optLong("endMs", 2500L),
                        intensity = e.optDouble("intensity", 0.8).toFloat()
                    )
                )
            }

            // Parse stickers
            val stArr = obj.optJSONArray("stickers") ?: JSONArray()
            val stickers = mutableListOf<StickerOverlay>()
            for (j in 0 until stArr.length()) {
                val s = stArr.getJSONObject(j)
                stickers.add(
                    StickerOverlay(
                        id = s.getString("id"),
                        symbol = s.getString("symbol"),
                        label = s.getString("label"),
                        category = s.optString("category", "Trending"),
                        positionX = s.optDouble("positionX", 0.5).toFloat(),
                        positionY = s.optDouble("positionY", 0.4).toFloat(),
                        scale = s.optDouble("scale", 1.0).toFloat(),
                        startMs = s.optLong("startMs", 0L),
                        endMs = s.optLong("endMs", 3000L)
                    )
                )
            }

            list.add(
                VideoProject(
                    id = id,
                    name = name,
                    aspectRatio = aspectRatio,
                    resolution = resolution,
                    fps = fps,
                    videoClips = clips,
                    audioTracks = audios,
                    textOverlays = texts,
                    effectOverlays = effs,
                    stickers = stickers,
                    filter = filter,
                    filterIntensity = filterIntensity,
                    adjustments = adjustments,
                    canvasBgType = canvasBgType,
                    lastModified = lastModified
                )
            )
        }
        return list
    }

    fun createStarterProjects(): List<VideoProject> {
        val project1 = VideoProject(
            id = "proj_cyberpunk_beat",
            name = "Cyberpunk Beat Edit",
            aspectRatio = AspectRatio.RATIO_9_16,
            resolution = "1080p FHD",
            fps = 60,
            filter = VideoFilter.CYBERPUNK,
            filterIntensity = 0.85f,
            videoClips = listOf(
                VideoClip(
                    id = "c1",
                    title = "Neon Drift Night",
                    stockTheme = StockTheme.NEON_DRIFT,
                    sourceDurationMs = 3500L,
                    startTrimMs = 0L,
                    endTrimMs = 3500L,
                    speed = 1.0f
                ),
                VideoClip(
                    id = "c2",
                    title = "Cyber City Skyline",
                    stockTheme = StockTheme.CYBERPUNK,
                    sourceDurationMs = 4000L,
                    startTrimMs = 0L,
                    endTrimMs = 4000L,
                    speed = 1.2f,
                    transitionIn = TransitionType.GLITCH
                ),
                VideoClip(
                    id = "c3",
                    title = "Analog Scan Grid",
                    stockTheme = StockTheme.RETRO_FILM,
                    sourceDurationMs = 3000L,
                    startTrimMs = 0L,
                    endTrimMs = 3000L,
                    speed = 1.0f,
                    transitionIn = TransitionType.FLASH_WHITE
                )
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "a1",
                    name = "Midnight Cyber Phonk",
                    artist = "Tokyo Nights",
                    type = AudioType.BGM,
                    startAtProjectMs = 0L,
                    durationMs = 9500L,
                    volume = 0.9f
                ),
                AudioClip(
                    id = "a2",
                    name = "Sub Bass Impact",
                    artist = "CapCut FX",
                    type = AudioType.SOUND_EFFECT,
                    startAtProjectMs = 3500L,
                    durationMs = 1200L,
                    volume = 1.0f
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = "t1",
                    text = "NEON DREAMS // 2026",
                    fontStyle = "Cyber Bold",
                    textColor = 0xFF00E5FF,
                    backgroundColor = 0xCC111216,
                    animationType = TextAnimation.NEON_FLICKER,
                    positionX = 0.5f,
                    positionY = 0.28f,
                    scale = 1.2f,
                    startMs = 500L,
                    endMs = 3800L
                ),
                TextOverlay(
                    id = "t2",
                    text = "Sync on the Beat ⚡",
                    fontStyle = "Bold Sans",
                    textColor = 0xFFFFFFFF,
                    backgroundColor = 0xAAEC4899,
                    animationType = TextAnimation.POP_IN,
                    positionX = 0.5f,
                    positionY = 0.72f,
                    scale = 1.0f,
                    startMs = 3600L,
                    endMs = 7000L
                )
            ),
            effectOverlays = listOf(
                EffectOverlay(
                    id = "e1",
                    effectType = EffectType.GLITCH_RGB,
                    startMs = 3400L,
                    endMs = 4200L,
                    intensity = 0.9f
                )
            ),
            stickers = listOf(
                StickerOverlay(
                    id = "s1",
                    symbol = "⚡",
                    label = "Lightning",
                    positionX = 0.82f,
                    positionY = 0.25f,
                    scale = 1.2f,
                    startMs = 600L,
                    endMs = 3800L
                )
            )
        )

        val project2 = VideoProject(
            id = "proj_sunset_vlog",
            name = "Summer Travel Vlog",
            aspectRatio = AspectRatio.RATIO_9_16,
            resolution = "4K UHD",
            fps = 30,
            filter = VideoFilter.GOLDEN_HOUR,
            filterIntensity = 0.75f,
            videoClips = listOf(
                VideoClip(
                    id = "c4",
                    title = "Pacific Sunset Waves",
                    stockTheme = StockTheme.SUNSET_WAVES,
                    sourceDurationMs = 4500L,
                    startTrimMs = 0L,
                    endTrimMs = 4500L,
                    speed = 0.8f // Slow motion
                ),
                VideoClip(
                    id = "c5",
                    title = "Coastal Highway Drive",
                    stockTheme = StockTheme.MOUNTAIN_ROADTRIP,
                    sourceDurationMs = 5000L,
                    startTrimMs = 0L,
                    endTrimMs = 5000L,
                    speed = 1.0f,
                    transitionIn = TransitionType.DISSOLVE
                )
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "a3",
                    name = "Sunset Lofi Breeze",
                    artist = "Chill Wave Records",
                    type = AudioType.BGM,
                    startAtProjectMs = 0L,
                    durationMs = 10000L,
                    volume = 0.85f
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = "t3",
                    text = "Golden Hour Memories ✨",
                    fontStyle = "Modern Serif",
                    textColor = 0xFFF59E0B,
                    backgroundColor = 0xAA000000,
                    animationType = TextAnimation.TYPEWRITER,
                    positionX = 0.5f,
                    positionY = 0.35f,
                    startMs = 300L,
                    endMs = 4200L
                )
            ),
            effectOverlays = listOf(
                EffectOverlay(
                    id = "e2",
                    effectType = EffectType.FILM_BURN,
                    startMs = 0L,
                    endMs = 2000L,
                    intensity = 0.7f
                )
            ),
            stickers = listOf(
                StickerOverlay(
                    id = "s2",
                    symbol = "🌅",
                    label = "Sunset",
                    positionX = 0.2f,
                    positionY = 0.2f,
                    scale = 1.1f,
                    startMs = 500L,
                    endMs = 4000L
                )
            )
        )

        val project3 = VideoProject(
            id = "proj_cafe_aesthetic",
            name = "Minimalist Coffee Reel",
            aspectRatio = AspectRatio.RATIO_1_1,
            resolution = "1080p FHD",
            fps = 30,
            filter = VideoFilter.WARM_SUNSET,
            filterIntensity = 0.6f,
            videoClips = listOf(
                VideoClip(
                    id = "c6",
                    title = "Morning Pour Over",
                    stockTheme = StockTheme.AESTHETIC_CAFE,
                    sourceDurationMs = 4000L,
                    startTrimMs = 0L,
                    endTrimMs = 4000L,
                    speed = 1.0f
                )
            ),
            audioTracks = listOf(
                AudioClip(
                    id = "a4",
                    name = "Coffeehouse Acoustic",
                    artist = "Sunday Vibes",
                    type = AudioType.BGM,
                    startAtProjectMs = 0L,
                    durationMs = 4000L,
                    volume = 0.75f
                )
            ),
            textOverlays = listOf(
                TextOverlay(
                    id = "t4",
                    text = "Slow Mornings ☕",
                    fontStyle = "Bold Sans",
                    textColor = 0xFFFFFFFF,
                    backgroundColor = 0x882A1B12,
                    animationType = TextAnimation.FADE_SLIDE,
                    positionX = 0.5f,
                    positionY = 0.8f,
                    startMs = 500L,
                    endMs = 3500L
                )
            )
        )

        return listOf(project1, project2, project3)
    }

    // Curated catalog for adding music
    fun getMusicCatalog(): List<AudioClip> {
        return listOf(
            AudioClip("bgm_1", "Tokyo Drift Neon", "Cyber Beats", AudioType.BGM, 0L, 12000L, 0.9f),
            AudioClip("bgm_2", "Lofi Sunset Chill", "Chillhop Cafe", AudioType.BGM, 0L, 15000L, 0.85f),
            AudioClip("bgm_3", "Cinematic Epic Horizon", "Hans Orchestra", AudioType.BGM, 0L, 14000L, 1.0f),
            AudioClip("bgm_4", "Viral Pop Bounce", "TikTok Grooves", AudioType.BGM, 0L, 10000L, 0.95f),
            AudioClip("bgm_5", "Deep Bass Phonk", "Drift Krew", AudioType.BGM, 0L, 11000L, 1.0f),
            AudioClip("bgm_6", "Acoustic Warm Glow", "Morning Brew", AudioType.BGM, 0L, 13000L, 0.8f)
        )
    }

    // Curated catalog for sound effects
    fun getSoundEffectsCatalog(): List<AudioClip> {
        return listOf(
            AudioClip("sfx_1", "Whoosh Transition", "CapCut SFX", AudioType.SOUND_EFFECT, 0L, 800L, 1.0f),
            AudioClip("sfx_2", "Sub Bass Drop", "CapCut SFX", AudioType.SOUND_EFFECT, 0L, 1500L, 1.0f),
            AudioClip("sfx_3", "Camera Shutter Click", "CapCut SFX", AudioType.SOUND_EFFECT, 0L, 600L, 0.9f),
            AudioClip("sfx_4", "Glitch Static Burst", "CapCut SFX", AudioType.SOUND_EFFECT, 0L, 900L, 0.85f),
            AudioClip("sfx_5", "Pop Bubble", "CapCut SFX", AudioType.SOUND_EFFECT, 0L, 400L, 0.9f),
            AudioClip("sfx_6", "Cinematic Horn Boom", "CapCut SFX", AudioType.SOUND_EFFECT, 0L, 2000L, 1.0f)
        )
    }

    // Available stock media clips for "New Project" / "Add Media"
    fun getStockClips(): List<VideoClip> {
        return listOf(
            VideoClip(UUID.randomUUID().toString(), "Neon Drift Night", null, StockTheme.NEON_DRIFT, 5000L, 0L, 5000L),
            VideoClip(UUID.randomUUID().toString(), "Cyberpunk Skyline", null, StockTheme.CYBERPUNK, 5000L, 0L, 5000L),
            VideoClip(UUID.randomUUID().toString(), "Sunset Ocean Waves", null, StockTheme.SUNSET_WAVES, 6000L, 0L, 6000L),
            VideoClip(UUID.randomUUID().toString(), "Mountain Roadtrip", null, StockTheme.MOUNTAIN_ROADTRIP, 5000L, 0L, 5000L),
            VideoClip(UUID.randomUUID().toString(), "Aesthetic Pour Over Cafe", null, StockTheme.AESTHETIC_CAFE, 4500L, 0L, 4500L),
            VideoClip(UUID.randomUUID().toString(), "Vintage 90s Film Reel", null, StockTheme.RETRO_FILM, 5000L, 0L, 5000L)
        )
    }

    // Curated stickers
    fun getStickerCatalog(): List<Pair<String, String>> {
        return listOf(
            "🔥" to "Fire",
            "⚡" to "Lightning",
            "✨" to "Sparkles",
            "💯" to "100",
            "🔴" to "Subscribe",
            "❤️" to "Love",
            "🎬" to "Clapper",
            "🚀" to "Rocket",
            "⭐" to "Star",
            "😮" to "Mind Blown",
            "👑" to "Crown",
            "🔊" to "Loud",
            "☕" to "Coffee",
            "🌴" to "Palm Tree",
            "🎯" to "Target",
            "👀" to "Look"
        )
    }
}
