package com.nothopeless.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD4724A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF3D1100),
    secondary = Color(0xFF7B6E64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFDDD4),
    onSecondaryContainer = Color(0xFF2E201B),
    tertiary = Color(0xFF5B7E75),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6EDE8),
    onTertiaryContainer = Color(0xFF0F2420),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A16),
    surfaceVariant = Color(0xFFF3DDD5),
    onSurfaceVariant = Color(0xFF52443D),
    outline = Color(0xFF84746C),
    outlineVariant = Color(0xFFD7C2BA),
    scrim = Color(0xFF000000),
)

val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
)

@Composable
fun NotHopelessTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content,
    )
}
