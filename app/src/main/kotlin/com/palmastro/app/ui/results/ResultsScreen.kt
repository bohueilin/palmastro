package com.palmastro.app.ui.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.viewmodel.DomainCard
import com.palmastro.app.viewmodel.ResultsViewModel

private val gradeColors = mapOf(
    "Growing" to Color(0xFF4CAF50),
    "Stable" to Color(0xFF2196F3),
    "Building" to Color(0xFFFF9800),
    "Watchout" to Color(0xFFF44336),
)

private val gradeNamesZh = mapOf(
    "Growing" to "成長期", "Stable" to "穩定期", "Building" to "累積期", "Watchout" to "注意期",
)

@Composable
fun ResultsScreen(
    onScanClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("掌紋星象", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onSettingsClick) { Text("設定") }
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
                    DomainCardItem(card)
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onScanClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) { Text("重新掃描") }
                }
            }
        }
    }
}

@Composable
private fun DomainCardItem(card: DomainCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth().height(6.dp),
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
