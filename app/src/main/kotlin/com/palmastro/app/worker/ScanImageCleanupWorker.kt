package com.palmastro.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class ScanImageCleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scansDir = File(applicationContext.filesDir, "scans")
        if (!scansDir.exists()) return Result.success()

        val cutoff = System.currentTimeMillis() - RETENTION_MS
        scansDir.listFiles()?.forEach { monthDir ->
            if (monthDir.isDirectory && monthDir.lastModified() < cutoff) {
                monthDir.deleteRecursively()
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "scan_image_cleanup"
        const val RETENTION_MS = 24 * 60 * 60 * 1000L
    }
}
