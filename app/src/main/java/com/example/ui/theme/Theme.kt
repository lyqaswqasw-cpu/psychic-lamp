package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LoopRed,
    secondary = GoldGlow,
    tertiary = LoopRedGlow,
    background = ObsidianBlack,
    surface = MidnightGrey,
    onPrimary = CleanWhite,
    onSecondary = ObsidianBlack,
    onBackground = CleanWhite,
    onSurface = CleanWhite
)

private val LightColorScheme = lightColorScheme(
    primary = LoopRed,
    secondary = GoldGlow,
    tertiary = LoopRedGlow,
    background = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF1F2937)
)

@Composable
fun MyApplicationTheme(
    themeName: String = "red",
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDark) {
        when (themeName) {
            "gold" -> darkColorScheme(
                primary = Color(0xFFFFD700),
                secondary = Color(0xFFFFC107),
                tertiary = Color(0xFFFFE082),
                background = Color(0xFF070707),
                surface = Color(0xFF141414),
                onPrimary = Color(0xFF070707),
                onSecondary = Color(0xFF070707),
                onBackground = Color.White,
                onSurface = Color.White
            )
            "purple" -> darkColorScheme(
                primary = Color(0xFF9D4EDD),
                secondary = Color(0xFFC77DFF),
                tertiary = Color(0xFFE0AAFF),
                background = Color(0xFF05030B),
                surface = Color(0xFF120E1F),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
            "emerald" -> darkColorScheme(
                primary = Color(0xFF00C9A7),
                secondary = Color(0xFF4D96FF),
                tertiary = Color(0xFF38ef7d),
                background = Color(0xFF030A09),
                surface = Color(0xFF0E1A17),
                onPrimary = Color(0xFF030A09),
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
            "blue" -> darkColorScheme(
                primary = Color(0xFF007BFF),
                secondary = Color(0xFF00D2FF),
                tertiary = Color(0xFF3333FF),
                background = Color(0xFF020710),
                surface = Color(0xFF0B1323),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
            else -> DarkColorScheme
        }
    } else {
        when (themeName) {
            "gold" -> lightColorScheme(
                primary = Color(0xFFB8860B),
                secondary = Color(0xFFDAA520),
                tertiary = Color(0xFFCD853F),
                background = Color(0xFFFDFBF7),
                surface = Color(0xFFFFFFFF),
                onPrimary = Color.White,
                onSecondary = Color.Black,
                onBackground = Color(0xFF222222),
                onSurface = Color(0xFF333333)
            )
            "purple" -> lightColorScheme(
                primary = Color(0xFF7B2CBF),
                secondary = Color(0xFF9D4EDD),
                tertiary = Color(0xFFC77DFF),
                background = Color(0xFFF8F5FC),
                surface = Color(0xFFFFFFFF),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF1E1B29),
                onSurface = Color(0xFF2D2640)
            )
            "emerald" -> lightColorScheme(
                primary = Color(0xFF00897B),
                secondary = Color(0xFF00A896),
                tertiary = Color(0xFF02C39A),
                background = Color(0xFFF0FDF9),
                surface = Color(0xFFFFFFFF),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF0F2A25),
                onSurface = Color(0xFF163E37)
            )
            "blue" -> lightColorScheme(
                primary = Color(0xFF0056B3),
                secondary = Color(0xFF007BFF),
                tertiary = Color(0xFF3399FF),
                background = Color(0xFFF0F6FF),
                surface = Color(0xFFFFFFFF),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF0A192F),
                onSurface = Color(0xFF112240)
            )
            else -> LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
