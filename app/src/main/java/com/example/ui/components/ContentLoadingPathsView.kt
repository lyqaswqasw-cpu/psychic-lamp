package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LoopRed
import kotlinx.coroutines.delay

@Composable
fun ContentLoadingPathsView(
    isLoading: Boolean,
    isChannelsLoading: Boolean,
    isMoviesLoading: Boolean,
    isSeriesLoading: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine active path stage based on loading flags
    val stage = when {
        isLoading -> 1 // Categories & Auth initialization
        isChannelsLoading -> 2 // Channels path
        isMoviesLoading -> 3 // Movies path
        isSeriesLoading -> 4 // Series path
        else -> 5 // Final caching & sync
    }

    val progressTarget = when (stage) {
        1 -> 0.2f
        2 -> 0.45f
        3 -> 0.70f
        4 -> 0.90f
        else -> 1.0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "path_progress"
    )

    LaunchedEffect(stage) {
        if (stage == 5) {
            delay(400)
            onFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070913)),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LoopRed.copy(alpha = 0.22f),
                        Color(0xFF150A1C).copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.8f
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.width * 0.8f
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 480.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111422))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon & Title
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(LoopRed.copy(alpha = 0.15f))
                        .border(1.5.dp, LoopRed.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = LoopRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تحديث ومزامنة مسارات المحتوى",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "جاري جلب التحديثات من المطور وحفظها محلياً بكفاءة",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = LoopRed,
                    trackColor = Color(0xFF1F2438)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Path Steps List
                PathStepItem(
                    title = "مسار الأقسام والفئات والتهيئة",
                    icon = Icons.Default.FolderSpecial,
                    isCompleted = stage > 1,
                    isCurrent = stage == 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                PathStepItem(
                    title = "مسار القنوات الحية والترددات",
                    icon = Icons.Default.LiveTv,
                    isCompleted = stage > 2,
                    isCurrent = stage == 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                PathStepItem(
                    title = "مسار مكتبة الأفلام والملخصات",
                    icon = Icons.Default.Movie,
                    isCompleted = stage > 3,
                    isCurrent = stage == 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                PathStepItem(
                    title = "مسار مكتبة المسلسلات والحلقات",
                    icon = Icons.Default.Tv,
                    isCompleted = stage > 4,
                    isCurrent = stage == 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                PathStepItem(
                    title = "مزامنة التخزين المؤقت والحفظ المحلي",
                    icon = Icons.Default.CloudSync,
                    isCompleted = stage > 5,
                    isCurrent = stage == 5
                )
            }
        }
    }
}

@Composable
fun PathStepItem(
    title: String,
    icon: ImageVector,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    val borderColor = when {
        isCompleted -> Color(0xFF4CAF50)
        isCurrent -> LoopRed
        else -> Color(0xFF2A3048)
    }

    val backgroundColor = when {
        isCompleted -> Color(0xFF1B3822)
        isCurrent -> LoopRed.copy(alpha = 0.15f)
        else -> Color(0xFF161B2E)
    }

    val textColor = when {
        isCompleted -> Color(0xFF81C784)
        isCurrent -> Color.White
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF4CAF50) else if (isCurrent) LoopRed else Color.Gray,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(18.dp)
            )
        } else if (isCurrent) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = LoopRed,
                strokeWidth = 2.dp
            )
        }
    }
}
