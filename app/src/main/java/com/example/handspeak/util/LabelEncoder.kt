package com.example.handspeak.util

import android.util.Log

/**
 * LabelEncoder - يحول أسماء الإشارات إلى أرقام والعكس
 * 
 * هذا يعادل LabelEncoder من scikit-learn المستخدم في Colab
 * 
 * في التدريب (Colab):
 * - "أ" → 0
 * - "ب" → 1
 * - "ت" → 2
 * - ...
 * 
 * في التطبيق:
 * - النموذج يعطي رقم (0-27)
 * - LabelEncoder يحوله إلى اسم ("أ", "ب", ...)
 */
class LabelEncoder(private val labels: List<String>) {
    
    companion object {
        private const val TAG = "LabelEncoder"
    }
    
    // Map من اسم التصنيف إلى رقمه
    private val labelToIndex: Map<String, Int> = labels.mapIndexed { index, label ->
        label to index
    }.toMap()
    
    // Map من الرقم إلى اسم التصنيف (هذا موجود بالفعل في labels list)
    // labels[index] يعطي الاسم مباشرة
    
    /**
     * تحويل اسم التصنيف إلى رقم (encode)
     * 
     * مثال:
     * - encode("أ") → 0
     * - encode("ب") → 1
     * 
     * @param label اسم التصنيف
     * @return الرقم المقابل، أو -1 إذا لم يوجد
     */
    fun encode(label: String): Int {
        return labelToIndex[label] ?: run {
            Log.w(TAG, "Label not found: $label")
            -1
        }
    }
    
    /**
     * تحويل الرقم إلى اسم التصنيف (decode)
     * 
     * مثال:
     * - decode(0) → "أ"
     * - decode(1) → "ب"
     * 
     * @param index الرقم
     * @return اسم التصنيف، أو null إذا كان الرقم غير صحيح
     */
    fun decode(index: Int): String? {
        return if (index in labels.indices) {
            labels[index]
        } else {
            Log.w(TAG, "Index out of range: $index (max: ${labels.size - 1})")
            null
        }
    }
    
    /**
     * تحويل قائمة أسماء إلى قائمة أرقام
     * 
     * @param labels قائمة أسماء التصنيفات
     * @return قائمة الأرقام المقابلة
     */
    fun encodeList(labels: List<String>): List<Int> {
        return labels.map { encode(it) }.filter { it >= 0 }
    }
    
    /**
     * تحويل قائمة أرقام إلى قائمة أسماء
     * 
     * @param indices قائمة الأرقام
     * @return قائمة أسماء التصنيفات
     */
    fun decodeList(indices: List<Int>): List<String> {
        return indices.mapNotNull { decode(it) }
    }
    
    /**
     * عدد التصنيفات
     */
    fun size(): Int = labels.size
    
    /**
     * الحصول على جميع التصنيفات
     */
    fun getLabels(): List<String> = labels.toList()
    
    /**
     * التحقق من وجود تصنيف
     */
    fun contains(label: String): Boolean = labelToIndex.containsKey(label)
}

