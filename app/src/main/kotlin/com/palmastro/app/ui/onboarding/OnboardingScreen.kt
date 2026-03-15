package com.palmastro.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.viewmodel.OnboardingViewModel
import java.time.LocalDate

private data class County(val name: String, val lat: Double, val lon: Double)

private val taiwanCounties = listOf(
    County("台北市", 25.033, 121.565),
    County("新北市", 25.012, 121.465),
    County("基隆市", 25.128, 121.739),
    County("桃園市", 24.994, 121.301),
    County("新竹市", 24.804, 120.969),
    County("新竹縣", 24.839, 121.004),
    County("苗栗縣", 24.560, 120.822),
    County("台中市", 24.148, 120.674),
    County("彰化縣", 24.076, 120.542),
    County("南投縣", 23.961, 120.685),
    County("雲林縣", 23.709, 120.432),
    County("嘉義市", 23.480, 120.449),
    County("嘉義縣", 23.452, 120.256),
    County("台南市", 22.999, 120.227),
    County("高雄市", 22.627, 120.301),
    County("屏東縣", 22.552, 120.549),
    County("宜蘭縣", 24.757, 121.753),
    County("花蓮縣", 23.992, 121.601),
    County("台東縣", 22.756, 121.145),
    County("澎湖縣", 23.571, 119.579),
    County("金門縣", 24.449, 118.377),
    County("連江縣", 26.160, 119.950),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("掌紋星象", fontSize = 28.sp, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        when (state.step) {
            0 -> HandSelectionStep(
                selected = state.dominantHand,
                onSelect = { viewModel.setHand(it) },
                onNext = { viewModel.nextStep() },
            )
            1 -> BirthdayStep(
                onSelect = { viewModel.setBirthday(it); viewModel.nextStep() },
            )
            2 -> BirthDetailsStep(
                onSkip = { viewModel.skipBirthDetails(); viewModel.nextStep() },
                onContinue = { hour, minute, county ->
                    viewModel.setBirthTime(hour, minute)
                    if (county != null) {
                        viewModel.setBirthPlace(county.name, county.lat, county.lon)
                    }
                    viewModel.nextStep()
                },
            )
            3 -> ToneStep(
                selected = state.tone,
                onSelect = { viewModel.setTone(it) },
                onComplete = { viewModel.completeOnboarding() },
            )
        }
    }
}

@Composable
private fun HandSelectionStep(selected: String, onSelect: (String) -> Unit, onNext: () -> Unit) {
    Text("選擇你的慣用手", fontSize = 20.sp)
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("left" to "左手", "right" to "右手").forEach { (key, label) ->
            OutlinedButton(
                onClick = { onSelect(key) },
                colors = if (selected == key) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier.width(120.dp),
            ) { Text(label, fontSize = 18.sp) }
        }
    }
    Spacer(Modifier.height(32.dp))
    Button(onClick = onNext) { Text("下一步") }
}

@Composable
private fun BirthdayStep(onSelect: (LocalDate) -> Unit) {
    var year by remember { mutableStateOf("1990") }
    var month by remember { mutableStateOf("1") }
    var day by remember { mutableStateOf("1") }

    Text("輸入你的生日", fontSize = 20.sp)
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("年") }, modifier = Modifier.width(100.dp))
        OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("月") }, modifier = Modifier.width(80.dp))
        OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("日") }, modifier = Modifier.width(80.dp))
    }
    Spacer(Modifier.height(32.dp))
    Button(onClick = {
        val date = LocalDate.of(year.toIntOrNull() ?: 1990, month.toIntOrNull() ?: 1, day.toIntOrNull() ?: 1)
        onSelect(date)
    }) { Text("下一步") }
}

@Composable
private fun BirthDetailsStep(
    onSkip: () -> Unit,
    onContinue: (hour: Int, minute: Int, county: County?) -> Unit,
) {
    var hour by remember { mutableStateOf("12") }
    var minute by remember { mutableStateOf("0") }
    var selectedCounty by remember { mutableStateOf<County?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Text("出生時間與地點（選填）", fontSize = 20.sp)
    Spacer(Modifier.height(8.dp))
    Text("提供完整資訊可獲得更精準的分析 (L2)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))

    // Birth time
    Text("出生時間", fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = hour,
            onValueChange = { hour = it },
            label = { Text("時") },
            modifier = Modifier.width(80.dp),
        )
        OutlinedTextField(
            value = minute,
            onValueChange = { minute = it },
            label = { Text("分") },
            modifier = Modifier.width(80.dp),
        )
    }

    Spacer(Modifier.height(20.dp))

    // Birth place
    Text("出生地點", fontSize = 16.sp)
    Spacer(Modifier.height(8.dp))
    Box {
        OutlinedButton(
            onClick = { dropdownExpanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                selectedCounty?.name ?: "選擇縣市",
                fontSize = 16.sp,
            )
        }
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("海外（不指定）") },
                onClick = {
                    selectedCounty = null
                    dropdownExpanded = false
                },
            )
            taiwanCounties.forEach { county ->
                DropdownMenuItem(
                    text = { Text(county.name) },
                    onClick = {
                        selectedCounty = county
                        dropdownExpanded = false
                    },
                )
            }
        }
    }

    Spacer(Modifier.height(32.dp))

    Button(
        onClick = {
            val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 12
            val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0
            onContinue(h, m, selectedCounty)
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("確認") }

    Spacer(Modifier.height(12.dp))

    TextButton(onClick = onSkip) { Text("跳過此步驟") }
}

@Composable
private fun ToneStep(selected: String, onSelect: (String) -> Unit, onComplete: () -> Unit) {
    Text("選擇分析風格", fontSize = 20.sp)
    Spacer(Modifier.height(24.dp))

    data class ToneOption(val key: String, val label: String, val desc: String)

    val tones = listOf(
        ToneOption("scientific", "科學分析", "客觀、數據導向"),
        ToneOption("healing", "療癒關懷", "溫暖、鼓勵"),
        ToneOption("roast_safe", "犀利直說", "直接、挑戰但安全"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tones.forEach { tone ->
            OutlinedButton(
                onClick = { onSelect(tone.key) },
                colors = if (selected == tone.key) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tone.label, fontSize = 16.sp)
                    Text(tone.desc, fontSize = 12.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(32.dp))
    Button(onClick = onComplete) { Text("開始使用") }
}
