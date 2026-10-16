package com.example.ui
import com.example.ui.theme.GoldGlow
import com.example.R

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.*
import com.example.ui.components.HomeLiveOnlineCounterWidget
import com.example.ui.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeHubScreen(
    viewModel: MainViewModel,
    onNavigateTab: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onSelectMovie: (Movie) -> Unit,
    onSelectSeries: (Series) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val homeConfig by viewModel.homeConfig.collectAsState()
    val heroBanners by viewModel.homeHeroBanners.collectAsState()
    val promoAds by viewModel.promoAds.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val events by viewModel.events.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val series by viewModel.series.collectAsState()
    val isGlobalAdsEnabled by viewModel.isGlobalAdsEnabled.collectAsState()
    
    val activeAccount by viewModel.activeAccount.collectAsState()
    val showChatTabOnline by viewModel.showChatTab.collectAsState()
    val showPackagesSectionOnline by viewModel.showPackagesSection.collectAsState()

    val allowedTypes = activeAccount?.allowedTypes ?: listOf("live", "movies", "series")
    val showLive = allowedTypes.contains("live")
    val showMovies = allowedTypes.contains("movies")
    val showSeries = allowedTypes.contains("series")
    val showPackages = showPackagesSectionOnline
    val showChat = showChatTabOnline

    val allChannels = remember(channels, events) {
        val eventChannelsAsChannels = events.map {
            com.example.model.Channel(
                id = it.id,
                name = it.name,
                logoUrl = it.imageUrl,
                streamUrl = it.streamUrl,
                forcedFormat = it.forcedFormat
            )
        }
        (eventChannelsAsChannels + channels).distinctBy { it.id.ifEmpty { it.name } }
    }

    val activeAds = remember(promoAds) { promoAds.filter { it.isEnabled } }
    val activeBanners = remember(heroBanners) { heroBanners.filter { it.isEnabled } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Announcements Bar (Online count removed as requested)
        if (homeConfig.showAnnouncements && homeConfig.announcementText.isNotBlank()) {
            item {
                HomeAnnouncementBar(
                    text = homeConfig.announcementText
                )
            }
        }

        // 2. Cinematic Hero Carousel
        if (homeConfig.showHeroCarousel && activeBanners.isNotEmpty()) {
            item {
                HomeHeroCarousel(
                    banners = activeBanners,
                    onBannerClick = { banner ->
                        when (banner.targetType) {
                            "tab" -> onNavigateTab(banner.targetValue.ifEmpty { "channels" })
                            "url" -> {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.targetValue))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> onNavigateTab("channels")
                        }
                    }
                )
            }
        }

        // Live Online Users Counter
        if (homeConfig.showOnlineUsers) {
            item {
                HomeLiveOnlineCounterWidget(viewModel = viewModel)
            }
        }

        // 4. Quick Navigation Hub (بوابة الأقسام الفخمة المتطابقة مع الصورة المرفقة تماماً)
        if (homeConfig.showQuickNav && (showLive || showMovies || showSeries || showPackages || showChat)) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    HomeSectionDividerTitle(
                        title = "بوابة الأقسام والتنقل السريع",
                        subtitle = "اختر وجهتك المفضلة واستمتع بتجربة سينمائية متكاملة"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HomeNavigationHub(
                        channelCount = if (allChannels.isNotEmpty()) allChannels.size else 2122,
                        mediaCount = (movies.size + series.size).let { if (it > 0) it else 30592 },
                        onNavigate = onNavigateTab
                    )
                }
            }
        }

        // 4. Promotional Ads & Announcements Showcase (قسم الإعلانات والترويج)
        if (isGlobalAdsEnabled && homeConfig.showPromoAds && activeAds.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeaderTitle(
                        title = "عروض وإعلانات مميزة",
                        subtitle = "تابع قنواتنا الرسمية وعروض الاشتراكات الحصرية والتحديثات",
                        icon = Icons.Default.Campaign
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        activeAds.forEach { ad ->
                            PromoAdCard(ad = ad)
                        }
                    }
                }
            }
        }

        // 5. Featured Live Channels Shelf (يختفي إذا تم إخفاء البث المباشر)
        if (homeConfig.showTrendingChannels && showLive && allChannels.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeaderTitle(
                            title = "أبرز القنوات المباشرة",
                            subtitle = "قنوات رياضية وترفيهية تبث الآن بأعلى جودة",
                            icon = Icons.Default.LiveTv
                        )
                        TextButton(onClick = { onNavigateTab("channels") }) {
                            Text("عرض الكل", color = Color(0xFFE50914), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allChannels.take(15)) { channel ->
                            HomeChannelQuickCard(
                                channel = channel,
                                onClick = { onPlayChannel(channel) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Latest 4K Movies Shelf (يختفي إذا تم إخفاء الأفلام)
        if (homeConfig.showLatestMovies && showMovies && movies.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeaderTitle(
                            title = "أحدث الأفلام المضافة",
                            subtitle = "مكتبة سينمائية عالمية بجودة 4K فائقة الوضوح",
                            icon = Icons.Default.Movie
                        )
                        TextButton(onClick = { onNavigateTab("library") }) {
                            Text("عرض الكل", color = Color(0xFFE50914), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(movies.take(15)) { movie ->
                            HomeMediaPosterCard(
                                title = movie.title,
                                posterUrl = movie.posterUrl,
                                rating = movie.rating,
                                year = movie.year,
                                badge = movie.quality.ifEmpty { "4K UHD" },
                                onClick = { onSelectMovie(movie) }
                            )
                        }
                    }
                }
            }
        }

        // 7. Popular Series Shelf (يختفي إذا تم إخفاء المسلسلات من الشريط السفلي)
        if (homeConfig.showPopularSeries && showSeries && series.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeaderTitle(
                            title = "أشهر المسلسلات الحصرية",
                            subtitle = "مواسم كاملة وحلقات متجددة بأعلى دقة",
                            icon = Icons.Default.Tv
                        )
                        TextButton(onClick = { onNavigateTab("library") }) {
                            Text("عرض الكل", color = Color(0xFFE50914), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(series.take(15)) { s ->
                            HomeMediaPosterCard(
                                title = s.title,
                                posterUrl = s.posterUrl,
                                rating = s.rating,
                                year = s.year,
                                badge = s.quality.ifEmpty { "FHD 1080p" },
                                onClick = { onSelectSeries(s) }
                            )
                        }
                    }
                }
            }
        }

        // 8. Football News Section (يظهر حصرياً بالرئيسية إذا تم تفعيله من الإعدادات)
        if (homeConfig.showNewsSection) {
            item {
                HomeNewsSection(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HomeAnnouncementBar(
    text: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Status indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF00E676).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer { alpha = dotAlpha }
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مباشر",
                        color = Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Announcement text
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HomeHeroCarousel(
    banners: List<HomeHeroBanner>,
    onBannerClick: (HomeHeroBanner) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { if (banners.isNotEmpty()) banners.size else 4 })
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll every 4 seconds
    LaunchedEffect(banners.size) {
        if (banners.size > 1) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) { page ->
            val banner = if (page < banners.size) banners[page] else banners.firstOrNull()
            val bannerTitle = if (page == 0) "دوري أبطال أوروبا" else (banner?.title ?: "دوري أبطال أوروبا")
            val bannerSubtitle = if (page == 0) "دوري أبطال أوروبا & البطولات الكبرى" else (banner?.subtitle ?: "أقوى المباريات والقنوات العالمية")
            val bannerDesc = "استمتع بأفضل المباريات بجودة عالية"

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .tvFocusable(shape = RoundedCornerShape(22.dp), onClick = {
                        if (banner != null) onBannerClick(banner)
                    }),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070A14)),
                border = BorderStroke(1.dp, Color(0xFF1E283E).copy(alpha = 0.8f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Hero Image (UEFA Champions League Trophy & Stadium)
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ucl_hero_banner_1788316403953),
                        contentDescription = bannerTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Cinematic Gradient Overlays for maximum text contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF050711).copy(alpha = 0.94f),
                                        Color(0xFF050711).copy(alpha = 0.75f),
                                        Color(0xFF050711).copy(alpha = 0.20f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF050711).copy(alpha = 0.70f),
                                        Color(0xFF050711).copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    // Content details
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Red "حصري" Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "حصري",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "🔥", fontSize = 11.sp)
                                }
                            }

                            // Translucent "جودة 4K" Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0B101D).copy(alpha = 0.85f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "جودة 4K",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Middle & Bottom Text and Action Buttons
                        Column {
                            Text(
                                text = bannerTitle,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = bannerSubtitle,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = bannerDesc,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Primary Red Button
                                Button(
                                    onClick = { if (banner != null) onBannerClick(banner) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "مشاهدة الآن",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Secondary Dark Glass Button
                                Surface(
                                    onClick = { if (banner != null) onBannerClick(banner) },
                                    color = Color(0xFF0F1526).copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF283450)),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "استكشاف المزيد",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pager indicators (Capsule indicator + 3 dots)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalDots = if (banners.isNotEmpty()) banners.size else 4
            repeat(totalDots) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color = if (isSelected) Color(0xFFDC2626) else Color.White.copy(alpha = 0.3f)
                val width = if (isSelected) 18.dp else 5.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(width = width, height = 5.dp)
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(iteration) }
                        }
                )
            }
        }
    }
}

@Composable
fun HomeSectionDividerTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left subtle red glowing line
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFFDC2626))
                        )
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Right subtle red glowing line
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFDC2626), Color.Transparent)
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HomeNavigationHub(
    channelCount: Int,
    mediaCount: Int,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Two Side-by-Side Cards (Live Streaming & Library)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start (Right) Card: بث مباشر
            HomeLiveChannelsCard(
                modifier = Modifier.weight(1f),
                channelCount = channelCount,
                onClick = { onNavigate("channels") }
            )

            // End (Left) Card: المكتبة
            HomeLibraryCard(
                modifier = Modifier.weight(1f),
                mediaCount = mediaCount,
                onClick = { onNavigate("library") }
            )
        }

        // Full-Width Bottom Card: غرفة الدردشة
        HomeChatRoomCard(
            onClick = { onNavigate("chat") }
        )
    }
}

@Composable
fun HomeLiveChannelsCard(
    modifier: Modifier = Modifier,
    channelCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(240.dp)
            .tvFocusable(shape = RoundedCornerShape(20.dp), onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E1C)),
        border = BorderStroke(1.dp, Color(0xFF1E283E).copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Red Icon + Channel Count Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Red TV Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE50914)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = "بث مباشر",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Dark Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF241217))
                        .border(0.5.dp, Color(0xFF591B24), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format("%,d", channelCount)} قناة",
                        color = Color(0xFFFF4D5A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Title and Subtitle
            Column {
                Text(
                    text = "بث مباشر",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "قنوات فضائية ورياضية",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Channel Logos Grid (2 rows x 3 badges)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BeinSportsLogoBadge(modifier = Modifier.weight(1f))
                    SscLogoBadge(modifier = Modifier.weight(1f))
                    OnTimeLogoBadge(modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MbcLogoBadge(modifier = Modifier.weight(1f))
                    KsaSportsLogoBadge(modifier = Modifier.weight(1f))
                    EkhbariyaLogoBadge(modifier = Modifier.weight(1f))
                }
            }

            // Bottom Arrow Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E1016))
                        .border(0.8.dp, Color(0xFF5A1C25), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color(0xFFFF6B7A),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeLibraryCard(
    modifier: Modifier = Modifier,
    mediaCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(240.dp)
            .tvFocusable(shape = RoundedCornerShape(20.dp), onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E1C)),
        border = BorderStroke(1.2.dp, Color(0xFF8B1D2C).copy(alpha = 0.85f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Movie posters fan collage in background bottom
            Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.movies_fan_art_1788316437956),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = 0.45f }
            )

            // Gradient fade over the posters
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0E1C),
                                Color(0xFF0A0E1C).copy(alpha = 0.6f),
                                Color(0xFF0A0E1C).copy(alpha = 0.2f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section (Red Icon + Media Count Badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Red Film Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE50914)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = "المكتبة",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Dark Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF241217))
                            .border(0.5.dp, Color(0xFF591B24), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${String.format("%,d", mediaCount)} عمل",
                            color = Color(0xFFFF4D5A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Title and Subtitle
                Column {
                    Text(
                        text = "المكتبة",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "أفلام ومسلسلات بدقة 4K",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Arrow Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E1016))
                            .border(0.8.dp, Color(0xFF5A1C25), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color(0xFFFF6B7A),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeChatRoomCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .tvFocusable(shape = RoundedCornerShape(22.dp), onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.5.dp, Color(0xFF334155))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // New High Quality Illustration
            AsyncImage(
                model = R.drawable.chat_community_illustration_1788387853849,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterEnd)
            )

            // Dynamic Gradient Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF0F172A).copy(alpha = 0.95f),
                                Color(0xFF0F172A).copy(alpha = 0.7f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 1000f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE50914).copy(alpha = 0.2f))
                            .border(0.5.dp, Color(0xFFE50914), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "بث مباشر وتفاعل",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "غرفة الدردشة الجماعية",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    Text(
                        text = "تفاعل مع آلاف المشاهدين، شارك التوقعات والآراء في بث حي ومباشر وحصري لمباريات اليوم.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(0.65f)
                    )
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(
                        "دخول الغرفة الآن",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Channel Logo Badges inside the Live Card
@Composable
private fun BeinSportsLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1F1133))
            .border(0.5.dp, Color(0xFF3B1E63), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "beIN",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "SPORTS",
                color = Color(0xFFC084FC),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SscLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF131828))
            .border(0.5.dp, Color(0xFF263252), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SSC",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun OnTimeLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF241014))
            .border(0.5.dp, Color(0xFF4A1A22), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "ON",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "TIME",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MbcLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF111420))
            .border(0.5.dp, Color(0xFF22283D), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "mbc",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun KsaSportsLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C1F16))
            .border(0.5.dp, Color(0xFF183F2C), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "الرياضية",
                color = Color(0xFF4ADE80),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = "🇸🇦", fontSize = 8.sp)
        }
    }
}

@Composable
private fun EkhbariyaLogoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF151026))
            .border(0.5.dp, Color(0xFF2E2252), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "الإخبارية",
            color = Color(0xFFA78BFA),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PromoAdCard(ad: PromoAd) {
    val context = LocalContext.current

    val isTelegram = ad.type.equals("telegram", ignoreCase = true) || ad.targetUrl.contains("t.me")
    val isVip = ad.type.equals("vip", ignoreCase = true)

    val primaryColor = when {
        isTelegram -> Color(0xFF229ED9)
        isVip -> Color(0xFFFFD700)
        else -> Color(0xFFE50914)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(18.dp), onClick = {
                if (ad.targetUrl.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.targetUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                    }
                }
            }),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101528)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (ad.imageUrl.isNotBlank()) {
                // Background subtle banner image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ad.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF101528).copy(alpha = 0.4f),
                                    Color(0xFF101528).copy(alpha = 0.95f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isTelegram) Icons.Default.Send else if (isVip) Icons.Default.Stars else Icons.Default.Campaign,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ad.badge.ifEmpty { "إعلان مميز" },
                                color = primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isTelegram) {
                        Text(
                            text = "@Telegram",
                            color = Color(0xFF229ED9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = ad.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (ad.description.isNotBlank()) {
                    Text(
                        text = ad.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (ad.targetUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.targetUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = ad.buttonText.ifEmpty { "انضم الآن" },
                            color = if (isVip) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowOutward,
                            contentDescription = null,
                            tint = if (isVip) Color.Black else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeChannelQuickCard(
    channel: Channel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .tvFocusable(shape = RoundedCornerShape(16.dp), onClick = { onClick() }),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111526)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A0D18)),
                contentAlignment = Alignment.Center
            ) {
                SafeAsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(3.dp),
                    shape = RoundedCornerShape(8.dp),
                    fallbackLabel = channel.name.take(2).uppercase()
                )

                // Live red dot
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = channel.category.ifEmpty { "بث مباشر" },
                color = Color(0xFF00E676),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HomeMediaPosterCard(
    title: String,
    posterUrl: String,
    rating: Double,
    year: Int,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(115.dp)
            .tvFocusable(shape = RoundedCornerShape(16.dp), onClick = { onClick() }),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111526)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                SafeAsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    fallbackLabel = title
                )

                // Top Quality Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(text = badge, color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }

                // Rating Badge
                if (rating > 0.0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = String.format("%.1f", rating), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (year > 0) {
                    Text(
                        text = "$year",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeaderTitle(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE50914).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(16.dp))
        }

        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}


@Composable
fun HomeNewsSection(viewModel: MainViewModel) {
    val newsItems by viewModel.newsItems.collectAsState()
    val isNewsLoading by viewModel.isNewsLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedNewsForDialog by remember { mutableStateOf<NewsItem?>(null) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val filteredNews = if (searchQuery.isBlank()) {
        newsItems
    } else {
        newsItems.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SectionHeaderTitle(
            title = "أحدث أخبار كرة القدم",
            subtitle = "تغطية شاملة ومستمرة لكل ما يخص عالم الساحرة المستديرة",
            icon = Icons.Default.Article
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Professional Search Bar
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .tvFocusable(),
            placeholder = { Text("ابحث في الأخبار...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = GoldGlow) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldGlow,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = GoldGlow,
                focusedContainerColor = Color(0xFF161B22),
                unfocusedContainerColor = Color(0xFF161B22)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isNewsLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldGlow)
            }
        } else if (filteredNews.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد أخبار متاحة حالياً.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredNews, key = { it.id }) { news ->
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .tvFocusable(shape = RoundedCornerShape(16.dp))
                            .clickable { selectedNewsForDialog = news },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2130)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column {
                            if (news.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = news.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = news.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = if (news.imageUrl.isNotBlank()) 2 else 3,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = news.description,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = if (news.imageUrl.isNotBlank()) 2 else 4,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = news.dateStr.take(10), // only YYYY-MM-DD
                                        color = GoldGlow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Elegant Full News Details Dialog
    if (selectedNewsForDialog != null) {
        val news = selectedNewsForDialog!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedNewsForDialog = null },
            containerColor = Color(0xFF0F141C),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(16.dp),
            confirmButton = {},
            dismissButton = {},
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (news.imageUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = news.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Close button overlay
                            IconButton(
                                onClick = { selectedNewsForDialog = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .tvFocusable()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { selectedNewsForDialog = null },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                    .tvFocusable()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (news.imageUrl.isNotBlank()) 250.dp else 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldGlow.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, GoldGlow.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "أخبار كرة القدم ⚽",
                                    color = GoldGlow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = news.dateStr.take(10),
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = news.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 22.sp
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Text(
                            text = news.description,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Button(
                        onClick = { selectedNewsForDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldGlow),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .tvFocusable()
                    ) {
                        Text("الرجوع للأخبار", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        )
    }
}
