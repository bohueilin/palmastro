package com.palmastro.app.share

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

sealed class ModelSource {
    data class Asset(val path: String) : ModelSource()
    data class FileSystem(val path: String) : ModelSource()
    data object NotAvailable : ModelSource()
}

object ModelManager {
    private const val MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task"
    private const val ASSET_PATH = "models/hand_landmarker.task"
    private const val MODEL_DIR = "models"
    private const val MODEL_FILE = "hand_landmarker.task"
    private const val MIN_SIZE_BYTES = 1_000_000L
    private const val MAX_RETRIES = 3

    private val EXPECTED_SHA256: String? = null

    private val downloading = AtomicBoolean(false)

    fun isModelReady(context: Context): Boolean {
        return hasAssetModel(context) || getModelFile(context).let { it.exists() && it.length() > MIN_SIZE_BYTES }
    }

    fun getModelSource(context: Context): ModelSource {
        if (hasAssetModel(context)) return ModelSource.Asset(ASSET_PATH)
        val file = getModelFile(context)
        if (file.exists() && file.length() > MIN_SIZE_BYTES) return ModelSource.FileSystem(file.absolutePath)
        return ModelSource.NotAvailable
    }

    fun getModelPath(context: Context): String = getModelFile(context).absolutePath

    fun downloadModel(context: Context): Result<File> {
        if (!downloading.compareAndSet(false, true)) {
            return Result.failure(IllegalStateException("Download already in progress"))
        }
        return try {
            downloadWithRetry(context)
        } finally {
            downloading.set(false)
        }
    }

    fun verifyModelIntegrity(context: Context): Boolean {
        val expected = EXPECTED_SHA256 ?: return true
        val file = getModelFile(context)
        if (!file.exists()) return false
        return sha256(file).equals(expected, ignoreCase = true)
    }

    private fun hasAssetModel(context: Context): Boolean {
        return try {
            context.assets.open(ASSET_PATH).use { true }
        } catch (_: Exception) {
            false
        }
    }

    private fun downloadWithRetry(context: Context): Result<File> {
        var lastError: Throwable? = null
        for (attempt in 1..MAX_RETRIES) {
            val result = attemptDownload(context)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
            if (attempt < MAX_RETRIES) {
                Thread.sleep(1000L * (1 shl (attempt - 1)))
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Download failed after $MAX_RETRIES attempts"))
    }

    private fun attemptDownload(context: Context): Result<File> = runCatching {
        val dir = File(context.filesDir, MODEL_DIR)
        dir.mkdirs()
        val target = File(dir, MODEL_FILE)
        val tmp = File(dir, "$MODEL_FILE.tmp")
        val lock = File(dir, "$MODEL_FILE.lock")

        if (lock.exists() && System.currentTimeMillis() - lock.lastModified() < 120_000) {
            error("Another process is downloading the model")
        }
        lock.createNewFile()

        try {
            val url = URL(MODEL_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000

            try {
                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    error("HTTP $responseCode from model server")
                }
                conn.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                conn.disconnect()
            }

            if (tmp.length() < MIN_SIZE_BYTES) {
                tmp.delete()
                error("Downloaded file too small: ${tmp.length()} bytes")
            }

            EXPECTED_SHA256?.let { expected ->
                val actual = sha256(tmp)
                if (!actual.equals(expected, ignoreCase = true)) {
                    tmp.delete()
                    error("SHA-256 mismatch: expected=$expected actual=$actual")
                }
            }

            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                error("Failed to rename tmp file to target")
            }

            target
        } finally {
            lock.delete()
        }
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
