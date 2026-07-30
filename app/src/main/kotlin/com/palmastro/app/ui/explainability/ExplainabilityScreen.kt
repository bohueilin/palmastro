package com.palmastro.app.ui.explainability

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.ui.results.confidenceDisplayName
import com.palmastro.app.ui.results.domainDisplayName
import com.palmastro.app.ui.results.gradeColor
import com.palmastro.app.viewmodel.DomainDetailViewModel
import com.palmastro.contracts.ExplainEntry
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Navigation contract for the Explainability screen (PRD 13.5). Uses the same
 * "domain"/"monthKey" arguments as domain detail, so it shares [DomainDetailViewModel].
 * NavGraph wiring is added by the ui-core agent (see integration notes).
 */
const val EXPLAINABILITY_ROUTE = "explainability/{domain}/{monthKey}"
fun explainabilityRoute(domain: String, monthKey: String) = "explainability/$domain/$monthKey"

/** Baseline every domain score starts from (ScoringEngineImpl / PRD scoring rules). */
private const val BASELINE_SCORE = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainabilityScreen(
    onBack: () -> Unit,
    viewModel: DomainDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.explain_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        }
    ) { padding ->
        val payload = state.payload
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            payload == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                // Calm empty state, not an error: red is reserved for true errors (PRD 37/42).
                Text(stringResource(R.string.detail_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                val palmEntries = payload.explainability.filter { it.signalId.startsWith("PALM", ignoreCase = true) }
                val astroEntries = payload.explainability.filterNot { it.signalId.startsWith("PALM", ignoreCase = true) }
                val maxAbs = payload.explainability.maxOfOrNull { abs(it.contribution) }?.takeIf { it > 0.0 } ?: 1.0
                val signalNames = payload.observations.associate { it.signalId to it.displayName }

                Column(
                    modifier = Modifier.fillMaxSize().padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        domainDisplayName(state.domain),
                        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.explain_intro),
                        fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Baseline
                    Spacer(Modifier.height(20.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.explain_baseline_label),
                                    fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                                )
                                Text(stringResource(R.string.explain_baseline_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "$BASELINE_SCORE",
                                fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Palm signals
                    Spacer(Modifier.height(24.dp))
                    SignalSection(
                        icon = Icons.Outlined.BackHand,
                        title = stringResource(R.string.explain_palm_signals),
                        entries = palmEntries,
                        maxAbs = maxAbs,
                        signalNames = signalNames,
                    )

                    // Astro signals
                    Spacer(Modifier.height(24.dp))
                    SignalSection(
                        icon = Icons.Outlined.NightsStay,
                        title = stringResource(R.string.explain_astro_signals),
                        entries = astroEntries,
                        maxAbs = maxAbs,
                        signalNames = signalNames,
                    )

                    // Confidence
                    Spacer(Modifier.height(24.dp))
                    SectionHeader(Icons.Outlined.Verified, stringResource(R.string.explain_confidence_label))
                    Spacer(Modifier.height(8.dp))
                    Text(confidenceDisplayName(payload.confidence), fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                    if (payload.confidenceReasons.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.explain_confidence_reasons),
                            fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        payload.confidenceReasons.forEach { reason ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                            }
                        }
                    }

                    // Quality factors
                    Spacer(Modifier.height(24.dp))
                    SectionHeader(Icons.Outlined.PhotoCamera, stringResource(R.string.explain_quality_factors))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.explain_scan_quality, state.scanQualityScore), fontSize = 14.sp, lineHeight = 22.sp)
                    Text(stringResource(R.string.explain_feature_coverage, (state.featureCoverage * 100).roundToInt()), fontSize = 14.sp, lineHeight = 22.sp)
                    Text(stringResource(R.string.explain_calc_level, payload.calcLevel.name), fontSize = 14.sp, lineHeight = 22.sp)

                    // Safety note (PRD 13.5 honesty statement)
                    Spacer(Modifier.height(24.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.explain_safety_note),
                                fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun SignalSection(
    icon: ImageVector,
    title: String,
    entries: List<ExplainEntry>,
    maxAbs: Double,
    signalNames: Map<String, String>,
) {
    SectionHeader(icon, title)
    Spacer(Modifier.height(8.dp))
    if (entries.isEmpty()) {
        Text(
            stringResource(R.string.explain_no_signals),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp,
        )
    } else {
        entries.forEach { entry ->
            ContributionRow(entry = entry, maxAbs = maxAbs, displayName = signalNames[entry.signalId] ?: prettifySignalId(entry.signalId))
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** "PALM_HEADLINE_LONG_CLEAR" -> "Headline long clear" as a readable fallback. */
private fun prettifySignalId(signalId: String): String {
    val words = signalId
        .removePrefix("PALM_").removePrefix("palm_")
        .removePrefix("ASTRO_").removePrefix("astro_")
        .replace('_', ' ')
        .lowercase(Locale.ROOT)
    return words.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}

@Composable
private fun ContributionRow(entry: ExplainEntry, maxAbs: Double, displayName: String) {
    val positive = entry.contribution >= 0
    val barColor = if (positive) gradeColor("Growing") else gradeColor("Watchout")
    val valueText = String.format(Locale.ROOT, "%+.1f", entry.contribution)
    val rowDesc = stringResource(R.string.explain_contribution_desc, displayName, valueText)
    val fraction = (abs(entry.contribution) / maxAbs).toFloat().coerceIn(0.05f, 1f)

    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = rowDesc }) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(displayName, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
            // Signed number is the non-color indicator of direction.
            Text(
                valueText,
                fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                color = barColor, modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clearAndSetSemantics {},
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(fraction).height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
                    .align(if (positive) Alignment.CenterStart else Alignment.CenterEnd),
            )
        }
    }
}
