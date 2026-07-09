package com.palmastro.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun share(context: Context, bitmap: Bitmap, textFallback: String) {
        val shareDir = File(context.cacheDir, "share")
        shareDir.mkdirs()
        val file = File(shareDir, "palmastro_share.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, textFallback)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share PalmAstro"))
    }

    fun buildSummaryText(
        monthKey: String,
        grade: String,
        confidence: String,
        domains: List<ShareCardRenderer.DomainScore>,
    ): String {
        val gradeNamesZh = mapOf(
            "Growing" to "Growing", "Stable" to "Stable", "Building" to "Building", "Watchout" to "Watch Out",
        )
        val domainLine = domains.joinToString("  ") { "${it.displayName}：${it.score}" }
        return """
            |PalmAstro — $monthKey Monthly Report
            |Grade: ${gradeNamesZh[grade] ?: grade}
            |$domainLine
            |Confidence: $confidence
        """.trimMargin()
    }

    fun buildDomainText(
        displayName: String,
        score: Int,
        grade: String,
        interpretation: String,
        actionToday: String,
    ): String {
        val gradeNamesZh = mapOf(
            "Growing" to "Growing", "Stable" to "Stable", "Building" to "Building", "Watchout" to "Watch Out",
        )
        val interpTruncated = if (interpretation.length > 100) interpretation.take(100) + "…" else interpretation
        val actionTruncated = if (actionToday.length > 80) actionToday.take(80) + "…" else actionToday
        return """
            |PalmAstro — ${displayName}Analysis
            |Score: $score（${gradeNamesZh[grade] ?: grade}）
            |Analysis：$interpTruncated
            |Action: $actionTruncated
        """.trimMargin()
    }
}
