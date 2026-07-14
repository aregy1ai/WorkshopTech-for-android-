package com.workshoptech

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.workshoptech.security.EncryptedPreferencesManager
import com.workshoptech.security.PrefKey
import com.workshoptech.util.FileManager
import com.workshoptech.workers.SyncWorker

/**
 * Application entry-point.
 *
 * Security: settings now persisted in EncryptedSharedPreferences (AES-256-GCM).
 * Performance: AppContainer (DB + repository) is created lazily on first access.
 */
class WorkshopTechApp : Application() {

    // ── Lazy singletons ───────────────────────────────────────────────────────
    val container: AppContainer by lazy { AppContainer(this) }

    // ── Settings (encrypted) ──────────────────────────────────────────────────
    private lateinit var prefs: EncryptedPreferencesManager

    var currentCountry: String  = "LY"; private set
    var currentCurrency: String = "LYD"; private set
    var isDarkMode: Boolean     = false; private set
    var isOnboardingDone: Boolean = false; private set

    override fun onCreate() {
        super.onCreate()
        _instance = this
        prefs = EncryptedPreferencesManager.getInstance(this)
        loadSettings()
        createNotificationChannels()
        SyncWorker.schedule(this)
        FileManager.cleanupTempFiles(this)
    }

    // ── Settings ──────────────────────────────────────────────────────────────
    private fun loadSettings() {
        currentCountry    = prefs.getString(PrefKey.COUNTRY,  "LY")
        currentCurrency   = prefs.getString(PrefKey.CURRENCY, "LYD")
        isDarkMode        = prefs.getBoolean(PrefKey.DARK_MODE, false)
        isOnboardingDone  = prefs.getBoolean(PrefKey.ONBOARDING, false)
    }

    fun saveSettings(
        country:  String?  = null,
        currency: String?  = null,
        darkMode: Boolean? = null,
        onboardingDone: Boolean? = null
    ) {
        country?.let        { prefs.putString(PrefKey.COUNTRY,   it); currentCountry  = it }
        currency?.let       { prefs.putString(PrefKey.CURRENCY,  it); currentCurrency = it }
        darkMode?.let       { prefs.putBoolean(PrefKey.DARK_MODE, it); isDarkMode     = it }
        onboardingDone?.let { prefs.putBoolean(PrefKey.ONBOARDING, it); isOnboardingDone = it }
    }

    // ── Notification channels ─────────────────────────────────────────────────
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        listOf(
            NotificationChannel(
                CHANNEL_TASKS,
                getString(R.string.notif_channel_tasks),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.notif_channel_tasks_desc) },

            NotificationChannel(
                CHANNEL_INSPECTIONS,
                getString(R.string.notif_channel_inspections),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.notif_channel_inspections_desc) },

            NotificationChannel(
                CHANNEL_SYSTEM,
                getString(R.string.notif_channel_system),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notif_channel_system_desc) }
        ).forEach(manager::createNotificationChannel)
    }

    // ── Companion ─────────────────────────────────────────────────────────────
    companion object {
        const val CHANNEL_TASKS       = "channel_tasks"
        const val CHANNEL_INSPECTIONS = "channel_inspections"
        const val CHANNEL_SYSTEM      = "channel_system"

        @Volatile private var _instance: WorkshopTechApp? = null
        fun get(): WorkshopTechApp =
            _instance ?: error("WorkshopTechApp not initialised — onCreate() not yet called")
    }
}
