package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LoopRed
import com.example.ui.theme.SoftGreyText
import kotlinx.coroutines.delay

@Composable
fun SplashLoadingScreen(
    isLoading: Boolean,
    isChannelsLoading: Boolean,
    isMoviesLoading: Boolean,
    isSeriesLoading: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetProgress = when {
        isLoading -> 0.35f
        isChannelsLoading -> 0.75f
        else -> 1.0f
    }

    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "real_loading_progress"
    )

    val statusText = when {
        isLoading -> "جاري استدعاء الأقسام والتهيئة الأمنية الحقيقية..."
        isChannelsLoading -> "جاري جلب القنوات والترددات الحية ومزامنتها..."
        isMoviesLoading || isSeriesLoading -> "جاري تحديث وتجهيز مكتبة الأفلام والمسلسلات..."
        else -> "اكتمل التحميل الحقيقي! جاري فتح الواجهة..."
    }

    LaunchedEffect(progress) {
        if (progress >= 1.0f) {
            delay(150)
            onFinished()
        }
    }

    // Infinite transitions for cinematic animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash_motion")

    // Ambient radial background glow pulse
    val bgGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_glow_scale"
    )

    // Orbital ring continuous rotation
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    // Counter orbital ring rotation
    val reverseRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rev_ring_rotation"
    )

    // Shimmer bar light sweep
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    val isComplete = progress >= 1.0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070913)),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layered Ambient Neon Aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.38f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LoopRed.copy(alpha = 0.28f * bgGlowScale),
                        Color(0xFF800020).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.85f * bgGlowScale
                ),
                center = center,
                radius = size.width * 0.85f * bgGlowScale
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // High-End Luxury Emblem / Glowing Logo Container
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing dashed orbital ring
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = ringRotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                LoopRed,
                                Color(0xFFFF4D4D),
                                Color.Transparent,
                                LoopRed.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner counter-rotating thin accent ring
                Canvas(
                    modifier = Modifier
                        .size(164.dp)
                        .graphicsLayer(rotationZ = reverseRingRotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFF7A00),
                                Color.Transparent,
                                Color(0xFFFF0055),
                                Color.Transparent
                            )
                        ),
                        startAngle = 90f,
                        sweepAngle = 220f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Core Glassmorphic Container with Official +90 Emblem
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1F172B),
                                    Color(0xFF110D1A),
                                    Color(0xFF09070F)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF334B).copy(alpha = 0.8f),
                                    Color.White.copy(alpha = 0.15f),
                                    Color(0xFFE50914).copy(alpha = 0.4f)
                                )
                            ),
                            CircleShape
                        )
                        .shadow(24.dp, CircleShape, spotColor = LoopRed),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.plus_90_app_logo_1787273202258),
                        contentDescription = "منصة +90",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // App Title "+90"
            Text(
                text = "منصة +90",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )

            // Subtitle Tagline Pill
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "أهلاً بك في منصة +90 للبث المباشر والسينما",
                    color = SoftGreyText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Percentage Badge Pill
            val badgeBgColor by animateColorAsState(
                targetValue = if (isComplete) Color(0xFF06331C) else Color(0xFF221118),
                animationSpec = tween(300),
                label = "badge_bg"
            )
            val badgeTextColor by animateColorAsState(
                targetValue = if (isComplete) Color(0xFF10B981) else Color(0xFFFF334B),
                animationSpec = tween(300),
                label = "badge_text"
            )
            val badgeBorderColor by animateColorAsState(
                targetValue = if (isComplete) Color(0xFF10B981).copy(alpha = 0.6f) else LoopRed.copy(alpha = 0.4f),
                animationSpec = tween(300),
                label = "badge_border"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(badgeBgColor)
                    .border(1.dp, badgeBorderColor, RoundedCornerShape(30.dp))
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = badgeTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Futuristic Glowing Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF161B2B))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            ) {
                // Progress Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFB91C1C),
                                    LoopRed,
                                    Color(0xFFFF4D4D)
                                )
                            )
                        )
                ) {
                    // Shimmer light effect moving over progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = shimmerTranslate * 300.dp.toPx()
                            }
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Real-Time Animated Status Text
            AnimatedContent(
                targetState = statusText,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "status_anim"
            ) { targetText ->
                Text(
                    text = targetText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3-Stage Progress Micro-Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Pair("التهيئة", progress >= 0.35f),
                    Pair("القنوات", progress >= 0.75f),
                    Pair("السينما", progress >= 1.0f)
                ).forEachIndexed { idx, (label, completed) ->
                    val isActive = !completed && (idx == 0 && progress < 0.35f || idx == 1 && progress in 0.35f..0.75f || idx == 2 && progress in 0.75f..0.99f)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (completed) Color(0xFF06331C)
                                else if (isActive) LoopRed.copy(alpha = 0.15f)
                                else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                1.dp,
                                if (completed) Color(0xFF10B981).copy(alpha = 0.4f)
                                else if (isActive) LoopRed.copy(alpha = 0.5f)
                                else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (completed) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) LoopRed else SoftGreyText.copy(alpha = 0.3f))
                            )
                        }
                        Text(
                            text = label,
                            color = if (completed) Color(0xFF34D399) else if (isActive) Color.White else SoftGreyText,
                            fontSize = 10.sp,
                            fontWeight = if (isActive || completed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Frosted Glass Security & Performance Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تشفير سحابي آمن 256-bit • خوادم فائقة السرعة Ultra Fast",
                    color = SoftGreyText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
