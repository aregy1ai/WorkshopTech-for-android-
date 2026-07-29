package com.workshoptech.integration

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.workshoptech.domain.model.DamageReport
import com.workshoptech.ml.DamageReportGenerator

object WhatsAppManager {
    private val templates = mapOf(
        "delivery" to "عزيزي %1$s،\nسيارتك %2$s جاهزة للتسليم.\nالتكلفة: %3$.0f د.ل\n\nورشة تك - AR-EGY",
        "delay" to "عزيزي %1$s،\nنعتذر عن التأخير. سيارة %2$s تحتاج %3$s إضافية.\n\nورشة تك - AR-EGY",
        "ready" to "عزيزي %1$s،\nسيارتك %2$s في المراحل النهائية.\nالوقت المتوقع: %3$s\n\nورشة تك - AR-EGY",
        "inspection" to "عزيزي %1$s،\nنتائج فحص سيارتك %2$s:\n%3$s\n\nورشة تك - AR-EGY"
    )

    fun sendMessage(context: Context, phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun sendTemplate(context: Context, templateKey: String, phoneNumber: String, vararg args: String): Boolean {
        val template = templates[templateKey] ?: return false
        val message = String.format(template, *args)
        return sendMessage(context, phoneNumber, message)
    }

    fun sendDeliveryNotification(context: Context, customerName: String, phoneNumber: String, vehicle: String, cost: Double): Boolean {
        return sendTemplate(context, "delivery", phoneNumber, customerName, vehicle, cost.toString())
    }

    fun sendDelayNotification(context: Context, customerName: String, phoneNumber: String, vehicle: String, reason: String): Boolean {
        return sendTemplate(context, "delay", phoneNumber, customerName, vehicle, reason)
    }

    fun sendInspectionReport(context: Context, customerName: String, phoneNumber: String, vehicle: String, report: DamageReport, cost: Double): Boolean {
        val summary = DamageReportGenerator.generateReport(report).take(200)
        return sendTemplate(context, "inspection", phoneNumber, customerName, vehicle, summary)
    }

    fun shareImage(context: Context, phoneNumber: String, imageUri: Uri, caption: String = ""): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                setPackage("com.whatsapp")
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }
}
