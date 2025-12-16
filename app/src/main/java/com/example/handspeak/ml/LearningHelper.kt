package com.example.handspeak.ml

import android.util.Log
import kotlin.math.roundToInt

/**
 * نظام إحصائيات التعلم
 * Learning Statistics System for tracking user progress with the LSTM model
 */
data class LearningStats(
    val label: String,
    val totalAttempts: Int = 0,
    val successfulDetections: Int = 0,
    val totalFramesProcessed: Int = 0,
    val averageConfidence: Float = 0f,
    val bestConfidence: Float = 0f,
    val worstConfidence: Float = 1f
) {
    val accuracyPercentage: Float
        get() = if (totalAttempts > 0) (successfulDetections * 100f) / totalAttempts else 0f
    
    val successRate: String
        get() = String.format("%.1f%%", accuracyPercentage)
    
    override fun toString(): String {
        return "$label: $successfulDetections/$totalAttempts (${successRate})"
    }
}

/**
 * مساعد نظام التعلم التفاعلي
 * Interactive Learning Helper with feedback system
 */
class LearningHelper(
    private val classifier: SignLanguageClassifier?
) {
    private val TAG = "LearningHelper"
    private val learningStats = mutableMapOf<String, LearningStats>()
    private var isLearningMode = false
    private var currentLearningLabel: String? = null
    
    // تتبع الإطارات والثقة لحساب الإحصائيات
    private var frameBuffer = mutableListOf<Float>()
    private val MAX_FRAMES_PER_ATTEMPT = 20
    
    /**
     * بدء وضع التعلم لحرف/كلمة معينة
     * Start learning mode for a specific label
     */
    fun startLearning(label: String) {
        isLearningMode = true
        currentLearningLabel = label
        frameBuffer.clear()
        Log.d(TAG, "🎓 بدء وضع التعلم: $label")
    }
    
    /**
     * إنهاء وضع التعلم
     * Stop learning mode
     */
    fun stopLearning() {
        isLearningMode = false
        currentLearningLabel = null
        frameBuffer.clear()
        Log.d(TAG, "✅ تم إنهاء وضع التعلم")
    }
    
    /**
     * معالجة نتيجة التصنيف أثناء وضع التعلم
     * Process classification result during learning mode
     */
    fun processLearningResult(
        predictedLabel: String,
        confidence: Float,
        isCorrect: Boolean
    ): String {
        if (!isLearningMode || currentLearningLabel == null) {
            return ""
        }
        
        frameBuffer.add(confidence)
        
        // تحديث الإحصائيات
        val currentStats = learningStats[currentLearningLabel] ?: LearningStats(currentLearningLabel!!)
        
        val updatedStats = currentStats.copy(
            totalAttempts = currentStats.totalAttempts + 1,
            successfulDetections = if (isCorrect) currentStats.successfulDetections + 1 else currentStats.successfulDetections,
            totalFramesProcessed = currentStats.totalFramesProcessed + frameBuffer.size,
            averageConfidence = if (frameBuffer.isNotEmpty()) frameBuffer.average().toFloat() else 0f,
            bestConfidence = maxOf(currentStats.bestConfidence, confidence),
            worstConfidence = minOf(currentStats.worstConfidence, confidence)
        )
        
        learningStats[currentLearningLabel!!] = updatedStats
        
        Log.d(TAG, "📊 إحصائيات التعلم: $updatedStats")
        
        return generateFeedback(isCorrect, confidence, updatedStats)
    }
    
    /**
     * توليد تعليقات ذكية
     * Generate smart feedback messages
     */
    private fun generateFeedback(
        isCorrect: Boolean,
        confidence: Float,
        stats: LearningStats
    ): String {
        return when {
            !isCorrect -> {
                when {
                    confidence >= 0.5f -> "❌ قريب جداً! هذا الحرف قريب من '${stats.label}' - حاول أن تكون أكثر دقة"
                    confidence >= 0.3f -> "🤔 اقتراب منطقي - حاول مرة أخرى بشكل أفضل"
                    else -> "💪 لا تستسلم! حاول مجدداً وتذكر صورة الإشارة الصحيحة"
                }
            }
            confidence >= 0.95f -> "🌟 رائع جداً! أداء ممتاز!\nدقتك: ${stats.successRate}"
            confidence >= 0.85f -> "👏 ممتاز! أداء جيد جداً\nدقتك: ${stats.successRate}"
            confidence >= 0.75f -> "✨ جيد جداً! استمر\nدقتك: ${stats.successRate}"
            confidence >= 0.65f -> "👍 جيد! لكن يمكن للأفضل\nدقتك: ${stats.successRate}"
            else -> "🎯 تقدم جيد! حاول مرة أخرى\nدقتك: ${stats.successRate}"
        }
    }
    
    /**
     * الحصول على إحصائيات حرف معين
     * Get stats for a specific label
     */
    fun getStats(label: String): LearningStats? {
        return learningStats[label]
    }
    
    /**
     * الحصول على جميع الإحصائيات
     * Get all stats
     */
    fun getAllStats(): Map<String, LearningStats> {
        return learningStats.toMap()
    }
    
    /**
     * إعادة تعيين الإحصائيات
     * Reset statistics
     */
    fun resetStats(label: String? = null) {
        if (label != null) {
            learningStats.remove(label)
            Log.d(TAG, "🔄 تم إعادة تعيين إحصائيات: $label")
        } else {
            learningStats.clear()
            Log.d(TAG, "🔄 تم إعادة تعيين جميع الإحصائيات")
        }
    }
    
    /**
     * حساب الحروف الضعيفة (أقل من 70% دقة)
     * Get weak letters (accuracy < 70%)
     */
    fun getWeakLetters(): List<String> {
        return learningStats
            .filter { it.value.accuracyPercentage < 70f }
            .keys
            .sorted()
    }
    
    /**
     * حساب الحروف القوية (أكثر من 85% دقة)
     * Get strong letters (accuracy > 85%)
     */
    fun getStrongLetters(): List<String> {
        return learningStats
            .filter { it.value.accuracyPercentage > 85f }
            .keys
            .sorted()
    }
    
    /**
     * تقرير التقدم الشامل
     * Get comprehensive progress report
     */
    fun getProgressReport(): String {
        if (learningStats.isEmpty()) {
            return "📊 لا توجد بيانات تعلم حتى الآن. ابدأ بالتعلم!"
        }
        
        val totalAttempts = learningStats.values.sumOf { it.totalAttempts }
        val totalSuccesses = learningStats.values.sumOf { it.successfulDetections }
        val overallAccuracy = if (totalAttempts > 0) (totalSuccesses * 100f) / totalAttempts else 0f
        
        val weak = getWeakLetters()
        val strong = getStrongLetters()
        
        val report = StringBuilder()
        report.append("📊 تقرير التقدم\n")
        report.append("═══════════════════════════\n")
        report.append("📈 الأداء الإجمالي: %.1f%%\n".format(overallAccuracy))
        report.append("🎯 عدد المحاولات: $totalAttempts\n")
        report.append("✅ المحاولات الناجحة: $totalSuccesses\n")
        report.append("═══════════════════════════\n")
        
        if (strong.isNotEmpty()) {
            report.append("⭐ حروف قوية (>85%):\n")
            strong.take(5).forEach { label ->
                val stats = learningStats[label]!!
                report.append("   $label: ${stats.successRate}\n")
            }
            report.append("\n")
        }
        
        if (weak.isNotEmpty()) {
            report.append("💡 حروف تحتاج تحسين (<70%):\n")
            weak.take(5).forEach { label ->
                val stats = learningStats[label]!!
                report.append("   $label: ${stats.successRate}\n")
            }
            report.append("\n")
        }
        
        report.append("═══════════════════════════\n")
        report.append("🎓 الحروف المتقنة: ${strong.size}/${learningStats.size}\n")
        
        return report.toString()
    }
    
    /**
     * نصيحة ذكية بناءً على الأداء
     * Get smart recommendation based on performance
     */
    fun getSmartRecommendation(): String {
        val weak = getWeakLetters()
        val stats = getAllStats()
        
        return when {
            stats.isEmpty() -> "🚀 ابدأ بتعلم الحروف الأساسية!"
            weak.isEmpty() -> "🏆 مبروك! أنت متقن لجميع الحروف المتعلمة!"
            weak.size <= 2 -> "💪 جيد جداً! ركز على: ${weak.joinToString(", ")}"
            else -> "📚 ركز على تعلم هذه الحروف الضعيفة:\n${weak.take(5).joinToString(", ")}"
        }
    }
    
    fun isLearningModeActive(): Boolean = isLearningMode
    fun getCurrentLearningLabel(): String? = currentLearningLabel
}
