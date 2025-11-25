# 🧠 تطبيق LSTM في HandSpeak

**تاريخ التحديث**: نوفمبر 2025

---

## ✅ ما تم تطبيقه

### 1. ✅ Frame Buffer System

**في SignToTextViewModel:**
- ✅ `frameBuffer` - لتجميع عدة إطارات
- ✅ `SEQUENCE_LENGTH = 10` - طول التسلسل
- ✅ `USE_LSTM = true` - تفعيل LSTM افتراضياً

### 2. ✅ SignLanguageClassifier.classifySequence()

**دالة جديدة للـ LSTM:**
```kotlin
fun classifySequence(sequence: List<FloatArray>, sequenceLength: Int = 10): Pair<String, Float>?
```

**المدخلات:**
- `sequence`: List<FloatArray> - تسلسل إطارات (كل إطار 63 features)
- `sequenceLength`: طول التسلسل (افتراضي 10)

**المخرجات:**
- `Pair<String, Float>` - (label, confidence)

### 3. ✅ تحديث processHandLandmarks()

**المنطق الجديد:**
```kotlin
if (useLSTM) {
    // جمع الإطارات في Buffer
    frameBuffer.add(normalizedLandmarks)
    
    // إذا وصلنا للطول المطلوب، نصنّف
    if (frameBuffer.size >= SEQUENCE_LENGTH) {
        classifier?.classifySequence(sequence, SEQUENCE_LENGTH)
    }
} else {
    // Dense: تصنيف إطار واحد
    classifier?.classify(normalizedLandmarks)
}
```

### 4. ✅ إعدادات التحكم

**في SettingsScreen:**
- ✅ مفتاح "استخدام LSTM"
- ✅ يمكن تفعيل/تعطيل من الإعدادات

---

## 🔄 كيف يعمل LSTM الآن

### التدفق:

```
1. إطار 1 → Buffer [إطار1]
2. إطار 2 → Buffer [إطار1, إطار2]
3. إطار 3 → Buffer [إطار1, إطار2, إطار3]
...
10. إطار 10 → Buffer [إطار1...إطار10]
    ↓
11. classifySequence([إطار1...إطار10]) → LSTM → "مرحبا"
    ↓
12. مسح Buffer → جاهز للتسلسل التالي
```

---

## 📊 الفرق بين LSTM و Dense

### Dense (إطار واحد):
```
إطار → [63] → Dense NN → "أ"
```

### LSTM (تسلسل إطارات):
```
[إطار1, إطار2, ..., إطار10] → [10×63] → LSTM → "مرحبا"
```

---

## ⚙️ الإعدادات

### في SettingsScreen:

**"استخدام LSTM":**
- ✅ **مفعّل**: معالجة تسلسل إطارات (دقة أعلى)
- ❌ **معطّل**: معالجة إطار واحد (أسرع)

---

## 🎯 المميزات

### ✅ LSTM:
1. **دقة أعلى** - يفهم الحركة الزمنية
2. **مناسب للإشارات المعقدة** - مثل "مرحبا"
3. **يفهم السياق** - يتذكر الإطارات السابقة

### ✅ Dense:
1. **أسرع** - معالجة فورية
2. **أبسط** - كود أقل تعقيداً
3. **مناسب للحروف** - إشارات بسيطة

---

## 🔧 الكود المهم

### SignLanguageClassifier.classifySequence():

```kotlin
fun classifySequence(sequence: List<FloatArray>, sequenceLength: Int = 10): Pair<String, Float>? {
    // 1. Padding إذا كان التسلسل أقصر
    val paddedSequence = if (sequence.size < sequenceLength) {
        val lastFrame = sequence.last()
        sequence + List(sequenceLength - sequence.size) { lastFrame.copyOf() }
    } else {
        sequence.takeLast(sequenceLength)
    }
    
    // 2. تحويل إلى ByteBuffer: [sequence_length, 63]
    val inputBuffer = ByteBuffer.allocateDirect(4 * sequenceLength * INPUT_SIZE)
    paddedSequence.forEach { frame ->
        frame.forEach { value -> inputBuffer.putFloat(value) }
    }
    
    // 3. Inference
    interpreter?.run(inputBuffer, outputArray)
    
    // 4. النتيجة
    return Pair(label, confidence)
}
```

### SignToTextViewModel.processHandLandmarks():

```kotlin
val useLSTM = prefs.getBoolean("use_lstm", true)

if (useLSTM) {
    // LSTM: جمع الإطارات
    frameBuffer.add(normalizedLandmarks)
    
    if (frameBuffer.size >= SEQUENCE_LENGTH) {
        val sequence = frameBuffer.toList()
        frameBuffer.clear()
        classifier?.classifySequence(sequence, SEQUENCE_LENGTH)
    }
} else {
    // Dense: إطار واحد
    classifier?.classify(normalizedLandmarks)
}
```

---

## 📱 UI Updates

### SignToTextUiState:

```kotlin
data class SignToTextUiState(
    ...
    val sequenceBufferSize: Int = 0, // عدد الإطارات في Buffer
    val useLSTM: Boolean = true // استخدام LSTM أو Dense
)
```

**عرض في UI:**
- "جمع الإطارات... (5/10)" - أثناء جمع الإطارات
- "مرحبا" - بعد التصنيف

---

## 🚀 الاستخدام

### 1. تفعيل LSTM:

```
الإعدادات → نموذج الذكاء الاصطناعي → استخدام LSTM ✅
```

### 2. استخدام التطبيق:

- ضع يدك أمام الكاميرا
- انتظر جمع 10 إطارات
- النتيجة تظهر تلقائياً

### 3. تعطيل LSTM (للسرعة):

```
الإعدادات → نموذج الذكاء الاصطناعي → استخدام LSTM ❌
```

---

## ✅ الخلاصة

### ما تم إضافته:
1. ✅ **Frame Buffer** - جمع الإطارات
2. ✅ **classifySequence()** - معالجة LSTM
3. ✅ **إعدادات التحكم** - تفعيل/تعطيل
4. ✅ **UI Updates** - عرض حالة Buffer

### الحالة:
- ✅ **LSTM مفعّل افتراضياً**
- ✅ **يمكن التبديل بين LSTM و Dense**
- ✅ **جاهز للاستخدام**

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **LSTM مطبّق ويعمل**










