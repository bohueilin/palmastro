package com.palmastro.app.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.contracts.Angle
import com.palmastro.app.viewmodel.ScanViewModel

private val angleNames = mapOf(
    Angle.FRONT to "正面", Angle.LEFT_TILT to "左傾", Angle.RIGHT_TILT to "右傾",
    Angle.NEAR to "近距離", Angle.FAR to "遠距離", Angle.UP_TILT to "上傾", Angle.DOWN_TILT to "下傾",
)

@Composable
fun ScanScreen(
    onComplete: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
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
        when {
            state.isProcessing -> {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("正在分析你的掌紋...", fontSize = 18.sp)
            }
            state.error != null -> {
                Text("發生錯誤", fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                Text(state.error ?: "", fontSize = 14.sp)
            }
            state.currentAngleIndex < Angle.entries.size -> {
                val currentAngle = Angle.entries[state.currentAngleIndex]
                Text("掃描進度", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                @Suppress("DEPRECATION")
                LinearProgressIndicator(
                    progress = state.completedAngles.size.toFloat() / Angle.entries.size,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("${state.completedAngles.size} / ${Angle.entries.size}", fontSize = 12.sp)
                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier.size(250.dp).clip(CircleShape).background(Color(0xFF2A2A3E)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isScanning) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("\uD83D\uDD90", fontSize = 80.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("請將手掌對準：${angleNames[currentAngle]}", fontSize = 20.sp)
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.startAngleScan() },
                    enabled = !state.isScanning,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(if (state.isScanning) "掃描中..." else "開始掃描", fontSize = 18.sp)
                }
            }
        }
    }
}
