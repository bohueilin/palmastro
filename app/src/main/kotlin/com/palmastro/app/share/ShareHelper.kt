package com.palmastro.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Fires the system share sheet for a rendered card. All user-visible text (chooser
 * title, fallback text) is resolved from string resources by the caller so the share
 * flow follows the app locale.
 */
object ShareHelper {

    fun share(context: Context, bitmap: Bitmap, textFallback: String, chooserTitle: String) {
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

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    /** Joins pre-localized lines into the text fallback shared next to the card image. */
    fun buildShareText(vararg lines: String): String =
        lines.filter { it.isNotBlank() }.joinToString("\n")

    fun truncate(text: String, maxChars: Int): String =
        if (text.length > maxChars) text.take(maxChars) + "…" else text
}
