package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.GoldGlow

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ViewerCountOverlay(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val homeConfig by viewModel.homeConfig.collectAsState()
    if (!homeConfig.showOnlineUsers) return

    val viewerCount by viewModel.currentViewerCount.collectAsState()

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        color = Color.Black.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live pulsing dot
            val infiniteTransition = rememberInfiniteTransition(label = "player_pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = pulseAlpha))
            )

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Viewers",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )

            // Odometer digits for viewer count
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                val countString = viewerCount.coerceAtLeast(1).toString()
                for (char in countString) {
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                        },
                        label = "viewer_digit"
                    ) { digit ->
                        Text(
                            text = digit.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RealOnlineCounterBadge(
    count: Int,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F2218).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 6.dp else 10.dp,
                vertical = if (isCompact) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 6.dp else 8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676).copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
            Text(
                text = if (isCompact) "$count" else "$count متصل",
                color = Color(0xFF00E676),
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * مكان مخصص وفاخر لعداد المتواجدين الآن يظهر حصرياً في قسم الرئيسية
 * ومصمم بأسلوب العدادات الرقمية المتطورة مع نبض حي وحركات سلسة.
 */
@Composable
fun HomeLiveOnlineCounterWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val homeConfig by viewModel.homeConfig.collectAsState()
    if (!homeConfig.showOnlineUsers) return

    val liveCount by viewModel.liveOnlineUsersCount.collectAsState()

    // Smooth pulsing live radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "online_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B141B)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF00E676).copy(alpha = 0.6f),
                    Color(0xFF00B0FF).copy(alpha = 0.3f),
                    Color(0xFF00E676).copy(alpha = 0.4f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0D1D18),
                            Color(0xFF0A131A),
                            Color(0xFF070E14)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right side (RTL context): Title, pulse dot & Description
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Glowing radar indicator
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                Color(0xFF00E676).copy(alpha = glowAlpha),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676).copy(alpha = glowAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المتواجدون الآن",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "مباشر",
                                    color = Color(0xFF00E676),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "مستخدمين يتصفحون التطبيق في هذه اللحظة",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Left side: Digital Mechanical Odometer Counter
                DigitalOdometerCounter(
                    count = liveCount,
                    modifier = Modifier
                )
            }
        }
    }
}

/**
 * تصميم العداد الرقمي بخانات أرقام مستقلة وتأثير حركي عند تغير العدد
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun DigitalOdometerCounter(
    count: Int,
    modifier: Modifier = Modifier
) {
    val countString = count.coerceAtLeast(1).toString()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF050B0E))
            .border(1.dp, Color(0xFF00E676).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Individual Digit Boxes
        for (char in countString) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 28.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF101C19))
                    .border(0.5.dp, Color(0xFF00E676).copy(alpha = 0.3f), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        slideInVertically(
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) { height -> height } + fadeIn() togetherWith
                        slideOutVertically(
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) { height -> -height } + fadeOut()
                    },
                    label = "digit_flip"
                ) { targetDigit ->
                    Text(
                        text = targetDigit.toString(),
                        color = Color(0xFF00E676),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(2.dp))

        Text(
            text = "متصل",
            color = Color(0xFF00E676),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
