# 🧠 تطبيق Dense Neural Network (256 → 128 → 64)

**تاريخ التحديث**: نوفمبر 2025

---

## ✅ ما تم تطبيقه

### 1. ✅ البنية المعمارية

**Dense Neural Network:**
```
Input (63 features)
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

### 2. ✅ في Colab (التدريب)

**الكود:**
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

### 3. ✅ في التطبيق

**الكود الحالي:**
```kotlin
// SignLanguageClassifier.kt
private const val INPUT_SIZE = 63  // 21 landmarks × 3
private const val OUTPUT_SIZE = 28  // حسب labels.json

// النموذج يعمل مع Dense أو LSTM
// المهم: Input=63, Output=28
```

**التصنيف:**
```kotlin
// المدخلات: 63 features
val inputBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE)

// المخرجات: 28 probabilities
val outputArray = Array(1) { FloatArray(28) }

// Inference
interpreter?.run(inputBuffer, outputArray)

// اختيار الأعلى
val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
val label = labelEncoder.decode(maxIndex)
```

---

## 🔄 التدفق الكامل

### من Landmarks إلى التصنيف:

```
1. MediaPipe → Hand Landmarks (21 × 3 = 63)
   ↓
2. Normalization → [0.0 - 1.0]
   ↓
3. Dense NN:
   - Dense(256) + ReLU + Dropout(0.3)
   - Dense(128) + ReLU + Dropout(0.3)
   - Dense(64) + ReLU + Dropout(0.2)
   - Dense(28) + Softmax
   ↓
4. Output → 28 probabilities
   ↓
5. LabelEncoder.decode(maxIndex) → "أ"
   ↓
6. ✅ عرض "أ" على الشاشة
```

---

## 📊 البنية المعمارية بالتفصيل

### Layer 1: Dense(256)
- **الوحدات**: 256
- **التفعيل**: ReLU
- **Dropout**: 0.3 (30%)
- **المدخلات**: 63 features
- **المخرجات**: 256 features

### Layer 2: Dense(128)
- **الوحدات**: 128
- **التفعيل**: ReLU
- **Dropout**: 0.3 (30%)
- **المدخلات**: 256 features
- **المخرجات**: 128 features

### Layer 3: Dense(64)
- **الوحدات**: 64
- **التفعيل**: ReLU
- **Dropout**: 0.2 (20%)
- **المدخلات**: 128 features
- **المخرجات**: 64 features

### Output Layer: Dense(28)
- **الوحدات**: 28 (عدد التصنيفات)
- **التفعيل**: Softmax
- **المدخلات**: 64 features
- **المخرجات**: 28 probabilities

---

## 🎯 Dropout

### الغرض:
- ✅ **تقليل Overfitting**
- ✅ **تحسين التعميم**
- ✅ **منع الاعتماد الزائد على ميزات معينة**

### القيم المستخدمة:
- **Layer 1**: 0.3 (30%)
- **Layer 2**: 0.3 (30%)
- **Layer 3**: 0.2 (20%)

---

## 📝 السكريبت المحدث

### scripts/create_dense_model.py

**البنية:**
```python
model = tf.keras.Sequential([
    layers.InputLayer(input_shape=(63,)),
    layers.Dense(256, activation='relu'),
    layers.Dropout(0.3),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.3),
    layers.Dense(64, activation='relu'),
    layers.Dropout(0.2),
    layers.Dense(28, activation='softmax')
])
```

**الاستخدام:**
```bash
cd scripts
python create_dense_model.py
# الناتج: arabic_sign_dense.tflite
```

---

## ✅ الخلاصة

### ✅ ما تم تطبيقه:
1. ✅ **Dense NN Architecture** - 256 → 128 → 64
2. ✅ **Dropout** - 0.3, 0.3, 0.2
3. ✅ **سكريبت للإنشاء** - `create_dense_model.py`
4. ✅ **دعم في التطبيق** - يعمل مع Dense أو LSTM

### ✅ البنية:
- **Input**: 63 features
- **Layer 1**: 256 units + Dropout(0.3)
- **Layer 2**: 128 units + Dropout(0.3)
- **Layer 3**: 64 units + Dropout(0.2)
- **Output**: 28 probabilities

### ✅ الاستخدام:
- ✅ **في Colab**: درّب النموذج بالبنية المذكورة
- ✅ **في التطبيق**: استخدم النموذج المدرب
- ✅ **الكود**: جاهز ويعمل

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **مكتمل - Dense NN (256 → 128 → 64)**

