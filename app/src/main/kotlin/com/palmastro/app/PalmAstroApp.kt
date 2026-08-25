package com.palmastro.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.palmastro.app.security.SecurityChecker
import com.palmastro.app.security.ThreatLevel
import com.palmastro.app.worker.ScanImageCleanupWorker
import com.palmastro.data.repository.InstallIdRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PalmAstroApp : Application() {
    // dagger.Lazy defers database creation (Keystore key unwrap + SQLCipher open)
    // off the main thread; the repository is first touched inside appScope.
    @Inject lateinit var installIdRepository: dagger.Lazy<InstallIdRepository>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // @Volatile: written from appScope, potentially read from any thread.
    @Volatile
    var securityThreatLevel: ThreatLevel = ThreatLevel.NONE
        private set

    override fun onCreate() {
        super.onCreate()
        CrashReporting.init(this)
        // Off the main thread: the security check forks `which su` and parses the
        // APK signing block, which would otherwise be paid before the first frame.
        // The channel is created first so no worker can post against a missing one.
        appScope.launch {
            createNotificationChannel()
            scheduleScanImageCleanup()
            runSecurityCheck()
        }
        ensureInstallId()
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

    private fun ensureInstallId() {
        appScope.launch {
            runCatching { installIdRepository.get().getOrCreate() }
                .onFailure { CrashReporting.recordException(it) }
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

    /**
     * Two schedules, because the periodic one alone cannot honour the "deleted
     * within 24 hours" promise in the privacy policy and the Play declaration:
     * without an explicit flex, WorkManager may place consecutive runs almost two
     * intervals apart. The launch sweep is what actually bounds the window, since
     * the app is opened far more often than once a day.
     */
    private fun scheduleScanImageCleanup() {
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniqueWork(
            LAUNCH_CLEANUP_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ScanImageCleanupWorker>().build(),
        )
        val cleanupRequest = PeriodicWorkRequestBuilder<ScanImageCleanupWorker>(
            CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS,
            CLEANUP_FLEX_MINUTES, TimeUnit.MINUTES,
        )
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        // UPDATE, not KEEP: KEEP is honoured regardless of versionCode, so an
        // already-enqueued schedule would never pick up the interval or flex.
        workManager.enqueueUniquePeriodicWork(
            ScanImageCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest,
        )
    }

    companion object {
        const val CHANNEL_SCAN_REMINDER = "scan_reminder"
        private const val LAUNCH_CLEANUP_WORK_NAME = "scan_image_cleanup_launch"
        private const val CLEANUP_INTERVAL_HOURS = 6L
        private const val CLEANUP_FLEX_MINUTES = 15L
    }
}
