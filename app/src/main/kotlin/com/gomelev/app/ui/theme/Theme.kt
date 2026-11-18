package com.gomelev.app.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive motion physics tokens.
 */
object ExpressiveMotion {
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = 750f
    )

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessLow
    )

    val colorTween: FiniteAnimationSpec<Color> = tween(
        durationMillis = 360,
        easing = FastOutSlowInEasing
    )
}

@Immutable
data class SemanticColors(
    val passAccent: Color,
    val passContainer: Color,
    val onPassContainer: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
    val failContainer: Color,
    val onFailContainer: Color,
    val studioBackdrop: Color,
    val studioBorder: Color,
    val stageTop: Color,
    val stageBottom: Color,
    val stageSpot: Color,
    val brandGradientStart: Color,
    val brandGradientEnd: Color,
    val isDark: Boolean
)

private val LightSemantic = SemanticColors(
    passAccent = Color(0xFF007A5E),
    passContainer = Color(0xFFD5F5EA),
    onPassContainer = Color(0xFF003D2E),
    warnContainer = Color(0xFFFFECC7),
    onWarnContainer = Color(0xFF5D3A00),
    failContainer = Color(0xFFFFE2E0),
    onFailContainer = Color(0xFF6B1316),
    studioBackdrop = Color(0xFFF3F8F6),
    studioBorder = Color(0xFFD0E0DC),
    stageTop = Color(0xFFFFFFFF),
    stageBottom = Color(0xFFE7F2EE),
    stageSpot = Color(0x4216B890),
    brandGradientStart = Color(0xFF006D5B),
    brandGradientEnd = Color(0xFF0B7E8C),
    isDark = false
)

private val DarkSemantic = SemanticColors(
    passAccent = Color(0xFF4BE2B8),
    passContainer = Color(0xFF0D4438),
    onPassContainer = Color(0xFFB3FAE3),
    warnContainer = Color(0xFF473004),
    onWarnContainer = Color(0xFFFFDF9B),
    failContainer = Color(0xFF54181D),
    onFailContainer = Color(0xFFFFD8D6),
    studioBackdrop = Color(0xFF132026),
    studioBorder = Color(0xFF283D47),
    stageTop = Color(0xFF1C2E36),
    stageBottom = Color(0xFF0D171C),
    stageSpot = Color(0x484BE2B8),
    brandGradientStart = Color(0xFF2ED9B1),
    brandGradientEnd = Color(0xFF45CFE0),
    isDark = true
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006B58),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2F5E6),
    onPrimaryContainer = Color(0xFF00211A),
    secondary = Color(0xFF0E6774),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFEFF4),
    onSecondaryContainer = Color(0xFF04262B),
    tertiary = Color(0xFF8C5A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE4B5),
    onTertiaryContainer = Color(0xFF2C1900),
    error = Color(0xFFBA2D33),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD8),
    onErrorContainer = Color(0xFF410004),
    background = Color(0xFFF3F7F6),
    onBackground = Color(0xFF0F1A1C),
    surface = Color(0xFFF9FCFB),
    onSurface = Color(0xFF0F1A1C),
    surfaceVariant = Color(0xFFDFE9E6),
    onSurfaceVariant = Color(0xFF435357),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF3F1),
    surfaceContainer = Color(0xFFE5EEEC),
    surfaceContainerHigh = Color(0xFFDBE7E4),
    surfaceContainerHighest = Color(0xFFD1DFDC),
    outline = Color(0xFF708185),
    outlineVariant = Color(0xFFC7D7D3)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF56E2BE),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0D4E40),
    onPrimaryContainer = Color(0xFFBDFCEB),
    secondary = Color(0xFF6CD6E3),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF134B54),
    onSecondaryContainer = Color(0xFFC2F3FA),
    tertiary = Color(0xFFFFCA6E),
    onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF633E00),
    onTertiaryContainer = Color(0xFFFFE5B8),
    error = Color(0xFFFFB4B0),
    onError = Color(0xFF68000A),
    errorContainer = Color(0xFF8C1D24),
    onErrorContainer = Color(0xFFFFDAD8),
    background = Color(0xFF0A1114),
    onBackground = Color(0xFFE6F0EE),
    surface = Color(0xFF0F181C),
    onSurface = Color(0xFFE6F0EE),
    surfaceVariant = Color(0xFF1F2D33),
    onSurfaceVariant = Color(0xFF9FB2B6),
    surfaceContainerLowest = Color(0xFF070C0E),
    surfaceContainerLow = Color(0xFF121C21),
    surfaceContainer = Color(0xFF17242A),
    surfaceContainerHigh = Color(0xFF1E2D34),
    surfaceContainerHighest = Color(0xFF273941),
    outline = Color(0xFF6C8086),
    outlineVariant = Color(0xFF283940)
)

private val ExpressiveTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
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
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
private fun animColor(target: Color): Color {
    val c by animateColorAsState(
        targetValue = target,
        animationSpec = ExpressiveMotion.colorTween,
        label = "themeColor"
    )
    return c
}

@Composable
fun GomelEvTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val targetScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val animatedScheme = targetScheme.copy(
        primary = animColor(targetScheme.primary),
        onPrimary = animColor(targetScheme.onPrimary),
        primaryContainer = animColor(targetScheme.primaryContainer),
        onPrimaryContainer = animColor(targetScheme.onPrimaryContainer),
        secondary = animColor(targetScheme.secondary),
        secondaryContainer = animColor(targetScheme.secondaryContainer),
        onSecondaryContainer = animColor(targetScheme.onSecondaryContainer),
        tertiary = animColor(targetScheme.tertiary),
        tertiaryContainer = animColor(targetScheme.tertiaryContainer),
        background = animColor(targetScheme.background),
        onBackground = animColor(targetScheme.onBackground),
        surface = animColor(targetScheme.surface),
        onSurface = animColor(targetScheme.onSurface),
        surfaceVariant = animColor(targetScheme.surfaceVariant),
        onSurfaceVariant = animColor(targetScheme.onSurfaceVariant),
        surfaceContainerLowest = animColor(targetScheme.surfaceContainerLowest),
        surfaceContainerLow = animColor(targetScheme.surfaceContainerLow),
        surfaceContainer = animColor(targetScheme.surfaceContainer),
        surfaceContainerHigh = animColor(targetScheme.surfaceContainerHigh),
        surfaceContainerHighest = animColor(targetScheme.surfaceContainerHighest),
        outline = animColor(targetScheme.outline),
        outlineVariant = animColor(targetScheme.outlineVariant)
    )

    val targetSem = if (darkTheme) DarkSemantic else LightSemantic
    val animatedSemantic = SemanticColors(
        passAccent = animColor(targetSem.passAccent),
        passContainer = animColor(targetSem.passContainer),
        onPassContainer = animColor(targetSem.onPassContainer),
        warnContainer = animColor(targetSem.warnContainer),
        onWarnContainer = animColor(targetSem.onWarnContainer),
        failContainer = animColor(targetSem.failContainer),
        onFailContainer = animColor(targetSem.onFailContainer),
        studioBackdrop = animColor(targetSem.studioBackdrop),
        studioBorder = animColor(targetSem.studioBorder),
        stageTop = animColor(targetSem.stageTop),
        stageBottom = animColor(targetSem.stageBottom),
        stageSpot = animColor(targetSem.stageSpot),
        brandGradientStart = animColor(targetSem.brandGradientStart),
        brandGradientEnd = animColor(targetSem.brandGradientEnd),
        isDark = darkTheme
    )

    CompositionLocalProvider(LocalSemanticColors provides animatedSemantic) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = ExpressiveTypography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}
