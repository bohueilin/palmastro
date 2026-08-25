package com.palmastro.app.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// The PalmAstro brand illustration system.
//
// One motif — palm lines rising into a constellation — drawn as vectors on a
// night-sky panel, in every empty state and onboarding step. Replacing the
// bitmap illustrations solved three problems at once: they carried baked-in
// white backgrounds that broke dark mode, they cost ~23 MB of APK, and they
// were generic stock art that could belong to any app. Vectors are crisp at
// every density, identical in both themes, and weigh nothing.
//
// The night sky is deliberate and theme-independent: it is the same surface as
// the post-scan constellation reveal (ConstellationReveal.kt), so the moment the
// product is "about" appears consistently from the first screen onward.
// ─────────────────────────────────────────────────────────────────────────────

/** Night-sky panel; mirrors iOS BrandPalette.nightSky / nightSkyHigh. */
private val NightSkyTop = Color(0xFF231A4A)
private val NightSkyBottom = Color(0xFF140F2E)
private val LineTeal = Color(0xFF4FD1C5)
private val EdgeLavender = Color(0xFF9F7AEA)
private val StarLight = Color(0xFFEDEAFB)

private const val EDGE_ALPHA = 0.42f
private const val EDGE_WIDTH_DP = 1.1f
private const val CURVE_CORE_DP = 2f
private const val CURVE_GLOW_DP = 7f
private const val CURVE_CORE_ALPHA = 0.92f
private const val CURVE_GLOW_ALPHA = 0.16f
private const val STAR_HALO_ALPHA = 0.20f
private const val STAR_HALO_FACTOR = 3.2f
private const val TWINKLE_MS = 2600
private const val TWINKLE_MIN = 0.72f
private const val TWINKLE_FULL = 1f
private const val DIM_STAR_ALPHA = 0.34f
private const val PANEL_RADIUS_DP = 20
private const val DEFAULT_STAR_DP = 2f
private const val ASPECT_X_SCALE = 0.5f
private const val FULL_TURN_RADIANS = 2.0 * PI

/** A star: normalized position, radius in dp, and whether it reads as "lit". */
@Immutable
private data class Star(val x: Float, val y: Float, val r: Float = 2.4f, val lit: Boolean = true)

/** A scene is a set of glowing curves (palm lines), edges, and stars. */
@Immutable
private data class Scene(
    val curves: List<List<Offset>> = emptyList(),
    val edges: List<Pair<Int, Int>> = emptyList(),
    val stars: List<Star> = emptyList(),
    val rings: List<Triple<Float, Float, Float>> = emptyList(), // cx, cy, radius
)

/** Every brand illustration slot in the app. */
enum class BrandScene {
    Welcome, Privacy, Identity, Birthday, Hands, BirthDetails, Tone, Ready, NoResults, NoHistory,
}

/**
 * [count] stars evenly spaced on an ellipse. The x radius is halved because the
 * panel is about twice as wide as it is tall, so a circle in normalized space
 * would draw as a wide oval on screen.
 */
private fun ring(cx: Float, cy: Float, r: Float, count: Int, radius: Float = DEFAULT_STAR_DP, lit: Boolean = true) =
    (0 until count).map { i ->
    val angle = (FULL_TURN_RADIANS * i / count) - PI / 2
    Star(cx + (r * cos(angle)).toFloat() * ASPECT_X_SCALE, cy + (r * sin(angle)).toFloat(), radius, lit)
    }

// Scene geometry, in normalized coordinates on a roughly 2.2:1 canvas.
    // A palm's four lines in the lower left, rising into a constellation upper right.

private val SceneWelcome = Scene(
    curves = listOf(
        listOf(Offset(0.08f, 0.62f), Offset(0.20f, 0.55f), Offset(0.32f, 0.56f), Offset(0.42f, 0.62f)),
        listOf(Offset(0.09f, 0.76f), Offset(0.22f, 0.73f), Offset(0.34f, 0.74f), Offset(0.44f, 0.79f)),
        listOf(Offset(0.14f, 0.64f), Offset(0.18f, 0.78f), Offset(0.24f, 0.88f), Offset(0.33f, 0.95f)),
    ),
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 1 to 5),
    stars = listOf(
        Star(0.42f, 0.62f, 2.6f), Star(0.56f, 0.44f), Star(0.68f, 0.26f, 3.0f),
        Star(0.82f, 0.34f), Star(0.90f, 0.18f, 2.2f), Star(0.60f, 0.62f, 1.8f),
        Star(0.30f, 0.22f, 1.6f), Star(0.48f, 0.14f, 1.4f),
    ),
    )
    // A shield drawn as constellation edges, with a bright keystone at its heart.

private val ScenePrivacy = Scene(
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 0),
    stars = listOf(
        Star(0.50f, 0.12f, 2.4f), Star(0.68f, 0.28f), Star(0.66f, 0.62f),
        Star(0.50f, 0.88f, 2.4f), Star(0.34f, 0.62f), Star(0.32f, 0.28f),
        Star(0.50f, 0.48f, 3.4f),
        Star(0.14f, 0.30f, 1.4f), Star(0.86f, 0.72f, 1.4f),
    ),
    )
    // One bright star, claimed: a single identity with a quiet orbit.

private val SceneIdentity = Scene(
    rings = listOf(Triple(0.50f, 0.50f, 0.30f)),
    edges = listOf(0 to 1, 0 to 2, 0 to 3),
    stars = listOf(
        Star(0.50f, 0.50f, 4.0f), Star(0.74f, 0.30f), Star(0.28f, 0.66f), Star(0.66f, 0.80f),
        Star(0.16f, 0.22f, 1.4f), Star(0.88f, 0.58f, 1.4f),
    ),
    )
    // A zodiac wheel: twelve stations, one of them yours.

private val SceneBirthday = Scene(
    rings = listOf(Triple(0.50f, 0.50f, 0.34f), Triple(0.50f, 0.50f, 0.20f)),
    stars = ring(0.50f, 0.50f, 0.68f, 12, 1.9f) + listOf(Star(0.50f, 0.16f, 3.6f)),
    )
    // Two palms, mirrored — the dominant-hand choice.

private val SceneHands = Scene(
    curves = listOf(
        listOf(Offset(0.10f, 0.42f), Offset(0.20f, 0.36f), Offset(0.30f, 0.38f), Offset(0.38f, 0.46f)),
        listOf(Offset(0.11f, 0.60f), Offset(0.21f, 0.56f), Offset(0.31f, 0.58f), Offset(0.39f, 0.66f)),
        listOf(Offset(0.90f, 0.42f), Offset(0.80f, 0.36f), Offset(0.70f, 0.38f), Offset(0.62f, 0.46f)),
        listOf(Offset(0.89f, 0.60f), Offset(0.79f, 0.56f), Offset(0.69f, 0.58f), Offset(0.61f, 0.66f)),
    ),
    edges = listOf(0 to 1, 1 to 2),
    stars = listOf(Star(0.38f, 0.46f, 2.4f), Star(0.50f, 0.24f, 3.2f), Star(0.62f, 0.46f, 2.4f)),
    )
    // A sky clock: the exact hour and place, if you know them.

private val SceneBirthDetails = Scene(
    rings = listOf(Triple(0.50f, 0.50f, 0.32f)),
    curves = listOf(
        listOf(Offset(0.50f, 0.50f), Offset(0.50f, 0.34f), Offset(0.50f, 0.26f), Offset(0.50f, 0.22f)),
        listOf(Offset(0.50f, 0.50f), Offset(0.58f, 0.54f), Offset(0.64f, 0.58f), Offset(0.68f, 0.60f)),
    ),
    stars = ring(0.50f, 0.50f, 0.64f, 4, 2.0f) + listOf(Star(0.50f, 0.50f, 3.0f)),
    )
    // Three voices: sharp, soft, direct — the tone choice, as three rhythms.

private val SceneTone = Scene(
    curves = listOf(
        listOf(Offset(0.16f, 0.26f), Offset(0.36f, 0.20f), Offset(0.56f, 0.30f), Offset(0.76f, 0.24f)),
        listOf(Offset(0.16f, 0.50f), Offset(0.36f, 0.56f), Offset(0.56f, 0.44f), Offset(0.76f, 0.50f)),
        listOf(Offset(0.16f, 0.74f), Offset(0.36f, 0.70f), Offset(0.56f, 0.80f), Offset(0.76f, 0.74f)),
    ),
    stars = listOf(Star(0.84f, 0.24f, 2.6f), Star(0.84f, 0.50f, 2.6f), Star(0.84f, 0.74f, 2.6f)),
    )
    // The full figure, complete and lit: everything the reading needs.

private val SceneReady = Scene(
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 1, 2 to 6),
    stars = listOf(
        Star(0.16f, 0.70f, 2.2f), Star(0.32f, 0.44f, 2.8f), Star(0.50f, 0.24f, 3.6f),
        Star(0.68f, 0.40f, 2.8f), Star(0.84f, 0.66f, 2.2f), Star(0.56f, 0.72f, 2.4f),
        Star(0.42f, 0.86f, 1.8f), Star(0.90f, 0.24f, 1.4f), Star(0.10f, 0.30f, 1.4f),
    ),
    )
    // Waiting: the shape is there, unlit, with one bright invitation.

private val SceneNoResults = Scene(
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4),
    stars = listOf(
        Star(0.18f, 0.66f, 2.0f, lit = false), Star(0.34f, 0.46f, 2.0f, lit = false),
        Star(0.50f, 0.30f, 4.0f), Star(0.68f, 0.46f, 2.0f, lit = false),
        Star(0.84f, 0.66f, 2.0f, lit = false), Star(0.50f, 0.78f, 1.6f, lit = false),
    ),
    )
    // A row of months; only the first has happened.

private val SceneNoHistory = Scene(
    edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4),
    stars = listOf(
        Star(0.16f, 0.50f, 3.4f), Star(0.34f, 0.50f, 2.0f, lit = false),
        Star(0.52f, 0.50f, 2.0f, lit = false), Star(0.70f, 0.50f, 2.0f, lit = false),
        Star(0.86f, 0.50f, 2.0f, lit = false),
        Star(0.28f, 0.24f, 1.4f), Star(0.74f, 0.76f, 1.4f),
    ),
    )

private fun sceneFor(scene: BrandScene): Scene = when (scene) {
    BrandScene.Welcome -> SceneWelcome
    BrandScene.Privacy -> ScenePrivacy
    BrandScene.Identity -> SceneIdentity
    BrandScene.Birthday -> SceneBirthday
    BrandScene.Hands -> SceneHands
    BrandScene.BirthDetails -> SceneBirthDetails
    BrandScene.Tone -> SceneTone
    BrandScene.Ready -> SceneReady
    BrandScene.NoResults -> SceneNoResults
    BrandScene.NoHistory -> SceneNoHistory
}

/**
 * A brand illustration on its night-sky panel.
 *
 * [description] is the TalkBack text; pass null for decorative use, which is the
 * default because these scenes never carry information the surrounding copy lacks.
 * The twinkle is suppressed under the system "remove animations" setting.
 */
@Composable
fun BrandIllustration(
    scene: BrandScene,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    description: String? = null,
) {
    val reduceMotion = rememberReduceMotion()
    val twinkle = rememberTwinkle(reduceMotion)
    val geometry = remember(scene) { sceneFor(scene) }
    Box(
    modifier = modifier
        .fillMaxWidth()
        .height(height)
        .clip(RoundedCornerShape(PANEL_RADIUS_DP.dp))
        .background(Brush.verticalGradient(listOf(NightSkyTop, NightSkyBottom)))
        .then(
            if (description != null) {
                Modifier.semantics { contentDescription = description }
            } else {
                Modifier
            },
        ),
    ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawScene(geometry, twinkle())
    }
    }
}

@Composable
private fun rememberTwinkle(reduceMotion: Boolean): () -> Float {
    if (reduceMotion) return { TWINKLE_FULL }
    val transition = rememberInfiniteTransition(label = "brand_twinkle")
    val value by transition.animateFloat(
    initialValue = TWINKLE_MIN,
    targetValue = TWINKLE_FULL,
    animationSpec = infiniteRepeatable(tween(TWINKLE_MS, easing = EaseInOutSine), RepeatMode.Reverse),
    label = "twinkle",
    )
    return { value }
}

private fun DrawScope.drawScene(scene: Scene, twinkle: Float) {
    scene.rings.forEach { (cx, cy, r) ->
    drawCircle(
        color = EdgeLavender.copy(alpha = 0.22f),
        radius = r * size.height,
        center = Offset(cx * size.width, cy * size.height),
        style = Stroke(width = EDGE_WIDTH_DP.dp.toPx()),
    )
    }
    scene.edges.forEach { (a, b) ->
    val from = scene.stars.getOrNull(a) ?: return@forEach
    val to = scene.stars.getOrNull(b) ?: return@forEach
    drawLine(
        color = EdgeLavender.copy(alpha = EDGE_ALPHA),
        start = Offset(from.x * size.width, from.y * size.height),
        end = Offset(to.x * size.width, to.y * size.height),
        strokeWidth = EDGE_WIDTH_DP.dp.toPx(),
        cap = StrokeCap.Round,
    )
    }
    scene.curves.forEach { points -> drawGlowCurve(points) }
    scene.stars.forEach { star -> drawStar(star, twinkle) }
}

private fun DrawScope.drawGlowCurve(points: List<Offset>) {
    val path = smoothPath(points)
    drawPath(path, LineTeal.copy(alpha = CURVE_GLOW_ALPHA), style = curveStroke(CURVE_GLOW_DP))
    drawPath(path, LineTeal.copy(alpha = CURVE_CORE_ALPHA), style = curveStroke(CURVE_CORE_DP))
}

private fun DrawScope.curveStroke(widthDp: Float) =
    Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Round)

private fun DrawScope.smoothPath(normalized: List<Offset>): Path {
    val pts = normalized.map { Offset(it.x * size.width, it.y * size.height) }
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts.first().x, pts.first().y)
    for (i in 1 until pts.size - 1) {
    val mid = Offset((pts[i].x + pts[i + 1].x) / 2f, (pts[i].y + pts[i + 1].y) / 2f)
    path.quadraticBezierTo(pts[i].x, pts[i].y, mid.x, mid.y)
    }
    path.lineTo(pts.last().x, pts.last().y)
    return path
}

/**
 * The four domains as small constellation glyphs, drawn in the domain's grade
 * color on a matching wash. These replaced 40dp-rendered 1280px photo crops:
 * at thumbnail size a glyph reads as a distinct mark where a photo reads as mud,
 * and it inherits the grade color so the card is legible at a glance.
 */
@Composable
fun DomainGlyph(domain: String, tint: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val points = remember(domain) { domainGlyph(domain) }
    Box(
    modifier = modifier
        .size(size)
        .clip(RoundedCornerShape(GLYPH_RADIUS_DP.dp))
        .background(tint.copy(alpha = GLYPH_WASH_ALPHA)),
    ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = this.size.minDimension * GLYPH_INSET
        val span = this.size.minDimension - inset * 2
        fun at(p: Offset) = Offset(inset + p.x * span, inset + p.y * span)
        for (i in 0 until points.size - 1) {
            drawLine(
                color = tint.copy(alpha = GLYPH_EDGE_ALPHA),
                start = at(points[i]), end = at(points[i + 1]),
                strokeWidth = GLYPH_EDGE_DP.dp.toPx(), cap = StrokeCap.Round,
            )
        }
        points.forEachIndexed { index, p ->
            val r = if (index == 0) GLYPH_LEAD_DOT_DP else GLYPH_DOT_DP
            drawCircle(color = tint, radius = r.dp.toPx(), center = at(p))
        }
    }
    }
}

private const val GLYPH_RADIUS_DP = 10
private const val GLYPH_WASH_ALPHA = 0.14f
private const val GLYPH_EDGE_ALPHA = 0.55f
private const val GLYPH_EDGE_DP = 1.3f
private const val GLYPH_DOT_DP = 1.9f
private const val GLYPH_LEAD_DOT_DP = 2.8f
private const val GLYPH_INSET = 0.20f

// Each domain gets a distinguishable path, as property data rather than
// in-function literals: a climb, a closed cycle, a held-together group, a pulse.
private val GlyphCareer = listOf(Offset(0f, 1f), Offset(0.34f, 0.56f), Offset(0.64f, 0.72f), Offset(1f, 0.10f))
private val GlyphWealth =
    listOf(Offset(0.5f, 0f), Offset(1f, 0.5f), Offset(0.5f, 1f), Offset(0f, 0.5f), Offset(0.5f, 0f))
private val GlyphFamily =
    listOf(Offset(0.5f, 0.05f), Offset(0.05f, 0.75f), Offset(0.95f, 0.75f), Offset(0.5f, 0.05f))
private val GlyphHealth = listOf(
    Offset(0f, 0.55f), Offset(0.26f, 0.55f), Offset(0.40f, 0.16f),
    Offset(0.58f, 0.90f), Offset(0.72f, 0.55f), Offset(1f, 0.55f),
)

private fun domainGlyph(domain: String): List<Offset> = when (domain) {
    "career" -> GlyphCareer
    "wealth" -> GlyphWealth
    "family" -> GlyphFamily
    else -> GlyphHealth
}

private fun DrawScope.drawStar(star: Star, twinkle: Float) {
    val center = Offset(star.x * size.width, star.y * size.height)
    val radius = star.r.dp.toPx()
    val alpha = if (star.lit) twinkle else DIM_STAR_ALPHA
    drawCircle(
    color = StarLight.copy(alpha = STAR_HALO_ALPHA * alpha),
    radius = radius * STAR_HALO_FACTOR,
    center = center,
    )
    drawCircle(color = StarLight.copy(alpha = alpha), radius = radius, center = center)
}
