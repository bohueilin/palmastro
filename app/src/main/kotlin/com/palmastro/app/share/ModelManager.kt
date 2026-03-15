package com.palmastro.app.share

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ModelManager {

    private const val MODEL_URL =
        "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task"
    private const val MODEL_DIR = "models"
    private const val MODEL_FILE = "hand_landmarker.task"
    private const val MIN_SIZE_BYTES = 1_000_000L // 1 MB sanity check

    fun isModelReady(context: Context): Boolean {
        val file = getModelFile(context)
        return file.exists() && file.length() > MIN_SIZE_BYTES
    }

    fun getModelPath(context: Context): String = getModelFile(context).absolutePath

    fun downloadModel(context: Context): Result<File> = runCatching {
        val dir = File(context.filesDir, MODEL_DIR)
        dir.mkdirs()
        val target = File(dir, MODEL_FILE)
        val tmp = File(dir, "$MODEL_FILE.tmp")

        val url = URL(MODEL_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000

        try {
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

        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            error("Failed to rename tmp file to target")
        }

        target
    }

    private fun getModelFile(context: Context): File =
        File(File(context.filesDir, MODEL_DIR), MODEL_FILE)
}
