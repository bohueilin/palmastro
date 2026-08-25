package com.palmastro.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.palmastro.data.dao.MonthlyResultDao
import com.palmastro.data.dao.UserProfileDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File

/**
 * Enforces the raw-media retention policy (PRD §15/§27): scan images expire
 * [RETENTION_MS] after capture (per-file mtime), or immediately when the user has
 * disabled raw media retention. Cleared months get their scanImagePath reset
 * so the UI never points at deleted files.
 *
 * Uses a Hilt entry point instead of @HiltWorker so the default WorkerFactory
 * keeps working without extra WorkManager-Hilt wiring.
 */
class ScanImageCleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CleanupEntryPoint {
        fun monthlyResultDao(): MonthlyResultDao
        fun userProfileDao(): UserProfileDao
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CleanupEntryPoint::class.java,
        )
        val monthlyResultDao = entryPoint.monthlyResultDao()

        val scansDir = File(applicationContext.filesDir, "scans")
        if (!scansDir.exists()) return Result.success()

        val retentionEnabled = entryPoint.userProfileDao().get()?.rawMediaRetention ?: true
        if (!retentionEnabled) {
            scansDir.deleteRecursively()
            monthlyResultDao.clearAllScanImagePaths()
            return Result.success()
        }

        val cutoff = System.currentTimeMillis() - RETENTION_MS
        scansDir.listFiles()?.forEach { child ->
            if (!child.isDirectory) {
                // Stray file directly under scans/: same per-file staleness rule.
                if (child.lastModified() < cutoff) child.delete()
                return@forEach
            }
            // Month directory (scans/<monthKey>/): judge each file by its own mtime.
            child.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) file.deleteRecursively()
            }
            if (child.listFiles().isNullOrEmpty()) {
                child.delete()
                monthlyResultDao.clearScanImagePathForMonth(child.name)
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "scan_image_cleanup"

        /** Hours the app publicly promises as the outer bound (privacy policy, Play). */
        const val DISCLOSED_WINDOW_HOURS = 24L

        /**
         * Deliberately shorter than the disclosed window: a file only expires on the
         * next cleanup run, so a 24h constant would let images outlive the promise by
         * however long the scheduler defers. The margin absorbs that deferral.
         */
        const val RETENTION_MS = 20 * 60 * 60 * 1000L
    }
}
