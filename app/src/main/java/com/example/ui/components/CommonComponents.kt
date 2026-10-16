package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.*
import com.example.ui.MainViewModel
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.LoopRed
import com.example.ui.theme.LoopRedGlow
import com.example.ui.theme.SoftGreyText
import com.example.ui.utils.*

@Composable
fun ChannelPersonAvatar(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    isMainChannel: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp)
) {
    val cleanName = remember(name) { name.trim() }

    val gradientColors = remember(cleanName) {
        val palettes = listOf(
            listOf(Color(0xFFE11D48), Color(0xFF7C3AED)), // Crimson to Violet
            listOf(Color(0xFF2563EB), Color(0xFF06B6D4)), // Sapphire to Cyan
            listOf(Color(0xFFD97706), Color(0xFFDC2626)), // Amber to Red
            listOf(Color(0xFF059669), Color(0xFF10B981)), // Emerald
            listOf(Color(0xFF7C3AED), Color(0xFFDB2777)), // Purple to Pink
            listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)), // Indigo to Cyan
            listOf(Color(0xFFEA580C), Color(0xFFF59E0B)), // Orange to Amber
            listOf(Color(0xFF0D9488), Color(0xFF3B82F6)), // Teal to Blue
            listOf(Color(0xFF9333EA), Color(0xFF4F46E5)), // Purple to Indigo
            listOf(Color(0xFFBE185D), Color(0xFF831843))  // Ruby
        )
        val index = (Math.abs(cleanName.hashCode()) % palettes.size)
        palettes[index]
    }

    val monogram = remember(cleanName) {
        val clean = cleanName.replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ").trim()
        val words = clean.split(Regex("""\s+""")).filter { it.isNotBlank() }
        when {
            words.isEmpty() -> if (cleanName.isNotBlank()) cleanName.take(2).uppercase() else "?"
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
        }
    }

    var imageLoadFailed by remember(logoUrl) { mutableStateOf(logoUrl.isNullOrBlank()) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        if (!logoUrl.isNullOrBlank() && !imageLoadFailed) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(logoUrl)
                    .crossfade(true)
                    .allowHardware(true)
                    .build(),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { imageLoadFailed = true }
            )
        }

        if (logoUrl.isNullOrBlank() || imageLoadFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMainChannel) Icons.Default.LiveTv else Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxSize(0.65f)
                )

                Text(
                    text = monogram,
                    color = Color.White,
                    fontSize = if (isMainChannel) 22.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ChannelGridCard(
    chan: Channel,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(18.dp), onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                ChannelPersonAvatar(
                    name = chan.name,
                    logoUrl = chan.logoUrl,
                    modifier = Modifier.fillMaxSize(),
                    isMainChannel = false,
                    shape = RoundedCornerShape(14.dp)
                )
                
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) LoopRed else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = chan.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun MovieGridCard(
    movie: Movie,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )

    Column(
        modifier = Modifier
            .width(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            SafeAsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = { onFavoriteToggle() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "مفضلة",
                    tint = if (isFavorite) LoopRed else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SeriesGridCard(
    series: Series,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .tvFocusable(shape = RoundedCornerShape(12.dp), onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            SafeAsyncImage(
                model = series.posterUrl,
                contentDescription = series.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = { onFavoriteToggle() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "مفضلة",
                    tint = if (isFavorite) LoopRed else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = series.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ModernLineLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "loader")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .offset(x = (androidx.compose.ui.unit.Dp(progress * 400)).coerceAtMost(400.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, LoopRed, Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun CategoryChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .tvFocusable(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .background(
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (isSelected) Color.White else SoftGreyText,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CategoryRow(
    categories: List<IPTVCategory>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    if (categories.isEmpty()) return
    val lazyListState = rememberLazyListState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLandscape) 2.dp else 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(
                name = "الكل",
                isSelected = selectedCategory == "all",
                onClick = { onSelectCategory("all") }
            )
        }

        items(categories) { cat ->
            CategoryChip(
                name = cat.name,
                isSelected = selectedCategory == cat.id,
                onClick = { onSelectCategory(cat.id) }
            )
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = SoftGreyText.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = SoftGreyText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
@Composable
fun MainChannelGridCard(
    chan: Channel,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(shape = RoundedCornerShape(20.dp), onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                ChannelPersonAvatar(
                    name = chan.name,
                    logoUrl = chan.logoUrl,
                    modifier = Modifier.fillMaxSize(),
                    isMainChannel = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Top Left Live badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(LoopRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بث مباشر",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Optional Favorite Heart Button
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) LoopRedGlow else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = chan.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle of premium verification
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldGlow,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "قناة رئيسية معتمدة",
                    color = SoftGreyText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Professional Video Stream Route Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161A28))
                    .border(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(9.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "مسار فيديو: FHD • HLS",
                    color = Color(0xFFBAE6FD),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CastMembersListSection(
    castList: List<CastMember>,
    title: String = "طاقم العمل والممثلين",
    modifier: Modifier = Modifier
) {
    if (castList.isEmpty()) return

    val anyHasPhoto = remember(castList) { castList.any { it.hasPhoto || it.effectivePhotoUrl.isNotBlank() } }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GoldGlow)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (anyHasPhoto) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF062817))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                    Text(
                        text = "صور الممثلين متوفرة",
                        color = Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A1A06))
                        .border(1.dp, Color(0xFFD97706).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(11.dp))
                    Text(
                        text = "أسماء نصية فقط",
                        color = Color(0xFFFDE68A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!anyHasPhoto) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1B151F))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "معلومات الممثلين متوفرة نصياً ببراعة دون صور مرفقة.",
                    color = Color(0xFFFDE68A).copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(castList) { member ->
                val photo = member.effectivePhotoUrl
                val hasValidPhoto = member.hasPhoto || (photo.isNotBlank() && (photo.startsWith("http://") || photo.startsWith("https://")))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(86.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasValidPhoto) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2235))
                                    .border(2.dp, GoldGlow.copy(alpha = 0.5f), CircleShape)
                                    .shadow(8.dp, CircleShape)
                            ) {
                                SafeAsyncImage(
                                    model = photo,
                                    contentDescription = member.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            val initialChar = if (member.name.isNotBlank()) member.name.take(1).uppercase() else "?"
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF2E1C38), Color(0xFF16192E))
                                        )
                                    )
                                    .border(1.5.dp, Color(0xFFD97706).copy(alpha = 0.4f), CircleShape)
                                    .shadow(6.dp, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initialChar,
                                    color = Color(0xFFFDE68A),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A1A06))
                                    .border(1.dp, Color(0xFFD97706), CircleShape)
                                    .padding(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = member.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    if (member.character.isNotEmpty()) {
                        Text(
                            text = member.character,
                            color = SoftGreyText,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SearchBarWidget(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val verticalPadding = if (isLandscape) 4.dp else 10.dp
    val fieldHeight = if (isLandscape) 38.dp else 48.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = verticalPadding)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight),
            placeholder = { Text(text = placeholder, color = SoftGreyText.copy(alpha = 0.6f), fontSize = if (isLandscape) 10.sp else 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = LoopRed, modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(if (isLandscape) 24.dp else 28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = SoftGreyText, modifier = Modifier.size(if (isLandscape) 14.dp else 16.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LoopRed,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = if (isLandscape) 10.sp else 12.sp, color = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun CinematicHeroBanner(movie: Movie, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable { onClick() }
    ) {
        SafeAsyncImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Backdrop dark cinematics overlay gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x88000000), Color(0xFF070913)),
                        startY = 0f
                    )
                )
        )

        // Floating Title text container
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Yellow)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "العرض الرئيسي المختار",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UserAvatar(avatarKey: String, size: androidx.compose.ui.unit.Dp = 36.dp) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "https://ui-avatars.com/api/?name=${avatarKey}&background=random&color=fff",
            contentDescription = "User Avatar",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun NotificationListView(viewModel: MainViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LoopRed)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "الإشعارات والرسائل الواردة",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (notifications.isEmpty()) {
            EmptyStateView("لا توجد إشعارات جديدة حالياً.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = notif.title,
                                    color = LoopRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(notif.timestamp)),
                                    color = SoftGreyText,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = notif.body,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
