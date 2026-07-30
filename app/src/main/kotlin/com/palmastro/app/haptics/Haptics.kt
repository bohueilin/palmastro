package com.palmastro.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PalmAstro haptic vocabulary (UX_ROADMAP 1.1, PRD 38-41).
 *
 * Four events, kept deliberately small so both platforms speak the same language:
 *  - [tickCapture]      crisp low tick when the capture button is pressed
 *  - [thumpQualityPass] short medium confirmation when an angle passes the quality gate
 *  - [buzzQualityFail]  two soft pulses when the gate asks for a retake — coaching, never punitive
 *  - [shimmerReveal]    three ascending soft ticks for scan complete / guidance reveal
 *
 * Amplitudes stay low on purpose: PRD 12.3 forbids anything that feels like an alarm, and a
 * quality fail is coaching, not an error. The player no-ops automatically when the device has
 * no vibrator or the system touch-feedback setting is off, so callers never need to guard.
 *
 * Requires android.permission.VIBRATE in the manifest (build-release agent owns that file).
 */
@Singleton
class HapticPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val vibrator: Vibrator? by lazy(::resolveVibrator)

    /** Capture button pressed: a crisp, barely-there acknowledgment. */
    fun tickCapture() = play {
        predefined(VibrationEffect.EFFECT_TICK)
            ?: VibrationEffect.createOneShot(TICK_MS, AMPLITUDE_SOFT)
    }

    /** Angle passed the quality gate: one short, warmer confirmation. */
    fun thumpQualityPass() = play {
        predefined(VibrationEffect.EFFECT_CLICK)
            ?: VibrationEffect.createOneShot(THUMP_MS, AMPLITUDE_MEDIUM)
    }

    /** Quality gate asked for a retake: two gentle pulses — a nudge, not a buzzer. */
    fun buzzQualityFail() = play {
        VibrationEffect.createWaveform(
            longArrayOf(0, FAIL_PULSE_MS, FAIL_GAP_MS, FAIL_PULSE_MS),
            intArrayOf(0, AMPLITUDE_SOFT, 0, AMPLITUDE_SOFT),
            NO_REPEAT,
        )
    }

    /** Scan complete / guidance reveal: three soft ticks rising in intensity. */
    fun shimmerReveal() = play {
        VibrationEffect.createWaveform(
            longArrayOf(0, SHIMMER_TICK_MS, SHIMMER_GAP_MS, SHIMMER_TICK_MS, SHIMMER_GAP_MS, SHIMMER_TICK_MS),
            intArrayOf(0, SHIMMER_LOW, 0, SHIMMER_MID, 0, SHIMMER_HIGH),
            NO_REPEAT,
        )
    }

    private fun play(effect: () -> VibrationEffect) {
        val v = vibrator ?: return
        if (!hapticsEnabled(v)) return
        runCatching { v.vibrate(effect()) }
    }

    /** Predefined effects exist from API 29; below that callers fall back to hand-tuned shapes. */
    private fun predefined(effectId: Int): VibrationEffect? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.createPredefined(effectId) else null

    /** Hardware present AND the system "touch feedback" preference is on (UX_ROADMAP 1.1). */
    private fun hapticsEnabled(v: Vibrator): Boolean =
        v.hasVibrator() && Settings.System.getInt(
            context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1,
        ) == 1

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private companion object {
        const val TICK_MS = 10L
        const val THUMP_MS = 20L
        const val FAIL_PULSE_MS = 15L
        const val FAIL_GAP_MS = 90L
        const val SHIMMER_TICK_MS = 12L
        const val SHIMMER_GAP_MS = 70L

        /** 0..255 scale; kept low per HapticFeedbackConstants philosophy — felt, not heard. */
        const val AMPLITUDE_SOFT = 40
        const val AMPLITUDE_MEDIUM = 110
        const val SHIMMER_LOW = 40
        const val SHIMMER_MID = 70
        const val SHIMMER_HIGH = 100
        const val NO_REPEAT = -1
    }
}

/** Hilt access for plain composables — scan UI resolves the singleton without viewmodel plumbing. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HapticsEntryPoint {
    fun hapticPlayer(): HapticPlayer
}

/** Resolves the singleton [HapticPlayer] from any composable inside the Hilt application. */
@Composable
fun rememberHapticPlayer(): HapticPlayer {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, HapticsEntryPoint::class.java).hapticPlayer()
    }
}
