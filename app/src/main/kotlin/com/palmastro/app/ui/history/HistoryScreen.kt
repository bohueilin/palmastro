package com.palmastro.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.palmastro.app.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.viewmodel.HistoryViewModel
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.palmastro.app.viewmodel.MonthSummary

private val gradeColors = mapOf("Growing" to Color(0xFF388E3C), "Stable" to Color(0xFF1976D2), "Building" to Color(0xFFE65100), "Watchout" to Color(0xFFD32F2F))
private val gradeNames = mapOf("Growing" to "Growing", "Stable" to "Stable", "Building" to "Building", "Watchout" to "Watch Out")
private val domainNames = mapOf("career" to "Career", "wealth" to "Wealth", "family" to "Family", "health" to "Health")
private val domainEmojis = mapOf("career" to "💼", "wealth" to "💰", "family" to "🏠", "health" to "❤️")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onMonthClick: (String) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("History") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.months.size <= 1 -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Only one record — scan more months to see trends", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.months) { month -> MonthCard(month = month, onClick = { onMonthClick(month.monthKey) }) }
            }
        }
    }
}

@Composable
private fun MonthCard(month: MonthSummary, onClick: () -> Unit) {
    val gradeText = gradeNames[month.grade] ?: month.grade
    val orderedDomains = listOf("career", "wealth", "family", "health")
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).semantics(mergeDescendants = true) { contentDescription = "${month.monthKey} $gradeText" }, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(month.monthKey, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Surface(color = (gradeColors[month.grade] ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
                    Text(gradeText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = gradeColors[month.grade] ?: MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            orderedDomains.forEach { domain ->
                val score = month.domainScores[domain] ?: 0
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(domainEmojis[domain] ?: "", fontSize = 14.sp, modifier = Modifier.width(24.dp))
                    Text(domainNames[domain] ?: domain, fontSize = 13.sp, modifier = Modifier.width(56.dp))
                    LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.weight(1f).height(6.dp).clearAndSetSemantics {}, color = gradeColors[month.grade] ?: MaterialTheme.colorScheme.primary)
                    Text("$score", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(36.dp).padding(start = 8.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Confidence: ${when (month.confidence) { "high" -> "High"; "med" -> "Medium"; "low" -> "Low"; else -> month.confidence }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
