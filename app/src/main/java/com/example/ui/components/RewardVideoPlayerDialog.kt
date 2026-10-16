package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private val VIDEO_AD_URLS = listOf(
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
)

private val SPONSORS = listOf(
    Pair("+90 VIP Sports & Live", "شاهد أقوى الدوريات والبطولات العالمية بدقة Full HD وبدون تقطيع"),
    Pair("باقة القنوات والسينما الشاملة", "أكثر من 5000 قناة رياضية وترفيهية وأحدث الأفلام والمسلسلات"),
    Pair("سيرفرات البث فائق السرعة", "بث مباشر ثابت ومستقر يدعم كل سرعات الإنترنت")
)

@OptIn(UnstableApi::class)
@Composable
fun RewardVideoPlayerDialog(
    adNumber: Int = 1,
    totalAds: Int = 2,
    onRewardEarned: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val randomVideoUrl = remember { VIDEO_AD_URLS.random() }
    val randomSponsor = remember { SPONSORS.random() }

    var isCompleted by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(20) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var showExitWarning by remember { mutableStateOf(false) }
    var isPlayerReady by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        val mediaSourceFactory = ExoPlayerDataSourceUtils.createMediaSourceFactory(context)
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                val mediaItem = MediaItem.fromUri(randomVideoUrl)
                setMediaItem(mediaItem)
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isPlayerReady = true
                } else if (playbackState == Player.STATE_ENDED) {
                    if (!isCompleted) {
                        isCompleted = true
                        secondsRemaining = 0
                        playbackProgress = 1f
                        onRewardEarned()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // If video source has issue, smoothly fulfill after short timeout
                if (!isCompleted) {
                    isCompleted = true
                    secondsRemaining = 0
                    playbackProgress = 1f
                    onRewardEarned()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Countdown Timer & Progress ticker
    LaunchedEffect(isCompleted, isPlayerReady) {
        if (!isCompleted) {
            val totalSeconds = 20
            while (secondsRemaining > 0) {
                delay(1000)
                if (exoPlayer.isPlaying || !isPlayerReady) {
                    secondsRemaining--
                    val elapsed = totalSeconds - secondsRemaining
                    playbackProgress = (elapsed.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                }
            }
            if (!isCompleted) {
                isCompleted = true
                playbackProgress = 1f
                onRewardEarned()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (isCompleted) {
                onDismiss()
            } else {
                showExitWarning = true
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Fullscreen Video Player Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (!isPlayerReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFD700),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "جاري تحميل إعلان الفيديو والمكافأة...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Top Gradient & Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Badge & Mute
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "إعلان $adNumber من $totalAds • Unity Reward",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Right Countdown or Done Button
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.clickable { onDismiss() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (adNumber < totalAds) "متابعة للإعلان التالي ($adNumber/$totalAds)" else "إغلاق ومتابعة",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "⏳ المكافأة بعد $secondsRemaining ث",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = { showExitWarning = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Overlay (Sponsor info & Reward button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { playbackProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFFFD700),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    // Sponsor Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = randomSponsor.first,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = randomSponsor.second,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Action Button
                    if (isCompleted) {
                        Button(
                            onClick = { onDismiss() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (adNumber < totalAds)
                                    "✨ اكتمل الإعلان $adNumber من $totalAds! اضغط لمشاهدة الإعلان التالي"
                                else
                                    "🎉 تم تفعيل 24 ساعة بدون إعلانات بنجاح! اضغط للمتابعة",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "معرفة المزيد عن عروض الباقات الرياضية",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Early exit warning dialog
            if (showExitWarning) {
                AlertDialog(
                    onDismissRequest = { showExitWarning = false },
                    title = {
                        Text(
                            text = "هل تريد الخروج؟",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "لم يكتمل الإعلان بعد! في حال الخروج الآن لن يتم تفعيل الـ 24 ساعة بدون إعلانات.",
                            color = Color.LightGray
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showExitWarning = false }
                        ) {
                            Text("متابعة المشاهدة", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showExitWarning = false
                                onDismiss()
                            }
                        ) {
                            Text("خروج دون مكافأة", color = Color.Red.copy(alpha = 0.8f))
                        }
                    },
                    containerColor = Color(0xFF222222),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
