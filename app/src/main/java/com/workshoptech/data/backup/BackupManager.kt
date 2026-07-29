package com.workshoptech.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.workshoptech.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

data class BackupData(val version: Int = 1, val timestamp: Long = System.currentTimeMillis(), val cases: List<CaseEntity> = emptyList(), val customers: List<CustomerEntity> = emptyList(), val photos: List<CasePhotoEntity> = emptyList(), val inspections: List<InspectionEntity> = emptyList(), val technicians: List<TechnicianEntity> = emptyList(), val damageFindings: List<DamageFindingEntity> = emptyList(), val inventory: List<InventoryEntity> = emptyList())
data class BackupInfo(val fileName: String, val sizeBytes: Long, val timestamp: Long, val caseCount: Int, val customerCount: Int)

object BackupManager {
    private const val BACKUP_DIR = "backups"
    private const val ENCRYPTION_KEY = "WorkshopTech2024!"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    suspend fun exportFull(context: Context, cases: List<CaseEntity>, customers: List<CustomerEntity>, photos: List<CasePhotoEntity>, inspections: List<InspectionEntity>, technicians: List<TechnicianEntity>, damageFindings: List<DamageFindingEntity>, inventory: List<InventoryEntity>): File = withContext(Dispatchers.IO) {
        val backupData = BackupData(cases = cases, customers = customers, photos = photos, inspections = inspections, technicians = technicians, damageFindings = damageFindings, inventory = inventory)
        val json = gson.toJson(backupData)
        val backupDir = File(context.filesDir, BACKUP_DIR).also { if (!it.exists()) it.mkdirs() }
        val file = File(backupDir, "workshop_backup_${dateFormat.format(Date())}.json.enc")
        encryptAndSave(json, file)
        file
    }

    suspend fun importFromJson(context: Context, uri: Uri): BackupData? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val encryptedBytes = inputStream.readBytes(); inputStream.close()
            gson.fromJson(decrypt(encryptedBytes), BackupData::class.java)
        } catch (e: Exception) { null }
    }

    fun listBackups(context: Context): List<BackupInfo> {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles()?.filter { it.name.endsWith(".json.enc") }?.sortedByDescending { it.lastModified() }?.map { BackupInfo(it.name, it.length(), it.lastModified(), 0, 0) } ?: emptyList()
    }

    fun cleanupOldBackups(context: Context, keepCount: Int = 5) {
        val backups = listBackups(context)
        if (backups.size > keepCount) { val backupDir = File(context.filesDir, BACKUP_DIR); backups.drop(keepCount).forEach { File(backupDir, it.fileName).delete() } }
    }

    fun getShareUri(context: Context, fileName: String): Uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(context.filesDir, "$BACKUP_DIR/$fileName"))
    fun formatSize(bytes: Long): String = when { bytes < 1024 -> "$bytes B"; bytes < 1024 * 1024 -> "${bytes / 1024} KB"; else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB" }
    fun formatDate(timestamp: Long): String = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun encryptAndSave(data: String, file: File) {
        val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES").apply { init(Cipher.ENCRYPT_MODE, key) }
        FileOutputStream(file).use { CipherOutputStream(it, cipher).use { cos -> cos.write(data.toByteArray()) } }
    }

    private fun decrypt(encryptedBytes: ByteArray): String {
        val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES").apply { init(Cipher.DECRYPT_MODE, key) }
        return String(cipher.doFinal(encryptedBytes))
    }
}
