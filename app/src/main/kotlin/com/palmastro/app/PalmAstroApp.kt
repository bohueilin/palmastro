package com.palmastro.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PalmAstroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_SCAN_REMINDER,
            "掃描提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "每月掃描提醒通知"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_SCAN_REMINDER = "scan_reminder"
    }
}
