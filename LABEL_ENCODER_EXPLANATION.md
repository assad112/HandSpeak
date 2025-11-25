# 🔢 LabelEncoder - شرح مفصل

**تاريخ التحديث**: نوفمبر 2025

---

## 📋 ما هو LabelEncoder؟

**LabelEncoder** هو أداة تحول أسماء التصنيفات (Labels) إلى أرقام والعكس.

### في التدريب (Colab):
```python
from sklearn.preprocessing import LabelEncoder

# البيانات
labels = ["أ", "ب", "ت", "ث", ...]

# إنشاء LabelEncoder
label_encoder = LabelEncoder()
encoded = label_encoder.fit_transform(labels)

# النتيجة:
# "أ" → 0
# "ب" → 1
# "ت" → 2
# "ث" → 3
# ...
```

### في التطبيق (Kotlin):
```kotlin
// البيانات
val labels = listOf("أ", "ب", "ت", "ث", ...)

// إنشاء LabelEncoder
val labelEncoder = LabelEncoder(labels)

// Encode (اسم → رقم)
val index = labelEncoder.encode("أ")  // → 0
val index = labelEncoder.encode("ب")  // → 1

// Decode (رقم → اسم)
val label = labelEncoder.decode(0)  // → "أ"
val label = labelEncoder.decode(1)  // → "ب"
```

---

## 🔄 كيف يعمل في التطبيق؟

### 1. ✅ التهيئة

**في SignLanguageClassifier:**
```kotlin
// تحميل labels من JSON
labels = JsonHelper.loadLabels(context)

// إنشاء LabelEncoder
labelEncoder = LabelEncoder(labels)
```

**ما يحدث:**
- تحميل قائمة التصنيفات من `labels.json`
- إنشاء Map من الاسم إلى الرقم
- جاهز للاستخدام

---

### 2. ✅ التصنيف (Classification)

**عند اكتشاف إشارة:**
```kotlin
// النموذج يعطي رقم (0-27)
val maxIndex = 5  // مثال: "ج"

// LabelEncoder يحوله إلى اسم
val label = labelEncoder.decode(maxIndex)  // → "ج"
```

**التدفق:**
```
1. MediaPipe → Landmarks (63 features)
   ↓
2. TFLite Model → Probabilities [0.1, 0.05, 0.02, ..., 0.8, ...]
   ↓
3. Find max index → 5
   ↓
4. LabelEncoder.decode(5) → "ج"
   ↓
5. ✅ عرض "ج" على الشاشة
```

---

## 📊 المقارنة: Colab vs التطبيق

### في Colab (Python):
```python
# التدريب
from sklearn.preprocessing import LabelEncoder

label_encoder = LabelEncoder()
y_encoded = label_encoder.fit_transform(y_labels)

# حفظ
import pickle
with open('label_encoder.pkl', 'wb') as f:
    pickle.dump(label_encoder, f)

# الاستخدام
y_pred_encoded = model.predict(X_test)
y_pred_labels = label_encoder.inverse_transform(y_pred_encoded)
```

### في التطبيق (Kotlin):
```kotlin
// التهيئة
val labels = JsonHelper.loadLabels(context)
val labelEncoder = LabelEncoder(labels)

// الاستخدام
val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
val label = labelEncoder.decode(maxIndex)
```

---

## 🎯 الفرق الرئيسي

### في Colab:
- ✅ يستخدم `sklearn.preprocessing.LabelEncoder`
- ✅ يحفظ في `label_encoder.pkl`
- ✅ يستخدم `inverse_transform()` للتحويل

### في التطبيق:
- ✅ يستخدم `LabelEncoder` class مخصص
- ✅ يستخدم `labels.json` بدلاً من `.pkl`
- ✅ يستخدم `decode()` للتحويل

**النتيجة:** نفس الوظيفة، تنفيذ مختلف!

---

## 🔧 الكود المضافة

### LabelEncoder.kt

**الوظائف:**
```kotlin
// Encode: اسم → رقم
fun encode(label: String): Int

// Decode: رقم → اسم
fun decode(index: Int): String?

// Encode list
fun encodeList(labels: List<String>): List<Int>

// Decode list
fun decodeList(indices: List<Int>): List<String>

// Utilities
fun size(): Int
fun getLabels(): List<String>
fun contains(label: String): Boolean
```

---

## 📝 مثال عملي

### مثال 1: Encode
```kotlin
val encoder = LabelEncoder(listOf("أ", "ب", "ت"))

val index1 = encoder.encode("أ")  // → 0
val index2 = encoder.encode("ب")  // → 1
val index3 = encoder.encode("ت")  // → 2
```

### مثال 2: Decode
```kotlin
val encoder = LabelEncoder(listOf("أ", "ب", "ت"))

val label1 = encoder.decode(0)  // → "أ"
val label2 = encoder.decode(1)  // → "ب"
val label3 = encoder.decode(2)  // → "ت"
```

### مثال 3: في التطبيق
```kotlin
// في SignLanguageClassifier.classify()
val probabilities = outputArray[0]  // [0.1, 0.05, 0.8, ...]
val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0  // → 2
val label = labelEncoder.decode(maxIndex)  // → "ت"
```

---

## ✅ الميزات

### 1. ✅ متطابق مع Colab
- نفس الوظيفة
- نفس الترتيب
- نفس النتيجة

### 2. ✅ سهل الاستخدام
- دالة `encode()` بسيطة
- دالة `decode()` بسيطة
- لا حاجة لملفات خارجية

### 3. ✅ آمن
- التحقق من النطاق
- معالجة الأخطاء
- Logging مفصل

---

## 🔍 التطابق مع Colab

### في Colab:
```python
# labels.json
["أ", "ب", "ت", "ث", ...]

# LabelEncoder
"أ" → 0
"ب" → 1
"ت" → 2
...
```

### في التطبيق:
```kotlin
// labels.json (نفس الترتيب!)
["أ", "ب", "ت", "ث", ...]

// LabelEncoder
"أ" → 0
"ب" → 1
"ت" → 2
...
```

**✅ متطابق تماماً!**

---

## 📊 الخلاصة

### ✅ ما تم إضافته:
1. ✅ **LabelEncoder.kt** - class جديد
2. ✅ **دمج في SignLanguageClassifier** - استخدام LabelEncoder
3. ✅ **متطابق مع Colab** - نفس الوظيفة

### ✅ كيف يعمل:
1. **التهيئة** - تحميل labels وإنشاء encoder
2. **التصنيف** - النموذج يعطي رقم
3. **Decode** - LabelEncoder يحوله إلى اسم
4. **العرض** - عرض الاسم على الشاشة

### ✅ الفوائد:
- ✅ متطابق مع Colab
- ✅ سهل الاستخدام
- ✅ آمن وموثوق
- ✅ معالجة أخطاء جيدة

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **مكتمل ومدمج**

