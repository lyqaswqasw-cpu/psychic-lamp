package com.example.ui

import android.app.Activity
import android.app.DownloadManager
import android.content.ClipboardManager
import android.os.Environment
import android.database.Cursor
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.composed
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import coil.compose.AsyncImage
import com.example.model.*
import com.example.data.ChatMessage
import com.example.R
import com.example.ui.components.VideoPlayerView
import com.example.ui.components.MoviePlayerView
import com.example.ui.components.SplashLoadingScreen
import com.example.ui.components.ContentLoadingPathsView
import com.example.ui.components.IntroSplashScreen
import com.example.ui.components.PackagesPanel
import com.example.ui.components.PackagesPlayerView
import com.example.ui.components.VpnBlockedScreen
import com.example.ui.components.AppVersionGateScreen
import com.example.utils.VpnSecurityManager
import com.example.utils.AppVersionStatus
import com.example.utils.AppVersionConfig
import com.example.ui.components.*
import com.example.ui.utils.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.data.ChatService

@Composable
fun MainApp(viewModel: MainViewModel) {
    val showSplashScreen by viewModel.showSplashScreen.collectAsState()
    val accessCode by viewModel.accessCode.collectAsState()
    val playingStreamUrl by viewModel.playingStreamUrl.collectAsState()
    val playingTitle by viewModel.playingTitle.collectAsState()
    val playingIsLive by viewModel.playingIsLive.collectAsState()
    val playingUserAgent by viewModel.playingUserAgent.collectAsState()
    val autoLandscape by viewModel.autoLandscape.collectAsState()
    val showAiHelpChat by viewModel.showAiHelpChat.collectAsState()
    val isGlobalAdsEnabled by viewModel.isGlobalAdsEnabled.collectAsState()
    val showRemoveAdsIcon by viewModel.showRemoveAdsIcon.collectAsState()
    val isEffectiveVpnBlocked by viewModel.isEffectiveVpnBlocked.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val isChannelsLoading by viewModel.isChannelsLoading.collectAsState()
    val isMoviesLoading by viewModel.isMoviesLoading.collectAsState()
    val isSeriesLoading by viewModel.isSeriesLoading.collectAsState()
    val codeUpdateRequiredMessage by viewModel.codeUpdateRequiredMessage.collectAsState()
    val isContentUpdating by viewModel.isContentUpdating.collectAsState()
    val homeConfig by viewModel.homeConfig.collectAsState()

    val context = LocalContext.current
    var splashCompleted by remember { mutableStateOf(false) }

    // Instant proactive cutoff if VPN is detected and blocking is active
    LaunchedEffect(isEffectiveVpnBlocked) {
        if (isEffectiveVpnBlocked) {
            viewModel.stopPlaying()
            viewModel.setPackagesPlayerFullScreen(false)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showSplashScreen) {
                IntroSplashScreen(onFinished = { /* Controlled by ViewModel */ })
            } else if (accessCode == null) {
                splashCompleted = false
                LoginScreen(viewModel = viewModel)
            } else if (isContentUpdating) {
                ContentLoadingPathsView(
                    isLoading = isLoading,
                    isChannelsLoading = isChannelsLoading,
                    isMoviesLoading = isMoviesLoading,
                    isSeriesLoading = isSeriesLoading,
                    onFinished = { viewModel.finishContentUpdate() }
                )
            } else if (!splashCompleted) {
                SplashLoadingScreen(
                    isLoading = isLoading,
                    isChannelsLoading = isChannelsLoading,
                    isMoviesLoading = isMoviesLoading,
                    isSeriesLoading = isSeriesLoading,
                    onFinished = { splashCompleted = true }
                )
            } else {
                DashboardScreen(viewModel = viewModel)
            }

            // High Quality Cinematic Video Player View
            playingStreamUrl?.let { url ->
                if (playingIsLive) {
                    VideoPlayerView(
                        streamUrl = url,
                        title = playingTitle,
                        isLive = true,
                        onClose = { viewModel.stopPlaying() },
                        modifier = Modifier.fillMaxSize(),
                        autoLandscape = autoLandscape,
                        userAgent = playingUserAgent,
                        viewModel = viewModel
                    )
                } else {
                    MoviePlayerView(
                        streamUrl = url,
                        title = playingTitle,
                        onClose = { viewModel.stopPlaying() },
                        modifier = Modifier.fillMaxSize(),
                        autoLandscape = autoLandscape,
                        userAgent = playingUserAgent,
                        viewModel = viewModel
                    )
                }
            }

            // High Quality Packages Video Player View
            val isPackagesPlayerFullScreen by viewModel.isPackagesPlayerFullScreen.collectAsState()
            val packagesActiveStreamUrl by viewModel.packagesActiveStreamUrl.collectAsState()
            val packagesActiveStreamName by viewModel.packagesActiveStreamName.collectAsState()
            val packagesChannelsList by viewModel.packagesChannelsList.collectAsState()

            if (isPackagesPlayerFullScreen && packagesActiveStreamUrl != null) {
                PackagesPlayerView(
                    streamUrl = packagesActiveStreamUrl!!,
                    title = packagesActiveStreamName,
                    channels = packagesChannelsList,
                    onSelectChannel = { chan ->
                        viewModel.setPackagesActiveStream(chan.url, chan.name)
                    },
                    isFullScreen = true,
                    onToggleFullScreen = {
                        viewModel.setPackagesPlayerFullScreen(false)
                    },
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
            }

            if (showAiHelpChat) {
                AiHelpChatScreen(
                    viewModel = viewModel,
                    onClose = { viewModel.setAiHelpChatOpen(false) }
                )
            }

            // Ultra-strict VPN Blocker Barrier: locks application immediately upon VPN detection
            if (isEffectiveVpnBlocked) {
                VpnBlockedScreen(
                    onVpnResolved = {
                        VpnSecurityManager.forceCheck(context)
                    }
                )
            }

            // Strict Remote App Version Control & Maintenance Barrier (Top-level Security Gate)
            val appVersionStatus by viewModel.appVersionStatus.collectAsState()
            val isVersionGateBypassed by viewModel.isVersionGateBypassed.collectAsState()
            if (!isVersionGateBypassed && (appVersionStatus is AppVersionStatus.Maintenance || appVersionStatus is AppVersionStatus.UpdateRequired)) {
                AppVersionGateScreen(
                    status = appVersionStatus,
                    viewModel = viewModel,
                    onRetryCheck = {
                        viewModel.checkAppVersionStatus().join()
                        viewModel.isAppAccessAllowed()
                    },
                    onBypassAndEnter = {
                        viewModel.bypassVersionGateAsDeveloper()
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val codeUpdateRequiredMessage by viewModel.codeUpdateRequiredMessage.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val series by viewModel.series.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val selectedMovie by viewModel.selectedMovie.collectAsState()
    val selectedSeries by viewModel.selectedSeries.collectAsState()

    var activeTab by remember { mutableStateOf("home") } // "home", "channels", "movies", "series", "favorites", "chat", "packages"
    var isDrawerOpen by remember { mutableStateOf(false) }
    var showRemoveAdsScreen by remember { mutableStateOf(false) }
    var librarySubTab by remember { mutableStateOf("movies") } // "movies", "series"
    val events by viewModel.events.collectAsState()

    val showChatTabOnline by viewModel.showChatTab.collectAsState()
    val showEventsTabOnline by viewModel.showEventsTab.collectAsState()
    val isGlobalAdsEnabled by viewModel.isGlobalAdsEnabled.collectAsState()
    val showRemoveAdsIcon by viewModel.showRemoveAdsIcon.collectAsState()
    val showPackagesSectionOnline by viewModel.showPackagesSection.collectAsState()

    LaunchedEffect(activeAccount, showChatTabOnline, showEventsTabOnline, showPackagesSectionOnline) {
        val allowedTypes = activeAccount?.allowedTypes ?: listOf("live", "movies", "series")
        val availableTabs = buildList {
            add("home")
            if (allowedTypes.contains("live")) {
                add("channels")
                if (showEventsTabOnline) add("events")
            }
            add("library")
            add("favorites")
            if (showPackagesSectionOnline) add("packages")
            if (showChatTabOnline) add("chat")
        }
        if (!availableTabs.contains(activeTab) && availableTabs.isNotEmpty()) {
            activeTab = availableTabs.first()
        }
    }

    // Real quality-checking dialog and download parameters
    var showQualityDialog by remember { mutableStateOf(false) }
    var detectedQualities by remember { mutableStateOf<List<String>>(emptyList()) }
    var isCheckingQualities by remember { mutableStateOf(false) }
    var pendingDownloadData by remember { mutableStateOf<Pair<String, String>?>(null) } // Title, StreamUrl
    var pendingPosterUrl by remember { mutableStateOf("") }
    var pendingId by remember { mutableStateOf("") }
    var pendingDownloadType by remember { mutableStateOf("movie") }
    var pendingSeriesTitle by remember { mutableStateOf("") }
    var pendingEpisodeNumber by remember { mutableStateOf(0) }
    var pendingSeasonNumber by remember { mutableStateOf(0) }
    var pendingDuration by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    // Smooth background automated local downloading tracking blocks
    val processingDownloadItem by viewModel.downloadingItem.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadPhase by viewModel.downloadPhase.collectAsState()
    val activeDownloadState by viewModel.activeDownloadState.collectAsState()
    val savedOfflineList by viewModel.downloads.collectAsState()
    var detectedDownloadQuality by remember { mutableStateOf("") }

    LaunchedEffect(pendingDownloadData) {
        val data = pendingDownloadData
        if (data != null) {
            isCheckingQualities = true
            try {
                detectedQualities = discoverAvailableQualities(data.second)
            } catch (e: Exception) {
                detectedQualities = listOf("1080p Full HD (الدقة الفعلية المتاحة)")
            } finally {
                isCheckingQualities = false
            }
        }
    }

    val isEventChannelsPlayerActive by viewModel.isEventChannelsPlayerActive.collectAsState()
    val context = LocalContext.current
    val autoLandscape by viewModel.autoLandscape.collectAsState()
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(autoLandscape) {
        activity?.requestedOrientation = if (autoLandscape) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Effect showing Toast when download status changes
    LaunchedEffect(downloadPhase) {
        if (downloadPhase == "completed") {
            Toast.makeText(context, "تم تحميل الفيديو بنجاح وحفظه في قسم التحميلات!", Toast.LENGTH_LONG).show()
        } else if (downloadPhase == "failed") {
            Toast.makeText(context, "عذراً، فشل تحميل هذا الفيديو. يرجى التحقق من اتصالك بالإنترنت والسرعة.", Toast.LENGTH_LONG).show()
        }
    }

    // Tab categories filter mapping
    val currentTabCategoryType = when (activeTab) {
        "channels" -> "channel"
        "events" -> "exclusive"
        "library" -> if (librarySubTab == "series") "series" else "movie"
        "movies" -> "movie"
        "series" -> "series"
        else -> "all"
    }

    val filteredCategories = remember(categories, currentTabCategoryType) {
        if (currentTabCategoryType == "all") {
            emptyList()
        } else {
            categories.filter { it.type == currentTabCategoryType }
        }
    }

    // Safely reset chosen category when tab shifts to avoid showing category items from other tabs
    LaunchedEffect(activeTab, librarySubTab) {
        viewModel.selectCategory("all")
        viewModel.updateSearchQuery("")
        if (activeTab != "channels" && activeTab != "events" && viewModel.playingIsLive.value && viewModel.playingStreamUrl.value != null) {
            viewModel.setLivePlayerFullScreen(true)
        }
        if (activeTab == "events") {
            viewModel.loadEventChannels()
        }
        if (activeTab == "library" || activeTab == "movies" || activeTab == "series") {
            viewModel.loadMoviesOnDemand()
            viewModel.loadSeriesOnDemand()
        }
    }

    // Filter contents dynamically based on selected tabs/searches
    val categoryName = remember(categories, selectedCategory) {
        categories.find { it.id == selectedCategory }?.name
    }

    val filteredChannels = remember(channels, selectedCategory, categoryName, searchQuery) {
        channels.filter {
            val matchesCategory = selectedCategory == "all" || 
                    it.category.trim().equals(selectedCategory.trim(), ignoreCase = true) || 
                    (categoryName != null && it.category.trim().equals(categoryName.trim(), ignoreCase = true))
            matchesCategory && (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
        }
    }
    val filteredEvents = remember(events, searchQuery) {
        events.filter {
            searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredMovies = remember(movies, selectedCategory, categoryName, searchQuery) {
        movies.filter {
            val matchesCategory = selectedCategory == "all" || 
                    it.category.trim().equals(selectedCategory.trim(), ignoreCase = true) || 
                    (categoryName != null && it.category.trim().equals(categoryName.trim(), ignoreCase = true))
            matchesCategory && (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
        }
    }
    val filteredSeries = remember(series, selectedCategory, categoryName, searchQuery) {
        series.filter {
            val matchesCategory = selectedCategory == "all" || 
                    it.category.trim().equals(selectedCategory.trim(), ignoreCase = true) || 
                    (categoryName != null && it.category.trim().equals(categoryName.trim(), ignoreCase = true))
            matchesCategory && (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
        }
    }

    // Favorites mapping
    val favoritesSet by viewModel.favorites.collectAsState()
    val favoriteChannels = remember(channels, favoritesSet) { channels.filter { favoritesSet.contains(it.id) } }
    val favoriteMovies = remember(movies, favoritesSet) { movies.filter { favoritesSet.contains(it.id) } }
    val favoriteSeries = remember(series, favoritesSet) { series.filter { favoritesSet.contains(it.id) } }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        // Update Required Banner
        codeUpdateRequiredMessage?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 60.dp)
                    .padding(horizontal = 16.dp)
                    .zIndex(100f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF5252))
                    .clickable { viewModel.applyCodeUpdate() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = message, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "إعادة إدخال الكود", color = Color.White, fontSize = 12.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                }
            }
        }
        Scaffold(
        topBar = {
            if (activeTab != "profile" && activeTab != "reels" && activeTab != "chat" && !(activeTab == "events" && isEventChannelsPlayerActive)) {
                if (!isLandscape) {
                // Minimal header top layout shown in portrait
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(vertical = 4.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Left side: Three lines menu button for Settings Drawer
                                IconButton(
                                    onClick = { isDrawerOpen = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .tvFocusable(shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "القائمة الجانبية",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                if (showRemoveAdsIcon) {
                                    // No Ads Premium Button
                                    IconButton(
                                        onClick = { showRemoveAdsScreen = true },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .tvFocusable(shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "بدون إعلانات",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Center: +90 Branding
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+90",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            // Right side: Notifications, Favorites and Telegram
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val showNotificationIcon by viewModel.showNotificationIcon.collectAsState()
                                if (showNotificationIcon) {
                                    // Notification Bell with Badge
                                    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
                                    
                                    IconButton(
                                        onClick = { 
                                            activeTab = "notifications"
                                            viewModel.markNotificationsAsRead()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (activeTab == "notifications") GoldGlow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                            .tvFocusable(shape = CircleShape)
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "الإشعارات",
                                                tint = if (activeTab == "notifications") GoldGlow else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            
                                            if (unreadCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(x = 4.dp, y = (-4).dp)
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(LoopRed),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (unreadCount > 9) "+9" else unreadCount.toString(),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                // Favorites Button
                                        IconButton(
                                            onClick = { activeTab = "favorites" },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (activeTab == "favorites") LoopRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                                .tvFocusable(shape = CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "المفضلة",
                                                tint = if (activeTab == "favorites") LoopRed else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                // Telegram Button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF229ED9).copy(alpha = 0.12f))
                                        .tvFocusable(shape = CircleShape, onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/jdj_q"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "لا يمكن فتح تليجرام حالياً", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Hand-etched elegant vector plane
                                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        val r = size.minDimension / 2
                                        drawCircle(
                                            color = Color(0xFF229ED9),
                                            radius = r
                                        )
                                        val p = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(r * 0.72f, r * 0.95f)
                                            lineTo(r * 1.45f, r * 0.60f)
                                            lineTo(r * 0.60f, r * 1.08f)
                                            close()
                                        }
                                        val p2 = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(r * 0.60f, r * 1.08f)
                                            lineTo(r * 0.72f, r * 0.95f)
                                            lineTo(r * 1.15f, r * 1.25f)
                                            lineTo(r * 1.45f, r * 0.60f)
                                            lineTo(r * 0.52f, r * 0.96f)
                                            close()
                                        }
                                        drawPath(path = p, color = Color(0xFFB0DDFC))
                                        drawPath(path = p2, color = onSurfaceColor)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Ultra compact landscape top bar as requested
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 2.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Left side: Three lines menu button for Settings Drawer (Smaller!)
                                IconButton(
                                    onClick = { isDrawerOpen = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .tvFocusable(shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "القائمة الجانبية",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                if (showRemoveAdsIcon) {
                                    // No Ads Premium Button
                                    IconButton(
                                        onClick = { showRemoveAdsScreen = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .tvFocusable(shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "بدون إعلانات",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Center: +90 Title (Landscape)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "+",
                                    color = LoopRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "90",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }

                            // Notification Bell (Landscape)
                            val showNotificationIcon by viewModel.showNotificationIcon.collectAsState()
                            if (showNotificationIcon) {
                                IconButton(
                                    onClick = { activeTab = "notifications" },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (activeTab == "notifications") GoldGlow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                        .tvFocusable(shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "الإشعارات",
                                        tint = if (activeTab == "notifications") GoldGlow else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(6.dp))

                            // Right side: Telegram (Smaller!)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF229ED9).copy(alpha = 0.12f))
                                    .tvFocusable(shape = CircleShape, onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/jdj_q"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "لا يمكن فتح تليجرام حالياً", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                    val onSurfaceColorTmp = MaterialTheme.colorScheme.onSurface
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        val r = size.minDimension / 2
                                        drawCircle(
                                            color = Color(0xFF229ED9),
                                            radius = r
                                        )
                                        val p = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(r * 0.72f, r * 0.95f)
                                            lineTo(r * 1.45f, r * 0.60f)
                                            lineTo(r * 0.60f, r * 1.08f)
                                            close()
                                        }
                                        val p2 = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(r * 0.60f, r * 1.08f)
                                            lineTo(r * 0.72f, r * 0.95f)
                                            lineTo(r * 1.15f, r * 1.25f)
                                            lineTo(r * 1.45f, r * 0.60f)
                                            lineTo(r * 0.52f, r * 0.96f)
                                            close()
                                        }
                                        drawPath(path = p, color = Color(0xFFB0DDFC))
                                        drawPath(path = p2, color = onSurfaceColorTmp)
                                    }
                            }
                        }
                    }
                }
            }
            }
        },
        bottomBar = {
            if (!isLandscape && activeTab != "profile" && activeTab != "reels" && activeTab != "chat" && !(activeTab == "events" && isEventChannelsPlayerActive)) {
                // Highly Professional Glassmorphic-style Bottom Navigation Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xEE111422), // 93% opacity dark premium blue/grey
                                        Color(0xEE080A12)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val showChatTabOnline by viewModel.showChatTab.collectAsState()
                            val showEventsTabOnline by viewModel.showEventsTab.collectAsState()
                            val showPackagesSectionOnline by viewModel.showPackagesSection.collectAsState()
                            
                            val allowedTypes = activeAccount?.allowedTypes ?: listOf("live", "movies", "series")

                            val tabs = buildList {
                                add(Triple("home", Icons.Default.Home, "الرئيسية"))
                                if (allowedTypes.contains("live")) {
                                    add(Triple("channels", Icons.Default.Sensors, "البث المباشر"))
                                    if (showEventsTabOnline) {
                                        add(Triple("events", Icons.Default.CalendarMonth, "الأحداث"))
                                    }
                                }
                                add(Triple("library", Icons.Default.VideoLibrary, "المكتبة"))
                                if (showPackagesSectionOnline) {
                                    add(Triple("packages", Icons.Default.Dashboard, "الباقات"))
                                }
                                if (showChatTabOnline) {
                                    add(Triple("chat", Icons.Default.ChatBubbleOutline, "الدردشة"))
                                }
                            }

                            tabs.forEach { (tabId, icon, label) ->
                                val isSelected = activeTab == tabId

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(
                                                    brush = Brush.horizontalGradient(
                                                        listOf(LoopRed, LoopRedGlow)
                                                    ),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                            } else Modifier
                                        )
                                        .tvFocusable(shape = RoundedCornerShape(20.dp), onClick = { activeTab = tabId })
                                        .padding(horizontal = if (isSelected) 14.dp else 8.dp, vertical = if (isSelected) 8.dp else 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = Color(0xFF7E849E),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                if (tabId == "chat") {
                                                    val badgeCount by viewModel.chatUnreadUsersCount.collectAsState()
                                                    if (badgeCount > 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .offset(x = 4.dp, y = (-4).dp)
                                                                .size(12.dp)
                                                                .background(Color(0xFFFF5252), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = badgeCount.toString(),
                                                                color = Color.White,
                                                                fontSize = 7.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = label,
                                                color = Color(0xFF7E849E),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLandscape) {
                if ((activeTab == "events" && isEventChannelsPlayerActive)) {
                    // Full screen for Main Channels & YouTube in Landscape without sidebars when player is active
                    TabContent(
                        activeTab = activeTab,
                        librarySubTab = librarySubTab,
                        onLibrarySubTabChange = { librarySubTab = it },
                        filteredCategories = filteredCategories,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        filteredChannels = filteredChannels,
                        filteredEvents = filteredEvents,
                        filteredMovies = filteredMovies,
                        filteredSeries = filteredSeries,
                        favoriteChannels = favoriteChannels,
                        favoriteMovies = favoriteMovies,
                        favoriteSeries = favoriteSeries,
                        viewModel = viewModel,
                        onNavigateTab = { activeTab = it },
                        onBackToChannels = { activeTab = "channels" }
                    )
                } else {
                    // Modern double-pane layout with unified navigation side bar for landscape devices
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left element of Row: Main Section Details
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            // Clean status row without statusBarsPadding to avoid extra vertical spacing
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "حساب Xtream IPTV مفعّل ونشط",
                                    color = SoftGreyText,
                                    fontSize = 11.sp
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                TabContent(
                                    activeTab = activeTab,
                                    librarySubTab = librarySubTab,
                                    onLibrarySubTabChange = { librarySubTab = it },
                                    filteredCategories = filteredCategories,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    filteredChannels = filteredChannels,
                                    filteredEvents = filteredEvents,
                                    filteredMovies = filteredMovies,
                                    filteredSeries = filteredSeries,
                                    favoriteChannels = favoriteChannels,
                                    favoriteMovies = favoriteMovies,
                                    favoriteSeries = favoriteSeries,
                                    viewModel = viewModel,
                                    onNavigateTab = { activeTab = it },
                                    onBackToChannels = { activeTab = "channels" }
                                )
                            }
                        }

                        // Divider Line
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color.White.copy(alpha = 0.05f))
                        )

                        // Right navigation bar for landscape - designed to be small, compact, and fully scrollable
                        Column(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface)
                                .navigationBarsPadding()
                                .padding(vertical = 8.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val showChatTabOnline by viewModel.showChatTab.collectAsState()
                            val showEventsTabOnline by viewModel.showEventsTab.collectAsState()
                            val showPackagesSectionOnline by viewModel.showPackagesSection.collectAsState()
                            val showReelsSectionOnline by viewModel.showReelsSection.collectAsState()
                            
                            val allowedTypes = activeAccount?.allowedTypes ?: listOf("live", "movies", "series")

                            val tabs = buildList {
                                add(Triple("home", Icons.Default.Home, "الرئيسية"))
                                if (allowedTypes.contains("live")) {
                                    add(Triple("channels", Icons.Default.Sensors, "بث مباشر"))
                                    if (showEventsTabOnline) {
                                        add(Triple("events", Icons.Default.CalendarMonth, "الأحداث"))
                                    }
                                }
                                add(Triple("library", Icons.Default.VideoLibrary, "المكتبة"))
                                if (showPackagesSectionOnline) {
                                    add(Triple("packages", Icons.Default.Dashboard, "الباقات"))
                                }
                                add(Triple("favorites", Icons.Default.Favorite, "المفضلة"))
                                if (showChatTabOnline) {
                                    add(Triple("chat", Icons.Default.ChatBubbleOutline, "الدردشة"))
                                }
                            }

                            tabs.forEach { (tabId, icon, label) ->
                                val isSelected = activeTab == tabId
                                val activeColor = if (isSelected) LoopRed else SoftGreyText
                                val bgAlpha = if (isSelected) 0.08f else 0f

                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = bgAlpha))
                                        .tvFocusable(shape = RoundedCornerShape(10.dp), onClick = { activeTab = tabId })
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = activeColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (tabId == "chat") {
                                            val badgeCount by viewModel.chatUnreadUsersCount.collectAsState()
                                            if (badgeCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = 4.dp, y = (-4).dp)
                                                        .size(14.dp)
                                                        .background(LoopRed, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = badgeCount.toString(),
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        color = activeColor,
                                        fontSize = 8.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Portrait Main Tab content switcher
                TabContent(
                    activeTab = activeTab,
                    librarySubTab = librarySubTab,
                    onLibrarySubTabChange = { librarySubTab = it },
                    filteredCategories = filteredCategories,
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    filteredChannels = filteredChannels,
                    filteredEvents = filteredEvents,
                    filteredMovies = filteredMovies,
                    filteredSeries = filteredSeries,
                    favoriteChannels = favoriteChannels,
                    favoriteMovies = favoriteMovies,
                    favoriteSeries = favoriteSeries,
                    viewModel = viewModel,
                    onNavigateTab = { activeTab = it },
                    onBackToChannels = { activeTab = "channels" }
                )
            }

            // Real Quality check and custom selection Dialog
            if (showQualityDialog) {
                Dialog(onDismissRequest = { showQualityDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "فحص الجودة الفعلية للبث",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            if (isCheckingQualities) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(vertical = 24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = LoopRed,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "جاري الاتصال بالسيرفر لفحص جودة الفيديو الحقيقية المتاحة...",
                                        color = SoftGreyText,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Text(
                                    text = "الجودات الحقيقية المتوفرة حالياً على رابط الفيديو:",
                                    color = GoldGlow,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    detectedQualities.forEach { quality ->
                                        Button(
                                            onClick = {
                                                val data = pendingDownloadData
                                                if (data != null) {
                                                    detectedDownloadQuality = quality
                                                    val dl = LocalDownload(
                                                        id = pendingId,
                                                        title = data.first,
                                                        posterUrl = pendingPosterUrl,
                                                        videoUrl = data.second,
                                                        quality = quality,
                                                        timestamp = System.currentTimeMillis(),
                                                        type = pendingDownloadType,
                                                        seriesTitle = pendingSeriesTitle,
                                                        episodeNumber = pendingEpisodeNumber,
                                                        seasonNumber = pendingSeasonNumber,
                                                        duration = pendingDuration
                                                    )
                                                    viewModel.startDownload(dl)
                                                    showDownloadDialog = true
                                                    showQualityDialog = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(text = quality, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = null,
                                                    tint = LoopRed,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedButton(
                                    onClick = { showQualityDialog = false },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftGreyText),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("إلغاء", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Cinematic Movie details dialog
            selectedMovie?.let { movie ->
                val isMovieDownloaded = savedOfflineList.any { it.id == movie.id && it.isCompleted }
                val isMovieDownloading = processingDownloadItem?.id == movie.id
                val downloadedMovie = savedOfflineList.firstOrNull { it.id == movie.id && it.isCompleted }
                
                MovieDetailsDialog(
                    movie = movie,
                    allMovies = movies,
                    isFavorite = favoritesSet.contains(movie.id),
                    isDownloaded = isMovieDownloaded,
                    isDownloading = isMovieDownloading,
                    onToggleFavorite = { viewModel.toggleFavorite(movie.id) },
                    onPlay = { 
                        val playUrl = downloadedMovie?.videoUrl ?: it
                        viewModel.startPlaying(playUrl, movie.title, isLive = false) 
                    },
                    onDownload = { m ->
                        pendingId = m.id
                        pendingPosterUrl = m.posterUrl
                        pendingDownloadData = Pair(m.title, m.streamUrl)
                        pendingDownloadType = "movie"
                        pendingSeriesTitle = ""
                        pendingEpisodeNumber = 0
                        pendingSeasonNumber = 0
                        pendingDuration = m.duration
                        isCheckingQualities = true
                        showQualityDialog = true
                    },
                    onSelectMovie = { viewModel.selectMovie(it) },
                    onDismiss = { viewModel.selectMovie(null) }
                )
            }

            // TV Series expansion dialog
            selectedSeries?.let { series ->
                val seasons by viewModel.seasons.collectAsState()
                val episodes by viewModel.episodes.collectAsState()
                val isLoadingEpisodes by viewModel.isLoading.collectAsState()
                val downloadedEpIds = remember(savedOfflineList) {
                    savedOfflineList.filter { it.isCompleted }.map { it.id }.toSet()
                }
                val downloadingEpIds = remember(processingDownloadItem) {
                    setOfNotNull(processingDownloadItem?.id)
                }

                SeriesDetailsDialog(
                    series = series,
                    seasons = seasons,
                    episodes = episodes,
                    isLoadingEpisodes = isLoadingEpisodes,
                    isFavorite = favoritesSet.contains(series.id),
                    downloadedEpisodeIds = downloadedEpIds,
                    downloadingEpisodeIds = downloadingEpIds,
                    onToggleFavorite = { viewModel.toggleFavorite(series.id) },
                    onPlayEpisode = { ep -> 
                        val downloadedEp = savedOfflineList.firstOrNull { it.id == ep.id && it.isCompleted }
                        val playUrl = downloadedEp?.videoUrl ?: ep.streamUrl
                        viewModel.startPlaying(playUrl, "${series.title} - ${ep.title}", isLive = false)
                        viewModel.selectSeries(null)
                    },
                    onDownloadEpisode = { ep ->
                        pendingId = ep.id
                        pendingPosterUrl = series.posterUrl
                        pendingDownloadData = Pair("${series.title} - ${ep.title}", ep.streamUrl)
                        pendingDownloadType = "episode"
                        pendingSeriesTitle = series.title
                        pendingEpisodeNumber = ep.number
                        pendingSeasonNumber = 1
                        pendingDuration = ep.duration
                        isCheckingQualities = true
                        showQualityDialog = true
                    },
                    onSeasonTabSelected = { seasonId -> viewModel.loadEpisodes(series.id, seasonId) },
                    onDismiss = { viewModel.selectSeries(null) }
                )
            }

            // Download progress dialog (fully backgroundable, customizable and non-blocking)
            if (showDownloadDialog && processingDownloadItem != null) {
                Dialog(onDismissRequest = { showDownloadDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(300.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { if (downloadPhase == "downloading") downloadProgress else 0.1f },
                                    color = LoopRed,
                                    strokeWidth = 6.dp,
                                    modifier = Modifier.size(72.dp)
                                )
                                Icon(
                                    imageVector = if (downloadPhase == "analyzing") Icons.Default.Search else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = when (downloadPhase) {
                                    "analyzing" -> "جاري فحص وتجهيز الرابط فائق السرعة..."
                                    "merging" -> "جاري حفظ وتجهيز الملف بالكامل..."
                                    "completed" -> "اكتمل التنزيل بنجاح!"
                                    else -> "جاري التحميل محلياً بأعلى سرعة..."
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            if (downloadPhase != "analyzing") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = activeDownloadState?.speedFormatted?.ifEmpty { "سريع جداً" } ?: "سريع جداً",
                                        color = GoldGlow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress.coerceIn(0f, 1f) },
                                    color = LoopRed,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                if (activeDownloadState?.downloadedSizeFormatted?.isNotEmpty() == true) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = activeDownloadState?.downloadedSizeFormatted ?: "",
                                            color = SoftGreyText,
                                            fontSize = 10.sp
                                        )
                                        if (activeDownloadState?.etaFormatted?.isNotEmpty() == true) {
                                            Text(
                                                text = activeDownloadState?.etaFormatted ?: "",
                                                color = SoftGreyText,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = { showDownloadDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("المتابعة في الخلفية والتصفح", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { 
                                    viewModel.cancelDownload()
                                    showDownloadDialog = false 
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إلغاء التحميل", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Left Sliding Drawer for Settings
    if (isDrawerOpen) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Semi-transparent background dim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { isDrawerOpen = false }
                )

                // Drawer content sliding from Left - Ultra Premium Design
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.85f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F111E),
                                    Color(0xFF070911)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE5A93C).copy(alpha = 0.35f), // Premium Gold Glow
                                    Color(0xFFEF4444).copy(alpha = 0.2f),  // Loop Red Glow
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .clickable(enabled = false) {}
                        .align(Alignment.CenterStart)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        // Premium Drawer Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0C16))
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = GoldGlow,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "الإعدادات والتحكم",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تطبيق +90 سينما",
                                        color = GoldGlow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            IconButton(
                                onClick = { isDrawerOpen = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Settings Panel content
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            SettingsPanel(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
        
        if (showRemoveAdsScreen) {
            com.example.ui.components.RemoveAdsScreen(
                viewModel = viewModel,
                onClose = { showRemoveAdsScreen = false }
            )
        }
    }
}




@Composable
fun TabContent(
    activeTab: String,
    librarySubTab: String,
    onLibrarySubTabChange: (String) -> Unit,
    filteredCategories: List<IPTVCategory>,
    selectedCategory: String,
    searchQuery: String,
    filteredChannels: List<Channel>,
    filteredEvents: List<EventChannel> = emptyList(),
    filteredMovies: List<Movie>,
    filteredSeries: List<Series>,
    favoriteChannels: List<Channel>,
    favoriteMovies: List<Movie>,
    favoriteSeries: List<Series>,
    viewModel: MainViewModel,
    onNavigateTab: (String) -> Unit = {},
    onBackToChannels: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState()
    val events by viewModel.events.collectAsState()
    val isChannelsLoading by viewModel.isChannelsLoading.collectAsState()
    val isMoviesLoading by viewModel.isMoviesLoading.collectAsState()
    val isSeriesLoading by viewModel.isSeriesLoading.collectAsState()
    val codeUpdateRequiredMessage by viewModel.codeUpdateRequiredMessage.collectAsState()
    val cardColumns by viewModel.cardColumnsCount.collectAsState()
    val favoritesSet by viewModel.favorites.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val isGlobalAdsEnabled by viewModel.isGlobalAdsEnabled.collectAsState()

    val playingStreamUrl by viewModel.playingStreamUrl.collectAsState()
    val playingIsLive by viewModel.playingIsLive.collectAsState()
    val playingTitle by viewModel.playingTitle.collectAsState()
    val playingUserAgent by viewModel.playingUserAgent.collectAsState()
    val isLivePlayerFullScreen by viewModel.isLivePlayerFullScreen.collectAsState()
    val autoLandscape by viewModel.autoLandscape.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (activeTab != "home" && activeTab != "library" && activeTab != "favorites" && activeTab != "settings" && activeTab != "about" && activeTab != "chat" && activeTab != "packages" && activeTab != "reels" && activeTab != "events") {
            // High level Search field
            SearchBarWidget(
                query = searchQuery,
                placeholder = if (activeTab == "events") "ابحث في القنوات الرئيسية..." else "ابحث بالاسم هنا...",
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // Dynamic categories horizontal shelf
            CategoryRow(
                categories = filteredCategories,
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.selectCategory(it) }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                "home" -> {
                    HomeHubScreen(
                        viewModel = viewModel,
                        onNavigateTab = { targetTab ->
                            onNavigateTab(targetTab)
                        },
                        onPlayChannel = { channel ->
                            viewModel.startPlaying(
                                url = channel.streamUrl,
                                title = channel.name,
                                isLive = true,
                                userAgent = channel.userAgent,
                                forcedFormat = channel.forcedFormat
                            )
                        },
                        onSelectMovie = { movie ->
                            viewModel.selectMovie(movie)
                        },
                        onSelectSeries = { s ->
                            viewModel.selectSeries(s)
                        }
                    )
                }
                "channels" -> {
                    if (isChannelsLoading) {
                        ModernLineLoader()
                    } else {
                        ChannelsTabContent(
                            channels = filteredChannels,
                            cardColumns = cardColumns,
                            favoritesSet = favoritesSet,
                            viewModel = viewModel
                        )
                    }
                }
                "events" -> {
                    com.example.ui.components.EventsGridView(
                        viewModel = viewModel
                    )
                }
                "notifications" -> {
                    NotificationListView(viewModel)
                }
                "movies" -> {
                    com.example.ui.components.UnifiedLibraryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "series" -> {
                    com.example.ui.components.UnifiedLibraryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "library" -> {
                    com.example.ui.components.UnifiedLibraryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "favorites" -> {
                    FavoritesTabContent(
                        favoriteChannels = favoriteChannels,
                        favoriteMovies = favoriteMovies,
                        favoriteSeries = favoriteSeries,
                        favoritesSet = favoritesSet,
                        viewModel = viewModel
                    )
                }
                "settings" -> {
                    SettingsPanel(viewModel = viewModel)
                }
                "packages" -> {
                    PackagesPanel(viewModel = viewModel)
                }
                "chat" -> {
                    com.example.ui.components.LoopChatWebView(viewModel = viewModel, onBackToChannels = onBackToChannels)
                }
                "profile" -> {
                    com.example.ui.components.LoopWebView(activeTab)
                }
                "reels" -> {
                    com.example.ui.components.LoopWebView(activeTab)
                }
                "downloads" -> {
                    DownloadsTab(viewModel = viewModel)
                }
            }
        }
    }
}



















@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = SoftGreyText, fontSize = 10.sp)
            }
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SettingsPanel(viewModel: MainViewModel) {
    val homeConfig by viewModel.homeConfig.collectAsState()
    val context = LocalContext.current
    val currentMode by viewModel.playerMode.collectAsState()
    val autoLandscape by viewModel.autoLandscape.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isDeveloperAuthenticated by viewModel.isDeveloperAuthenticated.collectAsState()

    var settingsSubPage by remember { mutableStateOf("home") } 
    var showDevPasswordDialog by remember { mutableStateOf(false) }
    var devPasswordInput by remember { mutableStateOf("") }

    when (settingsSubPage) {
        "home" -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_screen"),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Professional Header Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_settings_banner),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.8f), MaterialTheme.colorScheme.background),
                                        startY = 100f
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "إعدادات +90",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "تحكم كامل في تجربتك السينمائية",
                                color = LoopRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsCard(
                            title = "مكتبة المحتويات المحملة",
                            subtitle = "استعراض الأفلام والحلقات المحفوظة",
                            icon = Icons.Default.CloudDownload,
                            iconColor = Color.Green,
                            onClick = { settingsSubPage = "downloads" }
                        )

                        SettingsCard(
                            title = "ثيمات وألوان واجهة التطبيق",
                            subtitle = "اختر التنسيق والنمط البصري",
                            icon = Icons.Default.Palette,
                            iconColor = GoldGlow,
                            onClick = { settingsSubPage = "themes" }
                        )

                        SettingsCard(
                            title = "طريقة عرض وترتيب البطاقات",
                            subtitle = "تعديل عدد الأعمدة في شبكات العرض",
                            icon = Icons.Default.GridView,
                            iconColor = Color.Cyan,
                            onClick = { settingsSubPage = "card_layout" }
                        )
                    }
                }

                // 4. Auto Landscape settings
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "التدوير التلقائي الذكي للشاشة", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "تشغيل الشاشة تلقائياً بالكامل بالوضع الأفقي عند العرض فقط", color = SoftGreyText, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = autoLandscape,
                                    onCheckedChange = { viewModel.setAutoLandscape(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = SoftGreyText,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            }
                        }
                    }
                }


                // 5. Support Details and Link Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "دعم +90 الفني",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "نحن جاهزون لمساعدتك فوراً في إدخال وتفعيل اشتراكاتك أو سيرفرات Xtream ومشاكل تحميل القنوات.",
                                color = SoftGreyText,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/jdj_q"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تليجرام غير متوفر على جهازك", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229ED9)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "تواصل", tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "مراسلة الدعم والوكلاء", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Developer Control Panel Button inside settings
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp)
                    ) {
                        if (homeConfig.showDevSection) {
                                Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDeveloperAuthenticated) {
                                        settingsSubPage = "dev_control"
                                    } else {
                                        showDevPasswordDialog = true
                                    }
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GoldGlow.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = "قسم تحكم مطور", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "إدارة الأقسام ونشر قنوات بث أونلاين على السيرفر", color = SoftGreyText, fontSize = 10.sp)
                                }
                            }
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(16.dp).graphicsLayer(rotationZ = 180f))
                        }
                        }
                    }
                }

                // Dedicated secure Exit Button (Logout)
                item {
                    Button(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تسجيل خروج آمن من الحساب الحالي", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        "account" -> {
            val currentUsername by viewModel.chatUsername.collectAsState()
            val currentName by viewModel.chatName.collectAsState()
            val currentAvatar by viewModel.chatAvatar.collectAsState()
            val currentPassword = remember { viewModel.getChatPassword() }
            
            var newUsername by remember { mutableStateOf(currentUsername ?: "") }
            var newName by remember { mutableStateOf(currentName ?: "") }
            var newAvatar by remember { mutableStateOf(currentAvatar) }
            var newPassword by remember { mutableStateOf(currentPassword) }
            var passwordVisible by remember { mutableStateOf(false) }
            
            var isLoading by remember { mutableStateOf(false) }
            var statusMessage by remember { mutableStateOf<String?>(null) }
            var isSuccess by remember { mutableStateOf(false) }

            val avatars = remember {
                listOf(
                    Pair("neon_wolf", "ذئب النيون 🐺"),
                    Pair("cyber_phoenix", "فينيكس 🐦"),
                    Pair("golden_crown", "التاج الذهبي 👑"),
                    Pair("retro_gamer", "ريترو قيمر 🎮"),
                    Pair("cinema_star", "نجم السينما ⭐"),
                    Pair("vip_diamond", "ماسة VIP 💎")
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Return Back to general options row selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsSubPage = "home" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "العودة للإعدادات العامة",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تعديل الملف الشخصي",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "يمكنك تغيير الصورة الرمزية، الاسم المستعار المعروض، اسم المستخدم الفريد، وكلمة المرور الخاصة بك.",
                            color = SoftGreyText,
                            fontSize = 10.5.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Avatar Selection
                        Text(text = "اختر الصورة الرمزية الخاصة بك:", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            avatars.forEach { (key, label) ->
                                val isSelected = newAvatar == key
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) LoopRed.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) LoopRed else Color.White.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                        .clickable { newAvatar = key }
                                        .padding(4.dp)
                                ) {
                                    UserAvatar(avatarKey = key, size = 32.dp)
                                }
                            }
                        }
                        
                        // Display Name
                        Text(text = "الاسم الشخصي المعروض للجميع (يسمح بالتكرار):", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                            singleLine = true,
                            placeholder = { Text("مثال: أحمد العتيبي", color = SoftGreyText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoopRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            )
                        )
                        
                        // Username
                        Text(text = "اسم المستخدم الفريد (بالإنجليزية فقط):", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        OutlinedTextField(
                            value = newUsername,
                            onValueChange = { input ->
                                val filtered = input.filter { char ->
                                    char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char == '_' || char == '-'
                                }
                                if (filtered.length <= 9) {
                                    newUsername = filtered.lowercase()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                            singleLine = true,
                            placeholder = { Text("مثال: ahmed_99", color = SoftGreyText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoopRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            )
                        )

                        // Password
                        Text(text = "كلمة مرور حماية الحساب الخاص بك:", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = SoftGreyText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoopRed,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            )
                        )
                        
                        statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                color = if (isSuccess) Color.Green else LoopRedGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                val cleanedUsername = newUsername.trim().lowercase()
                                val cleanedName = newName.trim()
                                val cleanedPassword = newPassword.trim()
                                
                                if (cleanedName.isEmpty() || cleanedUsername.isEmpty() || cleanedPassword.isEmpty()) {
                                    statusMessage = "برجاء ملء جميع الحقول المطلوبة"
                                    isSuccess = false
                                    return@Button
                                }
                                isLoading = true
                                statusMessage = "جاري التحقق من تفرد اسم المستخدم وحفظ التعديلات..."
                                isSuccess = false
                                viewModel.updateChatUser(cleanedUsername, cleanedName, cleanedPassword, newAvatar) { success, err ->
                                    isLoading = false
                                    if (success) {
                                        isSuccess = true
                                        statusMessage = "تم تحديث معلومات الحساب بنجاح!"
                                    } else {
                                        isSuccess = false
                                        statusMessage = err ?: "فشل التحديث، اسم المستخدم مستخدم بالفعل!"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            } else {
                                Text("حفظ التغييرات الآن", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        "themes" -> {
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Return Back to general options row selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsSubPage = "home" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "العودة للإعدادات العامة",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ثيمات ومظهر واجهة التطبيق",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختر التنسيق والنمط البصري الذي يناسب راحتك البصرية:",
                            color = SoftGreyText,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ثيمات ألوان الواجهة (Themes):",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val themes = listOf(
                            Triple("red", "الأحمر المتوهج الملكي (Red Glow)", Color(0xFFE50914)),
                            Triple("gold", "الملكي الذهبي الفاخر (Premium Gold)", Color(0xFFFFD700)),
                            Triple("purple", "البربل الفضائي اللامع (Purple Nebula)", Color(0xFF9D4EDD)),
                            Triple("emerald", "الزمرد الهادئ والأنيق (Midnight Emerald)", Color(0xFF00C9A7)),
                            Triple("blue", "الياقوت الأزرق العميق (Ocean Blue)", Color(0xFF007BFF))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            themes.forEach { (themeId, label, colorAccent) ->
                                val isSelected = selectedTheme == themeId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colorAccent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colorAccent else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.setTheme(themeId) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(colorAccent)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else SoftGreyText,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "محدد",
                                            tint = colorAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        "downloads" -> {
            val savedOfflineList by viewModel.downloads.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsSubPage = "home" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "العودة للإعدادات العامة", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "المحتويات المحملة محلياً (أوفلاين)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (savedOfflineList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "مكتبة التنزيلات فارغة حالياً.", color = SoftGreyText, fontSize = 13.sp)
                            Text(text = "يمكنك تنزيل الأفلام والحلقات لتشغيلها بدون إنترنت بالكامل.", color = SoftGreyText.copy(alpha = 0.7f), fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedOfflineList) { download ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail
                                    SafeAsyncImage(
                                        model = download.posterUrl,
                                        contentDescription = download.title,
                                        modifier = Modifier
                                            .size(width = 60.dp, height = 90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Details column
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = download.title,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Detected Real Quality pill
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.Green.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = download.quality.ifEmpty { "1080p FHD" },
                                                    color = Color.Green,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "محفوظ على الجهاز",
                                                color = SoftGreyText,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Action triggers
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Play offline custom file
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(LoopRed)
                                                    .clickable {
                                                        // Instantly boots custom Movie Player with cached file/URL
                                                        viewModel.startPlaying(download.videoUrl, download.title, isLive = false)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "تشغيل الآن", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Delete file
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .clickable {
                                                        viewModel.deleteDownload(download.id)
                                                        Toast.makeText(context, "تم حذف الملف بنجاح", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = "حذف التنزيل", color = SoftGreyText, fontSize = 10.sp)
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

        "card_layout" -> {
            val cardColumns by viewModel.cardColumnsCount.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsSubPage = "home" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "العودة للإعدادات العامة", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "طريقة عرض وترتيب البطاقات",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "تحكم في شبكة القنوات ومحتوى الفيديو عبر تحديد حجم الشبكة المفضل لشاشتك:",
                    color = SoftGreyText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                val columnChoices = listOf(
                    Triple(2, "شبكة ثنائية 2*2 لسهولة التصفح", "بطاقات واضحة بحجم كبير تناسب القنوات الرياضية والتحكم اللمسي المريح"),
                    Triple(3, "شبكة ثلاثية 3*3 للعرض المتوازن", "التنظيم الافتراضي والمثالي لعرض تفاصيل القناة وعدد المربعات المريح"),
                    Triple(4, "شبكة رباعية 4*4 للمشاهدة السريعة", "بطاقات مصغرة تمكنك من رؤية ومسح أكبر عدد ممكن من القنوات والبثوث دفعة واحدة")
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    columnChoices.forEach { (count, label, desc) ->
                        val isSelected = cardColumns == count
                        val activeColor = MaterialTheme.colorScheme.primary

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) activeColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) activeColor else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setCardColumnsCount(count) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.0f).padding(end = 8.dp)) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else SoftGreyText,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = desc,
                                    color = SoftGreyText.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "محدد",
                                    tint = activeColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        "dev_control", "developer" -> {
            com.example.ui.components.DeveloperSettingsScreen(
                viewModel = viewModel,
                onDismiss = { settingsSubPage = "home" }
            )
        }
    }

    if (showDevPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDevPasswordDialog = false 
                devPasswordInput = ""
            },
            title = { Text("تحقق من صلاحية المطور", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة المرور لفتح لوحة المطور وإدارة السيرفر:", color = SoftGreyText, fontSize = 12.sp)
                    OutlinedTextField(
                        value = devPasswordInput,
                        onValueChange = { devPasswordInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldGlow,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.authenticateDeveloper(devPasswordInput)
                        if (viewModel.isDeveloperAuthenticated.value) {
                            showDevPasswordDialog = false
                            devPasswordInput = ""
                            settingsSubPage = "dev_control"
                        } else {
                            Toast.makeText(context, "كلمة المرور غير صحيحة", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldGlow)
                ) {
                    Text("دخول", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDevPasswordDialog = false 
                    devPasswordInput = ""
                }) {
                    Text("إلغاء", color = SoftGreyText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}





