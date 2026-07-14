package com.workshoptech.ml

import com.workshoptech.data.entity.DamageFindingEntity
import com.workshoptech.data.entity.DamageSeverity
import com.workshoptech.data.entity.DamageType

/**
 * Rule-based recommendation engine.
 *
 * Converts [DamageFindingEntity] list → repair task recommendations
 * with estimated cost ranges and priority levels.
 *
 * Designed for 22 Arab country markets with LYD as default currency.
 * Currency is customisable via [currency] parameter.
 */
class RecommendationEngine(private val currency: String = "LYD") {

    data class Recommendation(
        val repairType: String,
        val descriptionAr: String,
        val estimatedMinCost: Double,
        val estimatedMaxCost: Double,
        val estimatedHours: Float,
        val priority: String,
        val affectedParts: List<String>
    ) {
        val costRangeDisplay: String get() =
            "${"%.0f".format(estimatedMinCost)}–${"%.0f".format(estimatedMaxCost)} $currency"
    }

    /**
     * Generate repair recommendations from AI damage findings.
     */
    fun recommend(findings: List<DamageFindingEntity>): List<Recommendation> {
        if (findings.isEmpty()) return emptyList()

        // Group findings by type
        val byType = findings.groupBy { it.damageType }
        val recs   = mutableListOf<Recommendation>()

        byType.forEach { (type, group) ->
            val maxSeverity = group.maxOf { severityWeight(it.severity) }
            val parts       = group.mapNotNull { it.affectedPart }.distinct()
            recs += buildRecommendation(type, maxSeverity, parts)
        }

        return recs.sortedByDescending { priorityWeight(it.priority) }
    }

    /**
     * Estimate total job cost from a list of recommendations.
     */
    fun estimateTotalCost(recs: List<Recommendation>): Pair<Double, Double> {
        val min = recs.sumOf { it.estimatedMinCost }
        val max = recs.sumOf { it.estimatedMaxCost }
        return min to max
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private fun buildRecommendation(
        type: String,
        maxSeverity: Int,
        parts: List<String>
    ): Recommendation {
        val severity = when {
            maxSeverity >= 3 -> DamageSeverity.HIGH
            maxSeverity == 2 -> DamageSeverity.MEDIUM
            else             -> DamageSeverity.LOW
        }

        return when (type) {
            DamageType.SCRATCH -> Recommendation(
                repairType       = "PAINT_TOUCH_UP",
                descriptionAr    = "إصلاح الخدوش وإعادة الطلاء",
                estimatedMinCost = if (severity == DamageSeverity.HIGH) 200.0 else 50.0,
                estimatedMaxCost = if (severity == DamageSeverity.HIGH) 500.0 else 150.0,
                estimatedHours   = if (severity == DamageSeverity.HIGH) 4f    else 1.5f,
                priority         = if (severity == DamageSeverity.HIGH) "HIGH" else "MEDIUM",
                affectedParts    = parts
            )
            DamageType.DENT -> Recommendation(
                repairType       = "BODY_REPAIR",
                descriptionAr    = "إصلاح البعجة وتقويم الهيكل",
                estimatedMinCost = if (severity == DamageSeverity.HIGH) 400.0 else 100.0,
                estimatedMaxCost = if (severity == DamageSeverity.HIGH) 1200.0 else 300.0,
                estimatedHours   = if (severity == DamageSeverity.HIGH) 8f    else 2f,
                priority         = "HIGH",
                affectedParts    = parts
            )
            DamageType.RUST -> Recommendation(
                repairType       = "RUST_TREATMENT",
                descriptionAr    = "معالجة الصدأ وحماية المعدن",
                estimatedMinCost = 150.0,
                estimatedMaxCost = 600.0,
                estimatedHours   = 5f,
                priority         = "URGENT",
                affectedParts    = parts
            )
            DamageType.CRACK -> Recommendation(
                repairType       = "PART_REPLACEMENT",
                descriptionAr    = "تغيير القطعة المتشققة",
                estimatedMinCost = 200.0,
                estimatedMaxCost = 800.0,
                estimatedHours   = 3f,
                priority         = "HIGH",
                affectedParts    = parts
            )
            DamageType.PAINT_PEEL -> Recommendation(
                repairType       = "FULL_REPAINT",
                descriptionAr    = "إعادة طلاء القطعة كاملة",
                estimatedMinCost = 100.0,
                estimatedMaxCost = 400.0,
                estimatedHours   = 6f,
                priority         = "MEDIUM",
                affectedParts    = parts
            )
            else -> Recommendation(
                repairType       = "GENERAL_REPAIR",
                descriptionAr    = "إصلاح عام",
                estimatedMinCost = 50.0,
                estimatedMaxCost = 250.0,
                estimatedHours   = 2f,
                priority         = "LOW",
                affectedParts    = parts
            )
        }
    }

    private fun severityWeight(s: String) = when (s) {
        DamageSeverity.HIGH   -> 3
        DamageSeverity.MEDIUM -> 2
        else                  -> 1
    }

    private fun priorityWeight(p: String) = when (p) {
        "URGENT" -> 4
        "HIGH"   -> 3
        "MEDIUM" -> 2
        else     -> 1
    }
}
