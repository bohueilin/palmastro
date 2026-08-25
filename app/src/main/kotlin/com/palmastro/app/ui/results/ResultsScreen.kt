package com.palmastro.app.ui.results

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.itemsIndexed
import com.palmastro.app.ui.components.entranceReveal
import com.palmastro.app.ui.components.rememberReduceMotion
import com.palmastro.app.ui.theme.LocalPalmAstroExtendedColors
import com.palmastro.app.R
import com.palmastro.app.share.ShareCardRenderer
import com.palmastro.app.share.ShareHelper
import com.palmastro.app.ui.components.BrandIllustration
import com.palmastro.app.ui.components.BrandScene
import com.palmastro.app.ui.components.DomainGlyph
import com.palmastro.app.ui.components.SafetyNoteCard
import com.palmastro.app.ui.components.ScoreGauge
import com.palmastro.app.ui.components.ScoreGaugeMath
import com.palmastro.app.ui.components.ScoreGaugeStyle
import com.palmastro.app.ui.monthTitleLocalized
import com.palmastro.app.viewmodel.DomainCard
import com.palmastro.app.viewmodel.GuidanceSummary
import com.palmastro.app.viewmodel.ResultsState
import com.palmastro.app.viewmodel.ResultsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    freshArrival: Boolean = false,
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDomainClick: (String, String) -> Unit,
    onHistoryClick: () -> Unit,
    onGuidanceClick: (String) -> Unit = {},
    /** Non-null only when Results was pushed (History, deep link) and can be popped. */
    onBack: (() -> Unit)? = null,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var shareText by remember { mutableStateOf("") }
    var confirmRescan by rememberSaveable { mutableStateOf(false) }
    val chooserTitle = stringResource(R.string.share_chooser_title)

    Scaffold(
        topBar = {
            ResultsTopBar(
                state = state,
                onBack = onBack,
                onSettingsClick = onSettingsClick,
                onCardRendered = { bitmap, text ->
                    shareText = text
                    previewBitmap = bitmap
                },
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !state.hasResults -> EmptyResults(padding = padding, onScanClick = onScanClick)
            else -> ResultsList(
                state = state,
                padding = padding,
                freshArrival = freshArrival,
                // A scan always writes into the CURRENT month, replacing the reading on
                // screen; the stale-month invitation has nothing to replace, so it goes
                // straight on. A past month never reaches here — it offers no rescan.
                onScanClick = { if (state.isStale) onScanClick() else confirmRescan = true },
                onDomainClick = onDomainClick,
                onHistoryClick = onHistoryClick,
                onGuidanceClick = onGuidanceClick,
            )
        }
    }

    if (confirmRescan) {
        RescanConfirmDialog(
            onDismiss = { confirmRescan = false },
            onConfirm = {
                confirmRescan = false
                onScanClick()
            },
        )
    }

    previewBitmap?.let { bitmap ->
        ShareFlowDialog(
            bitmap = bitmap,
            shareText = shareText,
            chooserTitle = chooserTitle,
            onDismiss = { previewBitmap = null },
        )
    }
}

/**
 * Owns the share affordance end to end: it resolves the card's strings during
 * composition (they cannot be read from a click lambda) and hands the finished bitmap
 * and text fallback back to the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsTopBar(
    state: ResultsState,
    onBack: (() -> Unit)?,
    onSettingsClick: () -> Unit,
    onCardRendered: (Bitmap, String) -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val share = rememberShareContent(state)

    TopAppBar(
        title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            }
        },
        actions = {
            if (state.hasResults && state.shareCardsEnabled) {
                IconButton(onClick = {
                    view.announceForAccessibility(share.sharingAnnouncement)
                    // Rasterizing the card allocates ~2.4 MB and draws it in software;
                    // off the main thread so the toolbar tap never drops a frame.
                    scope.launch {
                        val bitmap = withContext(Dispatchers.Default) {
                            ShareCardRenderer.renderSummaryCard(share.summaryData, share.cardLabels)
                        }
                        onCardRendered(bitmap, share.text)
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.results_share))
                }
            }
            IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.common_settings)) }
        },
    )
}

/** Guards the one rescan entry point: a new scan replaces the current month's reading. */
@Composable
private fun RescanConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.results_rescan_confirm_title)) },
        text = { Text(stringResource(R.string.results_rescan_confirm_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.results_rescan)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Pre-resolved share-sheet content usable from non-composable click lambdas. */
private data class ShareContent(
    val sharingAnnouncement: String,
    val text: String,
    val summaryData: ShareCardRenderer.SummaryData,
    val cardLabels: ShareCardRenderer.CardLabels,
)

@Composable
private fun rememberShareContent(state: ResultsState): ShareContent {
    // The card is the one surface that leaves the device, so it carries the month the
    // reader sees on screen — never the raw storage key ("2026-08").
    val monthTitle = monthTitleLocalized(state.monthKey)
    val summaryHeader = stringResource(R.string.share_summary_header, monthTitle)
    val gradeDisplay = gradeDisplayName(state.grade)
    val gradeLine = stringResource(R.string.share_grade_label, gradeDisplay)
    val confidenceLine = stringResource(R.string.share_confidence_label, confidenceDisplayName(state.confidence))
    val domainScores = state.domainCards.map {
        ShareCardRenderer.DomainScore(domainDisplayName(it.domain), it.score, it.grade)
    }
    // The card leaves the app, so it carries the same reflection-not-advice note the
    // screen shows — on the image and on the text fallback beside it (PRD 13.7).
    val disclaimer = stringResource(R.string.results_safety_body)
    return ShareContent(
        sharingAnnouncement = stringResource(R.string.results_sharing),
        text = ShareHelper.buildShareText(
            summaryHeader,
            gradeLine,
            domainScores.joinToString("  ") { "${it.displayName} ${it.score}" },
            confidenceLine,
            disclaimer,
        ),
        summaryData = ShareCardRenderer.SummaryData(
            headerTitle = summaryHeader,
            // Drawn as the card's month caption, so it takes the localized title too.
            monthKey = monthTitle,
            grade = state.grade,
            gradeDisplay = gradeDisplay,
            confidenceLine = confidenceLine,
            domains = domainScores,
        ),
        cardLabels = ShareCardRenderer.CardLabels(
            analysis = stringResource(R.string.share_card_analysis),
            actions = stringResource(R.string.share_card_actions),
            reflection = stringResource(R.string.share_card_reflection),
            watermark = stringResource(R.string.share_watermark),
            disclaimer = disclaimer,
        ),
    )
}

@Composable
private fun ResultsList(
    state: ResultsState,
    padding: PaddingValues,
    freshArrival: Boolean,
    onScanClick: () -> Unit,
    onDomainClick: (String, String) -> Unit,
    onHistoryClick: () -> Unit,
    onGuidanceClick: (String) -> Unit,
) {
    // The "your month is ready" reveal (PRD §41: reinforce scan success): plays exactly
    // once, only when arriving from a completed scan - never on revisits, restores, or
    // under reduced motion. Motion is additive; content is fully readable without it.
    var entrancePlayed by rememberSaveable { mutableStateOf(false) }
    val play = freshArrival && !entrancePlayed && !rememberReduceMotion()
    LaunchedEffect(Unit) { entrancePlayed = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        monthBanners(state, onScanClick)
        item {
            HeroCard(
                monthKey = state.monthKey,
                grade = state.grade,
                confidence = state.confidence,
                topDomain = state.topDomain,
                scanQualityScore = state.scanQualityScore,
                comparabilityScore = state.comparabilityScore,
                averageScore = ScoreGaugeMath.averageScore(state.domainCards.map { it.score }),
                modifier = Modifier.entranceReveal(play, index = 0),
            )
        }
        state.guidance?.let { summary ->
            item {
                GuidanceEntryCard(
                    summary = summary,
                    onClick = { onGuidanceClick(state.monthKey) },
                    modifier = Modifier.entranceReveal(play, index = 1),
                )
            }
        }
        itemsIndexed(state.domainCards) { i, card ->
            DomainCardItem(
                card = card,
                approximate = state.deltaApproximate,
                onClick = { onDomainClick(card.domain, state.monthKey) },
                modifier = Modifier.entranceReveal(play, index = i + 2),
            )
        }
        item {
            ResultsActionRow(
                showRescan = !state.isPastMonth,
                onScanClick = onScanClick,
                onHistoryClick = onHistoryClick,
            )
        }
        item {
            SafetyNoteCard(
                body = stringResource(R.string.results_safety_body),
                modifier = Modifier.padding(top = 8.dp),
                title = stringResource(R.string.results_safety_title),
            )
        }
    }
}

/**
 * Month context above the hero. Both banners are mutually exclusive with a fresh
 * arrival, so neither takes an entrance reveal and the hero's reveal indices are unchanged.
 */
private fun LazyListScope.monthBanners(state: ResultsState, onScanClick: () -> Unit) {
    if (state.isStale) {
        item { NewMonthCard(monthKey = state.monthKey, onScanClick = onScanClick) }
    }
    if (state.isPastMonth) {
        item { PastMonthBadge() }
    }
}

@Composable
private fun ResultsActionRow(showRescan: Boolean, onScanClick: () -> Unit, onHistoryClick: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        // heightIn, not height: at large font scales these labels wrap to two
        // lines, and a pinned height would slice the second one off.
        // A scan can only ever write into the current month, so a month opened from
        // History offers History, not a rescan that would land somewhere else.
        if (showRescan) {
            OutlinedButton(
                onClick = onScanClick,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.results_rescan))
            }
        }
        OutlinedButton(
            onClick = onHistoryClick,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.results_history))
        }
    }
}

/**
 * Rescan invitation shown when the newest reading predates the current month — the
 * monthly loop the delta is built on. Deliberately an invitation: it names the month
 * the reading belongs to and offers the scan, with no urgency or streak pressure.
 */
@Composable
private fun NewMonthCard(monthKey: String, onScanClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                stringResource(R.string.results_new_month_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.results_new_month_desc, monthTitleLocalized(monthKey)),
                fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onScanClick,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.results_rescan))
            }
        }
    }
}

/** Marks a reading opened from History or a deep link, which looks identical otherwise. */
@Composable
private fun PastMonthBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(
                Icons.Outlined.History, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.results_past_month),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyResults(padding: PaddingValues, onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandIllustration(BrandScene.NoResults, height = 180.dp)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.results_welcome_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.results_welcome_desc),
            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 24.sp,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth(0.7f).heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) {
            Text(stringResource(R.string.results_start_scan), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HeroCard(
    monthKey: String,
    grade: String,
    confidence: String,
    topDomain: String?,
    scanQualityScore: Int,
    comparabilityScore: Int?,
    averageScore: Int,
    modifier: Modifier = Modifier,
) {
    val gc = gradeColor(grade)
    val themeRes = when (grade) {
        "Growing" -> R.string.results_theme_growing
        "Stable" -> R.string.results_theme_stable
        "Building" -> R.string.results_theme_building
        else -> R.string.results_theme_watchout
    }

    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(gc.copy(alpha = 0.15f), gc.copy(alpha = 0.05f))))
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // The reading's own month, never "now", so history months keep their date.
                Text(monthTitleLocalized(monthKey), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(gradeDisplayName(grade), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = gc)
                if (topDomain != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(themeRes, domainDisplayName(topDomain)),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(8.dp))
                HeroMetaRow(
                    confidence = confidence,
                    scanQualityScore = scanQualityScore,
                    comparabilityScore = comparabilityScore,
                )
            }
            Spacer(Modifier.width(16.dp))
            HeroOverallGauge(averageScore = averageScore, grade = grade, numeralColor = gc)
        }
    }
}

@Composable
private fun HeroMetaRow(confidence: String, scanQualityScore: Int, comparabilityScore: Int?) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.results_confidence, confidenceDisplayName(confidence)),
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MetaChip(
                icon = Icons.Outlined.PhotoCamera,
                text = stringResource(R.string.results_scan_quality_chip, scanQualityScore),
            )
        }
        // The basis for the arrows on the cards below (PRD §3.3): shown only when a
        // delta is actually rendered, so it never claims a comparison that is hidden.
        // Its own line — the two chips plus the confidence text overflow the hero column.
        if (comparabilityScore != null) {
            Spacer(Modifier.height(4.dp))
            MetaChip(
                icon = Icons.AutoMirrored.Outlined.CompareArrows,
                text = stringResource(R.string.results_comparability_chip, comparabilityScore),
            )
        }
    }
}

/** Tinted meta chip in the hero: icon is decorative, the label carries the meaning. */
@Composable
private fun MetaChip(icon: ImageVector, text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                icon, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Compact overall gauge on the right of the hero: the average of the domain scores,
 * labeled "overall" (PRD 13.3 dashboard score-at-a-glance). One merged TalkBack
 * element — caption and numeral are announced only via the gauge description.
 */
@Composable
private fun HeroOverallGauge(averageScore: Int, grade: String, numeralColor: Color) {
    val overallLabel = stringResource(R.string.gauge_overall_label)
    val desc = stringResource(R.string.gauge_content_desc, overallLabel, averageScore, gradeDisplayName(grade))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clearAndSetSemantics { contentDescription = desc },
    ) {
        ScoreGauge(
            score = averageScore,
            contentDescription = desc,
            style = ScoreGaugeStyle.Compact,
            numeralColor = numeralColor,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            overallLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * "This month" guidance entry (PRD §§11–13): month theme + first strength + first
 * mindful item, navigating to the full "Understand your reading" screen.
 */
@Composable
private fun GuidanceEntryCard(
    summary: GuidanceSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        // clip BEFORE clickable: Card appends its own clip after the caller's modifiers,
        // so an unclipped ripple would paint square corners over the rounded card.
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.guidance_entry_title),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    summary.monthTheme,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (summary.firstStrengthTitle.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.guidance_entry_strength, summary.firstStrengthTitle),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (summary.firstMindfulTitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.guidance_entry_mindful, summary.firstMindfulTitle),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.guidance_entry_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DomainCardHeader(
    card: DomainCard,
    displayName: String,
    gradeText: String,
    confidenceText: String,
    gc: Color,
    approximate: Boolean,
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            DomainGlyph(domain = card.domain, tint = gc)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(gradeText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(confidenceText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            DeltaIndicator(arrow = card.deltaArrow, delta = card.delta, approximate = approximate)
            Spacer(Modifier.width(8.dp))
            Text("${card.score}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = gc)
        }
    }
}

@Composable
private fun domainCardDescription(
    card: DomainCard,
    displayName: String,
    gradeText: String,
    confidenceText: String,
    approximate: Boolean,
): String {
    val deltaDesc = when (card.deltaArrow) {
        "up" -> stringResource(R.string.results_delta_up, card.delta ?: 0)
        "down" -> stringResource(R.string.results_delta_down, -(card.delta ?: 0))
        "flat" -> stringResource(R.string.results_delta_flat)
        else -> null
    }
    // The weakening the glyph shows visually must reach TalkBack too, or the caveat
    // exists only for sighted users.
    val approxDesc = stringResource(R.string.results_delta_approx)
        .takeIf { approximate && deltaDesc != null }
    return listOfNotNull(
        stringResource(R.string.results_score_desc, displayName, card.score, gradeText),
        confidenceText,
        deltaDesc,
        approxDesc,
    ).joinToString(", ")
}

@Composable
private fun DomainCardItem(
    card: DomainCard,
    approximate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gc = gradeColor(card.grade)
    val displayName = domainDisplayName(card.domain)
    val gradeText = gradeDisplayName(card.grade)
    val confidenceText = stringResource(R.string.results_card_confidence, confidenceDisplayName(card.confidence))
    val cardDesc = domainCardDescription(card, displayName, gradeText, confidenceText, approximate)

    Card(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardDesc },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            DomainCardHeader(card, displayName, gradeText, confidenceText, gc, approximate)
            if (card.insight.isNotBlank()) {
                Text(
                    card.insight,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
            LinearProgressIndicator(
                progress = { card.score / 100f },
                modifier = Modifier.fillMaxWidth().height(3.dp).clearAndSetSemantics {},
                color = gc, trackColor = gc.copy(alpha = 0.1f),
            )
        }
    }
}

/** Arrow glyph + signed number; described in text for TalkBack, never color-only. */
@Composable
private fun DeltaIndicator(arrow: String?, delta: Int?, approximate: Boolean) {
    if (arrow == null || delta == null) return
    val extended = LocalPalmAstroExtendedColors.current
    val (glyph, color) = when (arrow) {
        "up" -> "▲" to extended.deltaPositive
        "down" -> "▼" to extended.deltaNegative
        else -> "—" to extended.deltaNeutral
    }
    val signed = if (arrow == "flat") glyph else "$glyph${if (delta > 0) "+" else ""}$delta"
    // MED comparability (PRD delta rules): the two scans differ enough that the change
    // is only indicative, so it is weakened — neutral tint and an "approximately" mark —
    // rather than stated as flatly as a HIGH-comparability move.
    Text(
        if (approximate) "≈$signed" else signed,
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = if (approximate) extended.deltaNeutral else color,
        modifier = Modifier.padding(bottom = 6.dp).clearAndSetSemantics {},
    )
}
