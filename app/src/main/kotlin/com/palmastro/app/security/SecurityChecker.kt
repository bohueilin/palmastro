package com.palmastro.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

data class SecurityReport(
    val isRooted: Boolean,
    val isDebuggable: Boolean,
    val isEmulator: Boolean,
    val isSignatureValid: Boolean,
    val threats: List<String>,
) {
    val isSecure: Boolean get() = threats.isEmpty()
    val threatLevel: ThreatLevel get() = when {
        threats.any { it == "SIGNATURE_INVALID" } -> ThreatLevel.CRITICAL
        threats.any { it == "DEVICE_ROOTED" } -> ThreatLevel.HIGH
        threats.any { it == "APP_DEBUGGABLE" } -> ThreatLevel.MEDIUM
        threats.any { it == "RUNNING_ON_EMULATOR" } -> ThreatLevel.LOW
        else -> ThreatLevel.NONE
    }
}

enum class ThreatLevel { NONE, LOW, MEDIUM, HIGH, CRITICAL }

object SecurityChecker {

    fun check(context: Context): SecurityReport {
        val threats = mutableListOf<String>()

        val rooted = checkRoot()
        if (rooted) threats.add("DEVICE_ROOTED")

        val debuggable = checkDebuggable(context)
        if (debuggable) threats.add("APP_DEBUGGABLE")

        val emulator = checkEmulator()
        if (emulator) threats.add("RUNNING_ON_EMULATOR")

        val sigValid = checkSignature(context)
        if (!sigValid) threats.add("SIGNATURE_INVALID")

        return SecurityReport(rooted, debuggable, emulator, sigValid, threats)
    }

    private fun checkRoot(): Boolean {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup",
        )
        if (rootPaths.any { File(it).exists() }) return true

        val pathDirs = System.getenv("PATH")?.split(":") ?: emptyList()
        if (pathDirs.any { File("$it/su").exists() }) return true

        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            process.inputStream.bufferedReader().readLine() != null
        } catch (_: Exception) {
            false
        }
    }

    private fun checkDebuggable(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun checkEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("vbox")
    }

    @Suppress("DEPRECATION")
    private fun checkSignature(context: Context): Boolean {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
            } else {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES,
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.hasMultipleSigners() == false
                    && info.signingInfo?.signingCertificateHistory?.isNotEmpty() == true
            } else {
                info.signatures?.isNotEmpty() == true
            }
        } catch (_: Exception) {
            false
        }
    }
}
