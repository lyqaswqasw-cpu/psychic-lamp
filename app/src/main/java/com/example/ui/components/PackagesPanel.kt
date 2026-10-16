package com.example.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.model.*
import com.example.ui.utils.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request


@Composable
fun PackagesPanel(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // UI states
    var selectedCountry by rememberSaveable { mutableStateOf("") } // "" (none), "iq" (Iraq), "sa" (Saudi Arabia), or pkg id
    val channelsList by viewModel.packagesChannelsList.collectAsState()
    var isLoadingChannels by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    val publishedPackages by viewModel.publishedPackages.collectAsState()
    
    // Active player stream states
    val activeStreamUrl by viewModel.packagesActiveStreamUrl.collectAsState()
    val activeStreamName by viewModel.packagesActiveStreamName.collectAsState()
    val isPackagesPlayerFullScreen by viewModel.isPackagesPlayerFullScreen.collectAsState()
    
    // Fetch channels helper
    fun loadM3u(url: String, countryCode: String) {
        selectedCountry = countryCode
        isLoadingChannels = true
        errorMessage = null
        viewModel.setPackagesChannelsList(emptyList())
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string()
                
                if (bodyStr == null) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "تعذر تحميل البيانات، يرجى التحقق من اتصالك بالإنترنت."
                        isLoadingChannels = false
                    }
                    return@launch
                }
                
                val parsedList = mutableListOf<M3uChannel>()
                var currentName = ""
                var currentLogo = ""
                var currentGroup = ""
                
                bodyStr.lineSequence().forEach { lineRaw ->
                    val line = lineRaw.trim()
                    if (line.startsWith("#EXTINF:")) {
                        // Extract tvg-logo
                        val logoRegex = """tvg-logo="([^"]+)"""".toRegex()
                        currentLogo = logoRegex.find(line)?.groupValues?.get(1) ?: ""
                        
                        // Extract group-title
                        val groupRegex = """group-title="([^"]+)"""".toRegex()
                        currentGroup = groupRegex.find(line)?.groupValues?.get(1) ?: ""
                        
                        // Extract name
                        val commaIndex = line.lastIndexOf(',')
                        currentName = if (commaIndex != -1) {
                            line.substring(commaIndex + 1).trim()
                        } else {
                            "قناة بث مباشر"
                        }
                    } else if (line.isNotEmpty() && !line.startsWith("#")) {
                        if (currentName.isEmpty()) {
                            currentName = "قناة بدون اسم"
                        }
                        parsedList.add(
                            M3uChannel(
                                name = currentName,
                                url = line,
                                logoUrl = currentLogo.ifEmpty { null },
                                group = currentGroup.ifEmpty { null }
                            )
                        )
                        currentName = ""
                        currentLogo = ""
                        currentGroup = ""
                    }
                }
                
                withContext(Dispatchers.Main) {
                    viewModel.setPackagesChannelsList(parsedList)
                    isLoadingChannels = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "فشل في الاتصال بمزود الخدمة: ${e.localizedMessage}"
                    isLoadingChannels = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Upper Video Player Card (مشغل علوي احترافي)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MidnightGrey, RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                val streamUrl = activeStreamUrl
                if (streamUrl != null && !isPackagesPlayerFullScreen) {
                    PackagesPlayerView(
                        streamUrl = streamUrl,
                        title = activeStreamName,
                        channels = channelsList,
                        onSelectChannel = { chan ->
                            viewModel.setPackagesActiveStream(chan.url, chan.name)
                        },
                        isFullScreen = false,
                        onToggleFullScreen = {
                            viewModel.setPackagesPlayerFullScreen(true)
                        },
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel
                    )
                } else if (streamUrl != null && isPackagesPlayerFullScreen) {
                    // Placeholder when playing in full screen
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "جاري التشغيل في وضع ملء الشاشة...",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                // Placeholder when no channel is playing
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(MidnightGrey, ObsidianBlack.copy(alpha = 0.95f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(LoopRed.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, LoopRed.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = LoopRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "مشغل الباقات التلفزيونية الذكي",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختر باقة العراق 🇮🇶 أو السعودية 🇸🇦 أو الباقات المنشورة بالأسفل لتشغيل البث الفوري بجودة عالية.",
                            color = SoftGreyText,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // Divider
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        // 2. Packages Selector / Active Channels List
        if (selectedCountry.isEmpty()) {
            // Package grid selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "اختر باقة القنوات العربية:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Iraq package card
                    item {
                        PackageSelectionCard(
                            id = "iq",
                            name = "جمهورية العراق 🇮🇶",
                            imageUrl = "https://flagcdn.com/w320/iq.png",
                            description = "قنوات وطنية ورياضية",
                            m3uUrl = "https://iptv-org.github.io/iptv/countries/iq.m3u",
                            isSelected = selectedCountry == "iq",
                            onClick = { url, id -> loadM3u(url, id) }
                        )
                    }

                    // Saudi Arabia package card
                    item {
                        PackageSelectionCard(
                            id = "sa",
                            name = "المملكة العربية السعودية 🇸🇦",
                            imageUrl = "https://flagcdn.com/w320/sa.png",
                            description = "قنوات حكومية ودينية",
                            m3uUrl = "https://iptv-org.github.io/iptv/countries/sa.m3u",
                            isSelected = selectedCountry == "sa",
                            onClick = { url, id -> loadM3u(url, id) }
                        )
                    }

                    // Published dynamic packages
                    items(publishedPackages) { pkg ->
                        PackageSelectionCard(
                            id = pkg.id,
                            name = pkg.name,
                            imageUrl = pkg.imageUrl,
                            description = pkg.description.ifEmpty { "باقة قنوات منشورة أونلاين" },
                            m3uUrl = pkg.m3uUrl,
                            isSelected = selectedCountry == pkg.id,
                            onClick = { url, id -> loadM3u(url, id) }
                        )
                    }
                }
            }
        } else {
            // Selected package channel list
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Package title header with Back option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { selectedCountry = "" },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "عودة للباقات",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Text(
                            text = when(selectedCountry) {
                                "iq" -> "الباقة العراقية 🇮🇶"
                                "sa" -> "الباقة السعودية 🇸🇦"
                                else -> publishedPackages.find { it.id == selectedCountry }?.name ?: "باقة منشورة"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "القنوات (${channelsList.size})",
                        color = GoldGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Search box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن القناة بالاسم...", color = SoftGreyText, fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(16.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoopRed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = MidnightGrey,
                        unfocusedContainerColor = MidnightGrey
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Main channels list
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (isLoadingChannels) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = LoopRed, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("جاري تحميل قائمة القنوات فورا...", color = SoftGreyText, fontSize = 10.sp)
                            }
                        }
                    } else if (errorMessage != null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LoopRed, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(errorMessage!!, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (selectedCountry == "iq") {
                                            loadM3u("https://iptv-org.github.io/iptv/countries/iq.m3u", "iq")
                                        } else {
                                            loadM3u("https://iptv-org.github.io/iptv/countries/sa.m3u", "sa")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تحديث وإعادة المحاولة", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        val filteredList = channelsList.filter {
                            it.name.contains(searchQuery, ignoreCase = true)
                        }

                        if (filteredList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لم يتم العثور على قنوات بهذا الاسم.", color = SoftGreyText, fontSize = 11.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredList) { channel ->
                                    val isPlayingThis = activeStreamUrl == channel.url
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (isPlayingThis) LoopRed.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.04f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.setPackagesActiveStream(channel.url, channel.name)
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPlayingThis) LoopRed.copy(alpha = 0.08f) else MidnightGrey
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Channel logo
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(ObsidianBlack, RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SafeAsyncImage(
                                                    model = channel.logoUrl,
                                                    contentDescription = channel.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }

                                            // Channel info
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = channel.name,
                                                    color = if (isPlayingThis) LoopRed else Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = channel.group ?: "بث حي ومباشر",
                                                    color = SoftGreyText,
                                                    fontSize = 8.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Active play state icon
                                            Icon(
                                                imageVector = if (isPlayingThis) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                                contentDescription = null,
                                                tint = if (isPlayingThis) LoopRed else SoftGreyText.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
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
    }
}
}

@Composable
fun PackageSelectionCard(
    id: String,
    name: String,
    imageUrl: String,
    description: String,
    m3uUrl: String,
    isSelected: Boolean,
    onClick: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(
                1.5.dp,
                if (isSelected) LoopRed else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                onClick(m3uUrl, id)
            },
        colors = CardDefaults.cardColors(containerColor = MidnightGrey),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldGlow, CircleShape)
                        .shadow(4.dp, CircleShape)
                ) {
                    SafeAsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = description,
                        color = SoftGreyText,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PackagesPlayerView(
    streamUrl: String,
    title: String,
    channels: List<M3uChannel>,
    onSelectChannel: (M3uChannel) -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val autoLandscape by viewModel.autoLandscape.collectAsState()
    
    DisposableEffect(isFullScreen) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = if (autoLandscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        
        onDispose {
            if (isFullScreen) {
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
    }
    
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playerAspectRatioMode by remember { mutableStateOf("fit") }
    var isDrawerOpen by remember { mutableStateOf(false) }

    // Setup ExoPlayer with fast startup & wake mode
    val exoPlayer = remember {
        val mediaSourceFactory = ExoPlayerDataSourceUtils.createMediaSourceFactory(context)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1500,   // minBufferMs - lower for faster launch
                8000,   // maxBufferMs
                500,    // bufferForPlaybackMs - Extremely fast startup
                1000    // bufferForPlaybackAfterRebufferMs
            )
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Auto load stream when changed
    LaunchedEffect(streamUrl) {
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
            playbackError = null
            isBuffering = true
        } catch (e: Exception) {
            playbackError = "فشل تشغيل القناة"
        }
    }

    // Attach listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    playbackError = null
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "خطأ في البث: ${error.localizedMessage}"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Auto hide controls after 5 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(5000L)
            showControls = false
        }
    }

    // Physical back button handling in Full Screen
    if (isFullScreen) {
        BackHandler {
            onToggleFullScreen()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                }
            },
            update = { view ->
                view.resizeMode = if (playerAspectRatioMode == "fill") {
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                } else {
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { showControls = !showControls }
        )

        // Buffering indicator
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = GoldGlow,
                modifier = Modifier.size(48.dp).align(Alignment.Center)
            )
        }

        // Error message
        if (playbackError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, LoopRed.copy(alpha = 0.5f)),
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = LoopRed, modifier = Modifier.size(32.dp))
                    Text(playbackError!!, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LoopRed)
                    ) {
                        Text("إعادة المحاولة", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }

        // Custom Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250))
        ) {
            // Dark subtle gradient backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Close / Back button
                        IconButton(
                            onClick = {
                                if (isFullScreen) onToggleFullScreen() else viewModel.setPackagesActiveStream(null, "")
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Three lines menu button for Packages
                        IconButton(
                            onClick = { isDrawerOpen = !isDrawerOpen },
                            modifier = Modifier
                                .background(if (isDrawerOpen) LoopRed else Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "قائمة قنوات الباقة",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "بث مباشر: $title",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Bottom controls deck (Symmetrical play bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Sizing Toggle (Aspect Ratio)
                    IconButton(
                        onClick = {
                            playerAspectRatioMode = if (playerAspectRatioMode == "fit") "fill" else "fit"
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (playerAspectRatioMode == "fit") Icons.Default.AspectRatio else Icons.Default.Fullscreen,
                            contentDescription = "أبعاد الفيديو",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 2. Play / Pause
                    IconButton(
                        onClick = {
                            isPlaying = !isPlaying
                            if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                        },
                        modifier = Modifier.background(LoopRed, CircleShape).size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "تشغيل/إيقاف مؤقت",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 3. Volume Mute
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "كتم الصوت",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 4. Fullscreen Toggle
                    IconButton(
                        onClick = {
                            onToggleFullScreen()
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape).size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "ملء الشاشة",
                            tint = GoldGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Side drawer displaying Package Channels with search bar (Three lines menu)
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .align(Alignment.CenterStart)
                .background(MidnightGrey.copy(alpha = 0.95f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isDrawerOpen = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                    Text("قنوات الباقة", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Local simple search field inside drawer removed as requested

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(channels) { channel ->
                        val isCurrentChannel = channel.url == streamUrl
                        Card(
                            onClick = {
                                onSelectChannel(channel)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentChannel) LoopRed.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentChannel) LoopRed.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(ObsidianBlack, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SafeAsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Text(
                                    text = channel.name,
                                    color = if (isCurrentChannel) LoopRed else Color.White,
                                    fontSize = 11.sp,
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
}
