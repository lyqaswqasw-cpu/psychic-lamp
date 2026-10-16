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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.SoftGreyText
import com.example.ui.utils.SafeAsyncImage

// Theme constants matching the main app premium theme
private val LibraryPurplePrimary = Color(0xFF7928CA)
private val LibraryBlueGlow = Color(0xFF00F2FE)
private val LibraryBg = Color(0xFF080C14)
private val LibraryCardBg = Color(0xFF121422)
private val LibraryCardBorder = Color(0x1FFFFFFF)
private val DarkBg = Color(0xFF080C14)

/**
 * Unified Media Item to present Movies and Series seamlessly in the same UI
 */
data class UnifiedMediaItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String = "",
    val rating: String = "8.5",
    val year: Int = 2023,
    val category: String = "",
    val isSeries: Boolean = false,
    val originalMovie: Movie? = null,
    val originalSeries: Series? = null,
    val isNew: Boolean = false
)

@Composable
fun UnifiedLibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.movies.collectAsState()
    val series by viewModel.series.collectAsState()
    val isMoviesLoading by viewModel.isMoviesLoading.collectAsState()
    val isSeriesLoading by viewModel.isSeriesLoading.collectAsState()
    val favoritesSet by viewModel.favorites.collectAsState()
    val cardColumns by viewModel.cardColumnsCount.collectAsState()

    // Filter type: "all" (الكل), "movies" (أفلام), "series" (مسلسلات)
    var activeFilter by remember { mutableStateOf("all") }
    // Search query & visibility
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    // Category selection
    var selectedGenre by remember { mutableStateOf("الكل") }
    // Show full list mode when tapping "عرض الكل"
    var viewingAllSectionTitle by remember { mutableStateOf<String?>(null) }

    // Load data on demand if not loaded yet
    LaunchedEffect(Unit) {
        viewModel.loadMoviesOnDemand()
        viewModel.loadSeriesOnDemand()
    }

    // Convert movies & series into unified list
    val allUnifiedItems = remember(movies, series) {
        val movieList = movies.mapIndexed { idx, m ->
            UnifiedMediaItem(
                id = m.id,
                title = m.title,
                posterUrl = m.posterUrl,
                backdropUrl = m.backdropUrl,
                rating = if (m.rating > 0) String.format("%.1f", m.rating) else "8.5",
                year = if (m.year > 0) m.year else 2023,
                category = m.category,
                isSeries = false,
                originalMovie = m,
                isNew = idx < 12 // Mark first batch as new
            )
        }
        val seriesList = series.mapIndexed { idx, s ->
            UnifiedMediaItem(
                id = s.id,
                title = s.title,
                posterUrl = s.posterUrl,
                backdropUrl = s.backdropUrl,
                rating = if (s.rating > 0) String.format("%.1f", s.rating) else "8.8",
                year = if (s.year > 0) s.year else 2023,
                category = s.category,
                isSeries = true,
                originalSeries = s,
                isNew = idx < 12
            )
        }
        // Interleave movies and series for rich combined presentation
        val combined = mutableListOf<UnifiedMediaItem>()
        val maxLen = maxOf(movieList.size, seriesList.size)
        for (i in 0 until maxLen) {
            if (i < movieList.size) combined.add(movieList[i])
            if (i < seriesList.size) combined.add(seriesList[i])
        }
        combined
    }

    // Extract all distinct genres / categories for filtering
    val availableGenres = remember(allUnifiedItems) {
        val extracted = allUnifiedItems
            .map { it.category.trim() }
            .filter { it.isNotEmpty() && it != "all" && it != "movies" && it != "series" }
            .distinct()
            .take(15)
        listOf("الكل") + extracted
    }

    // Filtered list based on activeFilter, selectedGenre, and searchQuery
    val filteredItems = remember(allUnifiedItems, activeFilter, selectedGenre, searchQuery) {
        allUnifiedItems.filter { item ->
            val matchesType = when (activeFilter) {
                "movies" -> !item.isSeries
                "series" -> item.isSeries
                else -> true
            }
            val matchesGenre = selectedGenre == "الكل" || item.category.contains(selectedGenre, ignoreCase = true)
            val matchesSearch = searchQuery.isEmpty() || item.title.contains(searchQuery, ignoreCase = true)
            matchesType && matchesGenre && matchesSearch
        }
    }

    // Curated sub-sections
    val trendingItems = remember(filteredItems) {
        filteredItems.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }.take(15)
    }
    val recentlyAddedItems = remember(filteredItems) {
        filteredItems.filter { it.isNew }.take(15).ifEmpty { filteredItems.take(15) }
    }
    val featuredItems = remember(filteredItems) {
        filteredItems.drop(5).take(15)
    }

    val isLoading = (isMoviesLoading && movies.isEmpty()) || (isSeriesLoading && series.isEmpty())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // ==========================================
        // 1. TOP HEADER (المكتبة + Subtitle + Search Icon)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Search Button or Back if searching
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) searchQuery = ""
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSearchExpanded) LibraryPurplePrimary else Color(0xFF181A2A))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "بحث في المكتبة",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Right: Library Title with Purple Icon (As in screenshot)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "المكتبة",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            // Violet Icon badge next to title
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(LibraryPurplePrimary, LibraryBlueGlow)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "اكتشف الأفلام والمسلسلات المفضلة لديك",
                            color = SoftGreyText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Search Bar Animated Input
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث عن فيلم أو مسلسل بالاسم...",
                            color = SoftGreyText,
                            fontSize = 13.sp
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = SoftGreyText)
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = LibraryBlueGlow)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF141626),
                        unfocusedContainerColor = Color(0xFF141626),
                        focusedBorderColor = LibraryPurplePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ==========================================
        // 2. QUICK FILTER PILLS (الكل | أفلام | مسلسلات)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF131522))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = listOf(
                Triple("all", "الكل", null),
                Triple("movies", "أفلام", Icons.Default.Movie),
                Triple("series", "مسلسلات", Icons.Default.Tv)
            )

            filterOptions.forEach { (id, label, icon) ->
                val isSelected = activeFilter == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    brush = Brush.horizontalGradient(
                                        listOf(LibraryPurplePrimary, LibraryBlueGlow)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else Modifier
                        )
                        .clickable {
                            activeFilter = id
                            viewingAllSectionTitle = null
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SoftGreyText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else SoftGreyText,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // MAIN CONTENT BODY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LibraryPurplePrimary)
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = SoftGreyText,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد نتائج مطابقة في المكتبة",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جرب البحث باسم آخر أو تغيير قسم التصنيف",
                            color = SoftGreyText,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (searchQuery.isNotEmpty() || viewingAllSectionTitle != null) {
                // Full Grid Mode (When searching or when "عرض الكل" is tapped)
                Column(modifier = Modifier.fillMaxSize()) {
                    if (viewingAllSectionTitle != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { viewingAllSectionTitle = null }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LibraryBlueGlow)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("العودة للأقسام", color = LibraryBlueGlow, fontSize = 12.sp)
                            }
                            Text(
                                text = viewingAllSectionTitle ?: "",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cardColumns),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems, key = { "${if (it.isSeries) "s" else "m"}_${it.id}" }) { item ->
                            LibraryMediaCard(
                                item = item,
                                isFavorite = favoritesSet.contains(item.id),
                                onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                                onClick = {
                                    if (item.isSeries && item.originalSeries != null) {
                                        viewModel.selectSeries(item.originalSeries)
                                    } else if (item.originalMovie != null) {
                                        viewModel.selectMovie(item.originalMovie)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // ==========================================
                // ORGANIZED SHELVES (نفس تصميم الصورة تماماً)
                // ==========================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // SECTION 1: 🔥 الأكثر مشاهدة
                    if (trendingItems.isNotEmpty()) {
                        item {
                            LibrarySectionShelf(
                                title = "الأكثر مشاهدة",
                                emoji = "🔥",
                                items = trendingItems,
                                favoritesSet = favoritesSet,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onSeeAll = { viewingAllSectionTitle = "الأكثر مشاهدة 🔥" },
                                onItemClick = { item ->
                                    if (item.isSeries && item.originalSeries != null) {
                                        viewModel.selectSeries(item.originalSeries)
                                    } else if (item.originalMovie != null) {
                                        viewModel.selectMovie(item.originalMovie)
                                    }
                                }
                            )
                        }
                    }

                    // SECTION 2: ⭐ أضيف حديثًا (مع شارة "جديد")
                    if (recentlyAddedItems.isNotEmpty()) {
                        item {
                            LibrarySectionShelf(
                                title = "أضيف حديثًا",
                                emoji = "⭐",
                                items = recentlyAddedItems,
                                favoritesSet = favoritesSet,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onSeeAll = { viewingAllSectionTitle = "أضيف حديثًا ⭐" },
                                onItemClick = { item ->
                                    if (item.isSeries && item.originalSeries != null) {
                                        viewModel.selectSeries(item.originalSeries)
                                    } else if (item.originalMovie != null) {
                                        viewModel.selectMovie(item.originalMovie)
                                    }
                                }
                            )
                        }
                    }

                    // SECTION 3: ⭐ مميز لك
                    if (featuredItems.isNotEmpty()) {
                        item {
                            LibrarySectionShelf(
                                title = "مميز لك",
                                emoji = "⭐",
                                items = featuredItems,
                                favoritesSet = favoritesSet,
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onSeeAll = { viewingAllSectionTitle = "مميز لك ⭐" },
                                onItemClick = { item ->
                                    if (item.isSeries && item.originalSeries != null) {
                                        viewModel.selectSeries(item.originalSeries)
                                    } else if (item.originalMovie != null) {
                                        viewModel.selectMovie(item.originalMovie)
                                    }
                                }
                            )
                        }
                    }

                    // SECTION 4: تصفح الكل في المكتبة (Grid chunked inside LazyColumn)
                    if (filteredItems.size > 6) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تصفح المكتبة الكاملة",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val remainingChunked = filteredItems.drop(6).chunked(cardColumns)
                        items(remainingChunked) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (item in rowItems) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        LibraryMediaCard(
                                            item = item,
                                            isFavorite = favoritesSet.contains(item.id),
                                            onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                                            onClick = {
                                                if (item.isSeries && item.originalSeries != null) {
                                                    viewModel.selectSeries(item.originalSeries)
                                                } else if (item.originalMovie != null) {
                                                    viewModel.selectMovie(item.originalMovie)
                                                }
                                            }
                                        )
                                    }
                                }
                                if (rowItems.size < cardColumns) {
                                    repeat(cardColumns - rowItems.size) {
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

/**
 * Shelf component rendering a section title + "عرض الكل" + Horizontal Scroll of Cards
 */
@Composable
private fun LibrarySectionShelf(
    title: String,
    emoji: String,
    items: List<UnifiedMediaItem>,
    favoritesSet: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSeeAll: () -> Unit,
    onItemClick: (UnifiedMediaItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "عرض الكل" link
            Text(
                text = "عرض الكل",
                color = LibraryBlueGlow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeeAll() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )

            // Title with Emoji (e.g., 🔥 الأكثر مشاهدة)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = emoji,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Row of Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { "${if (it.isSeries) "s" else "m"}_${it.id}" }) { item ->
                LibraryShelfCard(
                    item = item,
                    isFavorite = favoritesSet.contains(item.id),
                    onToggleFavorite = { onToggleFavorite(item.id) },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

/**
 * Poster Card specifically formatted to match the left screenshot:
 * - Rounded poster
 * - "جديد" badge (purple) if recently added
 * - Rating pill on bottom-left (e.g. ⭐ 8.8)
 * - 3-dots button on bottom-right
 * - Title and Genre • Year under poster
 */
@Composable
private fun LibraryShelfCard(
    item: UnifiedMediaItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(132.dp)
            .clickable { onClick() }
    ) {
        // Poster Box
        Box(
            modifier = Modifier
                .width(132.dp)
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(LibraryCardBg)
                .border(1.dp, LibraryCardBorder, RoundedCornerShape(16.dp))
        ) {
            SafeAsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Subtle dark vignette at bottom of poster
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // "جديد" (New) badge on top right
            if (item.isNew) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LibraryPurplePrimary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "جديد",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom row on poster: Rating (left) and 3-dots (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★",
                        color = GoldGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.rating.take(3),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3-dots menu button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1B1D2D))
                    ) {
                        DropdownMenuItem(
                            text = { Text("فتح التفاصيل", color = Color.White, fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة", color = Color.White, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) Color(0xFFFF5252) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        // Category / Genre + Year
        val subtitle = remember(item.category, item.year) {
            val cat = item.category.replace("Category", "").replace("أفلام", "").replace("مسلسلات", "").trim()
            if (cat.isNotEmpty()) "$cat • ${item.year}" else "${item.year}"
        }
        Text(
            text = subtitle,
            color = SoftGreyText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Grid Card used for full library grid view
 */
@Composable
private fun LibraryMediaCard(
    item: UnifiedMediaItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    LibraryShelfCard(
        item = item,
        isFavorite = isFavorite,
        onToggleFavorite = onToggleFavorite,
        onClick = onClick
    )
}
