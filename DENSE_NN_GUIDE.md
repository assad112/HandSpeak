# 🧠 دليل استخدام Dense Neural Network

**تاريخ التحديث**: نوفمبر 2025

---

## 📋 نظرة عامة

التطبيق يدعم **Dense Neural Network** (الشبكة العصبية الكثيفة) مع الطبقات التالية:
- **256 وحدة** (Layer 1)
- **128 وحدة** (Layer 2)
- **64 وحدة** (Layer 3)
- **Dropout** لتقليل overfitting

---

## 🏗️ البنية المعمارية

### في Colab (التدريب):
```python
from tensorflow.keras import layers, models

model = models.Sequential([
    layers.InputLayer(input_shape=(63,)),  # 21 landmarks × 3
    layers.Dense(256, activation='relu'),
    layers.Dropout(0.3),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.3),
    layers.Dense(64, activation='relu'),
    layers.Dropout(0.2),
    layers.Dense(28, activation='softmax')  # عدد التصنيفات
])
```

### في التطبيق:
- ✅ **النموذج المدرب** - `arabic_sign_lstm.tflite` (يمكن أن يكون Dense أو LSTM)
- ✅ **SignLanguageClassifier** - يستخدم النموذج
- ✅ **Input: 63 features** (21 landmarks × 3)
- ✅ **Output: 28 تصنيف** (حسب labels.json)

---

## 🔄 كيف يعمل؟

### 1. ✅ المدخلات (Input)

**63 features من Hand Landmarks:**
```kotlin
// 21 landmark × 3 coordinates (x, y, z) = 63 features
private const val INPUT_SIZE = 63
```

**مثال:**
```
[0.5, 0.3, 0.1, 0.7, 0.2, 0.0, ..., 0.4, 0.6, 0.2]  // 63 قيمة
```

---

### 2. ✅ المعالجة (Processing)

**في النموذج:**
```
Input (63) 
  ↓
Dense(256) + ReLU + Dropout(0.3)
  ↓
Dense(128) + ReLU + Dropout(0.3)
  ↓
Dense(64) + ReLU + Dropout(0.2)
  ↓
Dense(28) + Softmax
  ↓
Output (28 probabilities)
```

---

### 3. ✅ المخرجات (Output)

**28 احتمالات (probabilities):**
```kotlin
val probabilities = outputArray[0]
// [0.01, 0.02, 0.05, ..., 0.85, ..., 0.01]
//  ↑    ↑    ↑              ↑
//  أ    ب    ت              (أعلى احتمال)
```

**اختيار الأعلى:**
```kotlin
val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
val label = labelEncoder.decode(maxIndex)  // → "أ"
```

---

## 📊 الفرق بين Dense و LSTM

### Dense Neural Network:
- ✅ **للبيانات الثابتة** (static data)
- ✅ **أسرع** في المعالجة
- ✅ **أبسط** في البنية
- ✅ **مناسب للصور** (single frame)

### LSTM:
- ✅ **للبيانات الزمنية** (temporal data)
- ✅ **أبطأ** قليلاً
- ✅ **أكثر تعقيداً**
- ✅ **مناسب للفيديو** (sequence of frames)

---

## 🎯 الاستخدام الحالي

### التطبيق يستخدم:
- ✅ **TFLite Model** - `arabic_sign_lstm.tflite`
- ✅ **يمكن أن يكون Dense أو LSTM** - حسب ما تم تدريبه
- ✅ **يعمل مع أي نوع** - المهم هو المدخلات والمخرجات

---

## 🔧 إذا أردت استخدام Dense فقط

### الخيار 1: تدريب نموذج Dense جديد

**في Colab:**
```python
from tensorflow.keras import layers, models

# Dense Neural Network
model = models.Sequential([
    layers.InputLayer(input_shape=(63,)),
    layers.Dense(256, activation='relu'),
    layers.Dropout(0.3),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.3),
    layers.Dense(64, activation='relu'),
    layers.Dropout(0.2),
    layers.Dense(28, activation='softmax')
])

# Compile
model.compile(
    optimizer='adam',
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# Train
history = model.fit(
    X_train, y_train,
    validation_data=(X_test, y_test),
    epochs=50,
    batch_size=32,
    callbacks=[keras.callbacks.EarlyStopping(patience=10)]
)

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save
with open('arabic_sign_dense.tflite', 'wb') as f:
    f.write(tflite_model)
```

---

### الخيار 2: استخدام النموذج الحالي

**النموذج الحالي يعمل:**
- ✅ سواء كان Dense أو LSTM
- ✅ المهم هو المدخلات (63 features) والمخرجات (28 probabilities)
- ✅ التطبيق لا يهتم بالبنية الداخلية

---

## 📝 الكود الحالي

### SignLanguageClassifier.kt

**المدخلات:**
```kotlin
private const val INPUT_SIZE = 63  // 21 landmarks × 3
```

**المخرجات:**
```kotlin
val outputArray = Array(1) { FloatArray(labels.size) }  // 28 probabilities
```

**التصنيف:**
```kotlin
val probabilities = outputArray[0]
val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
val label = labelEncoder.decode(maxIndex)
```

---

## ✅ الخلاصة

### النموذج الحالي:
- ✅ **يعمل مع Dense أو LSTM**
- ✅ **المدخلات: 63 features**
- ✅ **المخرجات: 28 probabilities**
- ✅ **لا يحتاج تغيير في الكود**

### إذا أردت Dense فقط:
1. ✅ **درّب نموذج Dense جديد** في Colab
2. ✅ **حوّله إلى TFLite**
3. ✅ **استبدل النموذج الحالي**
4. ✅ **الكود سيعمل بدون تغيير**

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **يدعم Dense و LSTM**

