# 🎨 معرض التصاميم - صفحة الهوم الجديدة

## 1. الواجهة الرئيسية

### Light Mode (الوضع الفاتح)
```
╔══════════════════════════════════════╗
║                                      ║
║      صفحة الهوم - HandSpeak         ║
║                                      ║
╠══════════════════════════════════════╣
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │                              │   ║
║   │     Account            👤    │   ║
║   │                              │   ║
║   │   (لون: Light Surface)       │   ║
║   └──────────────────────────────┘   ║
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │                              │   ║
║   │ ☑ DarkMod            🌙      │   ║
║   │                              │   ║
║   │ (مع Switch toggle)            │   ║
║   └──────────────────────────────┘   ║
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │                              │   ║
║   │ Favorite              ❤️      │   ║
║   │                              │   ║
║   └──────────────────────────────┘   ║
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │                              │   ║
║   │ Log out (Red Color)   ↪️      │   ║
║   │                              │   ║
║   │ (نص أحمر للتنبيه)            │   ║
║   └──────────────────────────────┘   ║
║                                      ║
╠══════════════════════════════════════╣
║ 🏠 Home  📚 Learn  📷 Cam  🎤 V 📝 T║
╚══════════════════════════════════════╝

الألوان:
- الخلفية: White / Light Gray
- البطاقات: Light Surface Color
- النص: Dark Color
- الأيقونات: Icon Color
- Log out: Red (#FF0000)
```

---

### Dark Mode (الوضع الداكن)
```
╔══════════════════════════════════════╗
║        🌙                            ║
║      صفحة الهوم - HandSpeak         ║
║        🌙                            ║
╠══════════════════════════════════════╣
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   │ 🟫                          🟫│   ║
║   │ 🟫    Account         👤   🟫│   ║
║   │ 🟫                          🟫│   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   └──────────────────────────────┘   ║
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   │ 🟫 ☑ DarkMod      🌙       🟫│   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   └──────────────────────────────┘   ║
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   │ 🟫 Favorite          ❤️     🟫│   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   └──────────────────────────────┘   ║
║                                      ║
║   ┌──────────────────────────────┐   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   │ 🟫 Log out (Red)     ↪️     🟫│   ║
║   │ 🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫🟫      │   ║
║   └──────────────────────────────┘   ║
║                                      ║
╠══════════════════════════════════════╣
║ 🏠 📚 📷 🎤 📝                       ║
╚══════════════════════════════════════╝

الألوان:
- الخلفية: Dark Gray / Black
- البطاقات: Dark Surface Color
- النص: Light Color
- الأيقونات: Light Icon Color
- Log out: Red (#FF0000) - يبقى أحمر دائماً
```

---

## 2. مسارات التنقل

### الخيارات الأربعة
```
┌─────────────────┐
│   Home Screen   │
└────────┬────────┘
         │
         ├─→ [Account]
         │   └─→ 🔄 Transition
         │       └─→ Login Screen 🔑
         │
         ├─→ [DarkMode]
         │   └─→ 🔄 Toggle (فوري)
         │       └─→ Light/Dark Theme
         │
         ├─→ [Favorite]
         │   └─→ 🔄 Transition
         │       └─→ History Screen 📋
         │
         └─→ [Log out]
             └─→ 🔄 Logout + Cleanup
                 └─→ Login Screen 🔑
                     (Backstack مُنظّف)
```

### الشريط السفلي
```
┌────────────────────────────────┐
│      Home Screen (حالي)         │
├────────────────────────────────┤
│                                │
│  🏠      📚      📷            │
│ Home    Learn   Camera         │
│ ✅      
│ (مختار)  (متاح)  (متاح)      │
│                                │
│      🎤      📝               │
│     Voice     Text             │
│    (متاح)    (متاح)          │
│                                │
└────────────────────────────────┘

عند الضغط على أي تبويب:
🏠 Home     → البقاء (لا تنقل)
📚 Learn    → LearnScreen
📷 Camera   → SignToTextScreen
🎤 Voice    → VoiceToSignScreen
📝 Text     → TextToSignScreen
```

---

## 3. آلية عمل Dark Mode

### الحالة 1: Light Mode (افتراضي)
```
┌────────────────────────────────┐
│   Switch = OFF (غير مفعّل)      │
└────────────────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│ settingsState.isDarkMode = false│
└────────────────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│  HandSpeakTheme(                │
│    darkTheme = false            │
│  )                              │
└────────────────────────────────┘
           │
           ▼
    🎨 Light Colors:
    - Background: White
    - Surface: Light Gray
    - Text: Dark Color
    - Icons: Dark Color
```

### الحالة 2: Dark Mode (عند التفعيل)
```
┌────────────────────────────────┐
│   Switch = ON (مفعّل)           │
└────────────────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│ settingsState.isDarkMode = true │
└────────────────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│  HandSpeakTheme(                │
│    darkTheme = true             │
│  )                              │
└────────────────────────────────┘
           │
           ▼
    🎨 Dark Colors:
    - Background: Dark Gray/Black
    - Surface: Dark Surface
    - Text: Light Color
    - Icons: Light Color
```

---

## 4. بنية البطاقات

### بطاقة عادية (Account/Favorite)
```
┌─────────────────────────────┐
│ ╔═══════════════════════════╗ │
│ ║                           ║ │
│ ║  Text       Icon          ║ │
│ ║ (بيسار)    (يمين)         ║ │
│ ║                           ║ │
│ ║ "Account"    👤           ║ │
│ ║                           ║ │
│ ╚═══════════════════════════╝ │
│                               │
│ Shape: RoundedCornerShape(12) │
│ Padding: 16dp من كل الجهات    │
│ Height: 56dp                  │
└─────────────────────────────┘
```

### بطاقة مع Toggle (DarkMode)
```
┌─────────────────────────────────┐
│ ╔═════════════════════════════╗ │
│ ║                             ║ │
│ ║  ☐ Switch  Text   Icon      ║ │
│ ║ (يسار)    (وسط)  (يمين)    ║ │
│ ║  ☑                          ║ │
│ ║ DarkMod          🌙         ║ │
│ ║                             ║ │
│ ╚═════════════════════════════╝ │
│                                 │
│ Switch: Scale 0.8f              │
│ Shape: RoundedCornerShape(12)  │
│ Padding: 16dp                   │
│ Height: 56dp                    │
└─────────────────────────────────┘
```

### بطاقة تحذير (Log out)
```
┌─────────────────────────────┐
│ ╔═══════════════════════════╗ │
│ ║                           ║ │
│ ║  🔴 Text (Red)   Icon 🔴   ║ │
│ ║  (يسار/أحمر)    (يمين/أحمر)║
│ ║                           ║ │
│ ║  Log out         ↪️        ║ │
│ ║                           ║ │
│ ╚═══════════════════════════╝ │
│                               │
│ Text Color: Color.Red         │
│ Icon Color: Color.Red         │
│ Shape: RoundedCornerShape(12) │
│ Padding: 16dp                 │
│ Height: 56dp                  │
└─────────────────────────────┘
```

---

## 5. مقاييس وأحجام

### الأحجام
```
┌─────────────────────────────────────┐
│         الحجم              القيمة     │
├─────────────────────────────────────┤
│ الشاشة الكاملة         fillMaxSize()  │
│ عرض البطاقة            fillMaxWidth() │
│ ارتفاع البطاقة         56.dp         │
│ أيقونة                 24.dp         │
│ Switch                 0.8f (مقياس)  │
│ النص الرئيسي           bodyLarge     │
└─────────────────────────────────────┘
```

### المسافات
```
┌─────────────────────────────────────┐
│      المسافة            القيمة       │
├─────────────────────────────────────┤
│ الهامش الخارجي         16.dp        │
│ المسافة بين البطاقات    12.dp        │
│ داخل البطاقة            16.dp        │
│ بين العناصر             12.dp        │
└─────────────────────────────────────┘
```

---

## 6. انتقالات الملاحة

### عملية التنقل
```
الضغط على بطاقة
    │
    ▼
    ┌────────────────────┐
    │ clickable modifier │
    │ (يكتشف الضغط)      │
    └────────────────────┘
    │
    ▼
    ┌────────────────────────────┐
    │ navController.navigate()    │
    │ (الانتقال إلى الشاشة)      │
    └────────────────────────────┘
    │
    ▼
    ┌──────────────────────────────┐
    │ Transition Animation         │
    │ (انتقال سلس - من Material) │
    └──────────────────────────────┘
    │
    ▼
    🎯 الشاشة الجديدة ظاهرة
```

---

## 7. الحالات المختلفة

### الحالة 1: المستخدم الجديد
```
تشغيل ← تسجيل دخول ← Home Screen الجديد
                          │
                          ├─→ رؤية الخيارات
                          ├─→ تفعيل Dark Mode
                          └─→ استكشاف الميزات
```

### الحالة 2: المستخدم المتكرر
```
فتح التطبيق ← Home Screen
                    │
                    ├─→ Dark Mode فعّل
                    ├─→ وصول سريع للميزات
                    └─→ تنقل سلس
```

### الحالة 3: تسجيل الخروج
```
Home Screen ← اضغط Log out
    │
    ├─→ viewModel.signOut()
    │   (تسجيل خروج)
    │
    ├─→ navController.navigate(Login)
    │   (انتقال إلى Login)
    │
    └─→ popUpTo(Home) { inclusive = true }
        (تنظيف Backstack)
            │
            ▼
        Login Screen جديدة (بدون Home في الخلفية)
```

---

## 8. الألوان في Light/Dark Modes

### Light Mode
```
Background:      #FFFFFF (أبيض)
Surface:         #F5F5F5 (رمادي فاتح)
Primary:         #6200EE (بنفسجي)
Secondary:       #03DAC6 (فيروزي)
Text Primary:    #000000 (أسود)
Text Secondary:  #666666 (رمادي غامق)
Icon Color:      #333333 (رمادي غامق)
Error/Warning:   #FF0000 (أحمر)
```

### Dark Mode
```
Background:      #121212 (أسود غامق)
Surface:         #1E1E1E (رمادي داكن)
Primary:         #BB86FC (بنفسجي فاتح)
Secondary:       #03DAC6 (فيروزي)
Text Primary:    #FFFFFF (أبيض)
Text Secondary:  #AAAAAA (رمادي فاتح)
Icon Color:      #FFFFFF (أبيض)
Error/Warning:   #FF0000 (أحمر) - يبقى نفسه
```

---

## 9. مثال على الكود

### بطاقة Account
```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable { navController.navigate(Screen.Login.route) },
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Account",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = "Account",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

## 10. الرسم البياني الكامل

```
┌──────────────────────────────────────┐
│        HandSpeak - Home Screen       │
│            (التطبيق الكامل)          │
├──────────────────────────────────────┤
│                                      │
│  ┌────────────────────────────────┐  │
│  │  User Interface (Compose)      │  │
│  │                                │  │
│  │  ┌──────────────────────────┐  │  │
│  │  │ 4 Buttons (Cards)        │  │  │
│  │  ├──────────────────────────┤  │  │
│  │  │ 1. Account → Login       │  │  │
│  │  │ 2. DarkMode ↔ Toggle    │  │  │
│  │  │ 3. Favorite → History   │  │  │
│  │  │ 4. Logout → Cleanup     │  │  │
│  │  └──────────────────────────┘  │  │
│  │                                │  │
│  │  ┌──────────────────────────┐  │  │
│  │  │ Bottom Navigation        │  │  │
│  │  ├──────────────────────────┤  │  │
│  │  │ 5 Tabs for Navigation    │  │  │
│  │  └──────────────────────────┘  │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ State Management               │  │
│  │ - SettingsViewModel            │  │
│  │ - AuthViewModel                │  │
│  │ - AuthRepository               │  │
│  └────────────────────────────────┘  │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ Dynamic Colors (Material 3)    │  │
│  │ - Light Mode Colors            │  │
│  │ - Dark Mode Colors             │  │
│  │ - Automatic Adaptation         │  │
│  └────────────────────────────────┘  │
│                                      │
└──────────────────────────────────────┘
```

---

**هذا معرض شامل للتصميم الجديد! 🎨**

جميع الصور والرسوم البيانية موضحة بصيغة ASCII للتوضيح الكامل.

---
