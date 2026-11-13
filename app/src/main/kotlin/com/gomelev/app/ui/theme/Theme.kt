package com.gomelev.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class SemanticColors(
    val passContainer: Color,
    val onPassContainer: Color,
    val passAccent: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
    val failContainer: Color,
    val onFailContainer: Color,
    val studioBackdrop: Color,
    val studioBorder: Color
)

val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(
        passContainer = Color(0xFFDDF5EC),
        onPassContainer = Color(0xFF074936),
        passAccent = Color(0xFF0E7C5D),
        warnContainer = Color(0xFFFEF1D6),
        onWarnContainer = Color(0xFF734500),
        failContainer = Color(0xFFFDE4E1),
        onFailContainer = Color(0xFF7B1B15),
        studioBackdrop = Color(0xFFF3F5F4),
        studioBorder = Color(0xFFE3E7E5)
    )
}

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0E7C5D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3F4E6),
    onPrimaryContainer = Color(0xFF063829),
    secondary = Color(0xFF3E4E4A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5EAE8),
    onSecondaryContainer = Color(0xFF17211E),
    tertiary = Color(0xFF285E6B),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111514),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF111514),
    surfaceVariant = Color(0xFFECEFED),
    onSurfaceVariant = Color(0xFF56605D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F6F5),
    surfaceContainer = Color(0xFFEDF0EE),
    surfaceContainerHigh = Color(0xFFE6EAE8),
    outlineVariant = Color(0xFFDDE2DF)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF4CE0B3),
    onPrimary = Color(0xFF02281D),
    primaryContainer = Color(0xFF0C3B2E),
    onPrimaryContainer = Color(0xFFA8F7DC),
    secondary = Color(0xFFB5C5C0),
    onSecondary = Color(0xFF1D2724),
    secondaryContainer = Color(0xFF232B28),
    onSecondaryContainer = Color(0xFFD8E3DF),
    tertiary = Color(0xFF8CCBDB),
    background = Color(0xFF0D0F10),
    onBackground = Color(0xFFF1F4F3),
    surface = Color(0xFF0D0F10),
    onSurface = Color(0xFFF1F4F3),
    surfaceVariant = Color(0xFF1E2324),
    onSurfaceVariant = Color(0xFFA0ACA8),
    surfaceContainerLowest = Color(0xFF090B0C),
    surfaceContainerLow = Color(0xFF141819),
    surfaceContainer = Color(0xFF191D1F),
    surfaceContainerHigh = Color(0xFF212628),
    outlineVariant = Color(0xFF282E30)
)

private val EditorialTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val MinimalShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GomelEvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dyn = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val fallback = if (darkTheme) DarkColors else LightColors
            // Keep clean minimalist neutral surfaces & dynamic accent harmony
            fallback.copy(
                primary = dyn.primary,
                primaryContainer = dyn.primaryContainer,
                onPrimaryContainer = dyn.onPrimaryContainer
            )
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val semantic = if (darkTheme) {
        SemanticColors(
            passContainer = Color(0xFF0E382C),
            onPassContainer = Color(0xFF9DF5D6),
            passAccent = Color(0xFF4CE0B3),
            warnContainer = Color(0xFF3E2904),
            onWarnContainer = Color(0xFFFFD88A),
            failContainer = Color(0xFF421412),
            onFailContainer = Color(0xFFFFB5AE),
            studioBackdrop = Color(0xFF171B1D),
            studioBorder = Color(0xFF272D30)
        )
    } else {
        SemanticColors(
            passContainer = Color(0xFFDDF5EC),
            onPassContainer = Color(0xFF074936),
            passAccent = Color(0xFF0E7C5D),
            warnContainer = Color(0xFFFEF1D6),
            onWarnContainer = Color(0xFF734500),
            failContainer = Color(0xFFFDE4E1),
            onFailContainer = Color(0xFF7B1B15),
            studioBackdrop = Color(0xFFF1F4F3),
            studioBorder = Color(0xFFE1E6E3)
        )
    }

    CompositionLocalProvider(LocalSemanticColors provides semantic) {
        MaterialTheme(
            colorScheme = baseScheme,
            typography = EditorialTypography,
            shapes = MinimalShapes,
            content = content
        )
    }
}
