package com.workshoptech.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.workshoptech.data.entity.TechnicianEntity

enum class TechnicianRole(val permissions: List<String>) {
    MANAGER(listOf("all")),
    TECHNICIAN(listOf("view_cases", "update_cases", "take_photos", "view_inspections")),
    ACCOUNTANT(listOf("view_cases", "view_reports", "manage_invoices"))
}

class AuthManager(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(context, "auth_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    private var currentUser: TechnicianEntity? = null

    val isLoggedIn: Boolean get() = currentUser != null
    val currentTechnician: TechnicianEntity? get() = currentUser
    val currentRole: TechnicianRole? get() = when (currentUser?.role) { "MANAGER" -> TechnicianRole.MANAGER; "TECHNICIAN" -> TechnicianRole.TECHNICIAN; "ACCOUNTANT" -> TechnicianRole.ACCOUNTANT; else -> null }

    fun login(pin: String): Boolean {
        val savedPin = prefs.getString("master_pin", "0000") ?: "0000"
        if (pin == savedPin) {
            currentUser = TechnicianEntity(technicianId = "default_admin", name = "مدير النظام", pin = pin, role = "MANAGER", phone = null, isActive = true, createdAt = System.currentTimeMillis(), lastLoginAt = System.currentTimeMillis())
            prefs.edit().putLong("last_login", System.currentTimeMillis()).apply()
            return true
        }
        return false
    }

    fun loginAs(technician: TechnicianEntity): Boolean { currentUser = technician.copy(lastLoginAt = System.currentTimeMillis()); return true }
    fun logout() { currentUser = null }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (oldPin == (prefs.getString("master_pin", "0000") ?: "0000")) { prefs.edit().putString("master_pin", newPin).apply(); return true }
        return false
    }

    fun hasPermission(permission: String): Boolean = currentRole?.permissions?.contains("all") == true || currentRole?.permissions?.contains(permission) == true
    fun isAutoLockNeeded(): Boolean = System.currentTimeMillis() - prefs.getLong("last_login", 0) > 15 * 60 * 1000
}
