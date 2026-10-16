import re

with open("app/src/main/java/com/example/ui/components/YouTubeScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update Immersive Sticky Mode
immersive_old = """    // Enforce Immediate Landscape & Immersive Mode when entering
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive Sticky Mode
        activity?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }"""

immersive_new = """    // Enforce Immediate Landscape & Strict Immersive Mode when entering
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        activity?.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }"""

content = content.replace(immersive_old, immersive_new)

# 2. Replace the whole overlay (from // 2. Gesture to // 7. Error Dialog)
start_str = "            // 2. Gesture / Tap Detection Overlay for Double-Tap Rewind/Forward and Toggle Controls"
end_str = "            // 7. Error Dialog (If video is un-embeddable or blocked)"
start_idx = content.find(start_str)
end_idx = content.find(end_str)

overlay_new = """            // 2. Legendary Overlay (Controls & Gestures)
            Box(modifier = Modifier.fillMaxSize()) {
                // Gesture Detection Layer (Always active, blocks touches from YouTubePlayerView)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isScreenLocked) {
                            detectTapGestures(
                                onTap = {
                                    showControls = !showControls
                                },
                                onDoubleTap = { offset ->
                                    if (!isScreenLocked) {
                                        val screenWidth = size.width
                                        if (offset.x < screenWidth / 2) {
                                            // Rewind 10s
                                            val newTime = (currentTimeSec - 10f).coerceAtLeast(0f)
                                            currentTimeSec = newTime
                                            youTubePlayerRef?.seekTo(newTime)
                                            doubleTapFeedback = "⏪ 10ث"
                                        } else {
                                            // Fast Forward 10s
                                            val newTime = (currentTimeSec + 10f).coerceAtMost(durationSec)
                                            currentTimeSec = newTime
                                            youTubePlayerRef?.seekTo(newTime)
                                            doubleTapFeedback = "10ث ⏩"
                                        }
                                    }
                                }
                            )
                        }
                )

                // Double Tap Feedback (Legendary Pill)
                AnimatedVisibility(
                    visible = doubleTapFeedback != null,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 1.2f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = doubleTapFeedback ?: "",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Buffering Indicator
                if (isBuffering && errorMessage == null) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF0044),
                        strokeWidth = 4.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                    )
                }

                // Controls Overlay with Smooth Animations
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Top Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.3f)
                                .align(Alignment.TopCenter)
                                .background(Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)
                                ))
                        )
                        // Bottom Gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.4f)
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                                ))
                        )
                        
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = video.title,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = video.channelTitle,
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            // Top Quick Actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendaryTopButton(label = aspectRatioMode, icon = Icons.Default.AspectRatio) {
                                    aspectRatioMode = when (aspectRatioMode) {
                                        "16:9" -> "FIT"
                                        "FIT" -> "FILL"
                                        "FILL" -> "ZOOM"
                                        else -> "16:9"
                                    }
                                }
                                LegendaryTopButton(label = playbackSpeed, icon = Icons.Default.Speed) { showSpeedDialog = true }
                                LegendaryTopButton(label = videoQuality, icon = Icons.Default.HighQuality) { showQualityDialog = true }
                                
                                IconButton(
                                    onClick = { isScreenLocked = !isScreenLocked },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(if (isScreenLocked) Color(0xFFFF0044).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "قفل",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Center Controls
                        if (!isScreenLocked) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.5f),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rewind
                                IconButton(
                                    onClick = {
                                        val newTime = (currentTimeSec - 10f).coerceAtLeast(0f)
                                        currentTimeSec = newTime
                                        youTubePlayerRef?.seekTo(newTime)
                                        doubleTapFeedback = "⏪ 10ث"
                                    },
                                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                                
                                // Play/Pause
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            youTubePlayerRef?.pause()
                                            isPlaying = false
                                        } else {
                                            youTubePlayerRef?.play()
                                            isPlaying = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFFFF0044), Color(0xFFAA0022))))
                                        .shadow(16.dp, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                
                                // Forward
                                IconButton(
                                    onClick = {
                                        val newTime = (currentTimeSec + 10f).coerceAtMost(durationSec)
                                        currentTimeSec = newTime
                                        youTubePlayerRef?.seekTo(newTime)
                                        doubleTapFeedback = "10ث ⏩"
                                    },
                                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Icon(Icons.Default.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                        }

                        // Bottom Controls
                        if (!isScreenLocked) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 32.dp, vertical = 24.dp)
                            ) {
                                // Time & Tools
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${formatSeconds(if(isSeeking) seekProgressSec else currentTimeSec)} / ${formatSeconds(durationSec)}",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                        IconButton(onClick = {
                                            isMuted = !isMuted
                                            if (isMuted) youTubePlayerRef?.mute() else youTubePlayerRef?.unMute()
                                        }, modifier = Modifier.size(36.dp)) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = if (isMuted) Color(0xFFFF0044) else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        IconButton(onClick = {
                                            val curOrient = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            if (curOrient == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            } else {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            }
                                        }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Legendary Slider
                                val currentPos = if (isSeeking) seekProgressSec else currentTimeSec
                                val progressFrac = if (durationSec > 0f) (currentPos / durationSec).coerceIn(0f, 1f) else 0f
                                
                                Slider(
                                    value = progressFrac,
                                    onValueChange = { frac ->
                                        isSeeking = true
                                        seekProgressSec = frac * durationSec
                                    },
                                    onValueChangeFinished = {
                                        isSeeking = false
                                        currentTimeSec = seekProgressSec
                                        youTubePlayerRef?.seekTo(seekProgressSec)
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFF0044),
                                        activeTrackColor = Color(0xFFFF0044),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(28.dp)
                                )
                            }
                        }
                    }
                }
            }

"""
if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + overlay_new + content[end_idx:]
else:
    print("Could not find start or end index for overlay")

# 3. Rename VideoJsTopButton to LegendaryTopButton
button_old = """@Composable
fun VideoJsTopButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}"""

button_new = """@Composable
fun LegendaryTopButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(text = label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}"""

content = content.replace(button_old, button_new)

with open("app/src/main/java/com/example/ui/components/YouTubeScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)
print("Updated successfully")
