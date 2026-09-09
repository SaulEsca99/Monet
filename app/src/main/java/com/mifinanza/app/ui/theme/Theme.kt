package com.mifinanza.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Light palette (default) ───────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary            = Color(0xFF00A37A),   // slightly darker teal for light bg
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFE0FBF4),
    background         = Color(0xFFF7F8FA),   // very light warm gray
    surface            = Color(0xFFFFFFFF),   // pure white cards
    surfaceVariant     = Color(0xFFF1F3F6),   // light input bg
    onBackground       = Color(0xFF0D1117),
    onSurface          = Color(0xFF0D1117),
    onSurfaceVariant   = Color(0xFF5A6478),
    outline            = Color(0xFFDDE2EC),
    outlineVariant     = Color(0xFFEEF1F6),
    error              = Color(0xFFE53935),
    secondaryContainer = Color(0xFFEEF9F6),
)

// ── Dark palette ──────────────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary            = GreenBrand,
    onPrimary          = Color(0xFF002B1E),
    primaryContainer   = Color(0xFF003D2A),
    background         = Color(0xFF080D14),
    surface            = Color(0xFF111827),
    surfaceVariant     = Color(0xFF1A2333),
    onBackground       = Color(0xFFF0F5FF),
    onSurface          = Color(0xFFF0F5FF),
    onSurfaceVariant   = Color(0xFF8896AA),
    outline            = Color(0xFF273347),
    outlineVariant     = Color(0xFF1E2D40),
    error              = RedBrand,
)

val AppTypography = Typography(
    displayLarge   = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black,  letterSpacing = (-2).sp),
    displayMedium  = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Black,  letterSpacing = (-1.5).sp),
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold,   letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge     = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge      = TextStyle(fontSize = 16.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp),
    bodySmall      = TextStyle(fontSize = 12.sp),
    labelSmall     = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
)

@Composable
fun MiFinanzaTheme(darkMode: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkMode) DarkColors else LightColors,
        typography  = AppTypography,
        content     = content
    )
}
