package com.palmastro.app.ui.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palmastro.app.R

// ─────────────────────────────────────────────────────────────────────────────
// The PalmAstro signature moment (UX_ROADMAP 1.2, PRD 41): while on-device
// analysis runs, the four captured palm lines draw themselves in calm teal
// (~1.2s, staggered), their endpoints ignite as stars, and thin constellation
// lines rise toward the night sky (~0.8s) as the analyzing label fades in.
// A gentle star-twinkle loops while processing continues.
//
// Calm by construction (PRD 36/67): slow FastOutSlowIn/sine easings, no flashing,
// and a bounded element budget — 4 lines + 11 star nodes + 5 edges + 4 ambient
// stars = 24 drawn elements. Under reduced motion the final frame renders
// statically with a standard progress indicator (PRD 41: never hide loading).
// ─────────────────────────────────────────────────────────────────────────────

// Brand palette (PRD 37): calm teal lines, royal-purple constellation, soft starlight.
private val LineTeal = Color(0xFF4FD1C5)
private val EdgeLavender = Color(0xFF9F7AEA)
private val StarLight = Color(0xFFEDEAFB)

// Reveal timeline: one eased 0..1 progress over 2s; sub-phases are windows of it.
private const val REVEAL_DURATION_MS = 2000
private const val LINE_STAGGER = 0.09f
private const val LINE_SPAN = 0.33f // last line finishes at 3*0.09+0.33 = 0.60 (~1.2s)
private const val NODE_START = 0.52f
private const val NODE_STAGGER = 0.02f
private const val NODE_SPAN = 0.14f
private const val EDGE_START = 0.62f
private const val EDGE_STAGGER = 0.045f
private const val EDGE_SPAN = 0.20f
private const val LABEL_START = 0.72f
private const val AMBIENT_START = 0.50f
private const val AMBIENT_END = 0.90f

// Twinkle loop: slow sine breathing, far below any flashing threshold (PRD 67).
private const val TWINKLE_MS = 1400
private const val TWINKLE_MIN = 0.65f
private const val TWINKLE_MAX = 1f

// Stroke/glow metrics (dp) — thin premium core with a soft multi-pass halo.
private const val GLOW_OUTER_DP = 12f
private const val GLOW_MID_DP = 6f
private const val CORE_DP = 2f
private const val GLOW_OUTER_ALPHA = 0.08f
private const val GLOW_MID_ALPHA = 0.20f
private const val CORE_ALPHA = 0.90f
private const val EDGE_WIDTH_DP = 1.2f
private const val EDGE_ALPHA = 0.45f
private const val NODE_RADIUS_DP = 2.2f
private const val HALO_RADIUS_DP = 6.5f
private const val HALO_ALPHA = 0.16f
private const val HALO_MIN_SCALE = 0.6f
private const val CORE_TWINKLE_FLOOR = 0.7f
private const val SKY_RADIUS_BOOST = 1.35f
private const val AMBIENT_RADIUS_DP = 1.2f
private const val AMBIENT_ALPHA = 0.35f

// Normalized geometry (x right, y down). Palm lines sit in the lower half;
// the constellation rises into the upper "sky" half.
private val HeartLine = listOf(Offset(0.16f, 0.66f), Offset(0.38f, 0.58f), Offset(0.62f, 0.57f), Offset(0.82f, 0.62f))
private val HeadLine = listOf(Offset(0.18f, 0.74f), Offset(0.44f, 0.72f), Offset(0.68f, 0.73f), Offset(0.80f, 0.77f))
private val LifeLine = listOf(Offset(0.24f, 0.70f), Offset(0.30f, 0.80f), Offset(0.38f, 0.90f), Offset(0.50f, 0.96f))
private val FateLine = listOf(Offset(0.55f, 0.96f), Offset(0.53f, 0.84f), Offset(0.52f, 0.72f), Offset(0.50f, 0.60f))
private val PalmLines = listOf(HeartLine, HeadLine, LifeLine, FateLine)

private val SkyStars = listOf(Offset(0.26f, 0.22f), Offset(0.50f, 0.10f), Offset(0.74f, 0.20f))

/** Line endpoints ignite first (indices 0..7), then the sky stars (8..10). */
private val StarNodes: List<Offset> = PalmLines.flatMap { listOf(it.first(), it.last()) } + SkyStars
private val PalmNodeCount = PalmLines.size * 2

/** Thin lines connecting palm endpoints upward into the constellation. */
private val ConstellationEdges = listOf(
    HeartLine.first() to SkyStars[0],
    SkyStars[0] to SkyStars[1],
    SkyStars[1] to SkyStars[2],
    SkyStars[2] to HeartLine.last(),
    FateLine.last() to SkyStars[1],
)

private val AmbientStars = listOf(Offset(0.12f, 0.34f), Offset(0.34f, 0.14f), Offset(0.65f, 0.28f), Offset(0.88f, 0.38f))

private fun window(t: Float, start: Float, end: Float): Float =
    ((t - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Full-screen processing state shown while the analysis pipeline runs (PRD 13.2 step 6).
 * Replaces the plain spinner screen; the caller's announceForAccessibility stays in ScanScreen.
 */
@Composable
fun ConstellationProcessingScreen(reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val progress = rememberRevealProgress(reduceMotion)
    val twinkle = rememberTwinkleAlpha(reduceMotion)
    val labelAlpha = { window(progress.value, LABEL_START, 1f) }
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.sc_complete_title),
            fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        ConstellationCanvas(
            progress = progress, twinkle = twinkle,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
        )
        Text(
            stringResource(R.string.sc_reveal_analyzing),
            fontSize = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha() },
        )
        Spacer(Modifier.height(12.dp))
        if (reduceMotion) {
            // Reduced motion: the constellation is static, so a standard indicator carries progress.
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
        }
        Text(
            stringResource(R.string.sc_complete_hint),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * One eased reveal progress 0..1. `reduceMotion` is stable for the composable's lifetime
 * (remembered once in ScanScreen), so branching before remember calls is safe here.
 */
@Composable
private fun rememberRevealProgress(reduceMotion: Boolean): State<Float> {
    if (reduceMotion) return remember { mutableStateOf(1f) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(REVEAL_DURATION_MS, easing = FastOutSlowInEasing))
    }
    return remember { derivedStateOf { progress.value } }
}

/** Gentle looping twinkle while processing continues; a constant 1f under reduced motion. */
@Composable
private fun rememberTwinkleAlpha(reduceMotion: Boolean): State<Float> {
    if (reduceMotion) return remember { mutableStateOf(TWINKLE_MAX) }
    val transition = rememberInfiniteTransition(label = "constellation_twinkle")
    return transition.animateFloat(
        initialValue = TWINKLE_MIN, targetValue = TWINKLE_MAX,
        animationSpec = infiniteRepeatable(tween(TWINKLE_MS, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "twinkle",
    )
}

@Composable
private fun ConstellationCanvas(progress: State<Float>, twinkle: State<Float>, modifier: Modifier = Modifier) {
    val desc = stringResource(R.string.sc_reveal_canvas_desc)
    val pathCache = remember { PalmPathCache() }
    Canvas(modifier = modifier.semantics { contentDescription = desc }) {
        val t = progress.value
        val tw = twinkle.value
        drawPalmLines(t, pathCache)
        drawConstellationEdges(t)
        drawStarNodes(t, tw)
        drawAmbientStars(t, tw)
    }
}

/**
 * Per-canvas-size cache: the four full palm paths and their measures are built once per
 * size, and one scratch path is reused for the draw-in segments — the twinkle loop keeps
 * this canvas redrawing forever, so per-frame Path/PathMeasure allocation is not allowed.
 */
private class PalmPathCache {
    private var builtFor: Size = Size.Unspecified
    private val fullPaths = mutableListOf<Path>()
    private val measures = mutableListOf<PathMeasure>()
    private val scratch = Path()

    fun ensureBuilt(scope: DrawScope) {
        if (builtFor == scope.size) return
        fullPaths.clear()
        measures.clear()
        PalmLines.forEach { points ->
            val path = with(scope) { palmPath(points) }
            fullPaths.add(path)
            measures.add(PathMeasure().apply { setPath(path, false) })
        }
        builtFor = scope.size
    }

    fun fullPath(index: Int): Path = fullPaths[index]

    /** First [fraction] of line [index]'s arc length, written into the reusable scratch. */
    fun partialPath(index: Int, fraction: Float): Path {
        val measure = measures[index]
        scratch.reset()
        measure.getSegment(0f, measure.length * fraction, scratch, true)
        return scratch
    }
}

// ── Draw phases ──────────────────────────────────────────────────────────────

private fun DrawScope.drawPalmLines(t: Float, cache: PalmPathCache) {
    cache.ensureBuilt(this)
    PalmLines.indices.forEach { index ->
        val start = index * LINE_STAGGER
        val fraction = window(t, start, start + LINE_SPAN)
        if (fraction <= 0f) return@forEach
        // Fully drawn lines reuse the cached full path; only mid-reveal lines segment.
        val path = if (fraction >= 1f) cache.fullPath(index) else cache.partialPath(index, fraction)
        // Soft glow: the same path stroked wide-and-faint down to a thin bright core (PRD 37).
        drawPath(path, LineTeal.copy(alpha = GLOW_OUTER_ALPHA), style = lineStroke(GLOW_OUTER_DP))
        drawPath(path, LineTeal.copy(alpha = GLOW_MID_ALPHA), style = lineStroke(GLOW_MID_DP))
        drawPath(path, LineTeal.copy(alpha = CORE_ALPHA), style = lineStroke(CORE_DP))
    }
}

private fun DrawScope.drawConstellationEdges(t: Float) {
    ConstellationEdges.forEachIndexed { index, edge ->
        val start = EDGE_START + index * EDGE_STAGGER
        val fraction = window(t, start, start + EDGE_SPAN)
        if (fraction <= 0f) return@forEachIndexed
        val from = toCanvas(edge.first)
        val to = toCanvas(edge.second)
        val tip = Offset(from.x + (to.x - from.x) * fraction, from.y + (to.y - from.y) * fraction)
        drawLine(
            color = EdgeLavender.copy(alpha = EDGE_ALPHA * fraction),
            start = from, end = tip,
            strokeWidth = EDGE_WIDTH_DP.dp.toPx(), cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawStarNodes(t: Float, twinkle: Float) {
    StarNodes.forEachIndexed { index, node ->
        val start = NODE_START + index * NODE_STAGGER
        val ignite = window(t, start, start + NODE_SPAN)
        if (ignite <= 0f) return@forEachIndexed
        val center = toCanvas(node)
        val boost = if (index >= PalmNodeCount) SKY_RADIUS_BOOST else 1f
        val haloScale = HALO_MIN_SCALE + (1f - HALO_MIN_SCALE) * ignite
        drawCircle(
            color = StarLight.copy(alpha = HALO_ALPHA * ignite * twinkle),
            radius = HALO_RADIUS_DP.dp.toPx() * boost * haloScale,
            center = center,
        )
        drawCircle(
            color = StarLight.copy(alpha = ignite * (CORE_TWINKLE_FLOOR + (1f - CORE_TWINKLE_FLOOR) * twinkle)),
            radius = NODE_RADIUS_DP.dp.toPx() * boost * ignite,
            center = center,
        )
    }
}

private fun DrawScope.drawAmbientStars(t: Float, twinkle: Float) {
    val appear = window(t, AMBIENT_START, AMBIENT_END)
    if (appear <= 0f) return
    AmbientStars.forEachIndexed { index, star ->
        // Alternate the twinkle phase so the sky breathes instead of blinking in unison.
        val phase = if (index % 2 == 0) twinkle else TWINKLE_MIN + TWINKLE_MAX - twinkle
        drawCircle(
            color = StarLight.copy(alpha = AMBIENT_ALPHA * appear * phase),
            radius = AMBIENT_RADIUS_DP.dp.toPx(),
            center = toCanvas(star),
        )
    }
}

// ── Geometry helpers ─────────────────────────────────────────────────────────

private fun DrawScope.toCanvas(normalized: Offset): Offset =
    Offset(normalized.x * size.width, normalized.y * size.height)

private fun DrawScope.lineStroke(widthDp: Float): Stroke =
    Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Round)

/** Smooth path through the points: quadratics through segment midpoints. */
private fun DrawScope.palmPath(normalized: List<Offset>): Path {
    val pts = normalized.map { toCanvas(it) }
    val path = Path()
    path.moveTo(pts.first().x, pts.first().y)
    for (i in 1 until pts.size - 1) {
        val mid = Offset((pts[i].x + pts[i + 1].x) / 2f, (pts[i].y + pts[i + 1].y) / 2f)
        path.quadraticBezierTo(pts[i].x, pts[i].y, mid.x, mid.y)
    }
    path.lineTo(pts.last().x, pts.last().y)
    return path
}
