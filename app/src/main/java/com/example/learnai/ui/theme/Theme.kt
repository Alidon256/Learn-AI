package com.example.learnai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Custom colors for futuristic aesthetic
val BluePrimary = Color(0xFF3B82F6) // Vibrant blue for primary actions
val BlueSecondary = Color(0xFF1E3A8A) // Darker blue for secondary elements
val AccentPink = Color(0xFFF472B6) // Pink accent for highlights
val BackgroundLight = Color(0xFFF8FAFC) // Light background
val BackgroundDark = Color(0xFF0F172A) // Dark background
val SurfaceLight = Color(0xFFFFFFFF) // Light surface
val SurfaceDark = Color(0xFF1E293B) // Dark surface
val OnPrimary = Color(0xFFFFFFFF) // Text/icons on primary
val OnSecondary = Color(0xFFFFFFFF) // Text/icons on secondary
val OnBackgroundLight = Color(0xFF0F172A) // Text on light background
val OnBackgroundDark = Color(0xFFE2E8F0) // Text on dark background
val OnSurfaceLight = Color(0xFF0F172A) // Text on light surface
val OnSurfaceDark = Color(0xFFE2E8F0) // Text on dark surface

// Gradient for futuristic background
val FuturisticGradient = Brush.linearGradient(
    colors = listOf(BlueSecondary, BluePrimary)
)

// Define color schemes
private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = AccentPink,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary,
    tertiary = AccentPink,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark
)

// Custom typography for futuristic look
private val FuturisticTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = 0.5.sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.5.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )
)

// Local composition for gradient
val LocalGradient = staticCompositionLocalOf { FuturisticGradient }

@Composable
fun LearnAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalGradient provides FuturisticGradient) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FuturisticTypography,
            content = content
        )
    }
}