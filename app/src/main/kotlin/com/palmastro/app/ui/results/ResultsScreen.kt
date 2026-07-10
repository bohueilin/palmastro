package com.palmastro.app.ui.results

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.share.ShareCardRenderer
import com.palmastro.app.share.ShareHelper
import com.palmastro.app.viewmodel.DomainCard
import com.palmastro.app.viewmodel.ResultsViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDomainClick: (String, String) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var shareText by remember { mutableStateOf("") }

    // Resolve every string needed inside non-composable click lambdas up front.
    val sharingAnnouncement = stringResource(R.string.results_sharing)
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val summaryHeader = stringResource(R.string.share_summary_header, state.monthKey)
    val gradeDisplay = gradeDisplayName(state.grade)
    val confidenceDisplay = confidenceDisplayName(state.confidence)
    val gradeLine = stringResource(R.string.share_grade_label, gradeDisplay)
    val confidenceLine = stringResource(R.string.share_confidence_label, confidenceDisplay)
    val cardLabels = ShareCardRenderer.CardLabels(
        analysis = stringResource(R.string.share_card_analysis),
        actions = stringResource(R.string.share_card_actions),
        reflection = stringResource(R.string.share_card_reflection),
        watermark = stringResource(R.string.share_watermark),
    )
    val domainScores = state.domainCards.map {
        ShareCardRenderer.DomainScore(domainDisplayName(it.domain), it.score, it.grade)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    if (state.hasResults && state.shareCardsEnabled) {
                        IconButton(onClick = {
                            view.announceForAccessibility(sharingAnnouncement)
                            val data = ShareCardRenderer.SummaryData(
                                headerTitle = summaryHeader,
                                monthKey = state.monthKey,
                                grade = state.grade,
                                gradeDisplay = gradeDisplay,
                                confidenceLine = confidenceLine,
                                domains = domainScores,
                            )
                            shareText = ShareHelper.buildShareText(
                                summaryHeader,
                                gradeLine,
                                domainScores.joinToString("  ") { "${it.displayName} ${it.score}" },
                                confidenceLine,
                            )
                            previewBitmap = ShareCardRenderer.renderSummaryCard(data, cardLabels)
                        }) { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.results_share)) }
                    }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.common_settings)) }
                },
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            !state.hasResults -> EmptyResults(padding = padding, onScanClick = onScanClick)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                item { HeroCard(state.monthKey, state.grade, state.confidence, state.topDomain, state.scanQualityScore) }
                items(state.domainCards) { card ->
                    DomainCardItem(card = card, onClick = { onDomainClick(card.domain, state.monthKey) })
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onScanClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                            Text(stringResource(R.string.results_rescan))
                        }
                        OutlinedButton(onClick = onHistoryClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                            Text(stringResource(R.string.results_history))
                        }
                    }
                }
                item { SafetyCard() }
            }
        }
    }

    previewBitmap?.let { bitmap ->
        SharePreviewDialog(
            bitmap = bitmap,
            onDismiss = { previewBitmap = null },
            onConfirm = {
                ShareHelper.share(context, bitmap, shareText, chooserTitle)
                previewBitmap = null
            },
        )
    }
}

@Composable
private fun EmptyResults(padding: PaddingValues, onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_empty_no_results),
            contentDescription = null,
            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.results_welcome_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.results_welcome_desc),
            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, lineHeight = 24.sp,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth(0.7f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text(stringResource(R.string.results_start_scan), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HeroCard(monthKey: String, grade: String, confidence: String, topDomain: String?, scanQualityScore: Int) {
    val gc = gradeColor(grade)
    val datePattern = stringResource(R.string.results_date_pattern)
    val configuration = LocalConfiguration.current
    // Hero date is derived from the result's monthKey, never from "now", so
    // historical months render their own date.
    val monthTitle = remember(monthKey, datePattern, configuration) {
        runCatching {
            val locale = configuration.locales.get(0)
            YearMonth.parse(monthKey).atDay(1).format(DateTimeFormatter.ofPattern(datePattern, locale))
        }.getOrDefault(monthKey)
    }
    val themeRes = when (grade) {
        "Growing" -> R.string.results_theme_growing
        "Stable" -> R.string.results_theme_stable
        "Building" -> R.string.results_theme_building
        else -> R.string.results_theme_watchout
    }

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(gc.copy(alpha = 0.15f), gc.copy(alpha = 0.05f))))
                .padding(24.dp),
        ) {
            Column {
                Text(monthTitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(gradeDisplayName(grade), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = gc)
                if (topDomain != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(themeRes, domainDisplayName(topDomain)),
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.results_confidence, confidenceDisplayName(confidence)),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera, contentDescription = null,
                                modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.results_scan_quality_chip, scanQualityScore),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val domainImages = mapOf(
    "career" to R.drawable.img_domain_career,
    "wealth" to R.drawable.img_domain_wealth,
    "family" to R.drawable.img_domain_family,
    "health" to R.drawable.img_domain_health,
)

@Composable
private fun DomainCardItem(card: DomainCard, onClick: () -> Unit) {
    val gc = gradeColor(card.grade)
    val displayName = domainDisplayName(card.domain)
    val gradeText = gradeDisplayName(card.grade)
    val confidenceText = stringResource(R.string.results_card_confidence, confidenceDisplayName(card.confidence))
    val deltaDesc = when (card.deltaArrow) {
        "up" -> stringResource(R.string.results_delta_up, card.delta ?: 0)
        "down" -> stringResource(R.string.results_delta_down, -(card.delta ?: 0))
        "flat" -> stringResource(R.string.results_delta_flat)
        else -> null
    }
    val cardDesc = listOfNotNull(
        stringResource(R.string.results_score_desc, displayName, card.score, gradeText),
        confidenceText,
        deltaDesc,
    ).joinToString(", ")

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cardDesc },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(domainImages[card.domain] ?: R.drawable.img_domain_career),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
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
                    DeltaIndicator(arrow = card.deltaArrow, delta = card.delta)
                    Spacer(Modifier.width(8.dp))
                    Text("${card.score}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = gc)
                }
            }
            if (card.insight.isNotBlank()) {
                Text(
                    card.insight,
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
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
private fun DeltaIndicator(arrow: String?, delta: Int?) {
    if (arrow == null || delta == null) return
    val (glyph, color) = when (arrow) {
        "up" -> "▲" to gradeColor("Growing")
        "down" -> "▼" to gradeColor("Watchout")
        else -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val text = if (arrow == "flat") glyph else "$glyph${if (delta > 0) "+" else ""}$delta"
    Text(
        text,
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color,
        modifier = Modifier.padding(bottom = 6.dp).clearAndSetSemantics {},
    )
}

@Composable
private fun SafetyCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text("🛡️", fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.results_safety_title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.results_safety_body),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp,
                )
            }
        }
    }
}
