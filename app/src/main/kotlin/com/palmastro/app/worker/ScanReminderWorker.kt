package com.palmastro.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.palmastro.app.MainActivity
import com.palmastro.app.PalmAstroApp

class ScanReminderWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, PalmAstroApp.CHANNEL_SCAN_REMINDER)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("掌紋星象")
            .setContentText("是時候掃描你的手掌了！開啟 PalmAstro 查看本月分析")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(1001, notification)
        } catch (_: SecurityException) {
            // User may have denied notification permission
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "scan_reminder"
    }
}
