package com.palmastro.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    background = Color(0xFFFFFBFF),
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Color(0xFF49454E),
    error = Color(0xFFD32F2F),
    outline = Color(0xFFCAC4D0),
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
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFEF5350),
    outline = Color(0xFF938F99),
)

@Composable
fun PalmAstroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
