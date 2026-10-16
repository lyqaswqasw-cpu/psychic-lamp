package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.foundation.BorderStroke

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.Episode
import com.example.model.Movie
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VodSettingsTab {
    AUDIO_SUBTITLE,
    QUALITY,
    SPEED,
    ASPECT_RATIO
}

@OptIn(UnstableApi::class)
@Composable
fun MoviePlayerView(
    streamUrl: String,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    autoLandscape: Boolean = false,
    userAgent: String = "",
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val sharedPrefs: SharedPreferences = remember(context) {
        context.getSharedPreferences("vod_player_prefs", Context.MODE_PRIVATE)
    }

    // Playback States
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }

    var progressMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferMs by remember { mutableLongStateOf(0L) }

    // Gestures & HUD Feedback
    var isLongPressFastForward by remember { mutableStateOf(false) }
    var showDoubleTapSeekLeft by remember { mutableStateOf(false) }
    var showDoubleTapSeekRight by remember { mutableStateOf(false) }
    var volumeHudLevel by remember { mutableStateOf<Float?>(null) }
    var brightnessHudLevel by remember { mutableStateOf<Float?>(null) }

    // Settings Sheets & Drawers
    var showSettingsSheet by remember { mutableStateOf(false) }
    var selectedSettingsTab by remember { mutableStateOf(VodSettingsTab.AUDIO_SUBTITLE) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playerAspectRatioMode by remember { mutableStateOf("16_9") }

 // "16_9", "fit", "fill"
    var isMuted by remember { mutableStateOf(false) }
    var isManualFullScreen by remember { mutableStateOf(false) }

    // Real-time Video Stream Inspection & Quality State
    var detectedWidth by remember { mutableIntStateOf(0) }
    var detectedHeight by remember { mutableIntStateOf(0) }
    var detectedBitrate by remember { mutableIntStateOf(0) }
    var tracksVersion by remember { mutableIntStateOf(0) }

    val smartQualityLabel = remember(detectedWidth, detectedHeight, tracksVersion) {
        when {
            detectedWidth >= 3840 || detectedHeight >= 2160 -> "4K UHD"
            detectedWidth >= 1920 || detectedHeight >= 1080 -> "1080p FHD"
            detectedWidth >= 1280 || detectedHeight >= 720 -> "720p HD"
            detectedWidth > 0 || detectedHeight > 0 -> "${detectedHeight}p SD"
            else -> "فحص ذكي"
        }
    }

    // Episodes & Series Data
    val episodes by viewModel?.episodes?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val movies by viewModel?.movies?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val isSeries = remember(streamUrl, episodes) {
        streamUrl.contains("/series/") || episodes.isNotEmpty()
    }

    // Identify current episode index
    val currentEpisodeIndex = remember(episodes, streamUrl, title) {
        episodes.indexOfFirst { it.streamUrl == streamUrl || it.title == title }
    }
    val hasNextEpisode = isSeries && currentEpisodeIndex != -1 && currentEpisodeIndex < episodes.size - 1
    val hasPreviousEpisode = isSeries && currentEpisodeIndex > 0
    val nextEpisode = if (hasNextEpisode) episodes[currentEpisodeIndex + 1] else null

    // Auto Next Episode Countdown Overlay (shown in last 40 seconds)
    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var nextEpisodeCountdown by remember { mutableIntStateOf(15) }
    var nextEpisodePromptDismissed by remember { mutableStateOf(false) }

    // Manage Screen Orientation, Insets, and Screen Wake Lock
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = if (autoLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Intercept back button to unlock or close player safely
    BackHandler(enabled = true) {
        if (isScreenLocked) {
            isScreenLocked = false
        } else if (showEpisodesDrawer) {
            showEpisodesDrawer = false
        } else if (showSettingsSheet) {
            showSettingsSheet = false
        } else {
            onClose()
        }
    }

    // Format Candidates resolution - Probes MKV first, then fallbacks to MP4, M3U8, TS, and direct URL
    val formatCandidates = remember(streamUrl) {
        if (streamUrl.isBlank()) return@remember listOf(streamUrl)
        val queryIndex = streamUrl.indexOf('?')
        val cleanUrl = if (queryIndex != -1) streamUrl.substring(0, queryIndex) else streamUrl
        val query = if (queryIndex != -1) streamUrl.substring(queryIndex) else ""

        val lastSlash = cleanUrl.lastIndexOf('/')
        val dotIndex = cleanUrl.lastIndexOf('.')
        val base = if (dotIndex > lastSlash) cleanUrl.substring(0, dotIndex) else cleanUrl

        val candidates = mutableListOf<String>()
        // 1. فحص صيغة MKV أولاً
        candidates.add("$base.mkv$query")
        // 2. التحويل التلقائي لصيغة MP4
        candidates.add("$base.mp4$query")
        // 3. التحويل لصيغة M3U8 (HLS)
        candidates.add("$base.m3u8$query")
        // 4. فحص صيغة TS
        candidates.add("$base.ts$query")
        // 5. الرابط الأصلي
        candidates.add(streamUrl)

        candidates.distinct()
    }

    var candidateIndex by remember { mutableIntStateOf(0) }
    val currentActiveUrl = remember(formatCandidates, candidateIndex) {
        if (candidateIndex in formatCandidates.indices) formatCandidates[candidateIndex] else streamUrl
    }

    var currentUaIndex by remember { mutableIntStateOf(0) }
    val effectiveUserAgent = if (userAgent.isNotEmpty()) userAgent else ExoPlayerDataSourceUtils.DEFAULT_USER_AGENTS[currentUaIndex % ExoPlayerDataSourceUtils.DEFAULT_USER_AGENTS.size]

    // Create ExoPlayer Instance with Ultra-Fast VOD LoadControl, Failover Error Policy & Wake Lock
    val exoPlayer = remember(effectiveUserAgent) {
        val mediaSourceFactory = ExoPlayerDataSourceUtils.createVodMediaSourceFactory(context, effectiveUserAgent)
        val loadControl = ExoPlayerDataSourceUtils.createVodLoadControl()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    var isViewDisposed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { isViewDisposed = true } }

    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            keepScreenOn = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // PiP Mode check
    val isPipModeState by viewModel?.isPipMode?.collectAsState() ?: remember { mutableStateOf(false) }
    val isInPipMode = isPipModeState || (activity?.isInPictureInPictureMode == true)

    if (isInPipMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    (playerView.parent as? ViewGroup)?.removeView(playerView)
                    playerView
                },
                update = { view ->
                    view.resizeMode = if (playerAspectRatioMode == "stretch" || playerAspectRatioMode == "fill") {
                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.then(
                    if (playerAspectRatioMode == "16_9") {
                        Modifier.aspectRatio(16f / 9f)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
            )
        }
        return
    }

    // Attach Listeners and handle Playback
    DisposableEffect(exoPlayer) {
        playerView.player = exoPlayer
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playbackError = null
                    try {
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        // Restore saved position on first ready
                        val savedPos = sharedPrefs.getLong("pos_${streamUrl.hashCode()}", 0L)
                        if (savedPos > 5000L && savedPos < durationMs - 15000L && exoPlayer.currentPosition < 2000L) {
                            exoPlayer.seekTo(savedPos)
                        }
                    } catch (e: Exception) {
                        Log.e("MoviePlayerView", "Error getting duration", e)
                    }
                } else if (state == Player.STATE_ENDED) {
                    if (hasNextEpisode && nextEpisode != null) {
                        viewModel?.startPlaying(nextEpisode.streamUrl, nextEpisode.title, isLive = false, userAgent = userAgent)
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    detectedWidth = videoSize.width
                    detectedHeight = videoSize.height
                    tracksVersion++
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                tracksVersion++
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSelected(i)) {
                                val format = group.getTrackFormat(i)
                                if (format.width > 0) detectedWidth = format.width
                                if (format.height > 0) detectedHeight = format.height
                                if (format.bitrate > 0) detectedBitrate = format.bitrate
                            }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MoviePlayerView", "Error on candidate $candidateIndex ($currentActiveUrl): ${error.message}", error)
                if (candidateIndex < formatCandidates.size - 1 && !isViewDisposed) {
                    // Try next format candidate (e.g. MKV -> MP4 -> M3U8 -> TS -> streamUrl)
                    candidateIndex++
                } else if (currentUaIndex < ExoPlayerDataSourceUtils.DEFAULT_USER_AGENTS.size - 1 && !isViewDisposed) {
                    // Retry candidates from 0 with next User-Agent
                    currentUaIndex++
                    candidateIndex = 0
                } else {
                    playbackError = "تعذر تشغيل هذا المحتوى حالياً. يرجى المحاولة مرة أخرى أو اختيار حلقة أخرى."
                    isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            try {
                // Save last position before disposal
                val lastPos = exoPlayer.currentPosition
                if (lastPos > 5000L) {
                    sharedPrefs.edit().putLong("pos_${streamUrl.hashCode()}", lastPos).apply()
                }
            } catch (e: Exception) {
                // Ignore
            }
            (playerView.parent as? ViewGroup)?.removeView(playerView)
            playerView.player = null
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Load media URL on change
    LaunchedEffect(currentActiveUrl, currentUaIndex) {
        try {
            if (!isViewDisposed && currentActiveUrl.isNotEmpty()) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                val cleanLower = currentActiveUrl.lowercase().substringBefore("?")
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(currentActiveUrl))
                    .apply {
                        when {
                            cleanLower.endsWith(".mp4") -> setMimeType(MimeTypes.VIDEO_MP4)
                            cleanLower.endsWith(".mkv") -> setMimeType(MimeTypes.VIDEO_MATROSKA)
                            cleanLower.endsWith(".m3u8") -> setMimeType(MimeTypes.APPLICATION_M3U8)
                            cleanLower.endsWith(".ts") -> setMimeType(MimeTypes.VIDEO_MP2T)
                            cleanLower.endsWith(".mpd") -> setMimeType(MimeTypes.APPLICATION_MPD)
                        }
                    }
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                playbackError = null
                isBuffering = true
            }
        } catch (e: Exception) {
            playbackError = "فشل في إعداد المشغل"
        }
    }

    // Progress updates & Position auto-saving
    LaunchedEffect(isPlaying) {
        while (isPlaying && !isViewDisposed) {
            try {
                progressMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                bufferMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                if (durationMs <= 0L) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
                // Save position every 3 seconds
                if (progressMs > 5000L) {
                    sharedPrefs.edit().putLong("pos_${streamUrl.hashCode()}", progressMs).apply()
                }
                // Trigger Next Episode Prompt in last 40 seconds
                if (hasNextEpisode && durationMs > 60000L && progressMs >= (durationMs - 40000L) && !nextEpisodePromptDismissed) {
                    showNextEpisodePrompt = true
                }
            } catch (e: Exception) {
                break
            }
            delay(1000L)
        }
    }

    // Next Episode Countdown Timer
    LaunchedEffect(showNextEpisodePrompt) {
        if (showNextEpisodePrompt) {
            nextEpisodeCountdown = 15
            while (nextEpisodeCountdown > 0 && showNextEpisodePrompt) {
                delay(1000L)
                nextEpisodeCountdown--
            }
            if (nextEpisodeCountdown == 0 && showNextEpisodePrompt && hasNextEpisode && nextEpisode != null) {
                showNextEpisodePrompt = false
                viewModel?.startPlaying(nextEpisode.streamUrl, nextEpisode.title, isLive = false, userAgent = userAgent)
            }
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying && !isScreenLocked) {
            delay(5000L)
            showControls = false
        }
    }

    // Reset HUD indicators after gesture ends
    LaunchedEffect(volumeHudLevel, brightnessHudLevel) {
        if (volumeHudLevel != null || brightnessHudLevel != null) {
            delay(1500L)
            volumeHudLevel = null
            brightnessHudLevel = null
        }
    }

    // Main Container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("cinematic_vod_player")
    ) {
        // Video View with Touch & Gesture Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked) {
                    if (isScreenLocked) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                    } else {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                            },
                            onDoubleTap = { offset ->
                                val screenWidth = size.width
                                if (offset.x < screenWidth * 0.4f) {
                                    // Double Tap Left: Rewind 10s
                                    val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                    exoPlayer.seekTo(newPos)
                                    showDoubleTapSeekLeft = true
                                    coroutineScope.launch {
                                        delay(800L)
                                        showDoubleTapSeekLeft = false
                                    }
                                } else if (offset.x > screenWidth * 0.6f) {
                                    // Double Tap Right: Forward 10s
                                    val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                    exoPlayer.seekTo(newPos)
                                    showDoubleTapSeekRight = true
                                    coroutineScope.launch {
                                        delay(800L)
                                        showDoubleTapSeekRight = false
                                    }
                                }
                            },
                            onLongPress = {
                                // 2x Speed on Long Press
                                isLongPressFastForward = true
                                exoPlayer.playbackParameters = PlaybackParameters(2.0f)
                            },
                            onPress = {
                                tryAwaitRelease()
                                if (isLongPressFastForward) {
                                    isLongPressFastForward = false
                                    exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                }
                            }
                        )
                    }
                }
                .pointerInput(isScreenLocked) {
                    if (!isScreenLocked) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val isLeftSide = change.position.x < size.width * 0.5f
                                if (isLeftSide) {
                                    // Vertical drag: Brightness
                                    activity?.window?.let { window ->
                                        val currentBrightness = window.attributes.screenBrightness.let { if (it < 0) 0.5f else it }
                                        val delta = -dragAmount.y / size.height
                                        val newBrightness = (currentBrightness + delta).coerceIn(0.02f, 1.0f)
                                        val lp = window.attributes
                                        lp.screenBrightness = newBrightness
                                        window.attributes = lp
                                        brightnessHudLevel = newBrightness
                                    }
                                } else {
                                    // Vertical drag: Volume
                                    audioManager?.let { am ->
                                        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val delta = (-dragAmount.y / (size.height * 0.4f)) * maxVol
                                        val newVol = (curVol + delta.toInt()).coerceIn(0, maxVol)
                                        am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        volumeHudLevel = newVol.toFloat() / maxVol.toFloat()
                                    }
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    (playerView.parent as? ViewGroup)?.removeView(playerView)
                    playerView
                },
                update = { view ->
                    view.resizeMode = if (playerAspectRatioMode == "stretch" || playerAspectRatioMode == "fill") {
                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier
                    .then(
                        if (playerAspectRatioMode == "16_9") {
                            Modifier.aspectRatio(16f / 9f)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )
            )
        }

        // Animated Quick-Seek Ripple Badges (+10s / -10s)
        AnimatedVisibility(
            visible = showDoubleTapSeekLeft,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 1.2f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .border(2.dp, PlayerAccentGold.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = PlayerAccentGold, modifier = Modifier.size(36.dp))
                    Text("10- ثوانٍ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        AnimatedVisibility(
            visible = showDoubleTapSeekRight,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 1.2f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 64.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .border(2.dp, PlayerAccentGold.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = PlayerAccentGold, modifier = Modifier.size(36.dp))
                    Text("10+ ثوانٍ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2X Speed Long Press Active Banner
        AnimatedVisibility(
            visible = isLongPressFastForward,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Surface(
                color = PlayerAccentGold,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Text(
                        text = "سرعة مضاعفة 2X ⚡",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // On-Screen Gesture HUD Indicators (Brightness & Volume)
        if (brightnessHudLevel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(24.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(brightnessHudLevel ?: 0.5f)
                                .background(PlayerAccentGold, RoundedCornerShape(3.dp))
                        )
                    }
                    Text("${((brightnessHudLevel ?: 0.5f) * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (volumeHudLevel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if ((volumeHudLevel ?: 0f) > 0f) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = PlayerAccentGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(volumeHudLevel ?: 0.5f)
                                .background(PlayerAccentGold, RoundedCornerShape(3.dp))
                        )
                    }
                    Text("${((volumeHudLevel ?: 0.5f) * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Screen Lock Floating Unlocker
        if (isScreenLocked) {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        isScreenLocked = false
                        showControls = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlayerAccentGold),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = "فتح القفل", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("إلغاء قفل الشاشة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Buffering Spinner
        if (isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = PlayerAccentGold,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Playback Error Alert
        if (playbackError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(20.dp)
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = PlayerAccentRed, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = playbackError ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onClose,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text("رجوع", color = Color.White)
                        }
                        Button(
                            onClick = {
                                playbackError = null
                                isBuffering = true
                                candidateIndex = 0
                                currentUaIndex = 0
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PlayerAccentGold)
                        ) {
                            Text("إعادة المحاولة", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // NEXT EPISODE COUNTDOWN PROMPT OVERLAY
        AnimatedVisibility(
            visible = showNextEpisodePrompt && !isScreenLocked,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 90.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xE6141416)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PlayerAccentGold.copy(alpha = 0.7f)),
                modifier = Modifier.width(310.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(18.dp))
                            Text("الحلقة التالية خلال $nextEpisodeCountdown ثوانٍ", color = PlayerAccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                showNextEpisodePrompt = false
                                nextEpisodePromptDismissed = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (nextEpisode != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = nextEpisode.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            showNextEpisodePrompt = false
                            if (nextEpisode != null) {
                                viewModel?.startPlaying(nextEpisode.streamUrl, nextEpisode.title, isLive = false, userAgent = userAgent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlayerAccentGold),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تشغيل الآن ⏭", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // TOP & BOTTOM CONTROLS HUD
        AnimatedVisibility(
            visible = showControls && !isScreenLocked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Gradient Overlays
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))))
                )

                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Title & Season Badge
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayerIconButton(
                                onClick = onClose,
                                icon = Icons.Default.ArrowBack,
                                contentDescription = "رجوع"
                            )
                            Column {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSeries) "مسلسلات VOD 🎬" else "أفلام سينما 🍿",
                                    color = PlayerAccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Left Actions: Episodes Drawer, Real-time Quality Badge, Settings Sheet, PiP, Screen Lock
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Smart Real-time Quality Badge Button
                        Button(
                            onClick = {
                                selectedSettingsTab = VodSettingsTab.QUALITY
                                showSettingsSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                            border = BorderStroke(1.dp, PlayerAccentGold.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = PlayerAccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = smartQualityLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Episodes Drawer Button
                        if (isSeries || movies.isNotEmpty()) {
                            Button(
                                onClick = { showEpisodesDrawer = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isSeries) "الحلقات" else "المقترحات", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Settings Dialog Button
                        PlayerIconButton(
                            onClick = {
                                selectedSettingsTab = VodSettingsTab.AUDIO_SUBTITLE
                                showSettingsSheet = true
                            },
                            icon = Icons.Default.Tune,
                            contentDescription = "إعدادات الصوت والترجمة والجودة"
                        )

                        // Picture in Picture
                        PlayerIconButton(
                            onClick = { launchPictureInPicture(activity) },
                            icon = Icons.Default.PictureInPictureAlt,
                            contentDescription = "صورة داخل صورة"
                        )

                        // Lock Screen
                        PlayerIconButton(
                            onClick = {
                                isScreenLocked = true
                                showControls = true
                            },
                            icon = Icons.Default.Lock,
                            contentDescription = "قفل الشاشة",
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // CENTER PLAY/PAUSE BIG BUTTON
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Previous Episode
                        if (hasPreviousEpisode) {
                            PlayerIconButton(
                                onClick = {
                                    val prev = episodes[currentEpisodeIndex - 1]
                                    viewModel?.startPlaying(prev.streamUrl, prev.title, isLive = false, userAgent = userAgent)
                                },
                                icon = Icons.Default.SkipPrevious,
                                contentDescription = "الحلقة السابقة",
                                size = 48,
                                iconSize = 26
                            )
                        }

                        // Rewind 10s
                        PlayerIconButton(
                            onClick = {
                                val pos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(pos)
                            },
                            icon = Icons.Default.Replay10,
                            contentDescription = "-10 ثوانٍ",
                            size = 52,
                            iconSize = 28
                        )

                        // Main Play / Pause
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(PlayerAccentGold, CircleShape)
                                .clickable {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        // Forward 10s
                        PlayerIconButton(
                            onClick = {
                                val pos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                                exoPlayer.seekTo(pos)
                            },
                            icon = Icons.Default.Forward10,
                            contentDescription = "+10 ثوانٍ",
                            size = 52,
                            iconSize = 28
                        )

                        // Next Episode
                        if (hasNextEpisode && nextEpisode != null) {
                            PlayerIconButton(
                                onClick = {
                                    viewModel?.startPlaying(nextEpisode.streamUrl, nextEpisode.title, isLive = false, userAgent = userAgent)
                                },
                                icon = Icons.Default.SkipNext,
                                contentDescription = "الحلقة التالية",
                                size = 48,
                                iconSize = 26
                            )
                        }
                    }
                }

                // BOTTOM CONTROLS & TIMELINE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Time Progress & Duration Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatPlayerTime(progressMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "-${formatPlayerTime((durationMs - progressMs).coerceAtLeast(0L))}",
                                color = PlayerTextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "•",
                                color = PlayerTextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatPlayerTime(durationMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interactive Slider
                    Slider(
                        value = progressMs.toFloat(),
                        onValueChange = { floatVal ->
                            progressMs = floatVal.toLong()
                            exoPlayer.seekTo(progressMs)
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = PlayerAccentGold,
                            activeTrackColor = PlayerAccentGold,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Secondary Quick Bottom Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Speed & Aspect Ratio
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Speed Badge
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                    val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                                    playbackSpeed = speeds[nextIdx]
                                    exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                }
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Aspect Ratio Switcher (16:9 vs تعبئة الشاشة)
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PlayerAccentGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        playerAspectRatioMode = if (playerAspectRatioMode == "16_9") "stretch" else "16_9"
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AspectRatio,
                                        contentDescription = "أبعاد الشاشة",
                                        tint = PlayerAccentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (playerAspectRatioMode == "stretch" || playerAspectRatioMode == "fill") {
                                            "تمديد الشاشة"
                                        } else {
                                            "16:9"
                                        },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Right: Mute toggle & Fullscreen toggle
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            PlayerIconButton(
                                onClick = {
                                    val nextVol = if (exoPlayer.volume > 0f) 0f else 1f
                                    exoPlayer.volume = nextVol
                                    isMuted = nextVol == 0f
                                },
                                icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "كتم الصوت",
                                size = 36,
                                iconSize = 18
                            )

                            PlayerIconButton(
                                onClick = {
                                    activity?.let { act ->
                                        if (act.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            isManualFullScreen = false
                                        } else {
                                            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            isManualFullScreen = true
                                        }
                                    }
                                },
                                icon = Icons.Default.Fullscreen,
                                contentDescription = "ملء الشاشة",
                                size = 36,
                                iconSize = 18,
                                tint = PlayerAccentGold
                            )
                        }
                    }
                }
            }
        }

        // EPISODES / RECOMMENDED MOVIES DRAWER
        AnimatedVisibility(
            visible = showEpisodesDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(340.dp)
        ) {
            Surface(
                color = Color(0xF5111113),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showEpisodesDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                        Text(
                            text = if (isSeries) "قائمة الحلقات 📺" else "أفلام مقترحة 🎬",
                            color = PlayerAccentGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isSeries) {
                        if (episodes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد حلقات متاحة", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(episodes.size) { idx ->
                                    val ep = episodes[idx]
                                    val isCurrent = ep.streamUrl == streamUrl || ep.title == title
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel?.startPlaying(ep.streamUrl, ep.title, isLive = false, userAgent = userAgent)
                                                showEpisodesDrawer = false
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) PlayerAccentRed.copy(alpha = 0.2f) else Color(0xFF1E1E22)
                                        ),
                                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, PlayerAccentGold) else null,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isCurrent) {
                                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(20.dp))
                                            } else {
                                                Text("${idx + 1}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                                Text(
                                                    text = ep.title,
                                                    color = if (isCurrent) PlayerAccentGold else Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (ep.duration.isNotEmpty()) {
                                                    Text(ep.duration, color = PlayerTextMuted, fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (movies.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا توجد أفلام مقترحة", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(movies.size) { idx ->
                                    val movie = movies[idx]
                                    val isCurrent = movie.streamUrl == streamUrl || movie.title == title
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel?.startPlaying(movie.streamUrl, movie.title, isLive = false, userAgent = userAgent)
                                                showEpisodesDrawer = false
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) PlayerAccentRed.copy(alpha = 0.2f) else Color(0xFF1E1E22)
                                        ),
                                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, PlayerAccentGold) else null,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = movie.title,
                                                color = if (isCurrent) PlayerAccentGold else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("${movie.year} • ★ ${movie.rating}", color = PlayerTextMuted, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ADVANCED SETTINGS MODAL DIALOG (Audio / Subtitles / Quality / Speed)
        if (showSettingsSheet) {
            AlertDialog(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = Color(0xFF161618),
                shape = RoundedCornerShape(18.dp),
                confirmButton = {
                    TextButton(onClick = { showSettingsSheet = false }) {
                        Text("تم", color = PlayerAccentGold, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إعدادات المشغل", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Settings, contentDescription = null, tint = PlayerAccentGold)
                    }
                },
                text = {
                    val audioTracks = remember(exoPlayer.currentTracks) { extractAudioTracks(exoPlayer) }
                    val subtitleTracks = remember(exoPlayer.currentTracks) { extractSubtitleTracks(exoPlayer) }
                    val qualityTracks = remember(exoPlayer.currentTracks) { extractQualityTracks(exoPlayer) }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Settings Category Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = if (selectedSettingsTab == VodSettingsTab.AUDIO_SUBTITLE) PlayerAccentGold else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSettingsTab = VodSettingsTab.AUDIO_SUBTITLE }
                            ) {
                                Text(
                                    text = "الصوت والترجمة",
                                    color = if (selectedSettingsTab == VodSettingsTab.AUDIO_SUBTITLE) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Surface(
                                color = if (selectedSettingsTab == VodSettingsTab.QUALITY) PlayerAccentGold else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSettingsTab = VodSettingsTab.QUALITY }
                            ) {
                                Text(
                                    text = "الجودة",
                                    color = if (selectedSettingsTab == VodSettingsTab.QUALITY) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Surface(
                                color = if (selectedSettingsTab == VodSettingsTab.SPEED) PlayerAccentGold else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSettingsTab = VodSettingsTab.SPEED }
                            ) {
                                Text(
                                    text = "السرعة",
                                    color = if (selectedSettingsTab == VodSettingsTab.SPEED) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        when (selectedSettingsTab) {
                            VodSettingsTab.AUDIO_SUBTITLE -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text("المسارات الصوتية:", color = PlayerAccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (audioTracks.isEmpty()) {
                                        item {
                                            Text("المسار الصوتي الافتراضي نشط", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        }
                                    } else {
                                        items(audioTracks.size) { idx ->
                                            val tr = audioTracks[idx]
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { applyTrackSelection(exoPlayer, tr, C.TRACK_TYPE_AUDIO) }
                                                    .background(if (tr.isSelected) PlayerAccentRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(tr.label, color = Color.White, fontSize = 11.sp)
                                                if (tr.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        Text("ملفات الترجمة:", color = PlayerAccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    items(subtitleTracks.size) { idx ->
                                        val sub = subtitleTracks[idx]
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { applyTrackSelection(exoPlayer, sub, C.TRACK_TYPE_TEXT) }
                                                .background(if (sub.isSelected) PlayerAccentRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sub.label, color = Color.White, fontSize = 11.sp)
                                            if (sub.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            VodSettingsTab.QUALITY -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Smart Stream Inspector Card
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, PlayerAccentGold.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Speed, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("فحص الجودة الفعلي الذكي", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(PlayerAccentGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("⚡ تشغيل سريع ومستقر", color = PlayerAccentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("الدقة الحقيقية:", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                                    Text(
                                                        text = if (detectedWidth > 0 && detectedHeight > 0) "${detectedWidth}x${detectedHeight} ($smartQualityLabel)" else "جاري الفحص التلقائي...",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("معدل البث:", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                                    Text(
                                                        text = if (detectedBitrate > 0) "${detectedBitrate / 1000} kbps" else "تلقائي فائق السرعة",
                                                        color = Color(0xFF4CAF50),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text("الجودات المتاحة للتبديل:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (qualityTracks.isEmpty()) {
                                            item {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(PlayerAccentRed.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("تلقائي ذكي (أفضل دقة متاحة)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        } else {
                                            items(qualityTracks.size) { idx ->
                                                val q = qualityTracks[idx]
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { applyTrackSelection(exoPlayer, q, C.TRACK_TYPE_VIDEO) }
                                                        .background(if (q.isSelected) PlayerAccentRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(q.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    if (q.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            VodSettingsTab.SPEED, VodSettingsTab.ASPECT_RATIO -> {
                                val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    speedOptions.forEach { spd ->
                                        val isSel = playbackSpeed == spd
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    playbackSpeed = spd
                                                    exoPlayer.playbackParameters = PlaybackParameters(spd)
                                                }
                                                .background(if (isSel) PlayerAccentGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(if (spd == 1.0f) "عادي (1.0x)" else "${spd}x", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
