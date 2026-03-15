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

        context.startActivity(Intent.createChooser(intent, "分享掌紋星象"))
    }

    fun buildSummaryText(
        monthKey: String,
        grade: String,
        confidence: String,
        domains: List<ShareCardRenderer.DomainScore>,
    ): String {
        val gradeNamesZh = mapOf(
            "Growing" to "成長期", "Stable" to "穩定期", "Building" to "累積期", "Watchout" to "注意期",
        )
        val domainLine = domains.joinToString("  ") { "${it.displayName}：${it.score}" }
        return """
            |掌紋星象 — $monthKey 月度報告
            |等級：${gradeNamesZh[grade] ?: grade}
            |$domainLine
            |信心度：$confidence
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
            "Growing" to "成長期", "Stable" to "穩定期", "Building" to "累積期", "Watchout" to "注意期",
        )
        val interpTruncated = if (interpretation.length > 100) interpretation.take(100) + "…" else interpretation
        val actionTruncated = if (actionToday.length > 80) actionToday.take(80) + "…" else actionToday
        return """
            |掌紋星象 — ${displayName}分析
            |分數：$score（${gradeNamesZh[grade] ?: grade}）
            |分析：$interpTruncated
            |行動建議：$actionTruncated
        """.trimMargin()
    }
}
