package com.palmastro.app.ui.results

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.share.ShareCardRenderer
import com.palmastro.app.share.ShareHelper
import com.palmastro.app.viewmodel.DomainCard
import com.palmastro.app.viewmodel.ResultsState
import com.palmastro.app.viewmodel.ResultsViewModel

private val gradeColors = mapOf(
    "Growing" to Color(0xFF388E3C),
    "Stable" to Color(0xFF1976D2),
    "Building" to Color(0xFFE65100),
    "Watchout" to Color(0xFFD32F2F),
)

private val gradeNamesZh = mapOf(
    "Growing" to "成長期", "Stable" to "穩定期", "Building" to "累積期", "Watchout" to "注意期",
)

@Composable
fun ResultsScreen(
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDomainClick: (domain: String, monthKey: String) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("掌紋星象", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row {
                if (state.hasResults) {
                    IconButton(onClick = {
                        view.announceForAccessibility("正在分享月度報告")
                        shareSummary(context, state)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "分享月度報告")
                    }
                }
                TextButton(onClick = onSettingsClick) { Text("設定") }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!state.hasResults) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("還沒有掃描結果", fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onScanClick, modifier = Modifier.height(56.dp)) {
                    Text("開始掃描掌紋", fontSize = 18.sp)
                }
            }
        } else {
            Text("${state.monthKey} 月報告", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    gradeNamesZh[state.grade] ?: state.grade,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradeColors[state.grade] ?: MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text("信心度：${state.confidence}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.domainCards) { card ->
                    DomainCardItem(
                        card = card,
                        onClick = { onDomainClick(card.domain, state.monthKey) },
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onScanClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("重新掃描") }
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("歷史記錄") }
                }
            }
        }
    }
}

private fun shareSummary(context: Context, state: ResultsState) {
    val domains = state.domainCards.map {
        ShareCardRenderer.DomainScore(it.displayName, it.score, it.grade)
    }
    val data = ShareCardRenderer.SummaryData(
        monthKey = state.monthKey,
        grade = state.grade,
        confidence = state.confidence,
        domains = domains,
    )
    val bitmap = ShareCardRenderer.renderSummaryCard(data)
    val text = ShareHelper.buildSummaryText(state.monthKey, state.grade, state.confidence, domains)
    ShareHelper.share(context, bitmap, text)
    bitmap.recycle()
}

@Composable
private fun DomainCardItem(card: DomainCard, onClick: () -> Unit) {
    val gradeZh = gradeNamesZh[card.grade] ?: card.grade
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.displayName} ${card.score}分 $gradeZh"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(card.displayName, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${card.score}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = gradeColors[card.grade] ?: MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = card.score / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clearAndSetSemantics {},
                color = gradeColors[card.grade] ?: MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                gradeNamesZh[card.grade] ?: card.grade,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
