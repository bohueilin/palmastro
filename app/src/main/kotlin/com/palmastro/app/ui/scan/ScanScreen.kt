package com.palmastro.app.ui.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.contracts.Angle
import com.palmastro.app.viewmodel.ScanViewModel

private val angleNames = mapOf(
    Angle.FRONT to "正面", Angle.LEFT_TILT to "左傾", Angle.RIGHT_TILT to "右傾",
    Angle.NEAR to "靠近", Angle.FAR to "遠離", Angle.UP_TILT to "上傾", Angle.DOWN_TILT to "下傾",
)

private val angleInstructions = mapOf(
    Angle.FRONT to "請將手掌正面朝上",
    Angle.LEFT_TILT to "請將手掌向左傾斜",
    Angle.RIGHT_TILT to "請將手掌向右傾斜",
    Angle.NEAR to "請將手掌靠近鏡頭",
    Angle.FAR to "請將手掌遠離鏡頭",
    Angle.UP_TILT to "請將手掌向上傾斜",
    Angle.DOWN_TILT to "請將手掌向下傾斜",
)

@Composable
fun ScanScreen(
    onComplete: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    var hasPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    LaunchedEffect(state.showFlash) {
        if (state.showFlash) {
            view.announceForAccessibility("拍攝完成")
        }
    }

    when {
        permissionDenied -> PermissionDeniedScreen(
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
        !hasPermission -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.isProcessing -> ProcessingScreen()
        state.error != null -> ErrorScreen(
            error = state.error ?: "",
            onDismiss = { viewModel.dismissError() },
        )
        else -> CaptureScreen(
            currentAngleIndex = state.currentAngleIndex,
            totalAngles = Angle.entries.size,
            completedCount = state.completedAngles.size,
            isCapturing = state.isCapturing,
            coachingHint = state.coachingHint,
            showFlash = state.showFlash,
            imageCapture = viewModel.imageCapture,
            onCapture = { viewModel.captureCurrentAngle() },
            onRetake = { viewModel.retakePreviousAngle() },
        )
    }
}

@Composable
private fun CaptureScreen(
    currentAngleIndex: Int,
    totalAngles: Int,
    completedCount: Int,
    isCapturing: Boolean,
    coachingHint: String?,
    showFlash: Boolean,
    imageCapture: androidx.camera.core.ImageCapture,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
) {
    val currentAngle = if (currentAngleIndex < totalAngles) Angle.entries[currentAngleIndex] else null

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (full screen)
        CameraPreview(
            imageCapture = imageCapture,
            modifier = Modifier.fillMaxSize(),
        )

        // Hand overlay guide
        HandOverlay(
            modifier = Modifier.fillMaxSize().clearAndSetSemantics {},
        )

        // Flash animation
        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f))
            )
        }

        // Top bar — angle instruction + progress
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentAngleIndex > 0) {
                    IconButton(onClick = onRetake) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "重拍上一角度",
                            tint = Color.White,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }

                Text(
                    "$completedCount / $totalAngles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )

                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(8.dp))

            if (currentAngle != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            angleNames[currentAngle] ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            angleInstructions[currentAngle] ?: "",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // Coaching hint (shown when quality gate fails)
        if (coachingHint != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                color = Color(0xFFFF6B35).copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    coachingHint,
                    modifier = Modifier
                        .padding(16.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Bottom — capture button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .semantics { contentDescription = "拍攝" }
                    .then(
                        if (!isCapturing) Modifier.clickable { onCapture() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("正在分析你的掌紋...", fontSize = 18.sp)
    }
}

@Composable
private fun ErrorScreen(error: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "發生錯誤",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(8.dp))
        Text(error, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDismiss) { Text("確定") }
    }
}

@Composable
private fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("需要相機權限", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "PalmAstro 需要使用相機來掃描你的手掌。請在設定中允許相機權限。",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings) {
            Text("前往設定")
        }
    }
}
