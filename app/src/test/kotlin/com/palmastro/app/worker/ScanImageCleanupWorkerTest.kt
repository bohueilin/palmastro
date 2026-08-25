package com.palmastro.app.worker

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the retention constant to the window disclosed in the privacy policy and the
 * Play permissions doc. Files expire only on a cleanup run, so the constant must sit
 * far enough inside 24h that scheduler deferral cannot push a deletion past it.
 */
class ScanImageCleanupWorkerTest {

    private val hourMs = 60 * 60 * 1000L

    @Test
    fun `retention window leaves margin below the disclosed 24 hours`() {
        val disclosedMs = ScanImageCleanupWorker.DISCLOSED_WINDOW_HOURS * hourMs
        assertTrue(ScanImageCleanupWorker.RETENTION_MS < disclosedMs)
        // At least one full cleanup interval of slack, so a deferred run still deletes
        // inside the disclosed window.
        assertTrue(disclosedMs - ScanImageCleanupWorker.RETENTION_MS >= 3 * hourMs)
    }

    @Test
    fun `a file older than the retention window is past the cutoff`() {
        val now = 1_700_000_000_000L
        val cutoff = now - ScanImageCleanupWorker.RETENTION_MS
        assertTrue(now - 21 * hourMs < cutoff)
        assertFalse(now - 19 * hourMs < cutoff)
    }
}
