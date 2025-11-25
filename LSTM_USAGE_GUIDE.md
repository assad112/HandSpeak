# 🧠 دليل استخدام LSTM في HandSpeak

**تاريخ التحديث**: نوفمبر 2025

---

## 📋 ما هو LSTM؟

**LSTM** (Long Short-Term Memory) هو نوع من الشبكات العصبية المتكررة (RNN) مصمم للتعامل مع **البيانات الزمنية** (temporal/sequential data).

---

## 🔄 الفرق بين LSTM و Dense

### Dense Neural Network:
```
✅ للبيانات الثابتة (Static Data)
✅ صورة واحدة → تصنيف واحد
✅ أسرع في المعالجة
✅ أبسط في البنية
```

**مثال:**
```
صورة واحدة → [63 features] → Dense NN → "أ"
```

### LSTM:
```
✅ للبيانات الزمنية (Temporal Data)
✅ تسلسل إطارات → تصنيف واحد
✅ يتذكر السياق الزمني
✅ مناسب للفيديو أو الحركة
```

**مثال:**
```
[إطار1, إطار2, إطار3, ...] → LSTM → "مرحبا"
```

---

## 🎯 متى يُستخدم LSTM؟

### ✅ حالات استخدام LSTM:

1. **الفيديو:**
   ```
   تسلسل إطارات من الفيديو → LSTM → تصنيف الحركة
   ```

2. **الحركة الزمنية:**
   ```
   [t1, t2, t3, t4, ...] → LSTM → فهم الحركة الكاملة
   ```

3. **الإشارات المعقدة:**
   ```
   إشارة "مرحبا" تحتاج عدة إطارات → LSTM → فهم الإشارة الكاملة
   ```

---

## 📊 الوضع الحالي في التطبيق

### ✅ ما موجود حالياً:

**التطبيق يستخدم:**
- ✅ **Single Frame Processing** - معالجة إطار واحد في كل مرة
- ✅ **Dense NN** - مناسب للصور الثابتة
- ✅ **النموذج:** `arabic_sign_lstm.tflite` (يمكن أن يكون Dense أو LSTM)

**الكود الحالي:**
```kotlin
// SignToTextViewModel.kt
// معالجة إطار واحد
val handResult = handDetectionHelper.detectHands(bitmap)
val normalizedLandmarks = handDetectionHelper.normalizeLandmarks(landmarks)
val result = classifier.classify(normalizedLandmarks)  // إطار واحد
```

---

## 🚀 كيف يمكن استخدام LSTM؟

### الطريقة 1: معالجة تسلسل الإطارات

**الفكرة:**
```
بدلاً من معالجة إطار واحد، نجمع عدة إطارات ونرسلها معاً
```

**الكود:**
```kotlin
class SignToTextViewModel {
    private val frameBuffer = mutableListOf<FloatArray>()  // Buffer للإطارات
    private val SEQUENCE_LENGTH = 10  // 10 إطارات
    
    fun processFrame(bitmap: Bitmap) {
        // 1. استخراج landmarks
        val landmarks = extractLandmarks(bitmap)
        val normalized = normalizeLandmarks(landmarks)
        
        // 2. إضافة إلى Buffer
        frameBuffer.add(normalized)
        
        // 3. إذا وصلنا لـ 10 إطارات، نرسلهم معاً
        if (frameBuffer.size >= SEQUENCE_LENGTH) {
            // تحويل إلى Array[10][63]
            val sequence = frameBuffer.takeLast(SEQUENCE_LENGTH).toTypedArray()
            
            // 4. تصنيف باستخدام LSTM
            val result = classifier.classifySequence(sequence)
            
            // 5. مسح Buffer
            frameBuffer.clear()
        }
    }
}
```

### الطريقة 2: تحديث SignLanguageClassifier

**للاستخدام مع LSTM:**
```kotlin
class SignLanguageClassifier(private val context: Context) {
    
    // للـ LSTM: Input shape = [sequence_length, 63]
    // مثال: [10, 63] = 10 إطارات × 63 features
    
    fun classifySequence(sequence: Array<FloatArray>): Pair<String, Float>? {
        // sequence: Array[10][63] = 10 إطارات
        
        // تحويل إلى ByteBuffer
        val inputBuffer = ByteBuffer.allocateDirect(4 * sequence.size * 63)
        sequence.forEach { frame ->
            frame.forEach { value ->
                inputBuffer.putFloat(value)
            }
        }
        inputBuffer.rewind()
        
        // Output
        val outputArray = Array(1) { FloatArray(labels.size) }
        
        // Inference
        interpreter?.run(inputBuffer, outputArray)
        
        // اختيار الأعلى
        val probabilities = outputArray[0]
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val label = labelEncoder?.decode(maxIndex) ?: ""
        
        return Pair(label, probabilities[maxIndex])
    }
}
```

---

## 📐 البنية المعمارية لـ LSTM

### في Colab (التدريب):

```python
from tensorflow.keras import layers, models

# LSTM Model
model = models.Sequential([
    layers.InputLayer(input_shape=(10, 63)),  # 10 إطارات × 63 features
    layers.LSTM(128, return_sequences=True),  # LSTM layer 1
    layers.Dropout(0.3),
    layers.LSTM(64),  # LSTM layer 2
    layers.Dropout(0.3),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.2),
    layers.Dense(64, activation='relu'),
    layers.Dense(28, activation='softmax')  # 28 تصنيف
])

# Compile
model.compile(
    optimizer='adam',
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# Train
history = model.fit(
    X_train, y_train,  # X_train shape: (samples, 10, 63)
    validation_data=(X_test, y_test),
    epochs=50,
    batch_size=32
)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save
with open('arabic_sign_lstm.tflite', 'wb') as f:
    f.write(tflite_model)
```

---

## 🔄 التدفق الكامل مع LSTM

### الحالة الحالية (Dense):
```
إطار واحد → [63 features] → Dense NN → "أ"
```

### مع LSTM:
```
إطار 1 → [63]
إطار 2 → [63]
إطار 3 → [63]
...
إطار 10 → [63]
  ↓
[10 × 63] → LSTM → "مرحبا"
```

---

## 💡 المميزات والعيوب

### ✅ مميزات LSTM:

1. **يفهم الحركة:**
   - يتذكر السياق الزمني
   - يفهم الحركة الكاملة للإشارة

2. **دقة أعلى:**
   - مناسب للإشارات المعقدة
   - يفهم التسلسل الزمني

3. **مناسب للفيديو:**
   - يمكن معالجة فيديو كامل
   - فهم الحركة عبر الزمن

### ❌ عيوب LSTM:

1. **أبطأ:**
   - يحتاج جمع عدة إطارات
   - معالجة أكثر تعقيداً

2. **أكثر تعقيداً:**
   - يحتاج Buffer للإطارات
   - يحتاج إدارة التسلسل

3. **يحتاج بيانات أكثر:**
   - يحتاج تسلسلات للتدريب
   - بيانات التدريب أكبر

---

## 🎯 متى تستخدم LSTM؟

### ✅ استخدم LSTM إذا:

1. **الإشارات معقدة:**
   - إشارات تحتاج حركة زمنية
   - مثل "مرحبا" أو "السلام عليكم"

2. **الفيديو:**
   - معالجة فيديو كامل
   - فهم الحركة عبر الزمن

3. **دقة أعلى:**
   - إذا كانت Dense لا تكفي
   - تحتاج فهم السياق الزمني

### ✅ استخدم Dense إذا:

1. **إشارات بسيطة:**
   - حروف منفردة
   - إشارات ثابتة

2. **سرعة:**
   - تحتاج معالجة سريعة
   - إطار واحد كافي

3. **بساطة:**
   - كود أبسط
   - أقل تعقيداً

---

## 🔧 التطبيق الحالي

### ✅ الوضع الحالي:

**التطبيق يستخدم:**
- ✅ **Single Frame** - إطار واحد في كل مرة
- ✅ **Dense NN** - مناسب للوضع الحالي
- ✅ **يعمل بشكل جيد** - للكلمات والحروف

**النموذج:**
- ✅ `arabic_sign_lstm.tflite` - يمكن أن يكون Dense أو LSTM
- ✅ **المهم:** Input=63, Output=28

---

## 🚀 إذا أردت استخدام LSTM

### الخطوات:

1. **تدريب نموذج LSTM:**
   ```python
   # في Colab
   # تدريب على تسلسلات (sequences)
   X_train shape: (samples, 10, 63)  # 10 إطارات × 63 features
   ```

2. **تحديث الكود:**
   ```kotlin
   // جمع إطارات في Buffer
   // إرسال تسلسل إلى LSTM
   ```

3. **استبدال النموذج:**
   ```bash
   # نسخ النموذج الجديد
   cp arabic_sign_lstm.tflite app/src/main/assets/
   ```

---

## ✅ الخلاصة

### الوضع الحالي:
- ✅ **Dense NN** - مناسب للوضع الحالي
- ✅ **Single Frame** - إطار واحد في كل مرة
- ✅ **يعمل بشكل جيد**

### LSTM:
- ✅ **للبيانات الزمنية** - تسلسل إطارات
- ✅ **للفيديو** - معالجة فيديو كامل
- ✅ **للإشارات المعقدة** - حركة زمنية

### التوصية:
- ✅ **استخدم Dense** للوضع الحالي (أبسط وأسرع)
- ✅ **استخدم LSTM** إذا احتجت فهم الحركة الزمنية

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **Dense مستخدم حالياً، LSTM متاح للاستخدام**










