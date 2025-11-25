# 🖐️ دليل استخدام Hand Landmarks

**تاريخ التحديث**: نوفمبر 2025

---

## 📋 نظرة عامة

**Hand Landmarks** هي 21 نقطة على اليد يتم استخراجها باستخدام MediaPipe.

---

## 🔢 البنية

### 21 Landmark Points:

```
0: WRIST (المعصم)
1-4: THUMB (الإبهام)
5-8: INDEX (السبابة)
9-12: MIDDLE (الوسطى)
13-16: RING (البنصر)
17-20: PINKY (الخنصر)
```

### كل نقطة تحتوي على:
- **x**: الإحداثي الأفقي (0.0 - 1.0)
- **y**: الإحداثي العمودي (0.0 - 1.0)
- **z**: العمق (نسبي)

**المجموع:** 21 × 3 = **63 features**

---

## 🔄 كيف يعمل في التطبيق؟

### 1. ✅ استخراج Landmarks

**في HandDetectionHelper:**
```kotlin
// MediaPipe يستخرج 21 landmark
val result = handLandmarker?.detect(mpImage)

// تحويل إلى HandLandmark objects
val landmarks = result.landmarks()[0].map {
    HandLandmark(it.x(), it.y(), it.z())
}
```

**النتيجة:**
```kotlin
List<HandLandmark>  // 21 عنصر
// كل عنصر: HandLandmark(x, y, z)
```

---

### 2. ✅ Normalization

**في HandDetectionHelper.normalizeLandmarks():**
```kotlin
fun normalizeLandmarks(landmarks: List<HandLandmark>): FloatArray {
    // 1. تحويل إلى array: [x1, y1, z1, x2, y2, z2, ..., x21, y21, z21]
    val array = FloatArray(63)  // 21 × 3
    
    // 2. Normalize x و y إلى 0-1
    // - إيجاد min و max
    // - تطبيق: (value - min) / (max - min)
    
    // 3. z يبقى كما هو (نسبي)
    
    return array  // FloatArray[63]
}
```

**النتيجة:**
```kotlin
FloatArray(63)  // [x1, y1, z1, x2, y2, z2, ..., x21, y21, z21]
// جميع القيم في نطاق 0.0 - 1.0
```

---

### 3. ✅ التصنيف

**في SignLanguageClassifier:**
```kotlin
// المدخلات: 63 features
val inputBuffer = ByteBuffer.allocateDirect(4 * 63)
normalizedLandmarks.forEach { putFloat(it) }

// المخرجات: 28 probabilities
val outputArray = Array(1) { FloatArray(28) }

// Inference
interpreter?.run(inputBuffer, outputArray)

// اختيار الأعلى
val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
val label = labelEncoder.decode(maxIndex)  // → "أ"
```

---

## 📊 التدفق الكامل

```
1. الكاميرا → Bitmap
   ↓
2. MediaPipe → HandLandmarkerResult
   ↓
3. استخراج 21 Landmark
   [HandLandmark(x, y, z), ...]  // 21 عنصر
   ↓
4. Normalization
   FloatArray[63]  // [x1, y1, z1, ..., x21, y21, z21]
   ↓
5. Dense NN (256 → 128 → 64 → 28)
   ↓
6. Output: 28 probabilities
   [0.01, 0.02, ..., 0.85, ...]
   ↓
7. LabelEncoder.decode(maxIndex)
   ↓
8. ✅ "أ"
```

---

## 🔧 الكود المهم

### HandDetectionHelper.kt

**استخراج Landmarks:**
```kotlin
fun detectHands(bitmap: Bitmap): HandDetectionResult? {
    val mpImage = BitmapImageBuilder(bitmap).build()
    val result = handLandmarker?.detect(mpImage)
    
    val landmarks = result.landmarks()[0].map {
        HandLandmark(it.x(), it.y(), it.z())
    }
    
    return HandDetectionResult(landmarks, confidence)
}
```

**Normalization:**
```kotlin
fun normalizeLandmarks(landmarks: List<HandLandmark>): FloatArray {
    // تحويل إلى array
    val array = FloatArray(landmarks.size * 3)
    landmarks.forEachIndexed { index, landmark ->
        array[index * 3] = landmark.x
        array[index * 3 + 1] = landmark.y
        array[index * 3 + 2] = landmark.z
    }
    
    // Normalize x و y
    val minX = array.filterIndexed { i, _ -> i % 3 == 0 }.minOrNull() ?: 0f
    val maxX = array.filterIndexed { i, _ -> i % 3 == 0 }.maxOrNull() ?: 1f
    val minY = array.filterIndexed { i, _ -> i % 3 == 1 }.minOrNull() ?: 0f
    val maxY = array.filterIndexed { i, _ -> i % 3 == 1 }.maxOrNull() ?: 1f
    
    array.forEachIndexed { index, value ->
        when (index % 3) {
            0 -> array[index] = if (maxX - minX != 0f) (value - minX) / (maxX - minX) else 0f
            1 -> array[index] = if (maxY - minY != 0f) (value - minY) / (maxY - minY) else 0f
            // z stays as is
        }
    }
    
    return array
}
```

---

## 📝 في SignToTextViewModel

**الاستخدام:**
```kotlin
// 1. اكتشاف اليد
val handResult = handDetectionHelper.detectHands(bitmap)

// 2. استخراج Landmarks
val landmarks = handResult.landmarks  // List<HandLandmark>

// 3. Normalization
val normalizedLandmarks = handDetectionHelper.normalizeLandmarks(landmarks)
// → FloatArray[63]

// 4. التصنيف
val result = classifier.classify(normalizedLandmarks)
// → Pair("أ", 0.85)
```

---

## ✅ الميزات

### 1. ✅ استخراج تلقائي
- MediaPipe يستخرج Landmarks تلقائياً
- 21 نقطة × 3 إحداثيات = 63 features

### 2. ✅ Normalization
- x و y → 0.0 - 1.0
- z → نسبي
- متطابق مع التدريب في Colab

### 3. ✅ جاهز للتصنيف
- FloatArray[63] → Dense NN
- Output → 28 probabilities

---

## 🎯 الخلاصة

### ✅ Landmarks مستخدمة في:
1. ✅ **HandDetectionHelper** - استخراج و Normalization
2. ✅ **SignLanguageClassifier** - التصنيف
3. ✅ **SignToTextViewModel** - الربط بين المكونات

### ✅ التدفق:
```
MediaPipe → 21 Landmarks → Normalization → 63 Features → Dense NN → 28 Probabilities → Label
```

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **مستخدم ويعمل**

