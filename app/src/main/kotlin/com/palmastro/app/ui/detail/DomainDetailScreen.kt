package com.palmastro.app.ui.detail

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.share.ShareCardRenderer
import com.palmastro.app.share.ShareHelper
import com.palmastro.app.ui.components.ScoreGauge
import com.palmastro.app.ui.components.ScoreGaugeMath
import com.palmastro.app.ui.results.SharePreviewDialog
import com.palmastro.app.ui.results.confidenceDisplayName
import com.palmastro.app.ui.results.domainDisplayName
import com.palmastro.app.ui.results.gradeColor
import com.palmastro.app.ui.results.gradeDisplayName
import com.palmastro.app.viewmodel.DomainDetailViewModel
import com.palmastro.contracts.Observation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainDetailScreen(
    onBack: () -> Unit,
    onJournalClick: () -> Unit = {},
    onExplainabilityClick: () -> Unit = {},
    viewModel: DomainDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val displayName = domainDisplayName(state.domain)
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var shareText by remember { mutableStateOf("") }

    // Strings needed inside click lambdas, resolved during composition.
    val sharingAnnouncement = stringResource(R.string.detail_sharing)
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val domainHeader = stringResource(R.string.share_domain_header, displayName)
    val cardLabels = ShareCardRenderer.CardLabels(
        analysis = stringResource(R.string.share_card_analysis),
        actions = stringResource(R.string.share_card_actions),
        reflection = stringResource(R.string.share_card_reflection),
        watermark = stringResource(R.string.share_watermark),
    )
    val payloadForShare = state.payload
    val gradeDisplayForShare = gradeDisplayName(payloadForShare?.scoreCard?.grade ?: "")
    val scoreLine = stringResource(R.string.share_score_format, payloadForShare?.scoreCard?.totalScore ?: 0, gradeDisplayForShare)
    val analysisLine = stringResource(R.string.share_analysis_label, ShareHelper.truncate(payloadForShare?.interpretation?.pattern.orEmpty(), 100))
    val actionLine = stringResource(R.string.share_action_label, ShareHelper.truncate(payloadForShare?.actionToday.orEmpty(), 80))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (state.payload != null && state.shareCardsEnabled) {
                        IconButton(onClick = {
                            val payload = state.payload ?: return@IconButton
                            view.announceForAccessibility(sharingAnnouncement)
                            val data = ShareCardRenderer.DomainDetailData(
                                headerTitle = domainHeader,
                                score = payload.scoreCard.totalScore,
                                grade = payload.scoreCard.grade,
                                gradeDisplay = gradeDisplayForShare,
                                interpretation = payload.interpretation.pattern,
                                actionToday = payload.actionToday,
                                prompt = payload.prompt,
                            )
                            shareText = ShareHelper.buildShareText(domainHeader, scoreLine, analysisLine, actionLine)
                            previewBitmap = ShareCardRenderer.renderDomainDetailCard(data, cardLabels)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.detail_share))
                        }
                    }
                },
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.detail_not_found),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            state.payload != null -> {
                val payload = state.payload!!
                val gc = gradeColor(payload.scoreCard.grade)
                Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                    // 1. Score gauge hero (PRD 13.4 item 1)
                    DetailHero(
                        gradeTint = gc,
                        score = payload.scoreCard.totalScore,
                        gradeDisplay = gradeDisplayName(payload.scoreCard.grade),
                        domainDisplay = displayName,
                    )

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(24.dp))

                        // 2. Interpretation: pattern / trigger / cost as labeled paragraphs.
                        SectionWithIcon(Icons.Outlined.Analytics, stringResource(R.string.detail_analysis))
                        Spacer(Modifier.height(8.dp))
                        LabeledParagraph(stringResource(R.string.detail_pattern_label), payload.interpretation.pattern)
                        if (payload.interpretation.trigger.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            LabeledParagraph(stringResource(R.string.detail_trigger_label), payload.interpretation.trigger)
                        }
                        if (payload.interpretation.cost.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            LabeledParagraph(stringResource(R.string.detail_cost_label), payload.interpretation.cost)
                        }

                        Spacer(Modifier.height(20.dp))
                        ScoreEducationCard(score = payload.scoreCard.totalScore, confidence = payload.confidence)

                        // 3. "How was this calculated?" -> Explainability
                        Spacer(Modifier.height(12.dp))
                        HowCalculatedRow(confidence = payload.confidence, onClick = onExplainabilityClick)

                        // 4. Observed signals
                        if (payload.observations.isNotEmpty()) {
                            Spacer(Modifier.height(28.dp))
                            SectionWithIcon(Icons.Outlined.TrendingUp, stringResource(R.string.detail_observations))
                            Spacer(Modifier.height(8.dp))
                            payload.observations.forEach { obs ->
                                ObservationItem(obs)
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        // 5. Blind spot
                        Spacer(Modifier.height(28.dp))
                        SectionWithIcon(Icons.Outlined.Visibility, stringResource(R.string.detail_blindspot))
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                payload.blindspot, fontSize = 15.sp, lineHeight = 24.sp,
                                modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }

                        // 6 + 7. Actions
                        Spacer(Modifier.height(28.dp))
                        SectionWithIcon(Icons.Outlined.Checklist, stringResource(R.string.detail_actions))
                        Spacer(Modifier.height(8.dp))
                        ActionChip(label = stringResource(R.string.detail_today), text = payload.actionToday, containerColor = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.height(8.dp))
                        ActionChip(label = stringResource(R.string.detail_week), text = payload.actionWeek, containerColor = MaterialTheme.colorScheme.secondaryContainer)

                        // 8. Reflection prompt
                        Spacer(Modifier.height(28.dp))
                        SectionWithIcon(Icons.Outlined.Psychology, stringResource(R.string.detail_reflection))
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("💭", fontSize = 24.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(payload.prompt, fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // 9. Journal entry
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onJournalClick,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_journal_button))
                        }

                        // Safety notes
                        if (payload.safetyNotes.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            payload.safetyNotes.forEach { note ->
                                Text("ℹ️ $note", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
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

/**
 * Domain-detail hero: the large brand score gauge (PRD 13.4 "Score gauge") on the
 * grade-tinted wash. The numeral takes the grade color while the arc keeps the brand
 * teal-to-purple sweep for every grade, so all four domains read as one family.
 */
@Composable
private fun DetailHero(gradeTint: Color, score: Int, gradeDisplay: String, domainDisplay: String) {
    val gaugeDesc = stringResource(
        R.string.gauge_content_desc, domainDisplay, ScoreGaugeMath.clampScore(score), gradeDisplay,
    )
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(gradeTint.copy(alpha = 0.15f), Color.Transparent)))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        ScoreGauge(
            score = score,
            contentDescription = gaugeDesc,
            numeralColor = gradeTint,
            gradeLabel = gradeDisplay,
        )
    }
}

@Composable
private fun HowCalculatedRow(confidence: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.detail_how_calculated), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.detail_how_calculated_desc) + " · " +
                        stringResource(R.string.results_card_confidence, confidenceDisplayName(confidence)),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LabeledParagraph(label: String, text: String) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 16.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionWithIcon(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ActionChip(label: String, text: String, containerColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = containerColor), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ObservationItem(obs: Observation) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(obs.displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(obs.evidenceSummary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ScoreEducationCard(score: Int, confidence: String) {
    val (tierRes, tierDescRes, tierGrade) = when {
        score >= 80 -> Triple(R.string.detail_tier_excellent, R.string.detail_tier_excellent_desc, "Growing")
        score >= 65 -> Triple(R.string.detail_tier_good, R.string.detail_tier_good_desc, "Stable")
        score >= 50 -> Triple(R.string.detail_tier_moderate, R.string.detail_tier_moderate_desc, "Building")
        score >= 35 -> Triple(R.string.detail_tier_building, R.string.detail_tier_building_desc, "Building")
        else -> Triple(R.string.detail_tier_attention, R.string.detail_tier_attention_desc, "Watchout")
    }
    val tierColor = gradeColor(tierGrade)

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_understanding_score), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tierColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(stringResource(tierRes), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tierColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_score_points, score), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(tierDescRes), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.detail_how_improve), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            val tipsRes = when {
                confidence.equals("low", ignoreCase = true) -> listOf(
                    R.string.detail_tip_rescan, R.string.detail_tip_steady, R.string.detail_tip_glare,
                )
                score < 50 -> listOf(
                    R.string.detail_tip_monthly, R.string.detail_tip_journal,
                    R.string.detail_tip_actions, R.string.detail_tip_small_changes,
                )
                else -> listOf(
                    R.string.detail_tip_compare, R.string.detail_tip_reflect, R.string.detail_tip_next_month,
                )
            }
            tipsRes.forEach { tip ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                    Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(tip), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                }
            }
        }
    }
}
