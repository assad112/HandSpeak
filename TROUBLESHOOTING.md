# 🔧 دليل حل المشاكل - HandSpeak

هذا الدليل يساعدك في حل المشاكل الشائعة التي قد تواجهها.

## 🚨 المشاكل الشائعة والحلول

### ❌ مشكلة: KAPT Error مع Java 17+

**الخطأ:**
```
java.lang.IllegalAccessError: class org.jetbrains.kotlin.kapt3.base.javac.KaptJavaCompiler 
cannot access class com.sun.tools.javac.main.JavaCompiler 
because module jdk.compiler does not export com.sun.tools.javac.main
```

**السبب:**
- تستخدم Java 17 أو أحدث
- KAPT يحتاج الوصول لفئات داخلية في JDK
- نظام الموديولات في Java 9+ يمنع هذا الوصول

**الحل ✅ (تم تطبيقه):**

تم إضافة JVM arguments في ملف `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 \
  --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

**خطوات الحل:**

1. **أعد تشغيل Gradle Daemon:**
   ```bash
   ./gradlew --stop
   ```

2. **نظّف المشروع:**
   ```bash
   ./gradlew clean
   ```

3. **أعد البناء:**
   ```bash
   ./gradlew build
   ```

4. **في Android Studio:**
   - File → Invalidate Caches / Restart
   - اختر "Invalidate and Restart"

---

### ❌ مشكلة: Gradle Sync Failed

**الخطأ:**
```
Gradle sync failed: ...
```

**الحلول:**

#### الحل 1: تحديث Gradle Wrapper
```bash
./gradlew wrapper --gradle-version=8.4
```

#### الحل 2: التحقق من JDK
1. File → Project Structure → SDK Location
2. تأكد من استخدام JDK 17 أو JDK 11
3. لا تستخدم JRE فقط

#### الحل 3: حذف ملفات Gradle المؤقتة
```bash
# Windows PowerShell
Remove-Item -Recurse -Force .gradle
Remove-Item -Recurse -Force build
Remove-Item -Recurse -Force app/build

# ثم
./gradlew clean
./gradlew build
```

#### الحل 4: تحديث repositories
تأكد من أن ملف `settings.gradle.kts` يحتوي على:
```kotlin
repositories {
    google()
    mavenCentral()
}
```

---

### ❌ مشكلة: Cannot find symbol: MediaPipe/TensorFlow

**الخطأ:**
```
error: cannot find symbol
import com.google.mediapipe...
```

**السبب:**
المكتبات لم يتم تحميلها بشكل صحيح.

**الحل:**

1. **تأكد من الاتصال بالإنترنت**

2. **Sync Gradle مع refresh:**
   ```bash
   ./gradlew --refresh-dependencies
   ```

3. **في Android Studio:**
   - File → Sync Project with Gradle Files
   - Build → Clean Project
   - Build → Rebuild Project

4. **تحقق من ملف libs.versions.toml:**
   تأكد من وجود جميع المكتبات

---

### ❌ مشكلة: Model file not found

**الخطأ:**
```
FileNotFoundException: arabic_sign_lstm.tflite
```

**السبب:**
ملفات النماذج غير موجودة في مجلد assets.

**الحل:**

1. **تحميل نموذج MediaPipe:**
   ```bash
   cd app/src/main/assets
   # حمّل من:
   # https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task
   ```

2. **إضافة نموذج TFLite:**
   - ضع ملف `arabic_sign_lstm.tflite` في `app/src/main/assets/`
   - أو استخدم نموذج تجريبي

3. **تحقق من البنية:**
   ```
   app/src/main/assets/
   ├── arabic_sign_lstm.tflite
   ├── hand_landmarker.task
   ├── labels.json
   └── sign_map.json
   ```

4. **Clean و Rebuild:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

---

### ❌ مشكلة: Camera permission denied

**الأعراض:**
التطبيق يطلب إذن الكاميرا ولا يعمل بعد منح الإذن.

**الحل:**

1. **إلغاء تثبيت التطبيق تماماً:**
   ```bash
   adb uninstall com.example.handspeak
   ```

2. **إعادة التثبيت:**
   ```bash
   ./gradlew installDebug
   ```

3. **منح الإذن يدوياً:**
   - الإعدادات → التطبيقات → HandSpeak
   - الأذونات → الكاميرا → السماح

4. **التحقق من AndroidManifest:**
   تأكد من وجود:
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   ```

---

### ❌ مشكلة: App crashes on startup

**الحلول:**

#### خطوة 1: فحص Logcat
```bash
adb logcat | grep -E "AndroidRuntime|HandSpeak"
```

#### خطوة 2: مشاكل شائعة ونحلها:

**إذا كان الخطأ: ClassNotFoundException**
```bash
./gradlew clean
./gradlew assembleDebug
```

**إذا كان الخطأ: ResourceNotFoundException**
- تحقق من ملف `strings.xml`
- تحقق من ملفات drawable

**إذا كان الخطأ: Database migration**
```bash
# في Kotlin code، أضف:
Room.databaseBuilder(context, AppDatabase::class.java, "handspeak_database")
    .fallbackToDestructiveMigration()  // للتطوير فقط!
    .build()
```

#### خطوة 3: Invalidate Caches
- File → Invalidate Caches / Restart
- اختر "Invalidate and Restart"

---

### ❌ مشكلة: Build takes too long

**الحلول:**

#### 1. تفعيل Parallel Builds
في `gradle.properties`:
```properties
org.gradle.parallel=true
org.gradle.caching=true
```

#### 2. زيادة Heap Size
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 ...
```

#### 3. تعطيل Unused Features مؤقتاً
في `app/build.gradle.kts`:
```kotlin
android {
    // للتطوير فقط
    aaptOptions {
        cruncherEnabled = false
    }
}
```

---

### ❌ مشكلة: Emulator camera not working

**الحل:**

1. **في AVD Manager:**
   - Edit Virtual Device
   - Show Advanced Settings
   - Camera:
     - Front: Webcam0 (أو Emulated)
     - Back: Webcam0 (أو Emulated)

2. **إعادة تشغيل Emulator**

3. **بديل: استخدام جهاز حقيقي**
   ```bash
   # تحقق من الأجهزة المتصلة
   adb devices
   
   # تشغيل على جهاز محدد
   adb -s <device-id> install app/build/outputs/apk/debug/app-debug.apk
   ```

---

### ❌ مشكلة: MediaPipe initialization failed

**الخطأ:**
```
Failed to create HandLandmarker
```

**الحلول:**

1. **تحقق من وجود الملف:**
   ```bash
   ls app/src/main/assets/hand_landmarker.task
   ```

2. **حجم الملف:**
   - يجب أن يكون حوالي 10-15 MB
   - إذا كان أصغر، الملف تالف - أعد التحميل

3. **في الكود، أضف try-catch:**
   ```kotlin
   try {
       handLandmarker = HandLandmarker.createFromOptions(context, options)
       Log.d(TAG, "HandLandmarker initialized successfully")
   } catch (e: Exception) {
       Log.e(TAG, "Failed to create HandLandmarker", e)
       onError("Error initializing hand detection: ${e.message}")
   }
   ```

---

### ❌ مشكلة: TensorFlow Lite GPU Delegate Error

**الخطأ:**
```
Supertypes of the following classes cannot be resolved:
class org.tensorflow.lite.gpu.GpuDelegate.Options, 
unresolved supertypes: org.tensorflow.lite.gpu.GpuDelegateFactory.Options
```

**السبب:**
نقص في تبعيات TensorFlow Lite GPU.

**الحل ✅ (تم تطبيقه):**

تم إضافة التبعيات المطلوبة:
```kotlin
// في gradle/libs.versions.toml
tensorflow-lite-gpu = { group = "org.tensorflow", name = "tensorflow-lite-gpu-delegate-plugin", version.ref = "tensorflow" }
tensorflow-lite-gpu-api = { group = "org.tensorflow", name = "tensorflow-lite-gpu-api", version.ref = "tensorflow" }
```

**الحل البديل (تعطيل GPU مؤقتاً):**

في `SignLanguageClassifier.kt`:
```kotlin
init {
    // تعطيل GPU مؤقتاً
    gpuDelegate = null
    
    val options = Interpreter.Options().apply {
        setNumThreads(4)  // استخدام CPU فقط
    }
    interpreter = Interpreter(model, options)
}
```

---

### ❌ مشكلة: TensorFlow Lite error

**الخطأ:**
```
Cannot load model / Invalid model file
```

**الحلول:**

1. **تحقق من تنسيق النموذج:**
   - يجب أن يكون `.tflite`
   - حجم معقول (1-50 MB)

2. **تحقق من Input/Output Shape:**
   ```kotlin
   val inputDetails = interpreter?.getInputTensor(0)
   Log.d(TAG, "Input shape: ${inputDetails?.shape()?.contentToString()}")
   // يجب أن يكون: [1, 63] أو [1, 1, 63]
   ```

3. **استخدام نموذج بسيط للاختبار:**
   يمكنك مؤقتاً تخطي استخدام النموذج:
   ```kotlin
   fun classify(landmarks: FloatArray): Pair<String, Float>? {
       // للاختبار فقط
       return Pair("اختبار", 0.95f)
   }
   ```

---

### ❌ مشكلة: Out of Memory (OOM)

**الأعراض:**
```
java.lang.OutOfMemoryError
```

**الحلول:**

1. **زيادة Heap في gradle.properties:**
   ```properties
   org.gradle.jvmargs=-Xmx4096m
   ```

2. **في AndroidManifest.xml:**
   ```xml
   <application
       android:largeHeap="true"
       ...>
   ```

3. **تحسين استخدام الذاكرة:**
   ```kotlin
   // إغلاق الموارد
   override fun onCleared() {
       super.onCleared()
       classifier.close()
       handDetectionHelper.close()
   }
   ```

---

## 🛠️ أدوات التشخيص

### فحص Build Configuration
```bash
./gradlew :app:dependencies
```

### فحص APK
```bash
./gradlew :app:assembleDebug
# ثم
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep assets
```

### مراقبة Logs
```bash
# All logs
adb logcat

# Filtered for HandSpeak
adb logcat | grep HandSpeak

# Errors only
adb logcat *:E

# Multiple filters
adb logcat | grep -E "HandSpeak|TensorFlow|MediaPipe|AndroidRuntime"
```

### فحص الذاكرة
```bash
adb shell dumpsys meminfo com.example.handspeak
```

---

## 📞 الحصول على المساعدة

إذا لم تحل المشكلة:

1. **GitHub Issues:**
   - افتح issue جديد
   - أرفق logs
   - اذكر إصدار Android
   - اذكر نوع الجهاز

2. **معلومات مفيدة للإبلاغ:**
   ```bash
   # نسخة Android Studio
   # نسخة Gradle
   ./gradlew --version
   
   # نسخة Java
   java -version
   
   # معلومات الجهاز
   adb shell getprop ro.build.version.release
   ```

3. **قالب الإبلاغ عن مشكلة:**
   ```markdown
   **الوصف:**
   [وصف المشكلة]
   
   **الخطوات:**
   1. ...
   2. ...
   
   **البيئة:**
   - Android Studio: [version]
   - Gradle: [version]
   - Java: [version]
   - الجهاز: [device]
   - Android: [version]
   
   **Logs:**
   ```
   [أرفق logs هنا]
   ```
   ```

---

## ✅ نصائح لتجنب المشاكل

1. **استخدم إصدارات مستقرة:**
   - Java 11 أو Java 17
   - أحدث Gradle Stable
   - أحدث Android Studio Stable

2. **Clean بشكل دوري:**
   ```bash
   ./gradlew clean
   ```

3. **Sync بعد كل تغيير في Gradle:**
   - File → Sync Project with Gradle Files

4. **استخدم جهاز حقيقي للاختبار:**
   - الكاميرا والأداء أفضل

5. **راقب Logs باستمرار:**
   ```bash
   adb logcat | grep HandSpeak
   ```

---

تم تحديثه: نوفمبر 2025  
إذا واجهت مشكلة غير موجودة هنا، افتح Issue على GitHub!

