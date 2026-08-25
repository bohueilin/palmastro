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
}
