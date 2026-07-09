package com.palmastro.app.ui.results

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.share.ShareCardRenderer
import com.palmastro.app.share.ShareHelper
import com.palmastro.app.viewmodel.DomainCard
import com.palmastro.app.viewmodel.ResultsState
import com.palmastro.app.viewmodel.ResultsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val gradeColors = mapOf("Growing" to Color(0xFF388E3C), "Stable" to Color(0xFF1976D2), "Building" to Color(0xFFE65100), "Watchout" to Color(0xFFD32F2F))

private fun gradeName(grade: String, lang: String): String = when (lang) {
    "zh-TW" -> when (grade) { "Growing" -> "成長期"; "Stable" -> "穩定期"; "Building" -> "累積期"; "Watchout" -> "注意期"; else -> grade }
    "zh-CN" -> when (grade) { "Growing" -> "成长期"; "Stable" -> "稳定期"; "Building" -> "积累期"; "Watchout" -> "注意期"; else -> grade }
    "ja" -> when (grade) { "Growing" -> "成長期"; "Stable" -> "安定期"; "Building" -> "蓄積期"; "Watchout" -> "注意期"; else -> grade }
    "hi" -> when (grade) { "Growing" -> "विकास"; "Stable" -> "स्थिर"; "Building" -> "निर्माण"; "Watchout" -> "सावधानी"; else -> grade }
    else -> when (grade) { "Growing" -> "Growing"; "Stable" -> "Stable"; "Building" -> "Building"; "Watchout" -> "Watch Out"; else -> grade }
}

private fun domainName(domain: String, lang: String): String = when (lang) {
    "zh-TW" -> when (domain) { "career" -> "事業"; "wealth" -> "財富"; "family" -> "家庭"; "health" -> "健康"; else -> domain }
    "zh-CN" -> when (domain) { "career" -> "事业"; "wealth" -> "财富"; "family" -> "家庭"; "health" -> "健康"; else -> domain }
    "ja" -> when (domain) { "career" -> "キャリア"; "wealth" -> "財運"; "family" -> "家庭"; "health" -> "健康"; else -> domain }
    "hi" -> when (domain) { "career" -> "करियर"; "wealth" -> "धन"; "family" -> "परिवार"; "health" -> "स्वास्थ्य"; else -> domain }
    else -> when (domain) { "career" -> "Career"; "wealth" -> "Wealth"; "family" -> "Family"; "health" -> "Health"; else -> domain }
}

private fun confidenceName(conf: String, lang: String): String {
    val level = when (conf.lowercase()) { "high" -> "high"; "med", "medium" -> "med"; "low" -> "low"; else -> conf }
    return when (lang) {
        "zh-TW" -> when (level) { "high" -> "高"; "med" -> "中"; "low" -> "低"; else -> conf }
        "zh-CN" -> when (level) { "high" -> "高"; "med" -> "中"; "low" -> "低"; else -> conf }
        "ja" -> when (level) { "high" -> "高い"; "med" -> "普通"; "low" -> "低い"; else -> conf }
        "hi" -> when (level) { "high" -> "उच्च"; "med" -> "मध्यम"; "low" -> "कम"; else -> conf }
        else -> when (level) { "high" -> "High"; "med" -> "Medium"; "low" -> "Low"; else -> conf }
    }
}

private fun labelConfidence(lang: String): String = when (lang) { "zh-TW" -> "信心度"; "zh-CN" -> "信心度"; "ja" -> "信頼度"; "hi" -> "विश्वास"; else -> "Confidence" }
private fun labelScanAgain(lang: String): String = when (lang) { "zh-TW" -> "重新掃描"; "zh-CN" -> "重新扫描"; "ja" -> "再スキャン"; "hi" -> "फिर स्कैन"; else -> "Scan Again" }
private fun labelHistory(lang: String): String = when (lang) { "zh-TW" -> "歷史記錄"; "zh-CN" -> "历史记录"; "ja" -> "履歴"; "hi" -> "इतिहास"; else -> "History" }
private fun labelNoResults(lang: String): String = when (lang) { "zh-TW" -> "還沒有掃描結果"; "zh-CN" -> "还没有扫描结果"; "ja" -> "スキャン結果がありません"; "hi" -> "अभी तक कोई परिणाम नहीं"; else -> "No scan results yet" }
private fun labelStartScan(lang: String): String = when (lang) { "zh-TW" -> "開始掃描"; "zh-CN" -> "开始扫描"; "ja" -> "スキャン開始"; "hi" -> "स्कैन शुरू करें"; else -> "Scan Your Palm" }
private fun labelWelcome(lang: String): String = when (lang) { "zh-TW" -> "歡迎使用 PalmAstro"; "zh-CN" -> "欢迎使用 PalmAstro"; "ja" -> "PalmAstroへようこそ"; "hi" -> "PalmAstro में आपका स्वागत है"; else -> "Welcome to PalmAstro" }
private fun labelWelcomeDesc(lang: String): String = when (lang) { "zh-TW" -> "掃描你的手掌，探索事業、財富、家庭和健康的洞察。"; "zh-CN" -> "扫描你的手掌，探索事业、财富、家庭和健康的洞察。"; "ja" -> "手のひらをスキャンして、キャリア、財運、家庭、健康の洞察を発見しましょう。"; "hi" -> "अपनी हथेली स्कैन करें और करियर, धन, परिवार और स्वास्थ्य के बारे में जानकारी प्राप्त करें।"; else -> "Scan your palm to discover insights about your career, wealth, family, and health." }

private val domainImages = mapOf("career" to R.drawable.img_domain_career, "wealth" to R.drawable.img_domain_wealth, "family" to R.drawable.img_domain_family, "health" to R.drawable.img_domain_health)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(onScanClick: () -> Unit, onSettingsClick: () -> Unit, onDomainClick: (String, String) -> Unit, onHistoryClick: () -> Unit, viewModel: ResultsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    var lang by remember { mutableStateOf("en") }
    val langs = listOf("en" to "English", "zh-TW" to "繁體中文", "zh-CN" to "简体中文", "ja" to "日本語", "hi" to "हिन्दी")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PalmAstro", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    if (state.hasResults) {
                        IconButton(onClick = { view.announceForAccessibility("Sharing report"); shareSummary(context, state) }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                    }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                },
            )
        }
    ) { padding ->
        if (!state.hasResults) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painter = painterResource(R.drawable.img_empty_no_results), contentDescription = null, modifier = Modifier.size(200.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Fit)
                Spacer(Modifier.height(24.dp))
                Text(labelWelcome(lang), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(labelWelcomeDesc(lang), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 24.sp)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth(0.7f).height(56.dp), shape = RoundedCornerShape(16.dp)) { Text(labelStartScan(lang), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                // Language selector
                item {
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        langs.forEach { (code, label) ->
                            FilterChip(selected = lang == code, onClick = { lang = code }, label = { Text(label, fontSize = 12.sp) })
                        }
                    }
                }
                // Grade hero card
                item {
                    val gc = gradeColors[state.grade] ?: MaterialTheme.colorScheme.primary
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(gc.copy(alpha = 0.15f), gc.copy(alpha = 0.05f)))).padding(24.dp)) {
                            Column {
                                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text(gradeName(state.grade, lang), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = gc)
                                Spacer(Modifier.height(4.dp))
                                Text("${labelConfidence(lang)}: ${confidenceName(state.confidence, lang)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                // Domain cards
                items(state.domainCards) { card ->
                    DomainCardItem(card = card, lang = lang, onClick = { onDomainClick(card.domain, state.monthKey) })
                }
                // Action buttons
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onScanClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text(labelScanAgain(lang)) }
                        OutlinedButton(onClick = onHistoryClick, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text(labelHistory(lang)) }
                    }
                }
                // Safety card
                item {
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Text("🛡️", fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(when (lang) { "zh-TW" -> "僅供個人參考"; "zh-CN" -> "仅供个人参考"; "ja" -> "個人的な参考用"; "hi" -> "केवल व्यक्तिगत संदर्भ"; else -> "For Personal Reflection" }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(when (lang) { "zh-TW" -> "PalmAstro 僅供自我成長與娛樂用途，非醫療或財務建議。所有分析在您的裝置上進行。"; "zh-CN" -> "PalmAstro 仅供自我成长与娱乐用途，非医疗或财务建议。所有分析在您的设备上进行。"; "ja" -> "PalmAstroは自己成長とエンターテインメント用です。医療・財務アドバイスではありません。すべての分析はデバイス上で行われます。"; "hi" -> "PalmAstro आत्म-विकास और मनोरंजन के लिए है। चिकित्सा या वित्तीय सलाह नहीं। सभी विश्लेषण आपके डिवाइस पर होते हैं।"; else -> "PalmAstro is for self-growth and entertainment. Not medical, financial, or professional advice. All analysis runs on your device." }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainCardItem(card: DomainCard, lang: String, onClick: () -> Unit) {
    val gc = gradeColors[card.grade] ?: MaterialTheme.colorScheme.primary
    val displayName = domainName(card.domain, lang)
    val gradeText = gradeName(card.grade, lang)

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).semantics(mergeDescendants = true) { contentDescription = "$displayName ${card.score} points" }, elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Row(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(domainImages[card.domain] ?: R.drawable.img_domain_career), contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(gradeText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${card.score}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = gc)
            }
            LinearProgressIndicator(progress = { card.score / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp).clearAndSetSemantics {}, color = gc, trackColor = gc.copy(alpha = 0.1f))
        }
    }
}

private fun shareSummary(context: Context, state: ResultsState) {
    val domains = state.domainCards.map { ShareCardRenderer.DomainScore(it.displayName, it.score, it.grade) }
    val data = ShareCardRenderer.SummaryData(state.monthKey, state.grade, state.confidence, domains)
    val bitmap = ShareCardRenderer.renderSummaryCard(data)
    val text = ShareHelper.buildSummaryText(state.monthKey, state.grade, state.confidence, domains)
    ShareHelper.share(context, bitmap, text)
    bitmap.recycle()
}
