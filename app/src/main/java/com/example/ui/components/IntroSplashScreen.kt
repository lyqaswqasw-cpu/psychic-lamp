package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LoopRed
import com.example.ui.theme.SoftGreyText
import kotlinx.coroutines.delay

@Composable
fun IntroSplashScreen(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "intro_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070913)),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LoopRed.copy(alpha = 0.22f), Color.Transparent),
                    radius = size.width * 0.75f
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.width * 0.75f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer(
                    alpha = alphaAnim.value,
                    scaleX = scaleAnim.value,
                    scaleY = scaleAnim.value
                )
        ) {
            // Halo Rings + Logo Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                // Outer Subtle Pulsing Ring
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .scale(pulseScale)
                        .alpha(0.2f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(LoopRed, Color(0xFFFF334B), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                // Rotating gradient arc
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = ringRotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                LoopRed,
                                Color.Transparent,
                                Color(0xFFFF4D4D),
                                Color.Transparent
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Middle Glassmorphic Container with Official +90 Emblem
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF140E1E))
                        .border(1.5.dp, LoopRed.copy(alpha = 0.5f), CircleShape)
                        .shadow(16.dp, CircleShape, spotColor = LoopRed),
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

            Spacer(modifier = Modifier.height(18.dp))

            // App Title
            Text(
                text = "منصة +90",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "المنصة العالمية للبث المباشر والسينما المتكاملة",
                color = SoftGreyText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Brand Capsule
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer(alpha = alphaAnim.value)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "تجربة سينمائية متطورة 4K UHD",
                color = SoftGreyText.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
