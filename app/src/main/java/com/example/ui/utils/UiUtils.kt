package com.example.ui.utils

import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Universal safe async image loader.
 */
@Composable
fun SafeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp),
    fallbackLabel: String? = null
) {
    val context = LocalContext.current
    val cleanModel = remember(model) {
        when (model) {
            null -> ""
            is String -> {
                val trimmed = model.trim()
                if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true)) ""
                else trimmed.replace(" ", "%20")
            }
            else -> model
        }
    }

    AsyncImage(
        model = remember(cleanModel) {
            ImageRequest.Builder(context)
                .data(cleanModel)
                .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .crossfade(true)
                .allowHardware(true)
                .build()
        },
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale
    )
}


fun Modifier.tvFocusable(
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    val baseModifier = this
        .scale(scale)
        .onFocusChanged { isFocused = it.isFocused }
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) Color.White else Color.Transparent,
            shape = shape
        )
    
    if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier.focusable()
    }
}

fun discoverAvailableQualities(url: String): List<String> {
    return listOf(
        "1080p Full HD",
        "720p HD",
        "480p SD"
    )
}
