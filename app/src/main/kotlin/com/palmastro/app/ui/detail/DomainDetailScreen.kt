package com.palmastro.app.ui.detail

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.palmastro.app.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.share.ShareCardRenderer
import com.palmastro.app.share.ShareHelper
import com.palmastro.app.viewmodel.DomainDetailState
import com.palmastro.app.viewmodel.DomainDetailViewModel
import com.palmastro.content.ContentComposerImpl
import com.palmastro.contracts.ContentInput
import com.palmastro.contracts.Tone
import com.palmastro.contracts.ScoringResult
import com.palmastro.contracts.Observation

private val gradeColors = mapOf(
    "Growing" to Color(0xFF388E3C), "Stable" to Color(0xFF1976D2),
    "Building" to Color(0xFFE65100), "Watchout" to Color(0xFFD32F2F),
)
private val gradeNames = mapOf(
    "Growing" to "Growing", "Stable" to "Stable", "Building" to "Building", "Watchout" to "Watch Out",
)
private val domainImages = mapOf("career" to R.drawable.img_domain_career, "wealth" to R.drawable.img_domain_wealth, "family" to R.drawable.img_domain_family, "health" to R.drawable.img_domain_health)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomainDetailScreen(
    onBack: () -> Unit,
    onJournalClick: () -> Unit = {},
    viewModel: DomainDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.displayName, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (state.payload != null) {
                        IconButton(onClick = { view.announceForAccessibility("Sharing analysis"); shareDomainDetail(context, state) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(state.error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            state.payload != null -> {
                val basePayload = state.payload!!
                val domain = state.domain
                var selectedLang by remember { mutableStateOf("en") }
                val availableLangs = listOf("en" to "English", "zh-TW" to "繁體中文", "zh-CN" to "简体中文", "ja" to "日本語", "hi" to "हिन्दी")
                val payload = if (selectedLang == "en") basePayload else {
                    val composer = ContentComposerImpl()
                    val mockScoring = ScoringResult(mapOf(domain to basePayload.scoreCard.totalScore), emptyMap(), basePayload.scoreCard.grade, basePayload.confidence, emptyList(), basePayload.explainability, emptyList(), "1.0.0")
                    val input = ContentInput(mockScoring, null, Tone.SCIENTIFIC, emptySet(), basePayload.calcLevel, basePayload.monthKey)
                    val translated = composer.composeInLanguage(input, selectedLang)
                    translated[domain] ?: basePayload
                }
                val gradeColor = gradeColors[payload.scoreCard.grade] ?: MaterialTheme.colorScheme.primary
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                ) {
                    // Hero score header with gradient
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .background(Brush.verticalGradient(listOf(gradeColor.copy(alpha = 0.15f), Color.Transparent)))
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${payload.scoreCard.totalScore}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = gradeColor)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                                Text("/ 100", fontSize = 16.sp, color = gradeColor.copy(alpha = 0.7f))
                                Text(gradeNames[payload.scoreCard.grade] ?: payload.scoreCard.grade, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = gradeColor)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(24.dp))

                        // Analysis section
                        // Language swap
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableLangs.forEach { (code, label) ->
                                FilterChip(selected = selectedLang == code, onClick = { selectedLang = code }, label = { Text(label, fontSize = 12.sp) })
                            }
                        }
                        Spacer(Modifier.height(20.dp))

                        SectionWithIcon(Icons.Outlined.Analytics, "Analysis")
                        Spacer(Modifier.height(8.dp))
                        Text(payload.interpretationZh, fontSize = 16.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(Modifier.height(28.dp))

                        // Blind spot

                        Spacer(Modifier.height(20.dp))
                        ScoreEducationCard(score = payload.scoreCard.totalScore, confidence = payload.confidence, domain = state.domain)

                        SectionWithIcon(Icons.Outlined.Visibility, "Blind Spot")
                        Spacer(Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                            Text(payload.blindspotZh, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        Spacer(Modifier.height(28.dp))

                        // Action items
                        SectionWithIcon(Icons.Outlined.Checklist, "Action Items")
                        Spacer(Modifier.height(8.dp))
                        ActionChip(label = "Today", text = payload.actionTodayZh, containerColor = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.height(8.dp))
                        ActionChip(label = "This Week", text = payload.actionWeekZh, containerColor = MaterialTheme.colorScheme.secondaryContainer)

                        Spacer(Modifier.height(28.dp))

                        // Reflection prompt
                        SectionWithIcon(Icons.Outlined.Psychology, "Reflection")
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("💭", fontSize = 24.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(payload.promptZh, fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Key signals
                        if (payload.observations.isNotEmpty()) {
                            Spacer(Modifier.height(28.dp))
                            SectionWithIcon(Icons.Outlined.TrendingUp, "Key Signals")
                            Spacer(Modifier.height(8.dp))
                            payload.observations.forEach { obs ->
                                ObservationItem(obs)
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        // Safety notes
                        if (payload.safetyNotesZh.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            payload.safetyNotesZh.forEach { note ->
                                Text("ℹ️ $note", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
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
            Text(obs.displayNameZh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(obs.evidenceSummaryZh, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

private fun shareDomainDetail(context: Context, state: DomainDetailState) {
    val payload = state.payload ?: return
    val data = ShareCardRenderer.DomainDetailData(state.displayName, payload.scoreCard.totalScore, payload.scoreCard.grade, payload.interpretationZh, payload.actionTodayZh, payload.promptZh)
    val bitmap = ShareCardRenderer.renderDomainDetailCard(data)
    val text = ShareHelper.buildDomainText(state.displayName, payload.scoreCard.totalScore, payload.scoreCard.grade, payload.interpretationZh, payload.actionTodayZh)
    ShareHelper.share(context, bitmap, text)
    bitmap.recycle()
}

@Composable
private fun ScoreEducationCard(score: Int, confidence: String, domain: String) {
    val (tier, tierDesc, tierColor) = when {
        score >= 80 -> Triple("Excellent", "This is an exceptionally strong reading. Your palm features and astrological signals are well-aligned.", androidx.compose.ui.graphics.Color(0xFF388E3C))
        score >= 65 -> Triple("Good", "A solid, positive reading. You have a strong foundation with room for further growth.", androidx.compose.ui.graphics.Color(0xFF1976D2))
        score >= 50 -> Triple("Moderate", "A balanced reading. Some strengths are present, with opportunities to develop others.", androidx.compose.ui.graphics.Color(0xFFE65100))
        score >= 35 -> Triple("Building", "Your reading suggests a period of growth. Focus on fundamentals and be patient.", androidx.compose.ui.graphics.Color(0xFFE65100))
        else -> Triple("Attention", "This area needs focus. Small, consistent changes can shift your trajectory.", androidx.compose.ui.graphics.Color(0xFFD32F2F))
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Understanding Your Score", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = tierColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(tier, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tierColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("$score out of 100", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Text(tierDesc, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Text("How to improve", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            val tips = when {
                confidence == "low" -> listOf(
                    "📸 Rescan in better lighting for a more accurate reading",
                    "🖐️ Keep your palm flat and steady during the scan",
                    "💡 Avoid shadows and glare on your palm",
                )
                score < 50 -> listOf(
                    "🔄 Scan monthly to track progress over time",
                    "📝 Use the journal to reflect on your actions",
                    "🎯 Focus on the daily and weekly action items",
                    "🌱 Small consistent changes create lasting shifts",
                )
                else -> listOf(
                    "📊 Compare with previous months in History",
                    "📝 Record reflections in your journal",
                    "🔄 Scan again next month to track your growth",
                )
            }
            tips.forEach { tip ->
                Text(tip, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
