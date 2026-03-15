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
                onNext = { viewModel.nextStep() },
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
private fun BirthDetailsStep(onSkip: () -> Unit, onNext: () -> Unit) {
    Text("出生時間與地點（選填）", fontSize = 20.sp)
    Spacer(Modifier.height(8.dp))
    Text("提供完整資訊可獲得更精準的分析 (L2)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onSkip) { Text("跳過，繼續") }
    Spacer(Modifier.height(12.dp))
    TextButton(onClick = onNext) { Text("我要填寫") }
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
