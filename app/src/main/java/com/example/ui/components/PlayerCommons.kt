package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.ui.theme.*

// Shared Color Palette for Cinematic Player System
val PlayerBgBlack = Color(0xFF070707)
val PlayerSurfaceTranslucent = Color(0xCC121212)
val PlayerSurfaceGlass = Color(0x991C1C1E)
val PlayerAccentGold = GoldGlow // #FFD700
val PlayerAccentRed = Color(0xFFE50914) // Shahid / TOD Red
val PlayerTextMuted = Color(0xFFB0B0B0)

data class TrackItem(
    val id: String,
    val label: String,
    val isSelected: Boolean,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1,
    val language: String = "",
    val resolution: String = ""
)

fun formatPlayerTime(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun launchPictureInPicture(activity: Activity?) {
    if (activity == null) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            try {
                @Suppress("DEPRECATION")
                activity.enterPictureInPictureMode()
            } catch (e2: Exception) {
                // Ignore if not supported
            }
        }
    }
}

@Composable
fun PlayerIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black.copy(alpha = 0.45f),
    tint: Color = Color.White,
    glowColor: Color = PlayerAccentGold,
    shape: Shape = CircleShape,
    size: Int = 42,
    iconSize: Int = 20,
    badgeText: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) glowColor else Color.Transparent,
                shape = shape
            )
            .clip(shape)
            .background(if (isFocused) containerColor.copy(alpha = 0.85f) else containerColor)
            .clickable { onClick() }
            .graphicsLayer {
                scaleX = if (isFocused) 1.08f else 1.0f
                scaleY = if (isFocused) 1.08f else 1.0f
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) glowColor else tint,
            modifier = Modifier.size(iconSize.dp)
        )
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .background(PlayerAccentRed, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
fun extractAudioTracks(exoPlayer: ExoPlayer): List<TrackItem> {
    val tracks = exoPlayer.currentTracks
    val list = mutableListOf<TrackItem>()
    var audioIdx = 1
    
    for (groupIndex in 0 until tracks.groups.size) {
        val group = tracks.groups[groupIndex]
        if (group.type == C.TRACK_TYPE_AUDIO) {
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                val lang = format.language ?: "Undetermined"
                val label = format.label ?: if (lang != "Undetermined") "مسار صوتي ($lang)" else "صوت $audioIdx"
                list.add(
                    TrackItem(
                        id = "audio_${groupIndex}_$trackIndex",
                        label = label,
                        isSelected = isSelected,
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        language = lang
                    )
                )
                audioIdx++
            }
        }
    }
    return list
}

@OptIn(UnstableApi::class)
fun extractSubtitleTracks(exoPlayer: ExoPlayer): List<TrackItem> {
    val tracks = exoPlayer.currentTracks
    val list = mutableListOf<TrackItem>()
    
    // Add 'Disable Subtitles' option
    var anySubtitleSelected = false
    val tempSubtitles = mutableListOf<TrackItem>()
    var subIdx = 1
    
    for (groupIndex in 0 until tracks.groups.size) {
        val group = tracks.groups[groupIndex]
        if (group.type == C.TRACK_TYPE_TEXT) {
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                if (isSelected) anySubtitleSelected = true
                val lang = format.language ?: "Undetermined"
                val label = format.label ?: if (lang != "Undetermined") "ترجمة ($lang)" else "ترجمة $subIdx"
                tempSubtitles.add(
                    TrackItem(
                        id = "sub_${groupIndex}_$trackIndex",
                        label = label,
                        isSelected = isSelected,
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        language = lang
                    )
                )
                subIdx++
            }
        }
    }
    
    list.add(
        TrackItem(
            id = "sub_off",
            label = "إيقاف الترجمة",
            isSelected = !anySubtitleSelected
        )
    )
    list.addAll(tempSubtitles)
    return list
}

@OptIn(UnstableApi::class)
fun extractQualityTracks(exoPlayer: ExoPlayer): List<TrackItem> {
    val tracks = exoPlayer.currentTracks
    val list = mutableListOf<TrackItem>()
    
    // Auto Quality Option
    var isManualSelected = false
    val qualityList = mutableListOf<TrackItem>()
    
    for (groupIndex in 0 until tracks.groups.size) {
        val group = tracks.groups[groupIndex]
        if (group.type == C.TRACK_TYPE_VIDEO) {
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                if (isSelected && exoPlayer.trackSelectionParameters.overrides.isNotEmpty()) {
                    isManualSelected = true
                }
                val height = format.height
                val width = format.width
                val resolutionLabel = when {
                    height >= 2160 || width >= 3840 -> "4K UHD (2160p)"
                    height >= 1080 || width >= 1920 -> "Full HD (1080p)"
                    height >= 720 || width >= 1280 -> "HD (720p)"
                    height >= 480 -> "SD (480p)"
                    height > 0 -> "${height}p"
                    else -> "فيديو عالي الدقة"
                }
                val bitrateMb = if (format.bitrate > 0) " (${format.bitrate / 1000000} Mbps)" else ""
                qualityList.add(
                    TrackItem(
                        id = "video_${groupIndex}_$trackIndex",
                        label = "$resolutionLabel$bitrateMb",
                        isSelected = isSelected,
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        resolution = "${height}p"
                    )
                )
            }
        }
    }
    
    list.add(
        TrackItem(
            id = "quality_auto",
            label = "تلقائي (الأفضل للجهاز والشبكة)",
            isSelected = !isManualSelected
        )
    )
    list.addAll(qualityList.distinctBy { it.label })
    return list
}

@OptIn(UnstableApi::class)
fun applyTrackSelection(exoPlayer: ExoPlayer, trackItem: TrackItem, trackType: Int) {
    if (trackItem.id == "sub_off") {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        return
    }
    if (trackItem.id == "quality_auto") {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .build()
        return
    }

    if (trackItem.groupIndex >= 0 && trackItem.trackIndex >= 0) {
        val tracks = exoPlayer.currentTracks
        if (trackItem.groupIndex < tracks.groups.size) {
            val group = tracks.groups[trackItem.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, trackItem.trackIndex)
            val builder = exoPlayer.trackSelectionParameters.buildUpon()
            if (trackType == C.TRACK_TYPE_TEXT) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            }
            builder.setOverrideForType(override)
            exoPlayer.trackSelectionParameters = builder.build()
        }
    }
}
