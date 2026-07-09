package com.palmastro.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.palmastro.app.security.SecurityChecker
import com.palmastro.app.security.ThreatLevel
import com.palmastro.app.worker.ScanImageCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PalmAstroApp : Application() {
    var securityThreatLevel: ThreatLevel = ThreatLevel.NONE
        private set

    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this)
        runSecurityCheck()
        createNotificationChannel()
        scheduleScanImageCleanup()
    }

    private fun runSecurityCheck() {
        val report = SecurityChecker.check(this)
        securityThreatLevel = report.threatLevel
        if (!report.isSecure) {
            CrashReporting.log("security_check: threats=${report.threats.joinToString(",")}")
            report.threats.forEach { threat ->
                CrashReporting.log("security_threat: $threat")
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_SCAN_REMINDER,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleScanImageCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<ScanImageCleanupWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ScanImageCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest,
        )
    }

    companion object {
        const val CHANNEL_SCAN_REMINDER = "scan_reminder"
    }
}
