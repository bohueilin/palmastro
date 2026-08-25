package com.palmastro.app.share

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

sealed class ModelSource {
    data class Asset(val path: String) : ModelSource()
    data class FileSystem(val path: String) : ModelSource()
    data object NotAvailable : ModelSource()
}

/**
 * Typed scan-pipeline error surfaced to the UI. The ViewModel exposes ONLY these
 * (never raw exception messages); the scan screen maps each case to a string
 * resource (see strings_scan_errors.xml) via [key].
 */
sealed class ScanError(val key: String, val retryable: Boolean) {
    /** Model could not be downloaded (offline, server error, timeout). */
    data object MODEL_DOWNLOAD_FAILED : ScanError("scan_error_model_download_failed", true)

    /** Model file failed checksum verification or the landmarker refused to load it. */
    data object MODEL_CORRUPT : ScanError("scan_error_model_corrupt", true)

    /** Capture/inference pipeline failed after the model was ready. */
    data object PROCESSING_FAILED : ScanError("scan_error_processing_failed", true)
}

/** Carrier so [Result]-based APIs can transport a typed [ScanError]. */
class ScanErrorException(val scanError: ScanError, message: String) : Exception(message)

object ModelManager {
    /**
     * Pinned MediaPipe hand_landmarker (0.10.9 era) float16 model, asset version 1.
     * NEVER point this at ".../latest/..." — the checksum below is for this exact asset.
     */
    private const val MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"

    /**
     * SHA-256 of the pinned asset above, computed 2026-07-09 from the canonical URL
     * (7,819,105 bytes). Downloaded files failing this check are deleted (MODEL_CORRUPT).
     */
    internal const val EXPECTED_SHA256 =
        "fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1"

    private const val ASSET_PATH = "models/hand_landmarker.task"
    private const val MODEL_DIR = "models"
    private const val MODEL_FILE = "hand_landmarker.task"
    private const val MIN_SIZE_BYTES = 1_000_000L
    private const val MAX_RETRIES = 3

    /** 64 KB: ~120 progress callbacks across the pinned model, smooth without churn. */
    private const val PROGRESS_CHUNK_BYTES = 64 * 1024

    private val downloading = AtomicBoolean(false)

    fun isModelReady(context: Context): Boolean {
        return hasAssetModel(context) ||
            getModelFile(context).let { it.exists() && it.length() > MIN_SIZE_BYTES }
    }

    fun getModelSource(context: Context): ModelSource {
        if (hasAssetModel(context)) return ModelSource.Asset(ASSET_PATH)
        val file = getModelFile(context)
        if (file.exists() && file.length() > MIN_SIZE_BYTES) return ModelSource.FileSystem(file.absolutePath)
        return ModelSource.NotAvailable
    }

    fun getModelPath(context: Context): String = getModelFile(context).absolutePath

    /**
     * Fetches the pinned model. [onProgress] reports (bytesRead, totalBytes) as the body
     * streams in so the UI can show a real bar instead of an open-ended spinner; totalBytes
     * is 0 when the server sends no Content-Length, and each retry restarts from 0.
     */
    fun downloadModel(context: Context, onProgress: (Long, Long) -> Unit): Result<File> {
        if (!downloading.compareAndSet(false, true)) {
            return Result.failure(
                ScanErrorException(ScanError.MODEL_DOWNLOAD_FAILED, "Download already in progress")
            )
        }
        return try {
            downloadWithRetry(context, onProgress)
        } finally {
            downloading.set(false)
        }
    }

    /**
     * Verifies the downloaded model against [EXPECTED_SHA256]. Bundled asset models are
     * trusted (they ship inside the signed APK). On mismatch the corrupt file is deleted
     * so the next [downloadModel] starts clean.
     */
    fun verifyModelIntegrity(context: Context): Boolean {
        if (hasAssetModel(context)) return true
        val file = getModelFile(context)
        if (!file.exists()) return false
        val ok = sha256(file).equals(EXPECTED_SHA256, ignoreCase = true)
        if (!ok) file.delete()
        return ok
    }

    /** Removes the downloaded model file (corrupt-model recovery path). */
    fun deleteDownloadedModel(context: Context) {
        val dir = File(context.filesDir, MODEL_DIR)
        File(dir, MODEL_FILE).delete()
        File(dir, "$MODEL_FILE.tmp").delete()
    }

    private fun hasAssetModel(context: Context): Boolean {
        return try {
            context.assets.open(ASSET_PATH).use { true }
        } catch (_: Exception) {
            false
        }
    }

    private fun downloadWithRetry(context: Context, onProgress: (Long, Long) -> Unit): Result<File> {
        var lastError: Throwable? = null
        for (attempt in 1..MAX_RETRIES) {
            val result = attemptDownload(context, onProgress)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
            // A checksum mismatch is not transient network flakiness; retrying the same
            // bytes is still worthwhile once (CDN edge corruption), so keep the loop,
            // but preserve the typed error for the caller.
            if (attempt < MAX_RETRIES) {
                Thread.sleep(1000L * (1 shl (attempt - 1)))
            }
        }
        return Result.failure(
            when (lastError) {
                is ScanErrorException -> lastError
                else -> ScanErrorException(
                    ScanError.MODEL_DOWNLOAD_FAILED,
                    lastError?.message ?: "Download failed after $MAX_RETRIES attempts"
                )
            }
        )
    }

    private fun attemptDownload(context: Context, onProgress: (Long, Long) -> Unit): Result<File> = runCatching {
        val dir = File(context.filesDir, MODEL_DIR)
        dir.mkdirs()
        val target = File(dir, MODEL_FILE)
        val tmp = File(dir, "$MODEL_FILE.tmp")
        val lock = File(dir, "$MODEL_FILE.lock")

        if (lock.exists() && System.currentTimeMillis() - lock.lastModified() < 120_000) {
            throw ScanErrorException(ScanError.MODEL_DOWNLOAD_FAILED, "Another download in progress")
        }
        lock.createNewFile()

        try {
            streamInto(tmp, onProgress)
            rejectIfUntrustworthy(tmp)
            promoteIntoPlace(tmp, target)
        } finally {
            lock.delete()
        }
    }

    private fun streamInto(tmp: File, onProgress: (Long, Long) -> Unit) {
        val url = URL(MODEL_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000

        try {
            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw ScanErrorException(
                    ScanError.MODEL_DOWNLOAD_FAILED, "HTTP $responseCode from model server"
                )
            }
            // Reported from zero at the start of every attempt so a retry restarts the
            // bar instead of appearing to jump backwards.
            val totalBytes = conn.contentLengthLong.coerceAtLeast(0L)
            onProgress(0L, totalBytes)
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    copyReportingProgress(input, output, totalBytes, onProgress)
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun copyReportingProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (Long, Long) -> Unit,
    ) {
        val buffer = ByteArray(PROGRESS_CHUNK_BYTES)
        var downloaded = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            downloaded += read
            onProgress(downloaded, totalBytes)
        }
    }

    /**
     * A truncated or tampered body must never reach the landmarker, so [tmp] is deleted
     * on failure — the next attempt then starts from a clean slate rather than resuming
     * bytes that already failed.
     */
    private fun rejectIfUntrustworthy(tmp: File) {
        if (tmp.length() < MIN_SIZE_BYTES) {
            val size = tmp.length()
            tmp.delete()
            throw ScanErrorException(
                ScanError.MODEL_DOWNLOAD_FAILED, "Downloaded file too small: $size bytes"
            )
        }

        val actual = sha256(tmp)
        if (!actual.equals(EXPECTED_SHA256, ignoreCase = true)) {
            tmp.delete()
            throw ScanErrorException(
                ScanError.MODEL_CORRUPT,
                "SHA-256 mismatch: expected=$EXPECTED_SHA256 actual=$actual"
            )
        }
    }

    /** Last step, so a reader of [getModelSource] only ever sees a fully verified file. */
    private fun promoteIntoPlace(tmp: File, target: File): File {
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            throw ScanErrorException(ScanError.MODEL_DOWNLOAD_FAILED, "Failed to move model into place")
        }
        return target
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getModelFile(context: Context): File =
        File(File(context.filesDir, MODEL_DIR), MODEL_FILE)
}
