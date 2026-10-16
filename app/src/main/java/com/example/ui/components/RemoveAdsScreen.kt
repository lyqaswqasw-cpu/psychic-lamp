package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.UnityAdsManager
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun RemoveAdsScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isAdFree = viewModel.isAdFree
    val adFreeExpiry by viewModel.adFreeExpiry.collectAsState()

    var timeRemainingText by remember { mutableStateOf("") }
    var isLoadingAd by remember { mutableStateOf(false) }
    var showRewardVideoPlayer by remember { mutableStateOf(false) }
    val adsWatchedCount by viewModel.adsWatchedCount.collectAsState()
    val requiredAds = 2

    // Helper to format remaining time
    fun formatRemainingTime(expiry: Long): String {
        val diff = expiry - System.currentTimeMillis()
        if (diff <= 0) return "انتهت فترة الحماية"
        val hours = (diff / (1000 * 60 * 60)).toInt()
        val mins = ((diff / (1000 * 60)) % 60).toInt()
        val secs = ((diff / (1000 * 60)) % 60).toInt()
        return String.format("%02d:%02d:%02d", hours, mins, secs)
    }

    LaunchedEffect(adFreeExpiry) {
        while (true) {
            if (adFreeExpiry > System.currentTimeMillis()) {
                timeRemainingText = formatRemainingTime(adFreeExpiry)
            } else {
                timeRemainingText = "انتهت فترة الحماية من الإعلانات"
            }
            delay(1000)
        }
    }

    fun handleRewardSuccess() {
        val newCount = viewModel.incrementAdsWatchedCount()
        if (newCount >= requiredAds) {
            viewModel.resetAdsWatchedCount()
            viewModel.grantAdFreeHours(24)
            Toast.makeText(context, "🎉 تم إكمال الإعلانات وتفعيل 24 ساعة بدون إعلانات بنجاح!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "✨ أحسنت! شاهد الإعلان الثاني (2/2) لتفعيل الـ 24 ساعة بدون إعلانات.", Toast.LENGTH_LONG).show()
        }
    }

    fun playRewardVideo() {
        isLoadingAd = true
        val activity = context as? Activity
        if (activity != null) {
            UnityAdsManager.showRewardedVideo(
                activity = activity,
                onRewardEarned = {
                    isLoadingAd = false
                    handleRewardSuccess()
                },
                onAdDismissed = {
                    isLoadingAd = false
                },
                onAdFailed = { _ ->
                    isLoadingAd = false
                    // Fallback to built-in rewarded player
                    showRewardVideoPlayer = true
                }
            )
        } else {
            isLoadingAd = false
            showRewardVideoPlayer = true
        }
    }

    // Fullscreen Video Ad Player Dialog (Fallback & Direct video flow)
    if (showRewardVideoPlayer) {
        val currentAdNum = (adsWatchedCount + 1).coerceAtMost(requiredAds)
        RewardVideoPlayerDialog(
            adNumber = currentAdNum,
            totalAds = requiredAds,
            onRewardEarned = {
                handleRewardSuccess()
            },
            onDismiss = {
                showRewardVideoPlayer = false
            }
        )
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✨ تجربة VIP خالية من الإعلانات",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Premium",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 8.dp)
                    )

                    Text(
                        text = "مشاهدة بدون أي إعلانات",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "استمتع بمشاهدة جميع القنوات والمباريات بأعلى جودة وبدون انقطاع! شاهد إعلاني فيديو قصيرين من Unity واحصل فوراً على 24 ساعة كاملة بدون إعلانات.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (isAdFree) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "✨ الحماية من الإعلانات مفعّلة حالياً",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الوقت المتبقي: $timeRemainingText",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    } else {
                        // Progress pill showing watched ads status
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (adsWatchedCount > 0) Color(0xFFFFD700).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (adsWatchedCount == 0) "الخطوة: 0 من 2 إعلانات" else "🎉 أحسنت! شاهدت 1 من 2 إعلانات",
                                    color = if (adsWatchedCount > 0) Color(0xFFFFD700) else Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (adsWatchedCount >= 1) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.4f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(if (adsWatchedCount >= 2) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }

                    // Feature highlights box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F121A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22283A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("سرعة فائقة واستجابة فورية بدون تقطيع", color = Color.White, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tv, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بث جميع المباريات والقنوات بدقة FHD/4K", color = Color.White, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تفعيل آمن وفوري لمدة 24 ساعة كاملة", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    Button(
                        onClick = { playRewardVideo() },
                        enabled = !isLoadingAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdFree) Color(0xFF4CAF50) else if (adsWatchedCount == 1) Color(0xFFFF9800) else Color(0xFFE50914)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoadingAd) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        isAdFree -> "تجديد 24 ساعة أخرى (مشاهدة إعلانين)"
                                        adsWatchedCount == 0 -> "شاهد الإعلان الأول (1/2)"
                                        else -> "شاهد الإعلان الثاني (2/2) لتفعيل الميزة"
                                    },
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
