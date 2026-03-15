package com.palmastro.app.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.palmastro.app.viewmodel.DomainDetailViewModel
import com.palmastro.contracts.Observation

private val gradeColors = mapOf(
    "Growing" to Color(0xFF4CAF50),
    "Stable" to Color(0xFF2196F3),
    "Building" to Color(0xFFFF9800),
    "Watchout" to Color(0xFFF44336),
)

private val gradeNamesZh = mapOf(
    "Growing" to "成長期", "Stable" to "穩定期", "Building" to "累積期", "Watchout" to "注意期",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainDetailScreen(
    onBack: () -> Unit,
    viewModel: DomainDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            state.payload != null -> {
                val payload = state.payload!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Score header
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${payload.scoreCard.totalScore}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = gradeColors[payload.scoreCard.grade] ?: MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            gradeNamesZh[payload.scoreCard.grade] ?: payload.scoreCard.grade,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = gradeColors[payload.scoreCard.grade] ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    SectionHeader("分析")
                    Text(payload.interpretationZh, fontSize = 16.sp, lineHeight = 24.sp)

                    Spacer(Modifier.height(20.dp))

                    SectionHeader("盲點")
                    Text(payload.blindspotZh, fontSize = 16.sp, lineHeight = 24.sp)

                    Spacer(Modifier.height(20.dp))

                    SectionHeader("行動建議")
                    ActionItem("今天", payload.actionTodayZh)
                    Spacer(Modifier.height(8.dp))
                    ActionItem("本週", payload.actionWeekZh)

                    Spacer(Modifier.height(20.dp))

                    SectionHeader("反思")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Text(
                            payload.promptZh,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    if (payload.observations.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        SectionHeader("觀察依據")
                        payload.observations.forEach { obs ->
                            ObservationItem(obs)
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (payload.safetyNotesZh.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        payload.safetyNotesZh.forEach { note ->
                            Text(note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ActionItem(label: String, text: String) {
    Row {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(label, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 15.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ObservationItem(obs: Observation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(obs.displayNameZh, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(obs.evidenceSummaryZh, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
