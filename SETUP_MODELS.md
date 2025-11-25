# إعداد النماذج والملفات المطلوبة

هذا الدليل يشرح كيفية إعداد جميع الملفات المطلوبة لتشغيل تطبيق HandSpeak.

## 📦 الملفات المطلوبة

### 1. نموذج TensorFlow Lite (arabic_sign_lstm.tflite)

هذا هو النموذج الرئيسي المدرب على لغة الإشارة العربية.

#### إذا كنت قد درّبت النموذج بالفعل:
```bash
# انسخ الملف من مجلد التدريب
cp /path/to/your/arabic_sign_lstm.tflite app/src/main/assets/
```

#### إذا لم تكن قد درّبت النموذج:
يمكنك استخدام النموذج التجريبي أو تدريب نموذج جديد باستخدام الكود التالي:

**خطوات التدريب على Google Colab:**

1. قم بتحميل dataset الصور إلى Google Drive
2. استخدم الكود التالي:

```python
# استيراد المكتبات
import mediapipe as mp
import cv2
import numpy as np
import pandas as pd
from tensorflow import keras
from tensorflow.keras import layers
import tensorflow as tf

# 1. استخراج معالم اليد من الصور
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(static_image_mode=True, max_num_hands=1)

def extract_landmarks(image_path):
    image = cv2.imread(image_path)
    results = hands.process(cv2.cvtColor(image, cv2.COLOR_BGR2RGB))
    
    if results.multi_hand_landmarks:
        landmarks = []
        for hand_landmarks in results.multi_hand_landmarks:
            for landmark in hand_landmarks.landmark:
                landmarks.extend([landmark.x, landmark.y, landmark.z])
        return landmarks
    return None

# 2. بناء dataset
# افترض أن لديك مجلد يحتوي على صور منظمة حسب التصنيف
# RGB_ArSL_dataset/
#   ├── أ/
#   ├── ب/
#   └── ...

# 3. بناء نموذج LSTM
def create_model(num_classes):
    model = keras.Sequential([
        layers.InputLayer(input_shape=(1, 63)),  # 21 landmarks × 3
        layers.LSTM(128, return_sequences=True),
        layers.Dropout(0.3),
        layers.LSTM(64),
        layers.Dropout(0.3),
        layers.Dense(128, activation='relu'),
        layers.Dropout(0.2),
        layers.Dense(64, activation='relu'),
        layers.Dense(num_classes, activation='softmax')
    ])
    return model

# 4. تدريب النموذج
model = create_model(num_classes=38)  # عدد التصنيفات
model.compile(
    optimizer='adam',
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# تدريب
history = model.fit(
    X_train, y_train,
    validation_data=(X_test, y_test),
    epochs=50,
    batch_size=32,
    callbacks=[
        keras.callbacks.EarlyStopping(patience=10, restore_best_weights=True)
    ]
)

# 5. تحويل إلى TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# حفظ
with open('arabic_sign_lstm.tflite', 'wb') as f:
    f.write(tflite_model)
```

### 2. نموذج MediaPipe Hand Landmarker

قم بتحميل نموذج MediaPipe من الرابط الرسمي:

```bash
# تحميل النموذج
wget https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task

# نسخ إلى assets
mv hand_landmarker.task app/src/main/assets/
```

**أو باستخدام curl:**
```bash
curl -O https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
mv hand_landmarker.task app/src/main/assets/
```

### 3. ملف التصنيفات (labels.json)

الملف موجود بالفعل، لكن يمكنك تعديله حسب نموذجك:

```json
[
  "أ", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", 
  "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "ه", "و", "ي",
  "مرحبا", "شكرا", "نعم", "لا", "من_فضلك", "آسف", 
  "صباح_الخير", "مساء_الخير", "كيف_حالك", "بخير"
]
```

**مهم:** يجب أن يتطابق ترتيب التصنيفات مع ترتيبها أثناء التدريب!

### 4. خريطة الإشارات (sign_map.json)

الملف موجود، لكن تأكد من تطابق أسماء المجلدات:

```json
{
  "مرحبا": {
    "label": "مرحبا",
    "type": "images",
    "folder": "marhaba"
  },
  "شكرا": {
    "label": "شكرا",
    "type": "images",
    "folder": "shokran"
  }
}
```

### 5. صور الإشارات

قم بإنشاء مجلد لكل إشارة في `app/src/main/assets/signs/`:

```
app/src/main/assets/signs/
├── alef/          # إشارة حرف أ
│   ├── 1.png
│   ├── 2.png
│   ├── 3.png
│   ├── 4.png
│   └── 5.png
├── baa/           # إشارة حرف ب
│   ├── 1.png
│   └── ...
├── marhaba/       # إشارة كلمة مرحبا
│   ├── 1.png
│   └── ...
└── ...
```

**نصائح للصور:**
- الدقة الموصى بها: 512×512 أو 1024×1024
- التنسيق: PNG (مع خلفية شفافة إن أمكن)
- عدد الصور: 3-10 صور لكل إشارة لعمل animation سلس
- تسمية الصور: 1.png, 2.png, 3.png... (بالترتيب)

## 🔍 التحقق من الملفات

بعد إضافة جميع الملفات، يجب أن يكون هيكل المجلد كالتالي:

```
app/src/main/assets/
├── arabic_sign_lstm.tflite       ✓ نموذج TFLite
├── hand_landmarker.task           ✓ نموذج MediaPipe
├── labels.json                    ✓ قائمة التصنيفات
├── sign_map.json                  ✓ خريطة الإشارات
└── signs/                         ✓ مجلد الصور
    ├── alef/
    │   ├── 1.png
    │   └── ...
    ├── baa/
    │   └── ...
    └── ...
```

## ✅ اختبار الإعداد

للتحقق من صحة الإعداد:

### 1. تشغيل المشروع
```bash
./gradlew assembleDebug
```

### 2. التحقق من وجود الملفات في APK
```bash
# فك ضغط APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep assets

# يجب أن ترى:
# assets/arabic_sign_lstm.tflite
# assets/hand_landmarker.task
# assets/labels.json
# assets/sign_map.json
# assets/signs/...
```

### 3. فحص Logs أثناء التشغيل
```bash
adb logcat | grep -E "SignLanguageClassifier|HandDetectionHelper"

# يجب أن ترى:
# Model loaded successfully. Labels count: 38
# HandLandmarker initialized successfully
```

## 🚨 حل المشاكل الشائعة

### خطأ: "Model file not found"
```
الحل:
- تأكد من وجود arabic_sign_lstm.tflite في app/src/main/assets/
- قم بـ Clean و Rebuild للمشروع
```

### خطأ: "Failed to create HandLandmarker"
```
الحل:
- تأكد من تحميل hand_landmarker.task من الرابط الصحيح
- تحقق من اسم الملف (يجب أن يكون بالضبط hand_landmarker.task)
```

### خطأ: "Invalid input shape"
```
الحل:
- تأكد من أن نموذج TFLite يتوقع input shape [1, 63]
- تحقق من تطابق عدد landmarks (21 × 3 = 63)
```

### الصور لا تظهر
```
الحل:
- تأكد من صحة أسماء المجلدات في sign_map.json
- تحقق من وجود الصور في المسار الصحيح
- تأكد من تسمية الصور: 1.png, 2.png, etc.
```

## 📚 موارد إضافية

- [MediaPipe Hand Landmarker Guide](https://developers.google.com/mediapipe/solutions/vision/hand_landmarker)
- [TensorFlow Lite Conversion](https://www.tensorflow.org/lite/convert)
- [RGB Arabic Sign Language Dataset](https://github.com/your-dataset-link)

## 💡 نصائح للحصول على أفضل أداء

1. **تحسين النموذج:**
   - استخدم Quantization لتقليل حجم النموذج
   - فعّل GPU Delegate للأجهزة المدعومة
   - قلل عدد الـ epochs إذا كان النموذج يعاني من overfitting

2. **تحسين الصور:**
   - استخدم صور عالية الجودة
   - اجعل الخلفية بسيطة وموحدة
   - استخدم إضاءة جيدة عند التقاط الصور

3. **تحسين الأداء:**
   - قلل حجم الصور المستخدمة في Animation
   - استخدم ImageCompression
   - قم بـ Cache الصور المحملة

## 🎯 الخلاصة

بعد اتباع هذه الخطوات، يجب أن يكون تطبيقك جاهزاً للتشغيل بكامل ميزاته!

إذا واجهت أي مشاكل، راجع قسم "حل المشاكل" أو افتح Issue على GitHub.











