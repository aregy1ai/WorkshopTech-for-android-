package com.workshoptech

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.workshoptech.util.FileManager
import com.workshoptech.workers.SyncWorker

class WorkshopTechApp : Application() {

    lateinit var container: AppContainer
        private set

    var currentCountry: String = "LY"
        private set
    var currentCurrency: String = "LYD"
        private set
    var isDarkMode: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        _instance = this
        container = AppContainer(this)
        createNotificationChannels()
        loadSettings()
        SyncWorker.schedule(this)
        FileManager.cleanupTempFiles(this)
    }

    // ─── Settings ────────────────────────────────────────────────────────────
    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentCountry  = prefs.getString(KEY_COUNTRY, "LY")  ?: "LY"
        currentCurrency = prefs.getString(KEY_CURRENCY, "LYD") ?: "LYD"
        isDarkMode      = prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun saveSettings(
        country:  String?  = null,
        currency: String?  = null,
        darkMode: Boolean? = null
    ) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().apply {
            country?.let  { putString(KEY_COUNTRY, it);  currentCountry = it }
            currency?.let { putString(KEY_CURRENCY, it); currentCurrency = it }
            darkMode?.let { putBoolean(KEY_DARK_MODE, it); isDarkMode = it }
            apply()
        }
    }

    // ─── Notification channels ───────────────────────────────────────────────
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(CHANNEL_TASKS,       getString(R.string.notif_channel_tasks),       NotificationManager.IMPORTANCE_HIGH)
                .apply { description = getString(R.string.notif_channel_tasks_desc) },
            NotificationChannel(CHANNEL_INSPECTIONS, getString(R.string.notif_channel_inspections), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = getString(R.string.notif_channel_inspections_desc) },
            NotificationChannel(CHANNEL_SYSTEM,      getString(R.string.notif_channel_system),      NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.notif_channel_system_desc) }
        ).forEach(manager::createNotificationChannel)
    }

    companion object {
        private const val PREFS_NAME   = "workshop_tech_prefs"
        private const val KEY_COUNTRY  = "country"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_DARK_MODE = "dark_mode"

        const val CHANNEL_TASKS       = "channel_tasks"
        const val CHANNEL_INSPECTIONS = "channel_inspections"
        const val CHANNEL_SYSTEM      = "channel_system"

        @Volatile private var _instance: WorkshopTechApp? = null
        fun get(): WorkshopTechApp =
            _instance ?: error("WorkshopTechApp not initialised")
    }
}
