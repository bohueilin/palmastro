package com.palmastro.app.security

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShareAuditLoggerTest {
    @TempDir
    lateinit var tempDir: File

    private val context = mockk<Context>()
    private lateinit var logger: ShareAuditLogger

    @BeforeEach
    fun setUp() {
        every { context.filesDir } returns tempDir
        logger = ShareAuditLogger(context)
    }

    @Test
    fun `logShare creates audit entry`() {
        logger.logShare(ShareAuditLogger.ShareType.SUMMARY_CARD, "2026-03")
        val entries = logger.getRecentEntries()
        assertEquals(1, entries.size)
        assertTrue(entries[0].contains("SUMMARY_CARD"))
        assertTrue(entries[0].contains("month=2026-03"))
    }

    @Test
    fun `logShare with domain includes domain in entry`() {
        logger.logShare(ShareAuditLogger.ShareType.DOMAIN_DETAIL, "2026-03", "career")
        val entries = logger.getRecentEntries()
        assertTrue(entries[0].contains("domain=career"))
    }

    @Test
    fun `getRecentEntries returns empty when no log file`() {
        val entries = logger.getRecentEntries()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun `clearLog removes all entries`() {
        logger.logShare(ShareAuditLogger.ShareType.SUMMARY_CARD, "2026-03")
        logger.logShare(ShareAuditLogger.ShareType.DOMAIN_DETAIL, "2026-03", "career")
        logger.clearLog()
        assertTrue(logger.getRecentEntries().isEmpty())
    }

    @Test
    fun `getRecentEntries respects limit`() {
        repeat(10) { logger.logShare(ShareAuditLogger.ShareType.SUMMARY_CARD, "2026-03") }
        val entries = logger.getRecentEntries(limit = 3)
        assertEquals(3, entries.size)
    }
}
