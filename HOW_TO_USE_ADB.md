# 📱 كيفية استخدام ADB (Android Debug Bridge)

**تاريخ التحديث**: نوفمبر 2025

---

## 🔍 ما هو ADB؟

**ADB** = Android Debug Bridge
- أداة من Google لاتصال الكمبيوتر بجهاز Android
- تسمح بتشغيل أوامر على الجهاز
- متوفرة في Android SDK

---

## 📍 أين تجد ADB؟

### الطريقة 1: في Android Studio

**الموقع:**
```
C:\Users\[اسم_المستخدم]\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

**أو:**
```
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
```

---

### الطريقة 2: من PowerShell (في مجلد المشروع)

**الخطوات:**
1. افتح PowerShell في مجلد المشروع
2. استخدم المسار الكامل:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt
   ```

---

### الطريقة 3: إضافة ADB إلى PATH

**الخطوات:**
1. افتح **Environment Variables**:
   - اضغط `Win + R`
   - اكتب: `sysdm.cpl`
   - اضغط Enter
   - اذهب إلى **Advanced → Environment Variables**

2. أضف إلى **Path**:
   ```
   C:\Users\[اسم_المستخدم]\AppData\Local\Android\Sdk\platform-tools
   ```

3. أعد تشغيل PowerShell

4. الآن يمكنك استخدام:
   ```bash
   adb logcat > logcat.txt
   ```

---

## 🚀 كيفية استخدام الأمر

### الطريقة 1: من PowerShell (في مجلد المشروع)

**الخطوات:**
1. افتح PowerShell في مجلد المشروع:
   ```
   C:\Users\HP\AndroidStudioProjects\HandSpeak
   ```

2. شغّل الأمر:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt
   ```

3. **الملف سيتم حفظه في:**
   ```
   C:\Users\HP\AndroidStudioProjects\HandSpeak\logcat.txt
   ```

---

### الطريقة 2: من Android Studio Terminal

**الخطوات:**
1. افتح Android Studio
2. اذهب إلى **View → Tool Windows → Terminal**
3. شغّل:
   ```bash
   adb logcat > logcat.txt
   ```

---

### الطريقة 3: من Command Prompt

**الخطوات:**
1. افتح Command Prompt (cmd)
2. اذهب إلى مجلد المشروع:
   ```cmd
   cd C:\Users\HP\AndroidStudioProjects\HandSpeak
   ```
3. شغّل:
   ```cmd
   "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt
   ```

---

## 📝 أوامر مفيدة

### 1. تحقق من الاتصال:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

**يجب أن ترى:**
```
List of devices attached
XXXXXXXX    device
```

---

### 2. مسح Logcat ثم تسجيل:
```powershell
# مسح السجل
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c

# تسجيل جديد
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt
```

---

### 3. تصفية Logcat (MainActivity فقط):
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat | Select-String "MainActivity" > logcat.txt
```

---

### 4. تصفية الأخطاء فقط:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat *:E > logcat_errors.txt
```

---

## 🎯 خطوات عملية

### للحصول على Logcat:

1. **افتح PowerShell في مجلد المشروع**

2. **تحقق من الاتصال:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
   ```

3. **مسح السجل القديم:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c
   ```

4. **شغّل التطبيق:**
   - افتح التطبيق على الجهاز
   - أو استخدم:
     ```powershell
     & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.handspeak/.MainActivity
     ```

5. **سجّل Logcat:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt
   ```

6. **انتظر 5-10 ثواني ثم اضغط `Ctrl+C` لإيقاف التسجيل**

7. **افتح الملف:**
   ```
   logcat.txt
   ```
   (في نفس المجلد)

---

## 📂 أين يتم حفظ الملف؟

**الملف `logcat.txt` سيتم حفظه في:**
- **المجلد الحالي** الذي شغّلت فيه الأمر
- إذا شغّلت من مجلد المشروع:
  ```
  C:\Users\HP\AndroidStudioProjects\HandSpeak\logcat.txt
  ```

---

## 🔧 بدائل أسهل

### الطريقة 1: استخدام Android Studio Logcat

**أسهل طريقة:**
1. افتح Android Studio
2. اذهب إلى **View → Tool Windows → Logcat**
3. شاهد السجل مباشرة
4. يمكنك حفظه: **File → Save Logcat to File**

---

### الطريقة 2: استخدام Android Studio Terminal

1. افتح Android Studio
2. اذهب إلى **View → Tool Windows → Terminal**
3. شغّل:
   ```bash
   adb logcat > logcat.txt
   ```
4. الملف سيتم حفظه في مجلد المشروع

---

## ⚠️ ملاحظات مهمة

### 1. يجب أن يكون الجهاز متصل:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

### 2. يجب تفعيل USB Debugging:
- Settings → About Phone → اضغط 7 مرات على "Build Number"
- Settings → Developer Options → فعّل "USB Debugging"

### 3. الملف قد يكون كبير:
- استخدم تصفية لتقليل الحجم
- أو أوقف التسجيل بعد بضع ثواني

---

## 📊 مثال كامل

```powershell
# 1. اذهب إلى مجلد المشروع
cd C:\Users\HP\AndroidStudioProjects\HandSpeak

# 2. تحقق من الاتصال
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices

# 3. مسح السجل
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c

# 4. شغّل التطبيق
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.handspeak/.MainActivity

# 5. سجّل Logcat (انتظر 10 ثواني ثم اضغط Ctrl+C)
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt

# 6. افتح الملف
notepad logcat.txt
```

---

## 💡 نصائح

1. ✅ **استخدم Android Studio Logcat** - أسهل وأسرع
2. ✅ **صفّف النتائج** - لتقليل الحجم
3. ✅ **احفظ فقط الأخطاء** - إذا كنت تبحث عن مشكلة
4. ✅ **استخدم PowerShell** - أفضل من cmd

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **دليل كامل**

