package com.palmastro.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Purple40 = Color(0xFF7E57C2)
private val PurpleLight = Color(0xFFB39DDB)
private val PurpleDark = Color(0xFF512DA8)
private val Teal40 = Color(0xFF26A69A)
private val TealLight = Color(0xFF80CBC4)
private val Surface = Color(0xFFF8F5FF)
private val OnSurface = Color(0xFF1C1B1F)
private val SurfaceVariant = Color(0xFFEDE7F6)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF21005E),
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00332E),
    tertiary = Color(0xFFFF7043),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF3A0B00),
    background = Color(0xFFFFFBFF),
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFF49454E),
    error = Color(0xFFD32F2F),
    // outline is the stated border tone, outlineVariant the subtler one; they were
    // previously the same hex, so the scheme had no outline hierarchy at all.
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFF8F8A99),
)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleLight,
    onPrimary = Color(0xFF21005E),
    primaryContainer = PurpleDark,
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = TealLight,
    onSecondary = Color(0xFF00332E),
    secondaryContainer = Color(0xFF00796B),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFF8A65),
    onTertiary = Color(0xFF3E1500),
    tertiaryContainer = Color(0xFF7A3B22),
    onTertiaryContainer = Color(0xFFFFDBCF),
    // The page must sit BELOW the card ground: background and surface used to be the
    // same hex, and the M3 baseline surfaceContainerLow is that hex a third time, so
    // every card dissolved into the page in dark mode.
    background = Color(0xFF121116),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceContainerLow = Color(0xFF221F27),
    surfaceContainerHigh = Color(0xFF2B2830),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    // Light error tone on dark surfaces (M3 convention): the previous #EF5350 left the
    // journal delete glyph at 2.7:1 on its own card, under the 3:1 non-text floor.
    error = Color(0xFFF2B8B5),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF6F6B75),
)

/**
 * Brand semantic colors that Material3's scheme doesn't cover: grade colors for
 * the four result grades plus delta indicators. Results/history screens consume
 * these via [LocalPalmAstroExtendedColors] (e.g.
 * `LocalPalmAstroExtendedColors.current.gradeGrowing`). Colors are calm by design
 * (PRD 12.3/37) — no aggressive red outside true error states, and every grade
 * pairs with an `on*` color that keeps >= 4.5:1 contrast.
 *
 * [meterTrack] is separate from `outlineVariant` on purpose: outlineVariant is the
 * SUBTLE outline token, while a meter's unfilled range carries information and must
 * hold >= 3:1 (WCAG 1.4.11) against every ground it lands on — including the
 * grade-tinted hero washes behind the score gauge.
 */
@Immutable
data class PalmAstroExtendedColors(
    val gradeGrowing: Color,
    val onGradeGrowing: Color,
    val gradeStable: Color,
    val onGradeStable: Color,
    val gradeBuilding: Color,
    val onGradeBuilding: Color,
    val gradeWatchOut: Color,
    val onGradeWatchOut: Color,
    val deltaPositive: Color,
    val deltaNegative: Color,
    val deltaNeutral: Color,
    val meterTrack: Color,
)

private val LightExtendedColors = PalmAstroExtendedColors(
    gradeGrowing = Color(0xFF2E7D32),
    onGradeGrowing = Color.White,
    gradeStable = Color(0xFF00695C),
    onGradeStable = Color.White,
    gradeBuilding = Color(0xFF5E35B1),
    onGradeBuilding = Color.White,
    gradeWatchOut = Color(0xFFB35A00),
    onGradeWatchOut = Color.White,
    deltaPositive = Color(0xFF2E7D32),
    deltaNegative = Color(0xFFB35A00),
    deltaNeutral = Color(0xFF49454E),
    meterTrack = Color(0xFF75707A),
)

private val DarkExtendedColors = PalmAstroExtendedColors(
    gradeGrowing = Color(0xFF81C784),
    onGradeGrowing = Color(0xFF0A2E10),
    gradeStable = Color(0xFF80CBC4),
    onGradeStable = Color(0xFF00201C),
    gradeBuilding = Color(0xFFB39DDB),
    onGradeBuilding = Color(0xFF21005E),
    gradeWatchOut = Color(0xFFFFB77C),
    onGradeWatchOut = Color(0xFF3A2000),
    deltaPositive = Color(0xFF81C784),
    deltaNegative = Color(0xFFFFB77C),
    deltaNeutral = Color(0xFFCAC4D0),
    meterTrack = Color(0xFF8A8590),
)

val LocalPalmAstroExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * App type scale: the values the screens had converged on by hand, defined once so
 * hierarchy is a token rather than a per-file discipline. Only styles the app
 * actually uses are overridden; everything else keeps the Material 3 defaults
 * (buttons/labelLarge, app bars/titleLarge, text fields/bodyLarge stay stock).
 */
private val PalmAstroTypography = Typography().let { base ->
    base.copy(
        titleMedium = base.titleMedium.copy(
            fontSize = 16.sp, lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp,
        ),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = base.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelMedium = base.labelMedium.copy(
            fontSize = 12.sp, lineHeight = 17.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
        ),
        labelSmall = base.labelSmall.copy(
            fontSize = 11.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
        ),
    )
}

/**
 * PalmAstro brand theme. The purple/teal brand palette is the DEFAULT (PRD 37);
 * Material You dynamic color is opt-in only via [dynamicColor] and stays off
 * for launch so the brand identity is stable across devices.
 */
@Composable
fun PalmAstroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalPalmAstroExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PalmAstroTypography,
            content = content,
        )
    }
}
