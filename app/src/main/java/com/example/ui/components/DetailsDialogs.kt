package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.model.*
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.LoopRed
import com.example.ui.theme.SoftGreyText
import com.example.ui.utils.*

private val RedPrimary = Color(0xFFE50914)
private val RedLight = Color(0xFFFF2E3B)
private val DarkBg = Color(0xFF090A13)
private val CardBg = Color(0xFF131525)

/**
 * Movie Details Screen - Matching the right screenshot design
 */
@Composable
fun MovieDetailsDialog(
    movie: Movie,
    allMovies: List<Movie> = emptyList(),
    isFavorite: Boolean,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onToggleFavorite: () -> Unit,
    onPlay: (String) -> Unit,
    onDownload: (Movie) -> Unit,
    onSelectMovie: (Movie) -> Unit = {},
    onDismiss: () -> Unit
) {
    var isStoryExpanded by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Detect related franchise movies
    val relatedFranchiseMovies = remember(movie.id, allMovies) {
        if (allMovies.isEmpty()) emptyList()
        else {
            val cleanTitle = movie.title
                .replace(Regex("\\b(19|20)\\d{2}\\b"), "")
                .replace(Regex("(?i)\\b(4k|uhd|fhd|hd|1080p|720p|bluray|web-dl|dvdrip|multi|dubbed|مترجم|مدبلج)\\b"), "")
                .replace(Regex("(?i)\\b(part|partie|pt|vol|cd|season|موسم|الجزء|جزء)\\s*\\d*"), "")
                .replace(Regex("[:\\-–_#]+$"), "")
                .trim()

            if (cleanTitle.length >= 3) {
                val normalizedClean = cleanTitle.lowercase()
                    .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
                    .replace("ة", "ه").replace("ى", "ي")

                val related = allMovies.filter { m ->
                    val otherNormalized = m.title.lowercase()
                        .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
                        .replace("ة", "ه").replace("ى", "ي")

                    (otherNormalized.contains(normalizedClean) || (normalizedClean.length >= 6 && otherNormalized.contains(normalizedClean.take(6)))) &&
                    (m.category == movie.category || otherNormalized.startsWith(normalizedClean.take(4)))
                }.distinctBy { it.id }

                if (related.size > 1) {
                    related.sortedBy { if (it.year > 0) it.year else 9999 }
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    // Genre list extraction
    val genreList = remember(movie.category, movie.title) {
        val extracted = mutableListOf<String>()
        val cat = movie.category.replace("Category", "").replace("أفلام", "").trim()
        if (cat.isNotEmpty()) extracted.add(cat)
        if (movie.title.contains("أكشن") || movie.category.contains("Action", ignoreCase = true)) extracted.add("أكشن")
        if (movie.title.contains("دراما") || movie.category.contains("Drama", ignoreCase = true)) extracted.add("دراما")
        if (movie.title.contains("رعب") || movie.category.contains("Horror", ignoreCase = true)) extracted.add("رعب")
        if (movie.title.contains("خيال") || movie.category.contains("Sci-Fi", ignoreCase = true)) extracted.add("خيال علمي")
        if (movie.title.contains("مغامرة") || movie.category.contains("Adventure", ignoreCase = true)) extracted.add("مغامرة")
        if (extracted.isEmpty()) listOf("دراما", "أكشن", "سينما 4K") else extracted.distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Header Overlay: Back button (Left) and 3-dots Menu (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .zIndex(10f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3-dots Menu Button
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات إضافية",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier.background(Color(0xFF181A2A))
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isFavorite) "إزالة من المحفوظات" else "حفظ الفيلم", color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) RedLight else Color.White
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onToggleFavorite()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تحميل للمشاهدة لاحقاً", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color.White) },
                                onClick = {
                                    showOptionsMenu = false
                                    if (!isDownloaded && !isDownloading) {
                                        onDownload(movie)
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }

                // Top Backdrop Image with Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    SafeAsyncImage(
                        model = movie.backdropUrl.ifEmpty { movie.posterUrl },
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay fading seamlessly into background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x66000000),
                                        Color(0xAA090A13),
                                        DarkBg
                                    )
                                )
                            )
                    )
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp),
                    contentPadding = PaddingValues(top = 220.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Title & Rating / Badges Row
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Big Bold Title
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Metadata Badges (Rating, Year, Age, Duration)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Star Rating Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1B1D2E))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "★", color = GoldGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (movie.rating > 0) String.format("%.1f", movie.rating) else "8.8",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Year
                                Text(
                                    text = if (movie.year > 0) "${movie.year}" else "2023",
                                    color = SoftGreyText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Age classification pill (+18 / 16+)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1A1C2C))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+18",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Format / Duration
                                Text(
                                    text = movie.duration.ifEmpty { "فيلم 4K" },
                                    color = SoftGreyText,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Genre Pills Row
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                genreList.take(4).forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF161828))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = SoftGreyText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Action Buttons (مشاهدة الآن + حفظ + تحميل)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Secondary: Bookmark Button (حفظ)
                            OutlinedButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFavorite) RedPrimary else Color.White.copy(alpha = 0.2f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isFavorite) RedPrimary.copy(alpha = 0.15f) else Color(0xFF131525)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) RedLight else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFavorite) "تم الحفظ" else "حفظ",
                                        color = if (isFavorite) RedLight else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Primary CTA: Play Now Button (مشاهدة الآن)
                            Button(
                                onClick = {
                                    onPlay(movie.streamUrl)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1.8f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(RedPrimary, RedLight)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isDownloaded) "تشغيل بدون نت" else "مشاهدة الآن",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Story Section (القصة)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text(
                                text = "القصة",
                                color = RedLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val descriptionText = movie.description.ifEmpty {
                                "تدور أحداث هذا العمل الملحمي في قالب مشوق ومثير، مليء بالمفاجآت والحبكات الدرامية التي تحبس الأنفاس بجودة سينمائية فائقة."
                            }
                            Text(
                                text = descriptionText,
                                color = SoftGreyText,
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                                maxLines = if (isStoryExpanded) 20 else 3,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (descriptionText.length > 100) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isStoryExpanded = !isStoryExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isStoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = RedLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Franchise / Related Movies
                    if (relatedFranchiseMovies.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "أفلام من نفس السلسلة",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(relatedFranchiseMovies) { m ->
                                        Column(
                                            modifier = Modifier
                                                .width(110.dp)
                                                .clickable { onSelectMovie(m) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(CardBg)
                                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                            ) {
                                                SafeAsyncImage(
                                                    model = m.posterUrl,
                                                    contentDescription = m.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = m.title,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.End
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

/**
 * Series Details Screen - Exact reproduction of right screenshot
 */
@Composable
fun SeriesDetailsDialog(
    series: Series,
    seasons: List<Season>,
    episodes: List<Episode>,
    isLoadingEpisodes: Boolean,
    isFavorite: Boolean,
    downloadedEpisodeIds: Set<String> = emptySet(),
    downloadingEpisodeIds: Set<String> = emptySet(),
    onToggleFavorite: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onDownloadEpisode: (Episode) -> Unit,
    onSeasonTabSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSeasonId by remember(seasons) { mutableStateOf(seasons.firstOrNull()?.id ?: "1") }
    var isStoryExpanded by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showSeasonPickerMenu by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSeasonId) {
        if (selectedSeasonId.isNotEmpty()) {
            onSeasonTabSelected(selectedSeasonId)
        }
    }

    // Genre list extraction
    val genreList = remember(series.category, series.title) {
        val extracted = mutableListOf<String>()
        val cat = series.category.replace("Category", "").replace("مسلسلات", "").trim()
        if (cat.isNotEmpty()) extracted.add(cat)
        if (series.title.contains("أكشن") || series.category.contains("Action", ignoreCase = true)) extracted.add("أكشن")
        if (series.title.contains("دراما") || series.category.contains("Drama", ignoreCase = true)) extracted.add("دراما")
        if (series.title.contains("مغامرة") || series.category.contains("Adventure", ignoreCase = true)) extracted.add("مغامرة")
        if (series.title.contains("خيال") || series.category.contains("Sci-Fi", ignoreCase = true)) extracted.add("خيال علمي")
        if (extracted.isEmpty()) listOf("دراما", "أكشن", "مغامرة", "خيال علمي") else extracted.distinct()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Header Overlay: Back button (Left) and 3-dots Menu (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .zIndex(10f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3-dots Menu Button
                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات إضافية",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier.background(Color(0xFF181A2A))
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isFavorite) "إزالة من المحفوظات" else "حفظ المسلسل", color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) RedLight else Color.White
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onToggleFavorite()
                                }
                            )
                        }
                    }
                }

                // Top Backdrop Image with Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    SafeAsyncImage(
                        model = series.backdropUrl.ifEmpty { series.posterUrl },
                        contentDescription = series.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay fading seamlessly into background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x66000000),
                                        Color(0xAA090A13),
                                        DarkBg
                                    )
                                )
                            )
                    )
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp),
                    contentPadding = PaddingValues(top = 220.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Title & Rating / Badges Row
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Big Bold Title
                            Text(
                                text = series.title,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Metadata Badges (Rating, Year, Age, Season)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Star Rating Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1B1D2E))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "★", color = GoldGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (series.rating > 0) String.format("%.1f", series.rating) else "9.4",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Year
                                Text(
                                    text = if (series.year > 0) "${series.year}" else "2023",
                                    color = SoftGreyText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Age classification pill (+18)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1A1C2C))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "+18",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Season Badge
                                Text(
                                    text = "موسم",
                                    color = SoftGreyText,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Genre Pills Row (دراما, أكشن, مغامرة, خيال علمي)
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                genreList.take(4).forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF161828))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = SoftGreyText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Action Buttons (مشاهدة الآن + حفظ)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Secondary: Bookmark Button (حفظ)
                            OutlinedButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFavorite) RedPrimary else Color.White.copy(alpha = 0.2f)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isFavorite) RedPrimary.copy(alpha = 0.15f) else Color(0xFF131525)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) RedLight else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFavorite) "تم الحفظ" else "حفظ",
                                        color = if (isFavorite) RedLight else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Primary CTA: Play First Episode Now (مشاهدة الآن)
                            Button(
                                onClick = {
                                    val firstEp = episodes.firstOrNull()
                                    if (firstEp != null) {
                                        onPlayEpisode(firstEp)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.8f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(RedPrimary, RedLight)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "مشاهدة الآن",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Story Section (القصة)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Text(
                                text = "القصة",
                                color = RedLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val descriptionText = series.description.ifEmpty {
                                "بعد عشرين عاماً من تدمير الحضارة الحديثة، يتعاون المهرب جويل مع فتاة صغيرة تدعى إيلي للتهريب عبر الولايات المتحدة الموبوءة، حيث يعتمد كلاهما على الآخر للبقاء على قيد الحياة."
                            }
                            Text(
                                text = descriptionText,
                                color = SoftGreyText,
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                                maxLines = if (isStoryExpanded) 20 else 3,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (descriptionText.length > 100) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isStoryExpanded = !isStoryExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isStoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = RedLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Seasons & Episodes Section (المواسم والحلقات)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            // Section Header
                            Text(
                                text = "المواسم والحلقات",
                                color = RedLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Season Dropdown Selector Pill ("الموسم 1 ⌵")
                            val currentSeasonTitle = remember(selectedSeasonId, seasons) {
                                seasons.find { it.id == selectedSeasonId }?.title?.ifEmpty { "الموسم $selectedSeasonId" } ?: "الموسم 1"
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF181A2D))
                                        .border(1.dp, RedPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable { showSeasonPickerMenu = true }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = RedLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentSeasonTitle,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSeasonPickerMenu,
                                    onDismissRequest = { showSeasonPickerMenu = false },
                                    modifier = Modifier.background(Color(0xFF1B1D2D))
                                ) {
                                    if (seasons.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("الموسم 1", color = Color.White) },
                                            onClick = {
                                                selectedSeasonId = "1"
                                                showSeasonPickerMenu = false
                                            }
                                        )
                                    } else {
                                        seasons.forEach { s ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = s.title.ifEmpty { "الموسم ${s.number}" },
                                                        color = if (s.id == selectedSeasonId) RedLight else Color.White,
                                                        fontWeight = if (s.id == selectedSeasonId) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    selectedSeasonId = s.id
                                                    showSeasonPickerMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Episodes List
                    if (isLoadingEpisodes) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = RedPrimary, strokeWidth = 3.dp)
                            }
                        }
                    } else if (episodes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "جاري تجهيز حلقات هذا الموسم...",
                                    color = SoftGreyText,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        itemsIndexed(episodes, key = { _, ep -> ep.id }) { index, episode ->
                            val isDownloaded = downloadedEpisodeIds.contains(episode.id)
                            val isDownloading = downloadingEpisodeIds.contains(episode.id)
                            ScreenshotStyleEpisodeCard(
                                episode = episode,
                                index = index + 1,
                                isDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                onPlay = { onPlayEpisode(episode) },
                                onDownload = { onDownloadEpisode(episode) }
                            )
                        }

                        // Bottom Link "عرض كل الحلقات ‹"
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "عرض كل الحلقات",
                                    color = RedLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = RedLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Episode Card - Exact reproduction from right screenshot:
 * - Rounded card container
 * - Episode thumbnail on left with circular play button overlay
 * - Episode Title (e.g. الحلقة 1), Subtitle (عندما تضيع في الظلام), Duration (47 دقيقة)
 * - Download circular icon button
 */
@Composable
private fun ScreenshotStyleEpisodeCard(
    episode: Episode,
    index: Int,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable { onPlay() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Action Icon: Download Button (Left side in RTL)
            IconButton(
                onClick = onDownload,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B1D2D))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        color = RedLight,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "تحميل الحلقة",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Info Column
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "الحلقة $index",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.title.ifEmpty { "حلقة كاملة بدقة عالية" },
                    color = SoftGreyText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = episode.duration.ifEmpty { "47 دقيقة" },
                    color = SoftGreyText.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Thumbnail with Play Overlay
            Box(
                modifier = Modifier
                    .width(105.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1C1E30)),
                contentAlignment = Alignment.Center
            ) {
                SafeAsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )

                // Circular Play Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
