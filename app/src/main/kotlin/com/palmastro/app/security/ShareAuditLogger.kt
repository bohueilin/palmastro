package com.palmastro.app.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareAuditLogger @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val logFile: File get() = File(appContext.filesDir, "share_audit.log")
    private val formatter = DateTimeFormatter.ISO_INSTANT

    fun logShare(shareType: ShareType, monthKey: String, domain: String? = null) {
        val entry = buildString {
            append(formatter.format(Instant.now()))
            append("|")
            append(shareType.name)
            append("|month=")
            append(monthKey)
            if (domain != null) {
                append("|domain=")
                append(domain)
            }
            append("\n")
        }
        logFile.appendText(entry)

        trimLogIfNeeded()
    }

    fun getRecentEntries(limit: Int = 50): List<String> {
        if (!logFile.exists()) return emptyList()
        return logFile.readLines().takeLast(limit)
    }

    fun clearLog() {
        if (logFile.exists()) logFile.delete()
    }

    private fun trimLogIfNeeded() {
        if (!logFile.exists()) return
        val lines = logFile.readLines()
        if (lines.size > 500) {
            logFile.writeText(lines.takeLast(200).joinToString("\n") + "\n")
        }
    }

    enum class ShareType {
        SUMMARY_CARD,
        DOMAIN_DETAIL,
    }
}
