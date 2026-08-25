package com.palmastro.app.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.palmastro.app.MainActivity
import com.palmastro.app.PalmAstroApp
import com.palmastro.app.R
import com.palmastro.data.dao.UserProfileDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Posts the monthly scan reminder. Reminders are opt-in (profile default "off"),
 * so this worker is only ever scheduled after the user enabled them. The
 * notification body is generic on purpose — it must never contain scores,
 * grades, or any other sensitive reading content.
 *
 * "1st of each month" cannot be expressed as a repeat interval — no fixed period
 * lands on the 1st — so that cadence runs as a one-shot that re-arms itself here,
 * recomputing the target date each time instead of drifting. "Every 30 days"
 * stays periodic.
 *
 * Uses a Hilt entry point instead of @HiltWorker so the default WorkerFactory
 * keeps working without extra WorkManager-Hilt wiring.
 */
class ScanReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun userProfileDao(): UserProfileDao
    }

    override suspend fun doWork(): Result {
        // Re-arm first: the calendar chain is one-shot, so the permission gate below
        // would otherwise end it permanently the first time notifications are off.
        rearmCalendarCadence()

        // Runtime permission gate: never attempt to post without POST_NOTIFICATIONS.
        if (!canPostNotifications()) return Result.success()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, PalmAstroApp.CHANNEL_SCAN_REMINDER)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(applicationContext.getString(R.string.sc_reminder_notif_title))
            .setContentText(applicationContext.getString(R.string.sc_reminder_notif_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post — fail quietly.
        }

        return Result.success()
    }

    /**
     * Queues the following month's run, but only while the profile still asks for the
     * calendar cadence, so switching to "every 30 days" or "off" in between wins.
     */
    private suspend fun rearmCalendarCadence() {
        val cadence = runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, ReminderEntryPoint::class.java)
                .userProfileDao().get()?.reminders
        }.getOrNull()
        if (cadence != CADENCE_MONTHLY) return
        // APPEND_OR_REPLACE, not REPLACE: REPLACE on the unique name this run owns
        // would cancel this very worker before it posts.
        enqueueCalendarCadence(WorkManager.getInstance(applicationContext), ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }

    companion object {
        /** Unique work for the "every 30 days" cadence (periodic). */
        const val WORK_NAME = "scan_reminder"

        /** Unique work for the "1st of each month" cadence (self-re-arming one-shot). */
        const val CALENDAR_WORK_NAME = "scan_reminder_monthly"

        const val CADENCE_30D = "30d"
        const val CADENCE_MONTHLY = "monthly"

        private const val NOTIFICATION_ID = 1001
        private const val INTERVAL_DAYS = 30L

        /** Mid-morning: late enough not to wake anyone, early enough to still be today. */
        private const val REMINDER_HOUR = 10

        /** Applies the user's cadence choice, replacing whatever was scheduled before. */
        fun schedule(context: Context, cadence: String) {
            val workManager = WorkManager.getInstance(context)
            when (cadence) {
                CADENCE_30D -> {
                    workManager.cancelUniqueWork(CALENDAR_WORK_NAME)
                    val request = PeriodicWorkRequestBuilder<ScanReminderWorker>(INTERVAL_DAYS, TimeUnit.DAYS)
                        // Without this WorkManager runs periodic work at the start of the
                        // first interval — i.e. seconds after the user opts in.
                        .setInitialDelay(INTERVAL_DAYS, TimeUnit.DAYS)
                        .build()
                    // UPDATE, not REPLACE: re-selecting the same option must not restart
                    // the countdown and re-fire the reminder.
                    workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
                }
                CADENCE_MONTHLY -> {
                    workManager.cancelUniqueWork(WORK_NAME)
                    enqueueCalendarCadence(workManager, ExistingWorkPolicy.REPLACE)
                }
                // "off", and any value an older build may have persisted.
                else -> cancel(context)
            }
        }

        /** Cancels both cadences — used for "off" and for the delete-all-data wipe. */
        fun cancel(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME)
            workManager.cancelUniqueWork(CALENDAR_WORK_NAME)
        }

        private fun enqueueCalendarCadence(workManager: WorkManager, policy: ExistingWorkPolicy) {
            val request = OneTimeWorkRequestBuilder<ScanReminderWorker>()
                .setInitialDelay(minutesUntilNextMonthStart(LocalDateTime.now()), TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniqueWork(CALENDAR_WORK_NAME, policy, request)
        }

        /** Minutes from [now] to [REMINDER_HOUR] on the 1st of the following month. */
        internal fun minutesUntilNextMonthStart(now: LocalDateTime): Long {
            val next = YearMonth.from(now).plusMonths(1).atDay(1).atTime(REMINDER_HOUR, 0)
            return ChronoUnit.MINUTES.between(now, next)
        }
    }
}
