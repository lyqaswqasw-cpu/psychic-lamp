package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import com.example.ui.utils.*
import com.example.utils.FastDownloaderEngine

@Composable
fun ChannelsTabContent(
    channels: List<Channel>,
    cardColumns: Int,
    favoritesSet: Set<String>,
    viewModel: MainViewModel
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .testTag("channels_grid"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (channels.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyStateView("لا توجد قنوات مباشرة بحسب الفئة أو البحث المجرى.")
            }
        } else {
            items(channels, key = { it.id }) { chan ->
                ChannelGridCard(
                    chan = chan,
                    isFavorite = favoritesSet.contains(chan.id),
                    onToggleFavorite = { viewModel.toggleFavorite(chan.id) }
                ) {
                    viewModel.startPlaying(chan.streamUrl, chan.name, isLive = true, userAgent = chan.userAgent, forcedFormat = chan.forcedFormat)
                }
            }
        }
    }
}

@Composable
fun MoviesTabContent(
    movies: List<Movie>,
    cardColumns: Int,
    favoritesSet: Set<String>,
    viewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("movies_grid"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (movies.isNotEmpty()) {
            item {
                CinematicHeroBanner(movie = movies.first()) {
                    viewModel.selectMovie(movies.first())
                }
            }
            item { SectionHeader("الأفلام الأكثر مشاهدة الآن", LoopRed) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movies.take(10)) { movie ->
                        MovieGridCard(
                            movie = movie,
                            isFavorite = favoritesSet.contains(movie.id),
                            onFavoriteToggle = { viewModel.toggleFavorite(movie.id) }
                        ) {
                            viewModel.selectMovie(movie)
                        }
                    }
                }
            }
            item { SectionHeader("المكتبة السينمائية الشاملة", GoldGlow) }
            items(movies.chunked(cardColumns)) { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (movie in chunk) {
                        Box(modifier = Modifier.weight(1f)) {
                            MovieGridCard(
                                movie = movie,
                                isFavorite = favoritesSet.contains(movie.id),
                                onFavoriteToggle = { viewModel.toggleFavorite(movie.id) }
                            ) {
                                viewModel.selectMovie(movie)
                            }
                        }
                    }
                    if (chunk.size < cardColumns) {
                        repeat(cardColumns - chunk.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesTabContent(
    series: List<Series>,
    cardColumns: Int,
    favoritesSet: Set<String>,
    viewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("series_grid"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (series.isNotEmpty()) {
            item {
                val topSeries = series.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.selectSeries(topSeries) }
                ) {
                    SafeAsyncImage(
                        model = topSeries.backdropUrl.ifEmpty { topSeries.posterUrl },
                        contentDescription = topSeries.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            item { SectionHeader("المسلسلات الحصرية الأكثر شهرة", LoopRed) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(series.take(10)) { s ->
                        SeriesGridCard(
                            series = s,
                            isFavorite = favoritesSet.contains(s.id),
                            onFavoriteToggle = { viewModel.toggleFavorite(s.id) }
                        ) {
                            viewModel.selectSeries(s)
                        }
                    }
                }
            }
            item { SectionHeader("كافة الأعمال الدرامية", GoldGlow) }
            items(series.chunked(cardColumns)) { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (s in chunk) {
                        Box(modifier = Modifier.weight(1f)) {
                            SeriesGridCard(
                                series = s,
                                isFavorite = favoritesSet.contains(s.id),
                                onFavoriteToggle = { viewModel.toggleFavorite(s.id) }
                            ) {
                                viewModel.selectSeries(s)
                            }
                        }
                    }
                    if (chunk.size < cardColumns) {
                        repeat(cardColumns - chunk.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    favoriteChannels: List<Channel>,
    favoriteMovies: List<Movie>,
    favoriteSeries: List<Series>,
    favoritesSet: Set<String>,
    viewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("favorites_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "مفضلتك الشخصية",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (favoriteChannels.isNotEmpty()) {
            item {
                SectionHeader("القنوات المباشرة المفضلة (${favoriteChannels.size})", LoopRed)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favoriteChannels) { chan ->
                        ChannelGridCard(
                            chan = chan,
                            isFavorite = favoritesSet.contains(chan.id),
                            onToggleFavorite = { viewModel.toggleFavorite(chan.id) }
                        ) {
                            viewModel.startPlaying(chan.streamUrl, chan.name, isLive = true, userAgent = chan.userAgent, forcedFormat = chan.forcedFormat)
                        }
                    }
                }
            }
        }
        if (favoriteMovies.isNotEmpty()) {
            item {
                SectionHeader("الأفلام المفضلة (${favoriteMovies.size})", LoopRed)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favoriteMovies) { movie ->
                        MovieGridCard(
                            movie = movie,
                            isFavorite = favoritesSet.contains(movie.id),
                            onFavoriteToggle = { viewModel.toggleFavorite(movie.id) }
                        ) {
                            viewModel.selectMovie(movie)
                        }
                    }
                }
            }
        }
        if (favoriteSeries.isNotEmpty()) {
            item {
                SectionHeader("المسلسلات المفضلة (${favoriteSeries.size})", LoopRed)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favoriteSeries) { series ->
                        SeriesGridCard(
                            series = series,
                            isFavorite = favoritesSet.contains(series.id),
                            onFavoriteToggle = { viewModel.toggleFavorite(series.id) }
                        ) {
                            viewModel.selectSeries(series)
                        }
                    }
                }
            }
        }
        if (favoriteChannels.isEmpty() && favoriteMovies.isEmpty() && favoriteSeries.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = SoftGreyText.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "لم تقم بإضافة أي محتوى للمفضلة بعد.", color = SoftGreyText, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val savedOfflineList by viewModel.downloads.collectAsState()
    val activeDownload by viewModel.activeDownloadState.collectAsState()
    val isDownloading = activeDownload != null && activeDownload?.phase in listOf("analyzing", "downloading", "merging")
    
    var selectedFilter by remember { mutableStateOf("all") } // "all", "movies", "series"
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val filteredList = remember(savedOfflineList, selectedFilter) {
        when (selectedFilter) {
            "movies" -> savedOfflineList.filter { it.type == "movie" }
            "series" -> savedOfflineList.filter { it.type == "episode" }
            else -> savedOfflineList
        }
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("مسح كافة التنزيلات؟", fontWeight = FontWeight.Bold) },
            text = { Text("سيتم حذف كافة ملفات الفيديو المحملة من ذاكرة هاتفك لتوفير المساحة.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllDownloads()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoopRed)
                ) {
                    Text("نعم، احذف الكل", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("إلغاء", color = SoftGreyText)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Storage Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📥 مكتبة التنزيلات المحلية",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "تشغيل فوري عالي السرعة وبدون استهلاك للإنترنت",
                    color = SoftGreyText,
                    fontSize = 11.sp
                )
            }

            if (savedOfflineList.isNotEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = GoldGlow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.getTotalDownloadsSizeFormatted(),
                            color = GoldGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active Download Live Card (If downloading)
        if (isDownloading && activeDownload != null) {
            val ad = activeDownload!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, LoopRed.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1428)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            SafeAsyncImage(
                                model = ad.posterUrl,
                                contentDescription = ad.title,
                                modifier = Modifier
                                    .size(width = 44.dp, height = 60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ad.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (ad.phase == "analyzing") "جاري الاتصال والفحص..." else if (ad.phase == "merging") "جاري تجميع المقاطع..." else "جاري التنزيل: ${ad.speedFormatted}",
                                        color = GoldGlow,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (ad.etaFormatted.isNotEmpty()) {
                                        Text(
                                            text = " • ${ad.etaFormatted}",
                                            color = SoftGreyText,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.cancelDownload() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ad.downloadedSizeFormatted,
                            color = SoftGreyText,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${(ad.progress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { ad.progress.coerceIn(0f, 1f) },
                        color = LoopRed,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Filter Tabs
        if (savedOfflineList.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("all", "الكل (${savedOfflineList.size})", Icons.Default.AllInclusive),
                    Triple("movies", "الأفلام (${savedOfflineList.count { it.type == "movie" }})", Icons.Default.Movie),
                    Triple("series", "المسلسلات (${savedOfflineList.count { it.type == "episode" }})", Icons.Default.Tv)
                ).forEach { (id, label, icon) ->
                    val isSelected = selectedFilter == id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = id },
                        color = if (isSelected) LoopRed else Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SoftGreyText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else SoftGreyText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { showClearAllConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "مسح الكل", tint = SoftGreyText, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Content / List
        if (savedOfflineList.isEmpty() && !isDownloading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = LoopRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد تنزيلات حتى الآن",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اضغط على زر (تحميل) في أي فيلم أو حلقة مسلسل لتنزيلها محلياً بسرعة فائقة ومشاهدتها بدون إنترنت بأي وقت!",
                        color = SoftGreyText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredList, key = { it.id }) { download ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                SafeAsyncImage(
                                    model = download.posterUrl,
                                    contentDescription = download.title,
                                    modifier = Modifier
                                        .size(width = 75.dp, height = 105.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (download.type == "episode") "حلقة" else "فيلم",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = download.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (download.seriesTitle.isNotEmpty() && download.type == "episode") {
                                    Text(
                                        text = download.seriesTitle,
                                        color = SoftGreyText,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF10341B),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = download.quality.ifEmpty { "1080p FHD" },
                                            color = Color(0xFF4CAF50),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (download.fileSizeBytes > 0) {
                                        Text(
                                            text = FastDownloaderEngine.formatBytes(download.fileSizeBytes),
                                            color = SoftGreyText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = "• متاح بدون نت",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { viewModel.startPlaying(download.videoUrl, download.title, isLive = false) },
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LoopRed),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تشغيل محلي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.deleteDownload(download.id) },
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftGreyText),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف", fontSize = 11.sp)
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
fun SectionHeader(title: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(4.dp, 16.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}
