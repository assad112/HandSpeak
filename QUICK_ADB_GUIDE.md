# 🚀 دليل سريع لاستخدام ADB Logcat

**تاريخ التحديث**: نوفمبر 2025

---

## ✅ ADB موجود ويعمل!

**الموقع:**
```
C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

**الإصدار:** 1.0.41

---

## 🎯 الطريقة الأسهل

### استخدم السكريبت الجاهز:

1. **في PowerShell (في مجلد المشروع):**
   ```powershell
   .\get_logcat.ps1
   ```

2. **سيقوم السكريبت بـ:**
   - ✅ التحقق من ADB
   - ✅ التحقق من الاتصال بالجهاز
   - ✅ مسح السجل القديم
   - ✅ بدء التسجيل
   - ✅ حفظ الملف في `logcat.txt`

---

## 📝 الطريقة اليدوية

### الخطوة 1: افتح PowerShell في مجلد المشروع

**المجلد الحالي:**
```
C:\Users\HP\AndroidStudioProjects\HandSpeak
```

---

### الخطوة 2: تحقق من الاتصال

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

**يجب أن ترى:**
```
List of devices attached
XXXXXXXX    device
```

---

### الخطوة 3: مسح السجل القديم

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c
```

---

### الخطوة 4: شغّل التطبيق

**افتح التطبيق على الجهاز، أو استخدم:**
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.handspeak/.MainActivity
```

---

### الخطوة 5: سجّل Logcat

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt
```

**انتظر 5-10 ثواني ثم اضغط `Ctrl+C` لإيقاف التسجيل**

---

### الخطوة 6: افتح الملف

**الملف موجود في:**
```
C:\Users\HP\AndroidStudioProjects\HandSpeak\logcat.txt
```

**لفتحه:**
```powershell
notepad logcat.txt
```

---

## 🔍 البحث في Logcat

### للبحث عن MainActivity فقط:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat | Select-String "MainActivity" > logcat_mainactivity.txt
```

---

### للبحث عن الأخطاء فقط:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat *:E > logcat_errors.txt
```

---

## 📊 مثال كامل (نسخ ولصق)

```powershell
# 1. تحقق من الاتصال
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices

# 2. مسح السجل
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c

# 3. شغّل التطبيق
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.handspeak/.MainActivity

# 4. سجّل Logcat (انتظر 10 ثواني ثم Ctrl+C)
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat > logcat.txt

# 5. افتح الملف
notepad logcat.txt
```

---

## 💡 نصائح

1. ✅ **استخدم السكريبت** - `.\get_logcat.ps1` (أسهل)
2. ✅ **أو استخدم Android Studio Logcat** - View → Tool Windows → Logcat
3. ✅ **صفّف النتائج** - لتقليل حجم الملف
4. ✅ **احفظ فقط الأخطاء** - إذا كنت تبحث عن مشكلة

---

## 📂 أين يتم حفظ الملف؟

**الملف `logcat.txt` سيتم حفظه في:**
```
C:\Users\HP\AndroidStudioProjects\HandSpeak\logcat.txt
```

---

## ⚠️ ملاحظات

1. **يجب أن يكون الجهاز متصل** بالكمبيوتر
2. **يجب تفعيل USB Debugging** على الجهاز
3. **الملف قد يكون كبير** - استخدم تصفية لتقليل الحجم

---

**آخر تحديث**: نوفمبر 2025  
**الحالة**: ✅ **جاهز للاستخدام**

