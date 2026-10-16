package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.model.Channel
import com.example.model.EventChannel
import com.example.ui.MainViewModel
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.LoopRed

val PrimaryDark = Color(0xFF0A0E1A)
val CardDark = Color(0xFF131929)
val BorderDark = Color(0xFF222B45)
val AccentGold = Color(0xFFFFB800)
val LiveRed = Color(0xFFE50914)
val LiveGreen = Color(0xFF00E676)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailDialog(
    match: EspnMatch,
    summary: EspnMatchSummary?,
    isLoadingSummary: Boolean,
    standings: List<EspnStandingEntry>,
    isStandingsLoading: Boolean,
    newsList: List<YsNewsItem>,
    isNewsLoading: Boolean,
    isSubscribed: Boolean,
    onToggleSubscription: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الملخص", "جدول الترتيب", "أخبار اللقاء", "الأحداث", "التشكيلة", "الإحصائيات")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PrimaryDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar with status bar padding
                Surface(
                    color = Color(0xFF0F1420),
                    border = BorderStroke(1.dp, Color(0xFF1E283E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CardDark)
                                .border(1.dp, BorderDark, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "تفاصيل اللقاء",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = match.leagueArabicName,
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = CardDark,
                            border = BorderStroke(1.dp, BorderDark),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SportsSoccer,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
                ) {
                    // Match Scoreboard Header Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF161E33), Color(0xFF0F1524))
                                    )
                                )
                                .border(BorderStroke(1.dp, BorderDark), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // League & Round
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E283E).copy(alpha = 0.7f),
                                    border = BorderStroke(0.5.dp, BorderDark)
                                ) {
                                    Text(
                                        text = "${match.leagueArabicName} ${if (match.roundName.isNotBlank()) "- ${match.roundName}" else ""}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Teams & Score Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Home Team
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.3f),
                                            border = BorderStroke(1.dp, BorderDark),
                                            modifier = Modifier.size(62.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                                AsyncImage(
                                                    model = match.homeTeam.logoUrl.ifEmpty { "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/default-team-logo-500.png" },
                                                    contentDescription = match.homeTeam.displayName,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = match.homeTeam.arabicName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Score / Status in Center
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        if (match.statusState == "in") {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = LiveRed,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.White)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "مباشر ${match.statusDisplay}",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        if (match.statusState == "pre") {
                                            val accurateTime = if (match.timestampMs > 0) {
                                                EspnSoccerService.formatArabicMatchTime(match.timestampMs)
                                            } else {
                                                match.formattedTime
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF1F293D),
                                                border = BorderStroke(1.dp, BorderDark),
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            ) {
                                                Text(
                                                    text = "VS",
                                                    color = AccentGold,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = accurateTime,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                text = "${match.homeTeam.score} - ${match.awayTeam.score}",
                                                color = Color.White,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            if (match.statusState == "in") {
                                                Text(
                                                    text = match.statusDisplay,
                                                    color = AccentGold,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "انتهت المباراة",
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Away Team
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.3f),
                                            border = BorderStroke(1.dp, BorderDark),
                                            modifier = Modifier.size(62.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                                AsyncImage(
                                                    model = match.awayTeam.logoUrl.ifEmpty { "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/default-team-logo-500.png" },
                                                    contentDescription = match.awayTeam.displayName,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = match.awayTeam.arabicName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Venue name
                                if (match.venueName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stadium,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = match.venueName,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notification Alert Bell Button
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { onToggleSubscription() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSubscribed) AccentGold.copy(alpha = 0.15f) else Color(0xFF1E283E)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSubscribed) AccentGold else BorderDark
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSubscribed) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (isSubscribed) AccentGold else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isSubscribed) "تم تفعيل التنبيهات للأهداف والبداية 🔔" else "تنبيهي عند بداية اللقاء وتسجيل الأهداف 🔕",
                                    color = if (isSubscribed) AccentGold else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Navigation Tabs
                    item {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = AccentGold,
                            edgePadding = 16.dp,
                            divider = {},
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) LiveRed.copy(alpha = 0.15f) else Color.Transparent,
                                            border = if (isSelected) BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f)) else null
                                        ) {
                                            Text(
                                                text = title,
                                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Tab Content Body
                    if (isLoadingSummary) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentGold)
                            }
                        }
                    } else if (summary == null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "جاري تحضير البيانات المباشرة للمباراة...",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        when (selectedTab) {
                            0 -> { // الملخص (Split Timeline)
                                item {
                                    SummaryTimelineView(summary = summary, homeTeam = match.homeTeam, awayTeam = match.awayTeam)
                                }
                            }
                            1 -> { // جدول الترتيب (Standings)
                                item {
                                    LeagueStandingsView(
                                        standings = standings,
                                        isLoading = isStandingsLoading,
                                        homeTeamName = match.homeTeam.arabicName,
                                        awayTeamName = match.awayTeam.arabicName
                                    )
                                }
                            }
                            2 -> { // أخبار اللقاء (News)
                                item {
                                    YsMatchNewsView(newsList = newsList, isLoading = isNewsLoading)
                                }
                            }
                            3 -> { // الأحداث (Events only)
                                item {
                                    EventsOnlyView(summary = summary, homeTeam = match.homeTeam, awayTeam = match.awayTeam)
                                }
                            }
                            4 -> { // التشكيلة (Lineups)
                                item {
                                    LineupsView(summary = summary)
                                }
                            }
                            5 -> { // الإحصائيات (Statistics)
                                item {
                                    StatisticsView(summary = summary, homeTeam = match.homeTeam, awayTeam = match.awayTeam)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Sub-Views for Tabs
// -------------------------------------------------------------

@Composable
fun SummaryTimelineView(
    summary: EspnMatchSummary,
    homeTeam: EspnTeam,
    awayTeam: EspnTeam
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "أبرز أحداث المباراة ⚡",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        if (summary.keyEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم تُسجل أحداث بارزة حتى الآن في هذه المباراة",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            // Render a beautiful split timeline
            Column(modifier = Modifier.fillMaxWidth()) {
                summary.keyEvents.forEach { event ->
                    val isHome = event.teamId == homeTeam.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Section (Away Team Events)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (!isHome) {
                                TimelineEventCard(event = event)
                            }
                        }

                        // Center Timeline Node (Minute & Dot)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(50.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (event.type) {
                                            "goal" -> Color(0xFF10B981)
                                            "sub" -> Color(0xFF3B82F6)
                                            "card-yellow" -> Color(0xFFF59E0B)
                                            "card-red" -> Color(0xFFEF4444)
                                            else -> Color(0xFF64748B)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = event.clock.replace("'", ""),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right Section (Home Team Events)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (isHome) {
                                TimelineEventCard(event = event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineEventCard(event: EspnKeyEvent) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when (event.type) {
                        "goal" -> "⚽ هدف!"
                        "sub" -> "🔄 تبديل"
                        "card-yellow" -> "🟨 بطاقة"
                        "card-red" -> "🟥 طرد"
                        else -> event.titleArabic
                    },
                    color = when (event.type) {
                        "goal" -> Color(0xFF10B981)
                        "sub" -> Color(0xFF3B82F6)
                        "card-yellow" -> Color(0xFFF59E0B)
                        "card-red" -> Color(0xFFEF4444)
                        else -> Color.White
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.playerName.ifEmpty { event.description },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (event.assistPlayerName.isNotBlank()) {
                Text(
                    text = "مساعدة: ${event.assistPlayerName}",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (event.subOutPlayerName.isNotBlank()) {
                Text(
                    text = "خروج: ${event.subOutPlayerName}",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LeagueStandingsView(
    standings: List<EspnStandingEntry>,
    isLoading: Boolean,
    homeTeamName: String,
    awayTeamName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "جدول ترتيب الدوري 🏆",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (standings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "جدول الترتيب غير متوفر حالياً لهذا الدوري",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            // Header row of the standing table
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1420), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .border(BorderStroke(1.dp, BorderDark), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ت", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text(text = "الفريق", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = "لعب", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text(text = "ف", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text(text = "ت", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text(text = "خ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text(text = "الفارق", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                Text(text = "نقاط", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }

            // Entries rows
            standings.forEach { entry ->
                val isCurrentMatchTeam = entry.teamArabicName.contains(homeTeamName) || homeTeamName.contains(entry.teamArabicName) ||
                        entry.teamArabicName.contains(awayTeamName) || awayTeamName.contains(entry.teamArabicName)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrentMatchTeam) AccentGold.copy(alpha = 0.12f) else Color.Transparent
                        )
                        .border(
                            BorderStroke(0.5.dp, if (isCurrentMatchTeam) AccentGold.copy(alpha = 0.4f) else BorderDark)
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank
                    Text(
                        text = entry.rank.toString(),
                        color = if (isCurrentMatchTeam) AccentGold else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    // Team logo & name
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = entry.teamLogoUrl.ifEmpty { "https://a.espncdn.com/combiner/i?img=/i/teamlogos/soccer/500/default-team-logo-500.png" },
                            contentDescription = entry.teamName,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.teamArabicName,
                            color = if (isCurrentMatchTeam) AccentGold else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrentMatchTeam) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Played, Wins, Draws, Losses, GD, Points
                    Text(text = entry.gamesPlayed.toString(), color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    Text(text = entry.wins.toString(), color = Color(0xFF10B981), fontSize = 11.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text(text = entry.draws.toString(), color = Color(0xFF64748B), fontSize = 11.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text(text = entry.losses.toString(), color = LiveRed, fontSize = 11.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text(text = entry.goalDifference, color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                    Text(
                        text = entry.points.toString(),
                        color = if (isCurrentMatchTeam) AccentGold else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun YsMatchNewsView(
    newsList: List<YsNewsItem>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "آخر أخبار كرة القدم والمباريات 📰",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (newsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد أخبار متاحة حالياً",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            newsList.forEach { article ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Article Image
                        if (article.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = article.imageUrl,
                                contentDescription = article.title,
                                modifier = Modifier
                                    .size(75.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        
                        // Title and Description
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = article.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = article.description,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = article.timeAgo,
                                    color = AccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventsOnlyView(
    summary: EspnMatchSummary,
    homeTeam: EspnTeam,
    awayTeam: EspnTeam
) {
    SummaryTimelineView(summary = summary, homeTeam = homeTeam, awayTeam = awayTeam)
}

@Composable
fun LineupsView(summary: EspnMatchSummary) {
    var selectedRosterTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Switch between Home and Away Lineup
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedRosterTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRosterTab == 0) AccentGold else CardDark
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selectedRosterTab == 0) AccentGold else BorderDark)
            ) {
                Text(
                    text = summary.homeRoster.teamName.ifEmpty { "الفريق المضيف" },
                    color = if (selectedRosterTab == 0) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = { selectedRosterTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRosterTab == 1) AccentGold else CardDark
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selectedRosterTab == 1) AccentGold else BorderDark)
            ) {
                Text(
                    text = summary.awayRoster.teamName.ifEmpty { "الفريق الضيف" },
                    color = if (selectedRosterTab == 1) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        val activeRoster = if (selectedRosterTab == 0) summary.homeRoster else summary.awayRoster

        if (activeRoster.starters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "التشكيلة الرسمية ستتوفر قبل بدء المباراة بـ 45 دقيقة",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            // Starting 11 Section
            Text(
                text = "التشكيلة الأساسية (11 لاعب) ⭐",
                color = AccentGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            activeRoster.starters.forEach { player ->
                PlayerRowItem(player = player)
            }

            // Bench Section
            if (activeRoster.bench.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "دكة البدلاء 🔄",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                activeRoster.bench.forEach { player ->
                    PlayerRowItem(player = player)
                }
            }
        }
    }
}

@Composable
fun PlayerRowItem(player: EspnRosterPlayer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Player Jersey Badge
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF334155),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = player.jersey.ifEmpty { "-" },
                            color = AccentGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Avatar / Headshot
                if (player.headshotUrl.isNotBlank()) {
                    AsyncImage(
                        model = player.headshotUrl,
                        contentDescription = player.name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name
                Text(
                    text = player.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Position Label
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (player.positionCategory) {
                    "G" -> Color(0xFF0284C7)
                    "D" -> Color(0xFF059669)
                    "M" -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                }
            ) {
                Text(
                    text = player.position,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun StatisticsView(
    summary: EspnMatchSummary,
    homeTeam: EspnTeam,
    awayTeam: EspnTeam
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Teams Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = homeTeam.arabicName,
                color = AccentGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "الإحصائيات المباشرة",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = awayTeam.arabicName,
                color = Color(0xFF60A5FA),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        if (summary.statistics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "إحصائيات المباراة ستتوفر مع انطلاق صافرة البداية",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            summary.statistics.forEach { stat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stat.homeValue,
                                color = AccentGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = stat.labelArabic,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = stat.awayValue,
                                color = Color(0xFF60A5FA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dual Color Comparison Bar
                        val total = (stat.homeNumeric + stat.awayNumeric).coerceAtLeast(1f)
                        val homeRatio = (stat.homeNumeric / total).coerceIn(0.1f, 0.9f)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF334155))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(homeRatio)
                                    .background(AccentGold)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f - homeRatio)
                                    .background(Color(0xFF60A5FA))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchInfoView(
    match: EspnMatch,
    summary: EspnMatchSummary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "معلومات تفصيلية 📋",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val accurateTime = if (match.timestampMs > 0) EspnSoccerService.formatArabicMatchTime(match.timestampMs) else match.formattedTime
        val infoItems = listOf(
            Pair("البطولة", match.leagueArabicName),
            Pair("الموعد والتاريخ", "${match.formattedDate} - $accurateTime"),
            Pair("الملعب", summary.gameInfo.venue.ifEmpty { match.venueName }),
            Pair("الطقس", summary.gameInfo.weather),
            Pair("التحكيم", summary.gameInfo.referees),
            Pair("الحضور الجماهيري", if (summary.gameInfo.attendance.isNotBlank()) summary.gameInfo.attendance else "غير معلن")
        )

        infoItems.forEach { (label, value) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = BorderStroke(1.dp, BorderDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
