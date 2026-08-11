package com.palmastro.app.ui.guidance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.haptics.rememberHapticPlayer
import com.palmastro.app.ui.components.entranceReveal
import com.palmastro.app.ui.components.rememberReduceMotion
import com.palmastro.app.ui.results.domainDisplayName
import com.palmastro.app.ui.results.gradeColor
import com.palmastro.app.viewmodel.GuidanceViewModel
import com.palmastro.content.Guidance
import com.palmastro.content.GuidanceItem
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Navigation contract for the "Understand your reading" guidance screen (PRD §§11–13).
 * Same pattern as ExplainabilityScreen: a route constant plus a builder helper.
 */
const val GUIDANCE_ROUTE = "guidance/{monthKey}"
fun guidanceRoute(monthKey: String) = "guidance/$monthKey"

/**
 * Guidance layer: what to lean into and what to be mindful of, plus a gentle week plan.
 * Positivity-first and calm by default (PRD 12.3): mindful cards use tertiary theme
 * colors, never error red; copy is action-oriented, never fear-based. The one-shot
 * entrance reveal is skipped under reduced motion and on revisits/restores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidanceScreen(
    onBack: () -> Unit,
    viewModel: GuidanceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val haptics = rememberHapticPlayer()
    // Entrance plays only on the same one-shot reveal event that fires the haptic:
    // sound, touch, and motion are one moment. Restores/revisits render settled.
    val reduceMotion = rememberReduceMotion()
    // One gentle shimmer when the guidance content first reveals (award-polish haptic vocabulary).
    // Gated via rememberSaveable so rotation / config change never re-fires the reveal.
    var revealed by rememberSaveable { mutableStateOf(false) }
    val playEntrance = remember { !revealed } && !reduceMotion
    LaunchedEffect(state.guidance != null) {
        if (state.guidance != null && !revealed) {
            revealed = true
            haptics.shimmerReveal()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guidance_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        }
    ) { padding ->
        val guidance = state.guidance
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            guidance == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                // Calm empty state — informative, never alarming (PRD 12.3).
                Text(
                    stringResource(R.string.guidance_not_found),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            else -> GuidanceContent(
                monthKey = state.monthKey,
                grade = state.grade,
                guidance = guidance,
                padding = padding,
                playEntrance = playEntrance,
            )
        }
    }
}

@Composable
private fun GuidanceContent(
    monthKey: String,
    grade: String,
    guidance: Guidance,
    padding: PaddingValues,
    playEntrance: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Box(Modifier.entranceReveal(playEntrance, index = 0, staggerMs = 50L)) {
            MonthThemeHero(monthKey = monthKey, grade = grade, monthTheme = guidance.monthTheme)
        }

        GuidanceItemsSection(
            modifier = Modifier.entranceReveal(playEntrance, index = 1, staggerMs = 50L),
            items = guidance.strengths,
            icon = Icons.Outlined.TrendingUp,
            title = stringResource(R.string.guidance_lean_into_title),
            description = stringResource(R.string.guidance_lean_into_desc),
            colors = GuidanceSectionColors(
                accent = MaterialTheme.colorScheme.primary,
                label = MaterialTheme.colorScheme.onPrimaryContainer,
                container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            ),
            topSpacing = 28.dp,
        )

        // Gentle by design: tertiary palette, NOT error red (PRD 12.3).
        GuidanceItemsSection(
            modifier = Modifier.entranceReveal(playEntrance, index = 2, staggerMs = 50L),
            items = guidance.mindful,
            icon = Icons.Outlined.Visibility,
            title = stringResource(R.string.guidance_mindful_title),
            description = stringResource(R.string.guidance_mindful_desc),
            colors = GuidanceSectionColors(
                accent = MaterialTheme.colorScheme.tertiary,
                label = MaterialTheme.colorScheme.onTertiaryContainer,
                container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            ),
            topSpacing = 18.dp,
        )

        if (guidance.weekPlan.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            GuidanceSectionHeader(
                icon = Icons.Outlined.Checklist,
                title = stringResource(R.string.guidance_week_title),
                description = stringResource(R.string.guidance_week_desc),
            )
            Spacer(Modifier.height(8.dp))
            guidance.weekPlan.forEachIndexed { index, step ->
                WeekPlanRow(index = index + 1, text = step)
            }
        }

        Spacer(Modifier.height(28.dp))
        GuidanceFooter()
        Spacer(Modifier.height(40.dp))
    }
}

/**
 * Section palette: [accent] tints the chip surfaces, [label] is the matching
 * on-container tone for the 11sp chip text (the raw accent measured ~2.6–3.6:1 on
 * the tinted chips, below the 4.5:1 small-text floor — design review F5), and
 * [container] is the card wash.
 */
@Immutable
private data class GuidanceSectionColors(
    val accent: Color,
    val label: Color,
    val container: Color,
)

@Composable
private fun GuidanceItemsSection(
    items: List<GuidanceItem>,
    icon: ImageVector,
    title: String,
    description: String,
    colors: GuidanceSectionColors,
    topSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier) {
        Spacer(Modifier.height(topSpacing))
        GuidanceSectionHeader(icon = icon, title = title, description = description)
        Spacer(Modifier.height(12.dp))
        items.forEach { item ->
            GuidanceItemCard(item = item, colors = colors)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MonthThemeHero(monthKey: String, grade: String, monthTheme: String) {
    val gc = gradeColor(grade)
    val datePattern = stringResource(R.string.results_date_pattern)
    val configuration = LocalConfiguration.current
    // Derived from the result's monthKey, never from "now", so historical months
    // render their own date (same rule as the Results hero).
    val monthTitle = remember(monthKey, datePattern, configuration) {
        runCatching {
            val locale = configuration.locales.get(0)
            YearMonth.parse(monthKey).atDay(1).format(DateTimeFormatter.ofPattern(datePattern, locale))
        }.getOrDefault(monthKey)
    }

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(gc.copy(alpha = 0.15f), gc.copy(alpha = 0.05f))))
            .semantics(mergeDescendants = true) { heading() }
            .padding(24.dp),
    ) {
        Column {
            Text(monthTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.guidance_theme_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(monthTheme, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
        }
    }
}

@Composable
private fun GuidanceSectionHeader(icon: ImageVector, title: String, description: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GuidanceItemCard(item: GuidanceItem, colors: GuidanceSectionColors) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.container),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(color = colors.accent.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    domainDisplayName(item.domain),
                    style = MaterialTheme.typography.labelSmall, color = colors.label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.action.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Surface(color = colors.accent.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            stringResource(R.string.guidance_action_label),
                            style = MaterialTheme.typography.labelSmall, color = colors.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(item.action, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeekPlanRow(index: Int, text: String) {
    val rowDesc = stringResource(R.string.guidance_week_step_desc, index, text)
    Row(
        modifier = Modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = rowDesc }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = CircleShape) {
            // Min-size (not fixed) so the step number never clips at large font scales.
            Box(
                modifier = Modifier.sizeIn(minWidth = 28.dp, minHeight = 28.dp).padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$index",
                    fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text, fontSize = 15.sp, lineHeight = 22.sp,
            modifier = Modifier.weight(1f).padding(top = 3.dp),
        )
    }
}

/** Footer safety/reflection note; reuses the shared Results disclaimer strings. */
@Composable
private fun GuidanceFooter() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Shield, contentDescription = null,
                modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.results_safety_title), fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.guidance_footer_reflection),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.results_safety_body),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                )
            }
        }
    }
}
