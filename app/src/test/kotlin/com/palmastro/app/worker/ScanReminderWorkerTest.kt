package com.palmastro.app.worker

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the "1st of each month" cadence: a fixed repeat interval drifts off the 1st
 * within two cycles, so the delay is recomputed from the calendar on every run.
 */
class ScanReminderWorkerTest {

    private fun minutesUntil(now: LocalDateTime) = ScanReminderWorker.minutesUntilNextMonthStart(now)

    @Test
    fun `lands on the 1st of the following month`() {
        val now = LocalDateTime.of(2026, 1, 1, 10, 0)
        assertEquals(31 * 24 * 60L, minutesUntil(now))
    }

    @Test
    fun `short months do not drift the target off the 1st`() {
        // A 30-day period would put the February run on 2 March; the calendar one does not.
        val now = LocalDateTime.of(2026, 2, 1, 10, 0)
        assertEquals(28 * 24 * 60L, minutesUntil(now))
    }

    @Test
    fun `rolls over the year boundary`() {
        val now = LocalDateTime.of(2026, 12, 31, 23, 0)
        assertEquals(11 * 60L, minutesUntil(now))
    }

    @Test
    fun `is always in the future`() {
        val lateInTheMonth = LocalDateTime.of(2026, 3, 31, 23, 59)
        assertTrue(minutesUntil(lateInTheMonth) > 0)
    }

    @Test
    fun `keeps this month's occurrence while it is still ahead`() {
        // A re-schedule at 09:00 on the 1st must still fire at 10:00 that same day —
        // jumping straight to next month silently drops a month's reminder.
        val beforeTodaysReminder = LocalDateTime.of(2026, 3, 1, 9, 0)
        assertEquals(60L, minutesUntil(beforeTodaysReminder))
    }

    @Test
    fun `moves to the following month once this month's occurrence has passed`() {
        val afterTodaysReminder = LocalDateTime.of(2026, 3, 1, 10, 1)
        assertEquals(31 * 24 * 60L - 1, minutesUntil(afterTodaysReminder))
    }

    @Test
    fun `the reminder hour itself counts as passed, not as still ahead`() {
        // Exactly on the hour is when the worker itself re-arms; treating it as "ahead"
        // would queue a second run for the same minute.
        val onTheHour = LocalDateTime.of(2026, 3, 1, 10, 0)
        assertEquals(31 * 24 * 60L, minutesUntil(onTheHour))
    }
}
