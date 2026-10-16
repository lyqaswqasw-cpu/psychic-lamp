package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.Channel
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.ads.UnityAdsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (connectivityManager != null) {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    return false
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    streamUrl: String,
    title: String,
    isLive: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    autoLandscape: Boolean = false,
    userAgent: String = "",
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    // Intercept back button to close player
    BackHandler(enabled = true) {
        onClose()
    }

    // Playback States
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }

    // Drawer and Chat Panels
    var isChatOpen by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }
    var showFormatMenu by remember { mutableStateOf(false) }

    // Gestures HUD
    var volumeHudLevel by remember { mutableStateOf<Float?>(null) }
    var brightnessHudLevel by remember { mutableStateOf<Float?>(null) }

    // View Model Observables
    val isChatEnabledOnline by viewModel.isChatEnabledOnline.collectAsState()
    val isGlobalAdsEnabled by viewModel.isGlobalAdsEnabled.collectAsState()
    val isShareButtonVisible by viewModel.isShareButtonVisible.collectAsState()
    val forcedFormat by viewModel.forcedFormat.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val playingServers by viewModel.playingServers.collectAsState()
    val currentServerIndex by viewModel.currentServerIndex.collectAsState()
    val playingEventChannels by viewModel.playingEventChannels.collectAsState()
    val currentEventChannelIndex by viewModel.currentEventChannelIndex.collectAsState()

    // Find current channel index for Next/Prev channel switching
    val currentChannelIndex = remember(channels, streamUrl, title) {
        channels.indexOfFirst { it.streamUrl == streamUrl || it.name == title }
    }
    val hasNextChannel = channels.isNotEmpty() && currentChannelIndex != -1
    val currentChannelObj = if (currentChannelIndex != -1) channels[currentChannelIndex] else null

    // Channel ID for Live Chat
    val channelId = remember(title) {
        title.replace("[^a-zA-Z0-9أ-ي]".toRegex(), "_").lowercase().ifEmpty { "default_channel" }
    }

    // Trigger Unity Interstitial Ad if enabled
    LaunchedEffect(Unit) {
        if (isGlobalAdsEnabled) {
            val activity = context as? Activity
            if (activity != null) {
                UnityAdsManager.showInterstitial(
                    activity = activity,
                    force = false,
                    onAdDismissed = {
                        Log.d("VideoPlayerView", "Unity Interstitial completed or dismissed")
                    },
                    onAdFailed = { err ->
                        Log.w("VideoPlayerView", "Unity Interstitial skipped/failed: $err")
                    }
                )
            }
        }
    }

    // Chat polling
    LaunchedEffect(isChatEnabledOnline) {
        if (!isChatEnabledOnline) isChatOpen = false
    }

    LaunchedEffect(channelId, isChatOpen) {
        if (isChatOpen) {
            while (true) {
                viewModel.fetchChatMessages(channelId)
                delay(3000L)
            }
        }
    }

    // Calculate current server URL based on index
    val currentServerUrl = remember(playingServers, currentServerIndex, streamUrl) {
        if (playingServers.isNotEmpty() && currentServerIndex in playingServers.indices) {
            playingServers[currentServerIndex].url
        } else {
            streamUrl
        }
    }

    // Initial Live Format Selection
    val initialFormat = remember(currentServerUrl, forcedFormat) {
        if (forcedFormat.isNotEmpty()) {
            forcedFormat
        } else {
            val lowerUrl = currentServerUrl.lowercase()
            if (lowerUrl.contains(".pdf") || lowerUrl.contains("pdf")) "pdf"
            else if (lowerUrl.contains(".m3u8") || lowerUrl.contains("m3u8")) "m3u8"
            else if (lowerUrl.contains(".ts") || lowerUrl.contains("ts") || lowerUrl.contains("output=ts")) "ts"
            else "ts"
        }
    }

    var currentFormat by remember(currentServerUrl, forcedFormat) { mutableStateOf(initialFormat) }
    LaunchedEffect(currentServerUrl, forcedFormat) { currentFormat = initialFormat }
    var pdfMimeFallbackIndex by remember { mutableIntStateOf(0) }

    // Dynamically formatted Stream URL
    val parsedStreamUrl = remember(currentServerUrl, currentFormat) {
        if (currentServerUrl.isEmpty() || forcedFormat.isEmpty()) return@remember currentServerUrl
        
        // Only attempt to change extensions if forcedFormat is explicitly provided and URL follows standard IPTV pattern
        if (currentServerUrl.contains("output=")) {
            val updated = currentServerUrl.replace("output=ts", "output=$currentFormat")
                .replace("output=m3u8", "output=$currentFormat")
                .replace("output=pdf", "output=$currentFormat")
            if (updated != currentServerUrl) return@remember updated
        }

        when (currentFormat) {
            "ts", "m3u8", "pdf" -> {
                val lowerUrl = currentServerUrl.lowercase()
                if (lowerUrl.endsWith(".$currentFormat") || lowerUrl.contains(".$currentFormat?")) {
                    currentServerUrl
                } else {
                    val lastSlash = currentServerUrl.lastIndexOf('/')
                    val dotIndex = currentServerUrl.lastIndexOf('.')
                    if (dotIndex > lastSlash && dotIndex != -1) {
                        val base = currentServerUrl.substring(0, dotIndex)
                        "$base.$currentFormat"
                    } else {
                        if (currentServerUrl.contains("?")) currentServerUrl.replace("?", ".$currentFormat?") else "$currentServerUrl.$currentFormat"
                    }
                }
            }
            else -> currentServerUrl
        }
    }

    // Aspect Ratio & Sound
    var playerAspectRatioMode by remember { mutableStateOf("16_9") }


    var isMuted by remember { mutableStateOf(false) }

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

    // Create Optimized Production Live ExoPlayer
    val exoPlayer = remember {
        val mediaSourceFactory = ExoPlayerDataSourceUtils.createMediaSourceFactory(context, userAgent)
        val loadControl = ExoPlayerDataSourceUtils.createLiveLoadControl()

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
    var reconnectTrigger by remember { mutableIntStateOf(0) }
    var retryCount by remember { mutableIntStateOf(0) }
    var isRecovering by remember { mutableStateOf(false) }
    var recoveryStatusText by remember { mutableStateOf<String?>(null) }

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
    val isPipModeState by viewModel.isPipMode.collectAsState()
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
            if (isLive) {
                ViewerCountOverlay(viewModel = viewModel, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp))
            }
        }
        return
    }

    // Load Stream Media Item using Optimized Live Configuration
    LaunchedEffect(parsedStreamUrl, reconnectTrigger) {
        try {
            if (!isViewDisposed) {
                LiveStreamLogger.resetSession()
                LiveStreamLogger.log("PLAYBACK_STARTED", "Loading URL: $parsedStreamUrl (Format: $currentFormat)")
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                
                val lowerUrl = parsedStreamUrl.lowercase()
                val fmt = currentFormat.lowercase().trim()
                val mimeType = when {
                    fmt == "ts" || lowerUrl.endsWith(".ts") || lowerUrl.contains(".ts?") || lowerUrl.contains(".ts/") -> "video/mp2t"
                    fmt == "m3u8" || lowerUrl.endsWith(".m3u8") || lowerUrl.contains(".m3u8?") || lowerUrl.contains(".m3u8/") -> "application/x-mpegURL"
                    fmt == "mpd" || lowerUrl.contains(".mpd") -> "application/dash+xml"
                    fmt == "normal" || fmt == "direct" -> {
                        when {
                            lowerUrl.contains(".m3u8") -> "application/x-mpegURL"
                            lowerUrl.contains(".ts") -> "video/mp2t"
                            lowerUrl.contains(".mpd") -> "application/dash+xml"
                            lowerUrl.contains(".mp4") -> "video/mp4"
                            lowerUrl.contains(".mkv") -> "video/x-matroska"
                            else -> null
                        }
                    }
                    lowerUrl.contains(".pdf") || fmt == "pdf" -> {
                        when (pdfMimeFallbackIndex % 3) {
                            0 -> "video/mp2t"
                            1 -> "application/x-mpegURL"
                            else -> null
                        }
                    }
                    else -> null
                }

                val mediaItem = ExoPlayerDataSourceUtils.createLiveMediaItem(parsedStreamUrl, mimeType)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                playbackError = null
                isBuffering = true
            }
        } catch (e: Exception) {
            LiveStreamLogger.e("STREAM_INIT_ERR", "Failed to start live stream: ${e.message}", e)
            playbackError = "فشل في تشغيل هذا البث المباشر"
        }
    }

    // Live Stream Conservative Telemetry & Non-Intrusive Watchdog
    LaunchedEffect(parsedStreamUrl, reconnectTrigger) {
        var lastPosition = -1L
        var lastPosChangeMs = android.os.SystemClock.elapsedRealtime()
        var bufferingStartMs = 0L

        while (true) {
            delay(2500L)
            if (!isViewDisposed && exoPlayer.playbackState != Player.STATE_IDLE && !isRecovering) {
                val now = android.os.SystemClock.elapsedRealtime()
                val currentPos = exoPlayer.currentPosition
                val bufferedPos = exoPlayer.bufferedPosition
                val isPlayingState = exoPlayer.isPlaying
                val isLoadingState = exoPlayer.isLoading
                val playbackState = exoPlayer.playbackState
                val isBufferingState = playbackState == Player.STATE_BUFFERING
                val isReadyState = playbackState == Player.STATE_READY
                val lastLoadSuccess = LiveStreamLogger.lastSuccessfulLoadMs
                val timeSinceLastLoad = if (lastLoadSuccess > 0) now - lastLoadSuccess else 0L

                // Periodic telemetry logging
                LiveStreamLogger.log(
                    "DIAGNOSTIC_METRICS",
                    "state=$playbackState, isPlaying=$isPlayingState, isLoading=$isLoadingState, pos=$currentPos, buf=$bufferedPos, sinceLastLoad=${timeSinceLastLoad}ms"
                )

                // Scenario A: Active playback is frozen on frame while in READY + PLAYING state
                if (isReadyState && isPlayingState) {
                    bufferingStartMs = 0L
                    if (currentPos != lastPosition) {
                        lastPosition = currentPos
                        lastPosChangeMs = now
                    } else if (currentPos > 0) {
                        val frozenDuration = now - lastPosChangeMs
                        if (frozenDuration >= 14000L && frozenDuration < 24000L) {
                            // Stage 1: Live edge resync
                            LiveStreamLogger.log("LIVE_POSITION_RESYNC", "Resyncing frozen playback ($frozenDuration ms) to live edge")
                            isRecovering = true
                            recoveryStatusText = "إعادة التزامن مع البث المباشر..."
                            exoPlayer.seekToDefaultPosition()
                            exoPlayer.prepare()
                        } else if (frozenDuration >= 24000L) {
                            // Stage 2: Full reconnect
                            LiveStreamLogger.log("RECOVERY_STARTED", "Reconnecting stream after extended playback freeze ($frozenDuration ms)")
                            isRecovering = true
                            recoveryStatusText = "إعادة الاتصال بالخادم..."
                            lastPosChangeMs = now
                            reconnectTrigger++
                        }
                    }
                }
                // Scenario B: Extended BUFFERING stall with NO network progress
                else if (isBufferingState) {
                    if (bufferingStartMs == 0L) bufferingStartMs = now
                    val bufferingDuration = now - bufferingStartMs
                    
                    // Do NOT interrupt if network is actively downloading segments/manifests
                    val isNetworkActive = isLoadingState || timeSinceLastLoad < 12000L
                    if (!isNetworkActive) {
                        if (bufferingDuration in 20000L..31999L) {
                            // Stage 1: Soft manifest prepare
                            LiveStreamLogger.log("PLAYLIST_REFRESH", "Soft re-preparing player to reload playlist ($bufferingDuration ms buffering, no net)")
                            isRecovering = true
                            recoveryStatusText = "تحديث قائمة البث..."
                            exoPlayer.prepare()
                        } else if (bufferingDuration >= 32000L) {
                            // Stage 2: Full reconnect
                            LiveStreamLogger.log("RECOVERY_STARTED", "Reconnecting stream after stalled buffering ($bufferingDuration ms)")
                            isRecovering = true
                            recoveryStatusText = "إعادة الاتصال بالخادم..."
                            bufferingStartMs = 0L
                            reconnectTrigger++
                        }
                    }
                } else {
                    bufferingStartMs = 0L
                }
            }
        }
    }

    // Attach Listeners, Telemetry Analytics & Unified Recovery Logic
    DisposableEffect(exoPlayer) {
        playerView.player = exoPlayer
        val analyticsListener = LiveStreamLogger.createAnalyticsListener()
        exoPlayer.addAnalyticsListener(analyticsListener)

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                when (state) {
                    Player.STATE_READY -> {
                        playbackError = null
                        isRecovering = false
                        recoveryStatusText = null
                        LiveStreamLogger.log("PLAYBACK_RESUMED", "Stream is playing smoothly (STATE_READY)")
                    }
                    Player.STATE_BUFFERING -> {
                        LiveStreamLogger.log("BUFFERING_STARTED", "Buffering stream data...")
                    }
                    Player.STATE_ENDED -> {
                        LiveStreamLogger.log("STREAM_ENDED", "Stream ended from server")
                        if (!isViewDisposed && !isRecovering) {
                            val isNetOk = isNetworkAvailable(context)
                            retryCount++
                            isBuffering = true
                            if (retryCount > 6) {
                                playbackError = if (isNetOk) "توقف البث من المصدر." else "انقطع الاتصال بالشبكة."
                            } else {
                                isRecovering = true
                                recoveryStatusText = "استئناف البث..."
                                coroutineScope.launch {
                                    val backoffDelay = minOf(1500L * (1 shl minOf(retryCount, 3)), 8000L)
                                    delay(if (isNetOk) backoffDelay else 3000L)
                                    if (!isViewDisposed) {
                                        reconnectTrigger++
                                        playbackError = null
                                    }
                                }
                            }
                        }
                    }
                    Player.STATE_IDLE -> {
                        LiveStreamLogger.log("PLAYBACK_STATE_IDLE", "ExoPlayer is IDLE")
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                LiveStreamLogger.log("ON_IS_PLAYING_CHANGED", "isPlaying=$playing")
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorCode = error.errorCode
                val errorMessage = error.message ?: "Unknown"
                LiveStreamLogger.e("PLAYER_ERROR", "ErrorCode: $errorCode (${error.errorCodeName}), Msg: $errorMessage", error)

                // BehindLiveWindowException recovery: Clean single live resync
                if (errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ||
                    error.cause?.javaClass?.name?.contains("BehindLiveWindowException") == true
                ) {
                    LiveStreamLogger.log("BEHIND_LIVE_WINDOW", "Behind live window detected. Catching up to current live edge.")
                    isRecovering = true
                    recoveryStatusText = "إعادة التزامن مع البث المباشر..."
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                    exoPlayer.play()
                    return
                }

                val isNetOk = isNetworkAvailable(context)
                if (!isViewDisposed) {
                    retryCount++
                    isBuffering = true
                    
                    if (parsedStreamUrl.lowercase().contains(".pdf") || currentFormat == "pdf") {
                        pdfMimeFallbackIndex++
                    }

                    // Inspect HTTP status codes
                    var httpStatusCode: Int? = null
                    var currThrowable: Throwable? = error.cause
                    while (currThrowable != null) {
                        if (currThrowable is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                            httpStatusCode = currThrowable.responseCode
                            break
                        }
                        currThrowable = currThrowable.cause
                    }

                    if (httpStatusCode != null) {
                        when (httpStatusCode) {
                            401, 403 -> {
                                LiveStreamLogger.log("AUTH_PROBLEM", "Stream auth or token expired (HTTP $httpStatusCode)")
                                playbackError = "انتهت صلاحية جلسة البث أو غير مصرح به (403)."
                                isBuffering = false
                                isRecovering = false
                                return
                            }
                            404 -> {
                                LiveStreamLogger.log("RESOURCE_NOT_FOUND", "Stream or segment not found on server (HTTP 404)")
                            }
                            429 -> {
                                LiveStreamLogger.log("RATE_LIMITED", "Rate limited by streaming server (HTTP 429)")
                            }
                            in 500..599 -> {
                                LiveStreamLogger.log("SERVER_ERROR", "Server internal error (HTTP $httpStatusCode)")
                            }
                        }
                    }

                    if (!isNetOk) {
                        LiveStreamLogger.log("NETWORK_PROBLEM", "No active internet connection during playback error")
                        playbackError = if (retryCount > 6) "لا يوجد اتصال بالشبكة." else null
                        coroutineScope.launch {
                            delay(3000L)
                            if (!isViewDisposed) {
                                isRecovering = true
                                recoveryStatusText = "إعادة الاتصال بالشبكة..."
                                reconnectTrigger++
                                playbackError = null
                            }
                        }
                    } else {
                        val backoffDelay = minOf(1500L * (1 shl minOf(retryCount, 3)), 8000L)
                        playbackError = if (retryCount > 6) "تعذر الاتصال بمصدر البث." else null
                        coroutineScope.launch {
                            delay(backoffDelay)
                            if (!isViewDisposed) {
                                isRecovering = true
                                recoveryStatusText = "إعادة المحاولة..."
                                reconnectTrigger++
                                playbackError = null
                            }
                        }
                    }
                } else {
                    playbackError = "خطأ في تشغيل البث المباشر."
                    isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            (playerView.parent as? ViewGroup)?.removeView(playerView)
            playerView.player = null
            exoPlayer.removeAnalyticsListener(analyticsListener)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && !isScreenLocked) {
            delay(5000L)
            showControls = false
        }
    }

    // Reset HUD indicators
    LaunchedEffect(volumeHudLevel, brightnessHudLevel) {
        if (volumeHudLevel != null || brightnessHudLevel != null) {
            delay(1500L)
            volumeHudLevel = null
            brightnessHudLevel = null
        }
    }

    // Infinite live pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val liveProgressFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "live_progress"
    )

    // Main Live Player Layout
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("cinematic_live_player")
    ) {
        // Player Video Canvas
        Box(
            modifier = Modifier
                .weight(if (isChatOpen || isDrawerOpen) 3f else 1f)
                .fillMaxHeight()
                .background(Color.Black)
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
                                onTap = { showControls = !showControls }
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
                                        // Brightness drag
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
                                        // Volume drag
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

            // On-Screen Gestures HUD (Brightness / Volume)
            if (brightnessHudLevel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
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
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
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
                androidx.compose.animation.AnimatedVisibility(
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

            // Buffering Spinner & Recovery Status Indicator
            if ((isBuffering || recoveryStatusText != null) && playbackError == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PlayerAccentRed,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    if (!recoveryStatusText.isNullOrEmpty()) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, PlayerAccentGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = recoveryStatusText!!,
                                color = PlayerAccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Error Alert Banner
            if (playbackError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181A)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .padding(20.dp)
                        .align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = PlayerAccentRed, modifier = Modifier.size(48.dp))
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
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PlayerAccentRed)
                            ) {
                                Text("إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // TOP & BOTTOM CONTROLS OVERLAY
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && !isScreenLocked,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Gradients
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
                            .height(140.dp)
                            .align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))))
                    )

                    // TOP BAR & SERVERS SLIDER
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Channel Info & Back
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PlayerIconButton(
                                    onClick = onClose,
                                    icon = Icons.Default.ArrowBack,
                                    contentDescription = "رجوع"
                                )
                                // Live Badge
                                Surface(
                                    color = PlayerAccentRed.copy(alpha = pulseAlpha),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Text(
                                            text = "مباشر LIVE",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
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
                                        text = currentChannelObj?.category ?: "بث تلفزيوني فائق الجودة",
                                        color = PlayerAccentGold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Top Action Icons (Channels Drawer, Chat, Audio commentary, Share, PiP, Lock)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Quick Channels Drawer Button
                                Button(
                                    onClick = {
                                        isDrawerOpen = !isDrawerOpen
                                        if (isDrawerOpen) isChatOpen = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDrawerOpen) PlayerAccentRed else Color.White.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("القنوات", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Live Chat Button
                                if (isChatEnabledOnline) {
                                    PlayerIconButton(
                                        onClick = {
                                            isChatOpen = !isChatOpen
                                            if (isChatOpen) isDrawerOpen = false
                                        },
                                        icon = Icons.Default.Chat,
                                        contentDescription = "دردشة البث المباشر",
                                        containerColor = if (isChatOpen) PlayerAccentRed else Color.Black.copy(alpha = 0.45f)
                                    )
                                }

                                // Audio / Multi-commentary Switcher
                                Box {
                                    val audioTracks = remember(exoPlayer.currentTracks) { extractAudioTracks(exoPlayer) }
                                    if (audioTracks.size > 1) {
                                        PlayerIconButton(
                                            onClick = { showAudioMenu = true },
                                            icon = Icons.Default.Audiotrack,
                                            contentDescription = "المعلق الصوتي",
                                            badgeText = "${audioTracks.size}"
                                        )
                                        DropdownMenu(
                                            expanded = showAudioMenu,
                                            onDismissRequest = { showAudioMenu = false },
                                            modifier = Modifier.background(Color(0xFF1E1E22))
                                        ) {
                                            Text(
                                                text = "اختر القناة الصوتية / المعلق:",
                                                color = PlayerAccentGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                            audioTracks.forEach { tr ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(tr.label, color = Color.White, fontSize = 12.sp)
                                                            if (tr.isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(16.dp))
                                                        }
                                                    },
                                                    onClick = {
                                                        applyTrackSelection(exoPlayer, tr, C.TRACK_TYPE_AUDIO)
                                                        showAudioMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Share Channel Link
                                if (isShareButtonVisible) {
                                    PlayerIconButton(
                                        onClick = {
                                            viewModel.generateShareLink(title, streamUrl) { link ->
                                                if (link.isNotEmpty()) {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(
                                                            Intent.EXTRA_TEXT,
                                                            "شاهد $title على تطبيق +90 البث المباشر المتميز من هنا:\n$link\n\nاضغط على الرابط أعلاه ليتم تشغيل القناة تلقائياً!"
                                                        )
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, null))
                                                } else {
                                                    Toast.makeText(context, "فشل إنشاء رابط المشاركة", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        icon = Icons.Default.Share,
                                        contentDescription = "مشاركة"
                                    )
                                }

                                // PiP Button
                                PlayerIconButton(
                                    onClick = { launchPictureInPicture(activity) },
                                    icon = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "صورة داخل صورة"
                                )

                                // Screen Lock Button
                                PlayerIconButton(
                                    onClick = {
                                        isScreenLocked = true
                                        showControls = true
                                    },
                                    icon = Icons.Default.Lock,
                                    contentDescription = "قفل الشاشة"
                                )

                                // Viewer Count (Real-time)
                                if (isLive) {
                                    ViewerCountOverlay(viewModel = viewModel)
                                }
                            }
                        }

                        // Horizontal Event Channels Switcher (linked channels)
                        if (playingEventChannels.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                                    border = BorderStroke(1.dp, PlayerAccentGold.copy(alpha = 0.4f)),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = PlayerAccentGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "القنوات:",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    itemsIndexed(playingEventChannels) { index, ch ->
                                        val isSelected = index == currentEventChannelIndex
                                        Surface(
                                            onClick = {
                                                if (!isSelected) {
                                                    viewModel.switchEventChannel(index)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) PlayerAccentGold else Color.Black.copy(alpha = 0.75f),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                                            )
                                        ) {
                                            Text(
                                                text = ch.name,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Horizontal Server Switcher Slider (SD, HD, FHD, 4K, Server 1, etc.)
                        if (playingServers.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                                    border = BorderStroke(1.dp, PlayerAccentGold.copy(alpha = 0.4f)),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dns,
                                            contentDescription = null,
                                            tint = PlayerAccentGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "السيرفرات:",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                LazyRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    itemsIndexed(playingServers) { index, srv ->
                                        val isSelected = index == currentServerIndex
                                        Surface(
                                            onClick = {
                                                if (!isSelected) {
                                                    viewModel.switchServer(index)
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) PlayerAccentRed else Color.Black.copy(alpha = 0.75f),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) PlayerAccentGold else Color.White.copy(alpha = 0.25f)
                                            ),
                                            shadowElevation = if (isSelected) 6.dp else 0.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .background(PlayerAccentGold, CircleShape)
                                                    )
                                                }
                                                Text(
                                                    text = srv.name.ifEmpty { "سيرفر ${index + 1}" },
                                                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                                if (srv.forcedFormat.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (isSelected) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = srv.forcedFormat.uppercase(),
                                                            color = if (isSelected) PlayerAccentGold else Color(0xFFCBD5E1),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // CENTER CHANNEL CONTROLLER (Previous Channel, Play/Pause, Next Channel)
                    Box(
                        modifier = Modifier.align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Previous Channel Button
                            if (hasNextChannel && currentChannelIndex > 0) {
                                PlayerIconButton(
                                    onClick = {
                                        val prevChan = channels[currentChannelIndex - 1]
                                        viewModel.startPlaying(prevChan.streamUrl, prevChan.name, isLive = true, userAgent = prevChan.userAgent)
                                    },
                                    icon = Icons.Default.SkipPrevious,
                                    contentDescription = "القناة السابقة",
                                    size = 48,
                                    iconSize = 26
                                )
                            }

                            // Main Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .background(PlayerAccentRed, CircleShape)
                                    .clickable {
                                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            // Next Channel Button
                            if (hasNextChannel && currentChannelIndex < channels.size - 1) {
                                PlayerIconButton(
                                    onClick = {
                                        val nextChan = channels[currentChannelIndex + 1]
                                        viewModel.startPlaying(nextChan.streamUrl, nextChan.name, isLive = true, userAgent = nextChan.userAgent)
                                    },
                                    icon = Icons.Default.SkipNext,
                                    contentDescription = "القناة التالية",
                                    size = 48,
                                    iconSize = 26
                                )
                            }
                        }
                    }

                    // BOTTOM BAR
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Animated Live Streaming Status Indicator Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PlayerAccentRed.copy(alpha = pulseAlpha), CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "بث مباشر فائق السرعة",
                                color = PlayerAccentRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(12.dp))

                            // Shimmer live progress track
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(liveProgressFraction)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PlayerAccentRed.copy(alpha = 0.2f), PlayerAccentRed)
                                            )
                                        )
                                )
                            }
                        }

                        // Bottom Actions Row (Format Selector, Aspect Ratio, Mute)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Format Selector (TS, M3U8, Direct) & Aspect Ratio
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Format Selector Dropdown
                                Box {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { showFormatMenu = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.SettingsInputComponent, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(14.dp))
                                            Text(
                                                text = "الصيغة: ${currentFormat.uppercase()}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showFormatMenu,
                                        onDismissRequest = { showFormatMenu = false },
                                        modifier = Modifier.background(Color(0xFF1E1E22))
                                    ) {
                                        if (forcedFormat.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text("الصيغة مجبرة من الإدارة: ${forcedFormat.uppercase()}", color = PlayerAccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                },
                                                onClick = {},
                                                enabled = false
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("بث مباشر (TS)", color = if (forcedFormat.isNotEmpty() && forcedFormat != "ts") Color.Gray else Color.White, fontSize = 12.sp)
                                                    if (currentFormat == "ts") Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                currentFormat = "ts"
                                                showFormatMenu = false
                                            },
                                            enabled = forcedFormat.isEmpty() || forcedFormat == "ts"
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("بث مباشر (M3U8)", color = if (forcedFormat.isNotEmpty() && forcedFormat != "m3u8") Color.Gray else Color.White, fontSize = 12.sp)
                                                    if (currentFormat == "m3u8") Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                currentFormat = "m3u8"
                                                showFormatMenu = false
                                            },
                                            enabled = forcedFormat.isEmpty() || forcedFormat == "m3u8"
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("بث مباشر (PDF)", color = if (forcedFormat.isNotEmpty() && forcedFormat != "pdf") Color.Gray else Color.White, fontSize = 12.sp)
                                                    if (currentFormat == "pdf") Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                currentFormat = "pdf"
                                                showFormatMenu = false
                                            },
                                            enabled = forcedFormat.isEmpty() || forcedFormat == "pdf"
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("رابط عادي (Direct / HTTP)", color = if (forcedFormat.isNotEmpty() && forcedFormat != "direct" && forcedFormat != "normal") Color.Gray else Color.White, fontSize = 12.sp)
                                                    if (currentFormat == "direct" || currentFormat == "normal") Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                currentFormat = "direct"
                                                showFormatMenu = false
                                            },
                                            enabled = forcedFormat.isEmpty() || forcedFormat == "direct" || forcedFormat == "normal"
                                        )
                                    }
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

                            // Right: Mute
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
                        }
                    }
                }
            }
        }

        // LIVE CHAT SIDE PANEL
        AnimatedVisibility(
            visible = isChatOpen && isChatEnabledOnline,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ChatPanel(viewModel = viewModel, channelId = channelId)
        }

        // CHANNELS DRAWER
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
        ) {
            ChannelsDrawer(
                viewModel = viewModel,
                onClose = { isDrawerOpen = false },
                onSelectChannel = { chan ->
                    viewModel.startPlaying(chan.streamUrl, chan.name, isLive = true, userAgent = chan.userAgent)
                    isDrawerOpen = false
                }
            )
        }
    }
}

@Composable
fun ChatPanel(viewModel: MainViewModel, channelId: String) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val prefs = remember(context) { context.getSharedPreferences("loop_live_prefs", Context.MODE_PRIVATE) }
    var senderName by remember { mutableStateOf(prefs.getString("chat_sender_name", "متابع") ?: "متابع") }
    var typedMessage by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = PlayerAccentRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("دردشة البث المباشر", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).background(Color.Green, CircleShape))
                Text("متصل", color = Color.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Sender Name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("اسمك:", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = senderName,
                onValueChange = {
                    val sanitized = it.take(15)
                    senderName = sanitized
                    prefs.edit().putString("chat_sender_name", sanitized).apply()
                },
                textStyle = TextStyle(color = PlayerAccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Messages List
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("لا توجد رسائل حالياً. كن أول من يشارك!", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chatMessages.size) { idx ->
                        val msg = chatMessages[idx]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(msg.sender, color = PlayerAccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                val timeStr = remember(msg.timestamp) {
                                    val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(msg.timestamp))
                                }
                                Text(timeStr, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(msg.text, color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Quick Emojis
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val emojis = listOf("🔥", "⚽", "👏", "😍", "😂", "👑", "👍")
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { typedMessage += emoji }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(emoji, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Input & Send
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = typedMessage,
                onValueChange = { typedMessage = it },
                placeholder = { Text("اكتب رسالة...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PlayerAccentRed,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color(0xFF151517),
                    unfocusedContainerColor = Color(0xFF151517),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 11.sp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (typedMessage.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(channelId, senderName, typedMessage.trim())
                        typedMessage = ""
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PlayerAccentRed)
            ) {
                Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ChannelsDrawer(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onSelectChannel: (Channel) -> Unit
) {
    val channels by viewModel.channels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.first().id
        }
    }

    val filteredChannels = remember(channels, selectedCategory, searchQuery) {
        channels.filter { chan ->
            val matchesCategory = selectedCategory == null || chan.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() || chan.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(Color(0xFF111114))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
            }
            Text("قائمة القنوات 📺", color = PlayerAccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث عن قناة...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PlayerAccentGold, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlayerAccentGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color(0xFF19191D),
                unfocusedContainerColor = Color(0xFF19191D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // Categories Filter
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories.size) { idx ->
                val cat = categories[idx]
                val isSelected = cat.id == selectedCategory
                Card(
                    modifier = Modifier.clickable { selectedCategory = cat.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PlayerAccentRed else Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = cat.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Channel list
        if (filteredChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد قنوات مطابقة", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredChannels.size) { idx ->
                    val chan = filteredChannels[idx]
                    var isFocused by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable()
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) PlayerAccentGold else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectChannel(chan) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = chan.name.take(1),
                                    color = PlayerAccentGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = chan.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
