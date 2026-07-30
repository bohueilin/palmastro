package com.palmastro.app.security

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {
    private const val TAG = "DatabaseKeyManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "palmastro_db_key"
    private const val PREFS_NAME = "palmastro_db_prefs"
    private const val PREFS_KEY_ENCRYPTED = "encrypted_db_key"
    private const val PREFS_KEY_IV = "encrypted_db_iv"
    private const val DATABASE_NAME = "palmastro.db"

    fun getOrCreateDatabaseKey(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingEncrypted = prefs.getString(PREFS_KEY_ENCRYPTED, null)
        val existingIv = prefs.getString(PREFS_KEY_IV, null)

        if (existingEncrypted != null && existingIv != null) {
            try {
                return decryptKey(
                    Base64.decode(existingEncrypted, Base64.NO_WRAP),
                    Base64.decode(existingIv, Base64.NO_WRAP),
                )
            } catch (e: Exception) {
                // Keystore key lost (backup restore / OS keystore quirk): the wrapped DB key
                // can never be unwrapped again, and retrying at every launch is a permanent
                // startup crash loop. Documented fresh-start recovery: the encrypted DB is
                // unreadable without this key anyway, so wipe it and regenerate from scratch.
                Log.w(TAG, "DB key unwrap failed; resetting encrypted storage for a fresh start", e)
                resetForFreshStart(context, prefs)
            }
        }

        val dbKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val (encrypted, iv) = encryptKey(dbKey)
        prefs.edit()
            .putString(PREFS_KEY_ENCRYPTED, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(PREFS_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        return dbKey
    }

    /**
     * Fresh-start recovery when the wrapped DB key is unrecoverable: drop the stale
     * wrapped key, the (possibly broken) Keystore entry, and the now-undecryptable
     * database so the caller can regenerate everything. Each step is best-effort —
     * a partial reset self-heals on the next launch via the same path.
     */
    private fun resetForFreshStart(context: Context, prefs: SharedPreferences) {
        prefs.edit().clear().apply()
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEYSTORE_ALIAS)
        }
        runCatching {
            // Removes the DB plus its -wal/-shm journals.
            SQLiteDatabase.deleteDatabase(context.getDatabasePath(DATABASE_NAME))
        }
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encryptKey(plainKey: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        return cipher.doFinal(plainKey) to cipher.iv
    }

    private fun decryptKey(encryptedKey: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encryptedKey)
    }
}
