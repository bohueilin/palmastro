package com.palmastro.app.ui.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.palmastro.app.R
import com.palmastro.app.viewmodel.ScanViewModel
import com.palmastro.contracts.Angle

@StringRes
private fun angleNameRes(angle: Angle): Int = when (angle) {
    Angle.FRONT -> R.string.scan_angle_front
    Angle.LEFT_TILT -> R.string.scan_angle_left_tilt
    Angle.RIGHT_TILT -> R.string.scan_angle_right_tilt
    Angle.NEAR -> R.string.scan_angle_near
    Angle.FAR -> R.string.scan_angle_far
    Angle.UP_TILT -> R.string.scan_angle_up_tilt
    Angle.DOWN_TILT -> R.string.scan_angle_down_tilt
}

@StringRes
private fun angleInstructionRes(angle: Angle): Int = when (angle) {
    Angle.FRONT -> R.string.scan_instruction_front
    Angle.LEFT_TILT -> R.string.scan_instruction_left_tilt
    Angle.RIGHT_TILT -> R.string.scan_instruction_right_tilt
    Angle.NEAR -> R.string.scan_instruction_near
    Angle.FAR -> R.string.scan_instruction_far
    Angle.UP_TILT -> R.string.scan_instruction_up_tilt
    Angle.DOWN_TILT -> R.string.scan_instruction_down_tilt
}

/**
 * Maps keys returned by CoachingHints.keyFor(reason) to localized strings.
 * Raw fail reasons are also accepted so pre-integration builds stay coherent.
 */
@StringRes
private fun coachingHintRes(key: String): Int = when (key) {
    "coach_blur", "blur" -> R.string.coach_blur
    "coach_glare", "glare" -> R.string.coach_glare
    "coach_low_light", "low_light", "under_exposure" -> R.string.coach_low_light
    "coach_over_exposure", "over_exposure" -> R.string.coach_over_exposure
    "coach_low_coverage", "low_coverage" -> R.string.coach_low_coverage
    "coach_pose_unstable", "pose_unstable" -> R.string.coach_pose_unstable
    "coach_hand_not_detected", "hand_not_detected" -> R.string.coach_hand_not_detected
    else -> R.string.coach_generic
}

/** Localized what/why/next error taxonomy (PRD scan acceptance criteria). */
private enum class ScanErrorKind(
    @StringRes val what: Int,
    @StringRes val why: Int,
    @StringRes val next: Int,
    @StringRes val action: Int,
) {
    MODEL_DOWNLOAD_FAILED(R.string.sc_err_download_what, R.string.sc_err_download_why, R.string.sc_err_download_next, R.string.sc_err_download_action),
    MODEL_CORRUPT(R.string.sc_err_corrupt_what, R.string.sc_err_corrupt_why, R.string.sc_err_corrupt_next, R.string.sc_err_corrupt_action),
    PROCESSING_FAILED(R.string.sc_err_processing_what, R.string.sc_err_processing_why, R.string.sc_err_processing_next, R.string.sc_err_processing_action),
}

private enum class PreScanPhase { EXPLAINER, TIPS, CAPTURE }

@Composable
fun ScanScreen(onComplete: () -> Unit, viewModel: ScanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    // Respect the system "remove animations" preference for animated overlays.
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    var phase by rememberSaveable { mutableStateOf(PreScanPhase.EXPLAINER) }
    var hasPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }
    // Camera permission is requested only when actually needed (after explainer + tips).
    LaunchedEffect(phase) {
        if (phase == PreScanPhase.CAPTURE && !hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(state.isComplete) { if (state.isComplete) onComplete() }

    val captureDoneAnnounce = stringResource(R.string.scan_capture_done)
    LaunchedEffect(state.showFlash) { if (state.showFlash) view.announceForAccessibility(captureDoneAnnounce) }
    val completeAnnounce = stringResource(R.string.sc_complete_announce)
    LaunchedEffect(state.isProcessing) { if (state.isProcessing) view.announceForAccessibility(completeAnnounce) }

    when {
        phase == PreScanPhase.EXPLAINER -> ExplainerScreen(onNext = { phase = PreScanPhase.TIPS })
        phase == PreScanPhase.TIPS -> TipsScreen(onStart = { phase = PreScanPhase.CAPTURE })
        permissionDenied -> PermissionDeniedScreen(onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            })
        })
        !hasPermission -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.modelDownloading -> ModelDownloadingScreen()
        state.modelError != null -> ErrorScreen(
            kind = if (state.modelError.orEmpty().contains("corrupt", ignoreCase = true)) ScanErrorKind.MODEL_CORRUPT else ScanErrorKind.MODEL_DOWNLOAD_FAILED,
            onAction = { viewModel.retryModelDownload() },
        )
        !state.modelReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.isProcessing -> CompletionProcessingScreen()
        state.error != null -> ErrorScreen(kind = ScanErrorKind.PROCESSING_FAILED, onAction = { viewModel.dismissError() })
        else -> CaptureScreen(
            currentAngleIndex = state.currentAngleIndex,
            totalAngles = Angle.entries.size,
            completedCount = state.completedAngles.size,
            isCapturing = state.isCapturing,
            coachingHintKey = state.coachingHint,
            showFlash = state.showFlash,
            reduceMotion = reduceMotion,
            imageCapture = viewModel.imageCapture,
            onCapture = { viewModel.captureCurrentAngle() },
            onRetake = { viewModel.retakePreviousAngle() },
        )
    }
}

// ── PRD 13.2 step 1: explain scan requirements ──
@Composable
private fun ExplainerScreen(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.sc_explainer_title),
            fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.sc_explainer_subtitle), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        InfoPoint(stringResource(R.string.sc_explainer_point_angles))
        InfoPoint(stringResource(R.string.sc_explainer_point_quality))
        InfoPoint(stringResource(R.string.sc_explainer_point_private))
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Text(stringResource(R.string.sc_explainer_next), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── PRD 13.2 step 2: lighting / positioning tips ──
@Composable
private fun TipsScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.sc_tips_title),
            fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.sc_tips_subtitle), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        InfoPoint(stringResource(R.string.sc_tips_light))
        InfoPoint(stringResource(R.string.sc_tips_position))
        InfoPoint(stringResource(R.string.sc_tips_steady))
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Text(stringResource(R.string.sc_tips_start), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoPoint(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CaptureScreen(
    currentAngleIndex: Int,
    totalAngles: Int,
    completedCount: Int,
    isCapturing: Boolean,
    coachingHintKey: String?,
    showFlash: Boolean,
    reduceMotion: Boolean,
    imageCapture: androidx.camera.core.ImageCapture,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
) {
    val currentAngle = if (currentAngleIndex < totalAngles) Angle.entries[currentAngleIndex] else null
    val view = LocalView.current
    val currentInstruction = currentAngle?.let { stringResource(angleInstructionRes(it)) }
    LaunchedEffect(currentAngleIndex) { currentInstruction?.let { view.announceForAccessibility(it) } }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(imageCapture = imageCapture, modifier = Modifier.fillMaxSize())
        HandOverlay(modifier = Modifier.fillMaxSize())
        // The animated scanning line is suppressed when the user removed animations.
        if (!reduceMotion) {
            ScanningOverlay(isScanning = !isCapturing, modifier = Modifier.fillMaxSize())
        }
        if (reduceMotion) {
            if (showFlash) Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
        } else {
            AnimatedVisibility(visible = showFlash, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.6f)))
            }
        }
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (currentAngleIndex > 0) {
                    IconButton(onClick = onRetake, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.scan_retake), tint = Color.White)
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                val progressDesc = stringResource(R.string.sc_capture_progress_desc, completedCount, totalAngles)
                Text(
                    stringResource(R.string.scan_progress, completedCount, totalAngles),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.semantics { contentDescription = progressDesc },
                )
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (currentAngle != null) {
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(angleNameRes(currentAngle)), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(angleInstructionRes(currentAngle)), fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
        if (coachingHintKey != null) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    stringResource(coachingHintRes(coachingHintKey)),
                    modifier = Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    fontSize = 16.sp, color = MaterialTheme.colorScheme.onTertiary, textAlign = TextAlign.Center,
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
            val captureDesc = stringResource(R.string.scan_capture)
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape)
                    .semantics { contentDescription = captureDesc }
                    .then(if (!isCapturing) Modifier.clickable { onCapture() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (isCapturing) CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Color.White, strokeWidth = 3.dp)
                else Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

@Composable
private fun ModelDownloadingScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.scan_downloading), fontSize = 18.sp)
    }
}

// ── PRD 13.2 step 6: scan completion, shown while analysis runs ──
@Composable
private fun CompletionProcessingScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.sc_complete_icon),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.sc_complete_title),
            fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sc_complete_body), fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sc_complete_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorScreen(kind: ScanErrorKind, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            stringResource(kind.what),
            fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(kind.why), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(kind.next), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(kind.action))
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            stringResource(R.string.scan_permission_title),
            fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.scan_permission_message), fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.scan_go_settings))
        }
    }
}
