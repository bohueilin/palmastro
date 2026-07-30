package com.palmastro.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure arc / score math behind [ScoreGauge], kept free of Compose and Android types so
 * it is unit-testable on the plain JVM (see ScoreGaugeLogicTest).
 */
object ScoreGaugeMath {
    /** The gauge is a 270-degree arc opening downward, starting at the lower-left. */
    const val START_ANGLE_DEGREES = 135f
    const val FULL_SWEEP_DEGREES = 270f
    const val MAX_SCORE = 100

    private const val HALF_TURN_DEGREES = 180.0

    fun clampScore(score: Int): Int = score.coerceIn(0, MAX_SCORE)

    /** Fraction of the arc (0..1) that a score fills; the animation target. */
    fun targetFraction(score: Int): Float = clampScore(score).toFloat() / MAX_SCORE

    /** Degrees of arc swept at [fraction] of the animation. */
    fun sweepAngle(fraction: Float): Float = fraction.coerceIn(0f, 1f) * FULL_SWEEP_DEGREES

    /** Where the sweep starts on first composition: already at the target when motion is reduced. */
    fun initialFraction(score: Int, reduceMotion: Boolean): Float =
        if (reduceMotion) targetFraction(score) else 0f

    /** Diameter growth is capped so a 2.0x font scale doesn't blow the layout apart. */
    const val MAX_DIAMETER_SCALE = 1.6f

    /**
     * Factor applied to the gauge diameter (and stroke) at the given accessibility font
     * scale: the numeral is sp-sized and grows with the font scale, so the fixed-dp ring
     * must grow with it or the text clips. Never shrinks below 1x; capped at 1.6x.
     */
    fun diameterScale(fontScale: Float): Float = fontScale.coerceIn(1f, MAX_DIAMETER_SCALE)

    /** Rounded average of domain scores for the Results overall gauge; empty list -> 0. */
    fun averageScore(scores: List<Int>): Int {
        if (scores.isEmpty()) return 0
        val mean = scores.sumOf { clampScore(it).toDouble() } / scores.size
        return mean.roundToInt()
    }

    /** Absolute angle in degrees of the arc-tip marker at [fraction]. */
    fun tipAngleDegrees(fraction: Float): Float = START_ANGLE_DEGREES + sweepAngle(fraction)

    /** Tip-marker center relative to the gauge center, on the arc circle of [radius]. */
    fun tipOffset(fraction: Float, radius: Float): Pair<Float, Float> {
        val radians = tipAngleDegrees(fraction) * PI / HALF_TURN_DEGREES
        return (radius * cos(radians)).toFloat() to (radius * sin(radians)).toFloat()
    }
}

/** Brand arc colors (PRD 37): calm teal sweeping into royal purple, identical for every grade. */
private val ArcTeal = Color(0xFF4FD1C5)
private val ArcPurple = Color(0xFF6B46C1)
private val ArcPurpleOnDark = Color(0xFF9F7AEA)

/** Fraction of the sweep-gradient circle covered by the 270-degree arc (270 / 360). */
private const val GRADIENT_END_STOP = 0.75f
private const val TIP_HALO_ALPHA = 0.35f
private const val TIP_HALO_FACTOR = 0.9f
private const val TIP_DOT_FACTOR = 0.4f

/** Visual parameters for the two supported gauge sizes. */
@Immutable
data class ScoreGaugeStyle(
    val diameter: Dp,
    val strokeWidth: Dp,
    val numeralFontSize: TextUnit,
    val labelFontSize: TextUnit,
) {
    companion object {
        /** Domain Detail hero. */
        val Large = ScoreGaugeStyle(diameter = 160.dp, strokeWidth = 14.dp, numeralFontSize = 44.sp, labelFontSize = 14.sp)

        /** Results hero / domain cards. */
        val Compact = ScoreGaugeStyle(diameter = 64.dp, strokeWidth = 6.dp, numeralFontSize = 20.sp, labelFontSize = 11.sp)
    }
}

/**
 * Circular 0-100 score gauge (PRD 13.4 "Score gauge"): a subtle outline track, a
 * teal-to-purple brand sweep with rounded caps, a small dot marker riding the arc tip,
 * and the score numeral (tabular figures, bold) centered with an optional grade label
 * beneath. The sweep animates on first composition with a calm spring (~800ms) and
 * lands instantly when the system "remove animations" setting is on (PRD 40/41).
 * Announced to TalkBack as ONE merged element via [contentDescription]; all inner
 * canvas and text semantics are cleared.
 */
@Composable
fun ScoreGauge(
    score: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: ScoreGaugeStyle = ScoreGaugeStyle.Large,
    numeralColor: Color = MaterialTheme.colorScheme.onSurface,
    gradeLabel: String? = null,
) {
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(ScoreGaugeMath.initialFraction(score, reduceMotion)) }
    LaunchedEffect(score, reduceMotion) {
        val target = ScoreGaugeMath.targetFraction(score)
        if (reduceMotion) progress.snapTo(target) else progress.animateTo(target, gaugeSpring())
    }
    // The sp-sized numeral grows with the accessibility font scale while dp sizes do not,
    // so scale the effective diameter (hero AND compact) with it — arc thickness stays
    // proportional. Capped at 1.6x so a 2.0x font scale never blows up the layout.
    val diameterScale = ScoreGaugeMath.diameterScale(LocalDensity.current.fontScale)
    Box(
        modifier = modifier
            .size(style.diameter * diameterScale)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        GaugeArc(
            progress = { progress.value },
            strokeWidth = style.strokeWidth * diameterScale,
            modifier = Modifier.fillMaxSize(),
        )
        GaugeCenterText(score = score, style = style, numeralColor = numeralColor, gradeLabel = gradeLabel)
    }
}

/**
 * Same reduced-motion signal the scan screen uses: the system "remove animations"
 * accessibility setting drives ANIMATOR_DURATION_SCALE to zero.
 */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Calm spring that settles in roughly 800ms with no bounce (PRD 41: motion clarifies, never performs). */
private fun gaugeSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow,
)

@Composable
private fun GaugeCenterText(score: Int, style: ScoreGaugeStyle, numeralColor: Color, gradeLabel: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = ScoreGaugeMath.clampScore(score).toString(),
            fontSize = style.numeralFontSize,
            fontWeight = FontWeight.Bold,
            color = numeralColor,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
        if (gradeLabel != null) {
            Text(
                text = gradeLabel,
                fontSize = style.labelFontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GaugeArc(progress: () -> Float, strokeWidth: Dp, modifier: Modifier = Modifier) {
    // Track at FULL alpha in both themes: outlineVariant already sits at the subtle end of
    // the scheme, and any alpha fade made the remaining-range ring nearly invisible on dark
    // surfaces. Intent: keep the track >= 3:1 against the surface (WCAG 1.4.11 non-text
    // contrast) so the "how much is left" affordance survives dark mode.
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val arcEnd = if (isSystemInDarkTheme()) ArcPurpleOnDark else ArcPurple
    Canvas(modifier = modifier) {
        // The progress lambda is read only here, in the draw phase, so the spring
        // animation invalidates drawing without recomposing the gauge.
        drawGauge(fraction = progress().coerceIn(0f, 1f), strokePx = strokeWidth.toPx(), trackColor = trackColor, arcEnd = arcEnd)
    }
}

private fun DrawScope.drawGauge(fraction: Float, strokePx: Float, trackColor: Color, arcEnd: Color) {
    // Inset so both the rounded stroke and the slightly larger tip halo stay inside bounds.
    val inset = strokePx * TIP_HALO_FACTOR
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    val topLeft = Offset(inset, inset)
    val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
    // rotate() also rotates the sweep-gradient shader, so gradient stop 0 lands
    // exactly on the arc start; the wrap-around stop keeps the start cap teal.
    rotate(degrees = ScoreGaugeMath.START_ANGLE_DEGREES) {
        drawArc(
            color = trackColor, startAngle = 0f, sweepAngle = ScoreGaugeMath.FULL_SWEEP_DEGREES,
            useCenter = false, topLeft = topLeft, size = arcSize, style = stroke,
        )
        if (fraction > 0f) {
            val brush = Brush.sweepGradient(0f to ArcTeal, GRADIENT_END_STOP to arcEnd, 1f to ArcTeal, center = center)
            drawArc(
                brush = brush, startAngle = 0f, sweepAngle = ScoreGaugeMath.sweepAngle(fraction),
                useCenter = false, topLeft = topLeft, size = arcSize, style = stroke,
            )
        }
    }
    drawTipMarker(fraction = fraction, radius = arcSize.minDimension / 2f, strokePx = strokePx, color = arcEnd)
}

/** A tiny star-like dot riding the arc tip: soft brand halo around a bright core. */
private fun DrawScope.drawTipMarker(fraction: Float, radius: Float, strokePx: Float, color: Color) {
    val (dx, dy) = ScoreGaugeMath.tipOffset(fraction, radius)
    val tipCenter = Offset(center.x + dx, center.y + dy)
    drawCircle(color = color.copy(alpha = TIP_HALO_ALPHA), radius = strokePx * TIP_HALO_FACTOR, center = tipCenter)
    drawCircle(color = Color.White, radius = strokePx * TIP_DOT_FACTOR, center = tipCenter)
}
