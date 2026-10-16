package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.EspnMatch
import com.example.data.EspnMatchSummary
import com.example.data.EspnSoccerService
import com.example.model.Channel
import com.example.model.EventChannel
import com.example.model.EventServer
import com.example.ui.MainViewModel
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.LoopRed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Design Colors matching the App's primary theme
private val AppBackground = Color(0xFF080C14)
private val AppSurfaceCard = Color(0xFF121826)
private val AppSurfaceElevated = Color(0xFF1B2234)
private val AppCardBorder = Color(0xFF26324A)
private val AppAccentGold = Color(0xFFFFD700)
private val AppPrimaryRed = Color(0xFFE50914)
private val AppLiveGreen = Color(0xFF22C55E)
private val AppTextMuted = Color(0xFF94A3B8)
private val AppTextSubtle = Color(0xFF64748B)

@Composable
fun EventsGridView(viewModel: MainViewModel) {
    val context = LocalContext.current
    val espnMatches: List<EspnMatch> by viewModel.espnMatches.collectAsState()
    val isEspnMatchesLoading: Boolean by viewModel.isEspnMatchesLoading.collectAsState()
    val selectedEspnLeague: String by viewModel.selectedEspnLeague.collectAsState()
    val selectedEspnDate: String by viewModel.selectedEspnDate.collectAsState()
    val selectedEspnMatch: EspnMatch? by viewModel.selectedEspnMatch.collectAsState()
    val selectedEspnSummary: EspnMatchSummary? by viewModel.selectedEspnSummary.collectAsState()
    val isEspnSummaryLoading: Boolean by viewModel.isEspnSummaryLoading.collectAsState()
    val favoriteTeams: Set<String> by viewModel.favoriteTeamNames.collectAsState()
    val selectedEspnStandings by viewModel.selectedEspnStandings.collectAsState()
    val isEspnStandingsLoading by viewModel.isEspnStandingsLoading.collectAsState()
    val ysNews by viewModel.ysNews.collectAsState()
    val isYsNewsLoading by viewModel.isYsNewsLoading.collectAsState()
    val subscribedMatches by viewModel.subscribedMatches.collectAsState()

    val events by viewModel.events.collectAsState()
    val allChannels by viewModel.channels.collectAsState()
    val matchEvents = events.filter { it.eventType == "match" }
    val streamEvents = events.filter { it.eventType != "match" }

    var activeFilterTab by remember { mutableStateOf("today") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var showFavoriteManagerDialog by remember { mutableStateOf(false) }
    var showAdminContentSection by remember { mutableStateOf(true) }
    
    var multiChannelSelection by remember { mutableStateOf<List<EventChannel>?>(null) }

    // Live ticking time to dynamically update kickoff countdowns every second
    var currentSystemTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentSystemTimeMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val liveMatches = remember(espnMatches) {
        espnMatches.filter { it.statusState == "in" }
    }

    val filteredMatches = remember(espnMatches, activeFilterTab, favoriteTeams, searchQuery) {
        val baseList = when (activeFilterTab) {
            "live" -> espnMatches.filter { it.statusState == "in" }
            "upcoming" -> espnMatches.filter { it.statusState == "pre" }
            "results" -> espnMatches.filter { it.statusState == "post" }
            "favorites" -> espnMatches.filter {
                favoriteTeams.contains(it.homeTeam.arabicName) ||
                favoriteTeams.contains(it.awayTeam.arabicName) ||
                favoriteTeams.contains(it.homeTeam.displayName) ||
                favoriteTeams.contains(it.awayTeam.displayName)
            }
            else -> espnMatches
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.homeTeam.arabicName.contains(searchQuery, ignoreCase = true) ||
                it.awayTeam.arabicName.contains(searchQuery, ignoreCase = true) ||
                it.homeTeam.displayName.contains(searchQuery, ignoreCase = true) ||
                it.awayTeam.displayName.contains(searchQuery, ignoreCase = true) ||
                it.leagueArabicName.contains(searchQuery, ignoreCase = true) ||
                it.leagueName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Match Details View (when a match is selected)
    val currentMatch = selectedEspnMatch
    if (currentMatch != null) {
        val isSubscribed = subscribedMatches.contains(currentMatch.id)
        MatchDetailDialog(
            match = currentMatch,
            summary = selectedEspnSummary,
            isLoadingSummary = isEspnSummaryLoading,
            standings = selectedEspnStandings,
            isStandingsLoading = isEspnStandingsLoading,
            newsList = ysNews,
            isNewsLoading = isYsNewsLoading,
            isSubscribed = isSubscribed,
            onToggleSubscription = { viewModel.toggleMatchSubscription(currentMatch.id) },
            onClose = { viewModel.clearSelectedEspnMatch() }
        )
        return
    }

    // Favorite Teams Management Dialog
    if (showFavoriteManagerDialog) {
        FavoriteTeamsDialog(
            favoriteTeams = favoriteTeams,
            onToggleFavorite = { teamName -> viewModel.toggleFavoriteTeam(teamName) },
            onDismiss = { showFavoriteManagerDialog = false }
        )
    }

    if (multiChannelSelection != null) {
        EventChannelSelectionDialog(
            channels = multiChannelSelection!!,
            allChannels = allChannels,
            onChannelSelected = { ch ->
                val channelsList = multiChannelSelection!!
                multiChannelSelection = null
                viewModel.startPlayingEvent(channelsList, channelsList.indexOf(ch))
            },
            onDismiss = { multiChannelSelection = null }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
            ) {
                // ... [Existing Header and Calendar items]
                item {
                    EventsCompactHeader(
                        onOpenDatePicker = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formatted = String.format(Locale.US, "%04d%02d%02d", year, month + 1, dayOfMonth)
                                    viewModel.setEspnDate(formatted)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        onRefresh = {
                            viewModel.loadEspnMatches(forceRefresh = true)
                            viewModel.loadEventChannels()
                        },
                        isSearchOpen = isSearchOpen,
                        onToggleSearch = { isSearchOpen = !isSearchOpen },
                        hasActiveSearch = searchQuery.isNotBlank(),
                        isRefreshing = isEspnMatchesLoading
                    )

                    AnimatedVisibility(
                        visible = isSearchOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        EventsSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClose = {
                                searchQuery = ""
                                isSearchOpen = false
                            },
                            matchesCount = filteredMatches.size
                        )
                    }
                }

                item {
                    SmartDateSelectorTable(
                        selectedDate = selectedEspnDate,
                        onDateSelected = { date -> viewModel.setEspnDate(date) }
                    )
                }

                item {
                    FastFilterPillsRow(
                        activeTab = activeFilterTab,
                        liveCount = liveMatches.size,
                        onTabSelected = { tab -> activeFilterTab = tab },
                        isSearchOpen = isSearchOpen,
                        onToggleSearch = { isSearchOpen = !isSearchOpen },
                        hasActiveSearch = searchQuery.isNotBlank()
                    )
                }

                if (isEspnMatchesLoading) {
                    items(4) {
                        SkeletonMatchCard()
                    }
                }

                if (liveMatches.isNotEmpty() && activeFilterTab != "results" && activeFilterTab != "upcoming") {
                    item {
                        SectionHeaderTitle(
                            title = "مباشر الآن 🔥",
                            subtitle = "${liveMatches.size} مباراة جارية حالياً",
                            showAllText = if (activeFilterTab != "live") "عرض الكل (${liveMatches.size})" else "",
                            onShowAllClick = { activeFilterTab = "live" }
                        )
                    }

                    itemsIndexed(liveMatches) { index, match ->
                        LiveMatchHighlightCard(
                            match = match,
                            cardIndex = index,
                            currentTimeMs = currentSystemTimeMs,
                            onClick = { viewModel.selectEspnMatch(match) },
                            onPlayLive = { viewModel.selectEspnMatch(match) }
                        )
                    }
                }

                item {
                    SectionHeaderTitle(
                        title = when (activeFilterTab) {
                            "live" -> "المباريات المباشرة 🔥"
                            "upcoming" -> "المباريات القادمة 🕒"
                            "results" -> "نتائج المباريات المنتهية 🏆"
                            "favorites" -> "مباريات فرقك المفضلة ⭐"
                            else -> "جدول مباريات اليوم 📅"
                        },
                        subtitle = if (filteredMatches.isNotEmpty()) "${filteredMatches.size} مباراة" else "",
                        showAllText = "",
                        onShowAllClick = {}
                    )
                }

                if (filteredMatches.isEmpty() && !isEspnMatchesLoading) {
                    item {
                        EmptyMatchesCard(
                            activeTab = activeFilterTab,
                            onResetFilter = {
                                activeFilterTab = "today"
                                searchQuery = ""
                                viewModel.setEspnLeague("all")
                            }
                        )
                    }
                } else {
                    itemsIndexed(filteredMatches) { index, match ->
                        val isFavorite = favoriteTeams.contains(match.homeTeam.arabicName) ||
                                         favoriteTeams.contains(match.awayTeam.arabicName) ||
                                         favoriteTeams.contains(match.homeTeam.displayName) ||
                                         favoriteTeams.contains(match.awayTeam.displayName)

                        SmartMatchRowCard(
                            match = match,
                            isFavorite = isFavorite,
                            cardIndex = index,
                            currentTimeMs = currentSystemTimeMs,
                            onToggleFavorite = { teamName -> viewModel.toggleFavoriteTeam(teamName) },
                            onClick = { viewModel.selectEspnMatch(match) }
                        )
                    }
                }

                // Admin Published Broadcast Events & Channels
                if (events.isNotEmpty()) {
                    val matchEventsGrouped = matchEvents.groupBy { it.name }
                    val uniqueMatchEvents = matchEventsGrouped.values.map { it.first() }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "أحداث مباشرة وحصرية 📡",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اختر الحدث لمشاهدة القنوات والسيرفرات المتوفرة",
                                    color = AppTextMuted,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    if (showAdminContentSection) {
                        if (uniqueMatchEvents.isNotEmpty()) {
                            item {
                                EventMatchesCarousel(
                                    matches = uniqueMatchEvents,
                                    allChannels = allChannels,
                                    onPlayMatch = { event ->
                                        playEventWithServers(
                                            event = event,
                                            allChannels = allChannels,
                                            allEvents = events,
                                            viewModel = viewModel,
                                            onShowMultiChannel = { multiChannelSelection = it }
                                        )
                                    }
                                )
                            }
                        }

                        if (streamEvents.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    val rows = streamEvents.chunked(2)
                                    rows.forEach { rowEvents ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            for (event in rowEvents) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    EventStreamCard(
                                                        event = event,
                                                        onPlay = {
                                                            playEventWithServers(
                                                                event = event,
                                                                allChannels = allChannels,
                                                                allEvents = events,
                                                                viewModel = viewModel,
                                                                onShowMultiChannel = { multiChannelSelection = it }
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                            if (rowEvents.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
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
private fun EventCountdownTimer(startTimeMs: Long) {
    var timeLeft by remember { mutableStateOf("") }
    LaunchedEffect(startTimeMs) {
        while (true) {
            val now = System.currentTimeMillis()
            val diff = startTimeMs - now
            if (diff <= 0) {
                timeLeft = "مباشر الآن 🔥"
                break
            }
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff / (1000 * 60)) % 60
            val seconds = (diff / 1000) % 60
            timeLeft = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            kotlinx.coroutines.delay(1000)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "countdown_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Text(
        text = timeLeft,
        color = AppAccentGold,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.graphicsLayer { this.alpha = if (timeLeft.contains("مباشر")) alpha else 1f }
    )
}

@Composable
private fun EventChannelSelectionDialog(
    channels: List<EventChannel>,
    allChannels: List<Channel>,
    onChannelSelected: (EventChannel) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = AppSurfaceCard,
            border = BorderStroke(1.dp, AppCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppAccentGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AppAccentGold.copy(alpha = 0.3f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = AppAccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "اختر قناة البث",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "متوفرة الآن للبث المباشر بأعلى جودة",
                    color = AppTextMuted,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(channels) { event ->
                        val targetChannel = allChannels.find { it.id == event.targetChannelId }
                        Surface(
                            onClick = { onChannelSelected(event) },
                            shape = RoundedCornerShape(12.dp),
                            color = AppSurfaceElevated,
                            border = BorderStroke(1.dp, AppCardBorder)
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (targetChannel?.logoUrl?.isNotBlank() == true) {
                                        AsyncImage(
                                            model = targetChannel.logoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().padding(2.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text(
                                            text = (targetChannel?.name ?: event.name).take(1),
                                            color = AppAccentGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = targetChannel?.name ?: event.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (event.servers.isNotEmpty()) "${event.servers.size} سيرفر متاح" else "سيرفر مباشر",
                                        color = AppTextSubtle,
                                        fontSize = 10.sp
                                    )
                                }

                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = AppPrimaryRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("إغلاق", color = AppTextMuted)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Top Header Bar
// -------------------------------------------------------------

@Composable
private fun EventsCompactHeader(
    onOpenDatePicker: () -> Unit,
    onRefresh: () -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    hasActiveSearch: Boolean,
    isRefreshing: Boolean = false
) {
    var spinTrigger by remember { mutableIntStateOf(0) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isRefreshing) 720f else spinTrigger * 360f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "refresh_rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right side (in RTL): Calendar & Reset buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Ultra-professional, elegant Calendar Button
            Surface(
                onClick = onOpenDatePicker,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF131B2E),
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF818CF8),
                            Color(0xFF6366F1).copy(alpha = 0.6f),
                            Color(0xFF38BDF8).copy(alpha = 0.35f)
                        )
                    )
                ),
                shadowElevation = 4.dp,
                modifier = Modifier.size(38.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Top calendar binder highlight details
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF818CF8))
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 14.dp, height = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF818CF8))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF818CF8))
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "اختيار التاريخ والتقويم",
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Reset / Refresh Button: Compact size (28dp), sleek, ultra-professional
            Surface(
                onClick = {
                    spinTrigger++
                    onRefresh()
                },
                shape = CircleShape,
                color = Color(0xFF0D1424),
                border = BorderStroke(1.dp, Color(0xFF24324A)),
                shadowElevation = 2.dp,
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة ضبط وتحديث",
                        tint = if (isRefreshing) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer(rotationZ = rotationAngle)
                    )
                }
            }
        }

        // Center: Title and Subtitle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "مباريات وأحداث اليوم",
                color = Color.White,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "مواعيد ونتائج حية وفق توقيت مكة المكرمة",
                color = Color(0xFF94A3B8),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Left side (in RTL): Search (عدسة) Button
        Surface(
            onClick = {
                onToggleSearch()
            },
            shape = RoundedCornerShape(12.dp),
            color = if (isSearchOpen || hasActiveSearch) Color(0xFF281A4E) else Color(0xFF131B2E),
            border = BorderStroke(
                1.2.dp,
                if (isSearchOpen || hasActiveSearch) {
                    Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFF2E3D59), Color(0xFF1E283E)))
                }
            ),
            shadowElevation = if (isSearchOpen || hasActiveSearch) 6.dp else 2.dp,
            modifier = Modifier.size(38.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchOpen) "إغلاق البحث" else "بحث",
                    tint = if (isSearchOpen || hasActiveSearch) Color(0xFFA78BFA) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(19.dp)
                )

                if (hasActiveSearch && !isSearchOpen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700))
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartDateSelectorTable(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val days = remember {
        val list = mutableListOf<DayItemModel>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val dayNameFormat = SimpleDateFormat("EEE", Locale.forLanguageTag("ar"))
        val dayNumberFormat = SimpleDateFormat("d", Locale.forLanguageTag("ar"))
        val monthFormat = SimpleDateFormat("MMMM", Locale.forLanguageTag("ar"))

        // Add 9 days: Yesterday (-1), Today (0), and next 7 days (+1 to +7)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        for (i in -1..7) {
            val key = keyFormat.format(cal.time)
            val label = when (i) {
                -1 -> "الأمس"
                0 -> "اليوم"
                1 -> "غداً"
                else -> dayNameFormat.format(cal.time)
            }
            val dayNum = dayNumberFormat.format(cal.time)
            val month = monthFormat.format(cal.time)
            val isToday = (i == 0)

            list.add(DayItemModel(key, label, dayNum, month, isToday))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val selectedIndex = days.indexOfFirst { it.key == selectedDate }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Smooth scroll to selected date on change
    LaunchedEffect(selectedDate, selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(
            onClick = {
                if (selectedIndex > 0) {
                    onDateSelected(days[selectedIndex - 1].key)
                }
            },
            enabled = selectedIndex > 0,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF111625))
                .border(1.dp, Color(0xFF26324A), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "اليوم السابق",
                tint = if (selectedIndex > 0) Color.White else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(15.dp)
            )
        }

        LazyRow(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(days) { day ->
                val isSelected = selectedDate == day.key

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(54.dp)
                ) {
                    Surface(
                        onClick = { onDateSelected(day.key) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color.Transparent else Color(0xFF111625),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF8B5CF6),
                                                Color(0xFF5B21B6)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        border = BorderStroke(
                            width = if (isSelected) 1.2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFA78BFA) else Color(0xFF1E283E)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day.label,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                maxLines = 1
                            )

                            Text(
                                text = day.dayNum,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                text = day.month,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF64748B),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color(0xFF8B5CF6))
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = {
                if (selectedIndex < days.size - 1) {
                    onDateSelected(days[selectedIndex + 1].key)
                }
            },
            enabled = selectedIndex < days.size - 1,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF111625))
                .border(1.dp, Color(0xFF26324A), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "اليوم التالي",
                tint = if (selectedIndex < days.size - 1) Color.White else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private data class DayItemModel(
    val key: String,
    val label: String,
    val dayNum: String,
    val month: String,
    val isToday: Boolean
)

@Composable
private fun EventsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    matchesCount: Int = 0
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF121828),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.85f),
                    Color(0xFF38BDF8).copy(alpha = 0.65f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Search Lens Badge (RTL right)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Field with auto-focus
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = "ابحث عن فريق، نادي، أو دوري...",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )

                // Trailing actions: Clear (if has text) and Close/Collapse button
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Close / Collapse Bar Button
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = Color(0xFF1E283E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق البحث",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (query.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp, end = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تم العثور على $matchesCount مباراة",
                        color = Color(0xFFA78BFA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "مسح البحث",
                        color = Color(0xFF94A3B8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onQueryChange("") }
                            .padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FastFilterPillsRow(
    activeTab: String,
    liveCount: Int,
    onTabSelected: (String) -> Unit,
    isSearchOpen: Boolean = false,
    onToggleSearch: () -> Unit = {},
    hasActiveSearch: Boolean = false
) {
    val filters = listOf(
        Triple("today", "مباريات اليوم", Icons.Default.CalendarMonth),
        Triple("live", if (liveCount > 0) "مباشر ($liveCount)" else "مباشر الآن", Icons.Default.Sensors),
        Triple("upcoming", "القادمة", Icons.Default.AccessTime),
        Triple("results", "النتائج", Icons.Default.BarChart)
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quick search toggle pill with magnifying glass
        item {
            Surface(
                onClick = onToggleSearch,
                shape = RoundedCornerShape(12.dp),
                color = if (isSearchOpen || hasActiveSearch) Color(0xFF8B5CF6).copy(alpha = 0.2f) else Color(0xFF111625),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSearchOpen || hasActiveSearch) Color(0xFFA78BFA) else Color(0xFF1E283E)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = if (isSearchOpen || hasActiveSearch) Color(0xFFA78BFA) else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بحث",
                        color = if (isSearchOpen || hasActiveSearch) Color(0xFFA78BFA) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = if (isSearchOpen || hasActiveSearch) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        items(filters) { (key, label, icon) ->
            val isSelected = activeTab == key
            Surface(
                onClick = { onTabSelected(key) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color.Transparent else Color(0xFF111625),
                modifier = Modifier.then(
                    if (isSelected) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6),
                                    Color(0xFF6D28D9)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) Color(0xFFA78BFA) else Color(0xFF1E283E)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderTitle(
    title: String,
    subtitle: String = "",
    showAllText: String = "",
    onShowAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = Color(0xFFEF4444), // Beautiful Rose/Red calendar icon matching screenshot
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (subtitle.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents, // Trophy icon matching screenshot
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFFA78BFA),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
private fun SkeletonMatchCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF111625).copy(alpha = alpha),
        border = BorderStroke(1.dp, Color(0xFF1E283E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 50.dp, height = 9.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 70.dp, height = 28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 50.dp, height = 9.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMatchHighlightCard(
    match: EspnMatch,
    cardIndex: Int = 0,
    currentTimeMs: Long,
    onClick: () -> Unit,
    onPlayLive: () -> Unit
) {
    SmartMatchRowCard(
        match = match,
        isFavorite = false,
        cardIndex = cardIndex,
        currentTimeMs = currentTimeMs,
        onToggleFavorite = {},
        onClick = onClick
    )
}

@Composable
private fun SmartMatchRowCard(
    match: EspnMatch,
    isFavorite: Boolean,
    cardIndex: Int = 0,
    currentTimeMs: Long,
    onToggleFavorite: (String) -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    val displayTime = remember(match.timestampMs, match.formattedTime) {
        if (match.timestampMs > 0) {
            EspnSoccerService.formatArabicMatchTime(match.timestampMs)
        } else {
            match.formattedTime
        }
    }

    val kickoffText = remember(match.timestampMs, match.statusState, currentTimeMs) {
        if (match.statusState == "in") {
            if (match.clock.isNotBlank()) match.clock else "مباشر"
        } else if (match.statusState == "post") {
            "انتهت"
        } else {
            if (match.timestampMs > 0) {
                val diff = match.timestampMs - currentTimeMs
                if (diff in 1..(59 * 60 * 1000L)) {
                    val mins = (diff / (60 * 1000)).coerceAtLeast(1)
                    "يبدأ بعد $mins د"
                } else if (diff in (60 * 60 * 1000L)..(24 * 60 * 60 * 1000L)) {
                    val hours = diff / (3600 * 1000)
                    val mins = (diff % (3600 * 1000)) / (60 * 1000)
                    if (mins > 0) "يبدأ بعد ${hours}س ${mins}د" else "يبدأ بعد ${hours}س"
                } else if (diff <= 0 && diff > -120 * 60 * 1000L) {
                    "يبدأ قريباً"
                } else {
                    "لم تبدأ"
                }
            } else {
                "لم تبدأ"
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val liveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isLive = match.statusState == "in"
    val isEnded = match.statusState == "post"

    // Theme based on cardIndex to perfectly align with the design screenshot
    val themeColor = remember(cardIndex) {
        when (cardIndex % 3) {
            0 -> Color(0xFF8B5CF6) // Premium Purple (La Liga Style)
            1 -> Color(0xFF3B82F6) // Premium Blue (Ligue 1 Style)
            else -> Color(0xFFEF4444) // Premium Red (Premier League Style)
        }
    }

    val cardBackgroundBrush = remember(cardIndex) {
        when (cardIndex % 3) {
            0 -> Brush.verticalGradient(colors = listOf(Color(0xFF0F0B18), Color(0xFF160E25)))
            1 -> Brush.verticalGradient(colors = listOf(Color(0xFF070E1A), Color(0xFF0D172B)))
            else -> Brush.verticalGradient(colors = listOf(Color(0xFF140809), Color(0xFF220E12)))
        }
    }

    val bottomGlowBrush = remember(cardIndex) {
        when (cardIndex % 3) {
            0 -> Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF8B5CF6).copy(alpha = 0.22f)))
            1 -> Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF3B82F6).copy(alpha = 0.22f)))
            else -> Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFFEF4444).copy(alpha = 0.22f)))
        }
    }

    val borderColor = remember(cardIndex, isLive, isFavorite) {
        if (isLive) {
            Color(0xFFEF4444).copy(alpha = 0.6f)
        } else if (isFavorite) {
            Color(0xFFFFD700).copy(alpha = 0.6f)
        } else {
            themeColor.copy(alpha = 0.35f)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF111625),
        border = BorderStroke(
            width = if (isLive) 1.2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
        ) {
            // High-quality stadium lights background overlay
            AsyncImage(
                model = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600&auto=format&fit=crop",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.06f)
            )

            // Custom glowing colors
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(bottomGlowBrush)
            )

            // Favorite star on top-left (RTL end)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = { onToggleFavorite(match.homeTeam.arabicName) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "مفضلة",
                        tint = if (isFavorite) Color(0xFFFFD700) else Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Teams and main details Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home Team Column (RTL Right)
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF161C2C),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        AsyncImage(
                            model = match.homeTeam.logoUrl.ifEmpty { "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/default-team-logo-500.png" },
                            contentDescription = match.homeTeam.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = match.homeTeam.arabicName,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }

                // Center Match info & timer details
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // League Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        if (match.leagueLogoUrl.isNotBlank()) {
                            AsyncImage(
                                model = match.leagueLogoUrl,
                                contentDescription = match.leagueName,
                                modifier = Modifier.size(13.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = match.leagueArabicName,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Score or Kickoff Time
                    if (!isLive && !isEnded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF090D1A))
                                .border(
                                    BorderStroke(
                                        width = 1.dp,
                                        color = themeColor.copy(alpha = 0.5f)
                                    ),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = displayTime,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E283E).copy(alpha = 0.3f),
                            border = BorderStroke(0.8.dp, Color(0xFF1E283E).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = kickoffText,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Scores
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = match.homeTeam.score,
                                color = if (isEnded && match.homeTeam.isWinner) Color(0xFFFFD700) else Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "-",
                                color = Color(0xFF64748B),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                text = match.awayTeam.score,
                                color = if (isEnded && match.awayTeam.isWinner) Color(0xFFFFD700) else Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLive) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF1E283E).copy(alpha = 0.3f),
                            border = BorderStroke(
                                0.8.dp,
                                if (isLive) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFF1E283E).copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444).copy(alpha = liveAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }

                                Text(
                                    text = if (isLive) "مباشر" else "انتهت",
                                    color = if (isLive) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Away Team Column (RTL Left)
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF161C2C),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        AsyncImage(
                            model = match.awayTeam.logoUrl.ifEmpty { "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/default-team-logo-500.png" },
                            contentDescription = match.awayTeam.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = match.awayTeam.arabicName,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Favorite Teams Rows
// -------------------------------------------------------------

@Composable
private fun FavoriteTeamsHorizontalRow(
    favoriteTeams: Set<String>,
    onTeamClick: (String) -> Unit,
    onManageClick: () -> Unit
) {
    val sampleTeams = listOf(
        Pair("ريال مدريد", "https://a.espncdn.com/i/teamlogos/soccer/500/86.png"),
        Pair("برشلونة", "https://a.espncdn.com/i/teamlogos/soccer/500/83.png"),
        Pair("ليفربول", "https://a.espncdn.com/i/teamlogos/soccer/500/364.png"),
        Pair("مانشستر سيتي", "https://a.espncdn.com/i/teamlogos/soccer/500/382.png"),
        Pair("أرسنال", "https://a.espncdn.com/i/teamlogos/soccer/500/359.png"),
        Pair("الهلال", "https://a.espncdn.com/i/teamlogos/soccer/500/7338.png"),
        Pair("النصر", "https://a.espncdn.com/i/teamlogos/soccer/500/7339.png"),
        Pair("الاتحاد", "https://a.espncdn.com/i/teamlogos/soccer/500/7337.png")
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sampleTeams) { (name, logo) ->
            val isFav = favoriteTeams.contains(name)
            Surface(
                onClick = { onTeamClick(name) },
                shape = RoundedCornerShape(12.dp),
                color = AppSurfaceCard,
                border = BorderStroke(
                    1.dp,
                    if (isFav) AppAccentGold else AppCardBorder
                ),
                modifier = Modifier.width(82.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(36.dp)) {
                        AsyncImage(
                            model = logo,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isFav) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AppAccentGold,
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopStart)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Empty State & Dialogs
// -------------------------------------------------------------

@Composable
private fun EmptyMatchesCard(
    activeTab: String,
    onResetFilter: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        shape = RoundedCornerShape(16.dp),
        color = AppSurfaceCard,
        border = BorderStroke(1.dp, AppCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SportsSoccer,
                contentDescription = null,
                tint = AppTextSubtle,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (activeTab) {
                    "live" -> "لا توجد مباريات جارية حالياً"
                    "upcoming" -> "لا توجد مباريات قادمة في هذا اليوم"
                    "results" -> "لا توجد نتائج مسجلة لهذا اليوم"
                    "favorites" -> "لا توجد مباريات لأنديتك المفضلة اليوم"
                    else -> "لا توجد مباريات مسجلة لهذا التاريخ في الدوريات المحددة"
                },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "جرب تغيير التاريخ من شريط الأيام أعلاه أو اختيار دوري آخر",
                color = AppTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onResetFilter,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryRed)
            ) {
                Text(
                    text = "عرض كل مباريات اليوم",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FavoriteTeamsDialog(
    favoriteTeams: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allSelectableTeams = listOf(
        "ريال مدريد", "برشلونة", "أتلتيكو مدريد", "ليفربول", "مانشستر سيتي", "أرسنال", "مانشستر يونايتد",
        "تشيلسي", "توتنهام", "بايرن ميونخ", "بوروسيا دورتموند", "باريس سان جيرمان", "إنتر ميلان", "ميلان",
        "يوفنتوس", "الهلال", "النصر", "الاتحاد", "الأهلي السعودي", "الأهلي المصري", "الزمالك", "الترجي"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppSurfaceCard,
            border = BorderStroke(1.dp, AppCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إدارة الأندية المفضلة ⭐",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = "اختر أنديتك لتصلك إشعارات المباريات وتظهر في أعلى الصفحة",
                    color = AppTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    items(allSelectableTeams) { teamName ->
                        val isFav = favoriteTeams.contains(teamName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleFavorite(teamName) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = teamName,
                                color = if (isFav) AppAccentGold else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isFav) FontWeight.Bold else FontWeight.Normal
                            )

                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isFav) AppAccentGold else AppTextSubtle
                            )
                        }
                        HorizontalDivider(color = AppCardBorder.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryRed)
                ) {
                    Text(
                        text = "حفظ وإغلاق",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Admin Published Events Carousel & Stream Cards
// -------------------------------------------------------------

private fun playEventWithServers(
    event: EventChannel,
    allChannels: List<Channel>,
    allEvents: List<EventChannel>,
    viewModel: MainViewModel,
    onShowMultiChannel: (List<EventChannel>) -> Unit
) {
    // 1. Resolve multiple channels if targetChannelIds is present
    if (event.targetChannelIds.isNotEmpty()) {
        val resolved = event.targetChannelIds.mapNotNull { id ->
            // Check in events (broadcast channels with servers)
            val fromEvents = allEvents.find { it.id == id && it.eventType == "stream" }
            if (fromEvents != null) return@mapNotNull fromEvents
            
            // Check in standard broadcast channels
            val fromChannels = allChannels.find { it.id == id }
            if (fromChannels != null) {
                return@mapNotNull EventChannel(
                    id = fromChannels.id,
                    name = fromChannels.name,
                    imageUrl = fromChannels.logoUrl,
                    streamUrl = fromChannels.streamUrl,
                    forcedFormat = fromChannels.forcedFormat,
                    servers = emptyList()
                )
            }
            null
        }
        
        if (resolved.size > 1) {
            onShowMultiChannel(resolved)
            return
        } else if (resolved.size == 1) {
            viewModel.startPlayingEvent(resolved)
            return
        }
    }

    // 2. Fallback to single target channel or direct streamUrl with servers
    val targetChannel = allChannels.find { it.id == event.targetChannelId }
    val defaultUrl = if (event.streamUrl.isNotBlank()) event.streamUrl else targetChannel?.streamUrl ?: ""
    val defaultFormat = if (event.forcedFormat.isNotBlank()) event.forcedFormat else targetChannel?.forcedFormat ?: "m3u8"

    val serversList = if (event.servers.isNotEmpty()) {
        event.servers
    } else if (defaultUrl.isNotBlank()) {
        listOf(
            EventServer(
                id = "srv_1",
                name = "السيرفر الأساسي",
                url = defaultUrl,
                forcedFormat = defaultFormat.ifEmpty { "m3u8" }
            )
        )
    } else {
        emptyList()
    }

    val primaryUrl = serversList.firstOrNull()?.url ?: defaultUrl
    val primaryFormat = serversList.firstOrNull()?.forcedFormat ?: defaultFormat

    if (primaryUrl.isNotBlank() || serversList.isNotEmpty()) {
        viewModel.startPlaying(
            url = primaryUrl,
            title = event.name,
            isLive = true,
            forcedFormat = primaryFormat,
            servers = serversList
        )
    }
}

@Composable
private fun EventMatchesCarousel(
    matches: List<EventChannel>,
    allChannels: List<Channel>,
    onPlayMatch: (EventChannel) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { matches.size })

    LaunchedEffect(matches.size) {
        if (matches.size > 1) {
            while (true) {
                delay(3500)
                val nextPage = (pagerState.currentPage + 1) % matches.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
        ) { page ->
            val match = matches[page]
            val targetChannel = allChannels.find { it.id == match.targetChannelId }
            MatchCardItem(match = match, targetChannel = targetChannel, onPlay = { onPlayMatch(match) })
        }

        if (matches.size > 1) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(matches.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 7.dp else 4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AppAccentGold else Color.Gray.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchCardItem(
    match: EventChannel,
    targetChannel: Channel?,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(22.dp),
        color = AppSurfaceCard,
        border = BorderStroke(1.2.dp, AppCardBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Prioritize Match Image for prominent match banners
            AsyncImage(
                model = if (match.imageUrl.isNotBlank()) match.imageUrl else targetChannel?.logoUrl,
                contentDescription = match.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dynamic High Contrast Premium Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, AppAccentGold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = AppAccentGold,
                                modifier = Modifier.size(11.dp)
                            )
                            if (match.startTimeMs > System.currentTimeMillis()) {
                                EventCountdownTimer(startTimeMs = match.startTimeMs)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(5.dp).background(AppLiveGreen, CircleShape))
                                    Text(
                                        text = "مباشر الآن",
                                        color = AppAccentGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AppPrimaryRed, Color(0xFF991B1B))
                                )
                            )
                            .border(1.2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = match.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp
                    )
                    
                    if (targetChannel != null || match.targetChannelIds.isNotEmpty()) {
                        val displayText = if (match.targetChannelIds.isNotEmpty()) {
                            "متوفر على ${match.targetChannelIds.size} قنوات بث"
                        } else {
                            "متوفر على قناة ${targetChannel?.name}"
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = AppLiveGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(0.5.dp, AppLiveGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "HD 4K",
                                    color = AppLiveGreen,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = displayText,
                                color = AppTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventStreamCard(
    event: EventChannel,
    onPlay: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(14.dp),
        color = AppSurfaceCard,
        border = BorderStroke(1.dp, AppCardBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, 
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(AppPrimaryRed.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = event.name,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
