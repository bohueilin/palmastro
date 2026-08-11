package com.palmastro.app.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.ui.results.confidenceDisplayName
import com.palmastro.app.ui.results.domainDisplayName
import com.palmastro.app.ui.results.gradeColor
import com.palmastro.app.ui.results.gradeDisplayName
import com.palmastro.app.ui.results.onGradeColor
import com.palmastro.app.ui.theme.LocalPalmAstroExtendedColors
import com.palmastro.app.viewmodel.HistoryViewModel
import com.palmastro.app.viewmodel.MonthSummary

private val orderedDomains = listOf("career", "wealth", "family", "health")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onMonthClick: (String) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.months.isEmpty() -> EmptyHistory(padding)
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.months.size == 1) {
                    item {
                        Text(
                            stringResource(R.string.history_single_record),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.months) { month -> MonthCard(month = month, onClick = { onMonthClick(month.monthKey) }) }
            }
        }
    }
}

@Composable
private fun EmptyHistory(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_empty_no_history),
            contentDescription = null,
            modifier = Modifier.size(180.dp).clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.history_empty_title),
            fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.history_empty_desc),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonthCard(month: MonthSummary, onClick: () -> Unit) {
    val gradeText = gradeDisplayName(month.grade)
    val gc = gradeColor(month.grade)
    val confidenceText = confidenceDisplayName(month.confidence)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, role = Role.Button)
            .semantics(mergeDescendants = true) { contentDescription = "${month.monthKey} $gradeText" },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(month.monthKey, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
                // Solid chip with the paired on-color: the raw grade color on a 15% tint
                // measured ~3.6:1 for two grades — below the 4.5:1 small-text floor.
                Surface(color = gc, shape = MaterialTheme.shapes.small) {
                    Text(
                        gradeText,
                        fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium,
                        color = onGradeColor(month.grade),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            orderedDomains.forEach { domain ->
                val score = month.domainScores[domain] ?: 0
                val delta = month.deltas[domain]
                val domainText = domainDisplayName(domain)
                val deltaDesc = delta?.let { stringResource(R.string.history_delta_desc, domainText, it) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = listOfNotNull("$domainText $score", deltaDesc).joinToString(", ")
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(domainText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.widthIn(min = 64.dp))
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.weight(1f).height(6.dp).clearAndSetSemantics {},
                        color = gc,
                    )
                    Text(
                        "$score",
                        fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.widthIn(min = 36.dp).padding(start = 8.dp).clearAndSetSemantics {},
                    )
                    DeltaText(delta)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.results_confidence, confidenceText),
                fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Signed month-over-month change; glyph + number so direction is not color-only. */
@Composable
private fun DeltaText(delta: Int?) {
    val extended = LocalPalmAstroExtendedColors.current
    val (text, color) = when {
        delta == null -> "" to MaterialTheme.colorScheme.onSurfaceVariant
        delta > 0 -> "▲+$delta" to extended.deltaPositive
        delta < 0 -> "▼$delta" to extended.deltaNegative
        else -> "—" to extended.deltaNeutral
    }
    Text(
        text,
        fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = color,
        modifier = Modifier.widthIn(min = 48.dp).padding(start = 6.dp).clearAndSetSemantics {},
    )
}
