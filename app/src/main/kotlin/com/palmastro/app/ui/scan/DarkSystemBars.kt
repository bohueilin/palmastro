package com.palmastro.app.ui.scan

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Forces light (white) system-bar icons while a full-bleed dark screen is on top.
 *
 * MainActivity calls the bare enableEdgeToEdge(), whose default style derives the icon
 * tint from the system theme — so on a light-theme phone the clock and battery render
 * black over the night-sky panel and the camera preview (1.3:1). On API 26-28 the same
 * default also paints window.navigationBarColor with a near-opaque white scrim, which
 * the appearance flags do NOT undo, so the color is cleared here too.
 *
 * Every value is restored on dispose, keeping the rest of the app's light surfaces intact.
 */
@Suppress("DEPRECATION") // window.navigationBarColor: no replacement below API 35
@Composable
internal fun DarkSystemBarsEffect() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val activity = generateSequence(view.context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().first()
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, view)
        val previousLightStatus = controller.isAppearanceLightStatusBars
        val previousLightNav = controller.isAppearanceLightNavigationBars
        val previousNavColor = window.navigationBarColor
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        window.navigationBarColor = Color.TRANSPARENT
        onDispose {
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNav
            window.navigationBarColor = previousNavColor
        }
    }
}
