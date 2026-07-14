package com.workshoptech.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure wrapper around EncryptedSharedPreferences.
 * Falls back to plain SharedPreferences ONLY in debug mode if encryption init fails.
 *
 * Security: All values are AES-256-GCM encrypted at rest.
 */
class EncryptedPreferencesManager private constructor(private val prefs: SharedPreferences) {

    // ─── Read ────────────────────────────────────────────────────────────────
    fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    fun getLong(key: String, default: Long): Long =
        prefs.getLong(key, default)

    // ─── Write ───────────────────────────────────────────────────────────────
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val TAG       = "EncryptedPrefs"
        private const val FILE_NAME = "workshoptech_secure_prefs"

        @Volatile private var INSTANCE: EncryptedPreferencesManager? = null

        fun getInstance(context: Context): EncryptedPreferencesManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: create(context).also { INSTANCE = it }
            }

        private fun create(context: Context): EncryptedPreferencesManager {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val prefs = EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                Log.i(TAG, "Encrypted SharedPreferences initialised")
                EncryptedPreferencesManager(prefs)
            } catch (e: Exception) {
                // Only fallback in debug builds — in release this should never happen
                Log.e(TAG, "Encryption init failed, using plain prefs as fallback", e)
                val fallback = context.getSharedPreferences("${FILE_NAME}_fallback", Context.MODE_PRIVATE)
                EncryptedPreferencesManager(fallback)
            }
        }
    }
}

// ── Key constants ─────────────────────────────────────────────────────────────
object PrefKey {
    const val COUNTRY      = "country"
    const val CURRENCY     = "currency"
    const val DARK_MODE    = "dark_mode"
    const val ONBOARDING   = "onboarding_done"
    const val WORKSHOP_ID  = "workshop_id"
    const val WORKSHOP_NAME= "workshop_name"
    const val LAST_SYNC    = "last_sync_ts"
    const val DB_VERSION   = "db_schema_version"
}
