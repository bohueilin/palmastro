package com.palmastro.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The app's single reduced-motion signal (PRD §41): the system "remove animations"
 * accessibility setting drives ANIMATOR_DURATION_SCALE to zero. Compose animations
 * do NOT honor that scale automatically, so every motion surface must consult this.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
    }
}

private const val ENTRANCE_DURATION_MS = 220
private const val ENTRANCE_RISE_DP = 8

/**
 * One-shot staggered entrance for rare reveal moments (monthly results/guidance —
 * PRD §41 "reinforce scan success"). Fades in with a small rise, [index] * [staggerMs]
 * after composition. When [play] is false (reduced motion, revisits, restores) the
 * content renders settled immediately — motion is never load-bearing.
 */
@Composable
fun Modifier.entranceReveal(play: Boolean, index: Int, staggerMs: Long = 40L): Modifier {
    val alpha = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (play && alpha.value < 1f) {
            delay(index * staggerMs)
            alpha.animateTo(1f, tween(ENTRANCE_DURATION_MS, easing = FastOutSlowInEasing))
        }
    }
    val risePx = with(LocalDensity.current) { ENTRANCE_RISE_DP.dp.toPx() }
    return graphicsLayer {
        this.alpha = alpha.value
        translationY = (1f - alpha.value) * risePx
    }
}
