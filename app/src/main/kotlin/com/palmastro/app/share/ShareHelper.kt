package com.palmastro.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Fires the system share sheet for a rendered card. All user-visible text (chooser
 * title, fallback text) is resolved from string resources by the caller so the share
 * flow follows the app locale.
 */
object ShareHelper {

    /**
     * Older renders are dropped on the next share, but not immediately: some targets
     * (messaging apps that queue uploads) read the stream lazily after the chooser
     * returns, so a file stays readable for this long before it is swept.
     */
    private const val STALE_RENDER_GRACE_MS = 10 * 60 * 1000L

    /** Bitmap.compress ignores quality for PNG; Skia picks its own deflate level. */
    private const val PNG_QUALITY_IGNORED = 100

    /**
     * Encodes the card and returns its content URI. Blocking work — call it from a
     * background dispatcher, never from a click lambda.
     */
    fun writeShareFile(context: Context, bitmap: Bitmap): Uri {
        val shareDir = File(context.cacheDir, "share")
        shareDir.mkdirs()
        val now = System.currentTimeMillis()
        // Sweep earlier renders so a previous share target's still-live URI grant cannot
        // resolve to a card the user shared later.
        shareDir.listFiles()?.forEach { if (now - it.lastModified() > STALE_RENDER_GRACE_MS) it.delete() }

        // Unique per share, for the same reason: no two cards ever share a content URI.
        val file = File(shareDir, "palmastro_share_$now.png")
        file.outputStream().buffered().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY_IGNORED, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    /** Hands the written card to the system chooser. Main thread. */
    fun startChooser(context: Context, uri: Uri, textFallback: String, chooserTitle: String) {
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
