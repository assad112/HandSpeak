# 📖 دليل إنشاء ملف arabic_sign_lstm.tflite

## 🎯 الطرق المتاحة

هناك **3 طرق** لإنشاء الملف:

---

## ⚡ الطريقة 1: نموذج تجريبي سريع (للاختبار فقط)

**الوقت:** 1-2 دقيقة  
**النتيجة:** نموذج يعمل لكن غير دقيق

### الخطوات:

1. **افتح Terminal في مجلد المشروع:**
```bash
cd scripts
```

2. **شغّل السكريبت:**
```bash
python create_dummy_model.py
```

3. **انسخ الملف إلى assets:**
```bash
# Windows
copy arabic_sign_lstm.tflite ..\app\src\main\assets\

# Linux/Mac
cp arabic_sign_lstm.tflite ../app/src/main/assets/
```

4. **في Android Studio:**
   - `Build > Clean Project`
   - `Build > Rebuild Project`

✅ **جاهز!** النموذج الآن في التطبيق (لكن نتائجه غير دقيقة)

---

## 🎓 الطريقة 2: نموذج LSTM تجريبي (أفضل للاختبار)

**الوقت:** 5-10 دقائق  
**النتيجة:** نموذج LSTM يعمل لكن يحتاج بيانات حقيقية

### الخطوات:

1. **تثبيت المتطلبات:**
```bash
pip install tensorflow numpy
```

2. **شغّل السكريبت:**
```bash
cd scripts
python train_arabic_letters_model.py
```

3. **انسخ الملف:**
```bash
# Windows
copy arabic_letters_lstm.tflite ..\app\src\main\assets\arabic_sign_lstm.tflite

# Linux/Mac
cp arabic_letters_lstm.tflite ../app/src/main/assets/arabic_sign_lstm.tflite
```

4. **Rebuild المشروع**

---

## 🏆 الطريقة 3: تدريب نموذج حقيقي (موصى به)

**الوقت:** ساعات إلى أيام  
**النتيجة:** نموذج دقيق يعمل بشكل جيد

### المتطلبات:

1. **بيانات تدريب:**
   - صور أو landmarks للحروف العربية
   - 100-500 عينة لكل حرف
   - أشخاص مختلفين

2. **تعديل السكريبت:**
   - افتح `train_arabic_letters_model.py`
   - استبدل `generate_sample_data()` بتحميل البيانات الحقيقية

3. **التدريب:**
```bash
python train_arabic_letters_model.py
```

---

## 🔧 المتطلبات الأساسية

### تثبيت Python Libraries:

```bash
pip install tensorflow numpy
```

أو من ملف requirements.txt:
```bash
pip install -r requirements.txt
```

---

## 📊 مواصفات النموذج

### المدخلات (Input):
- **الشكل:** `[63]` أو `[5, 63]` (لـ LSTM)
- **63 = 21 نقطة × 3 إحداثيات** (x, y, z)

### المخرجات (Output):
- **28 حرف عربي** (أ، ب، ت، ...)
- **+ كلمات** (مرحبا، شكرا، ...)

### البنية:
- **Dense:** 256 → 128 → 64 → 28
- **LSTM:** LSTM(128) → LSTM(64) → Dense(64) → 28

---

## ✅ التحقق من النموذج

بعد إنشاء الملف، تحقق من:

1. **الموقع:**
   ```
   app/src/main/assets/arabic_sign_lstm.tflite
   ```

2. **الحجم:**
   - نموذج تجريبي: ~500 KB - 2 MB
   - نموذج حقيقي: 2-10 MB

3. **الاختبار:**
   - شغّل التطبيق
   - افتح صفحة الكاميرا
   - جرب إشارة

---

## 🐛 حل المشاكل

### خطأ: "Model file not found"
- تأكد من نسخ الملف إلى `app/src/main/assets/`
- Clean و Rebuild المشروع

### خطأ: "Interpreter not initialized"
- تحقق من أن الملف غير مضغوط
- تأكد من تنسيق الملف (TFLite)

### خطأ: "Invalid input shape"
- تحقق من أن النموذج يتوقع `[63]` أو `[5, 63]`
- راجع `SignLanguageClassifier.kt`

---

## 📝 ملاحظات مهمة

1. **النموذج التجريبي:**
   - يعمل للاختبار فقط
   - النتائج غير دقيقة
   - يجب استبداله بنموذج مدرب

2. **النموذج الحقيقي:**
   - يحتاج بيانات تدريب حقيقية
   - التدريب يستغرق وقتاً طويلاً
   - النتائج دقيقة

3. **التحديث:**
   - عند تحديث النموذج، احذف القديم أولاً
   - Clean و Rebuild المشروع

---

## 🚀 الخطوات السريعة (ملخص)

```bash
# 1. انتقل لمجلد scripts
cd scripts

# 2. شغّل السكريبت
python create_dummy_model.py

# 3. انسخ الملف
copy arabic_sign_lstm.tflite ..\app\src\main\assets\

# 4. في Android Studio: Clean > Rebuild
```

✅ **تم!** النموذج جاهز للاستخدام

