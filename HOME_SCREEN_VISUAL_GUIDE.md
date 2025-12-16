# 🎨 شرح مرئي للتصميم الجديد

## الصفحة الرئيسية (Home Screen)

```
╔════════════════════════════════════════╗
║                                        ║
║    📱 HANDSPEAK - HOME SCREEN         ║
║                                        ║
║                                        ║
║  ┌────────────────────────────────┐   ║
║  │                                │   ║
║  │  Account              👤       │   ║ ← بطاقة 1
║  │                                │   ║
║  └────────────────────────────────┘   ║
║                                        ║
║  ┌────────────────────────────────┐   ║
║  │                                │   ║
║  │  ☑ DarkMod           🌙        │   ║ ← بطاقة 2 (مع toggle)
║  │                                │   ║
║  └────────────────────────────────┘   ║
║                                        ║
║  ┌────────────────────────────────┐   ║
║  │                                │   ║
║  │  Favorite            ❤️        │   ║ ← بطاقة 3
║  │                                │   ║
║  └────────────────────────────────┘   ║
║                                        ║
║  ┌────────────────────────────────┐   ║
║  │                                │   ║
║  │  Log out (Red)       ↪️        │   ║ ← بطاقة 4 (أحمر للتحذير)
║  │                                │   ║
║  └────────────────────────────────┘   ║
║                                        ║
║                                        ║
╠════════════════════════════════════════╣
║  🏠      📚      📷      🎤      📝   ║ ← شريط التنقل السفلي
║  Home   Learn   Camera  Voice   Text   ║
╚════════════════════════════════════════╝
```

---

## الحالات المختلفة

### 1. Light Mode (الوضع الفاتح)
```
┌──────────────────────────┐
│ Light Background         │
├──────────────────────────┤
│ ┌──────────────────────┐ │
│ │ Light Surface        │ │
│ │ Dark Text            │ │
│ │ Light Icons          │ │
│ └──────────────────────┘ │
├──────────────────────────┤
│ Light Navigation Bar     │
└──────────────────────────┘
```

### 2. Dark Mode (الوضع الداكن)
```
┌──────────────────────────┐
│ Dark Background          │
├──────────────────────────┤
│ ┌──────────────────────┐ │
│ │ Dark Surface         │ │
│ │ Light Text           │ │
│ │ Light Icons          │ │
│ └──────────────────────┘ │
├──────────────────────────┤
│ Dark Navigation Bar      │
└──────────────────────────┘
```

---

## مسارات التنقل

### من صفحة الهوم:
```
Home Screen
    │
    ├──→ [Account] ────────→ Login Screen
    │
    ├──→ [DarkMod] ────────→ Toggle Dark Mode
    │
    ├──→ [Favorite] ──────→ History Screen
    │
    ├──→ [Log out] ──────→ Login Screen (+ Clear Backstack)
    │
    └──→ Bottom Navigation:
        ├──→ 📚 Learn ────────→ Learn Screen
        ├──→ 📷 Camera ──────→ SignToText Screen
        ├──→ 🎤 Voice ────────→ VoiceToSign Screen
        └──→ 📝 Text ─────────→ TextToSign Screen
```

---

## بنية البيانات

### HomeScreenNew Composable
```
HomeScreenNew(navController, settingsViewModel, viewModel)
    │
    ├── Scaffold
    │   ├── bottomBar: NavigationBar
    │   │   ├── Home (selected=true)
    │   │   ├── Learn
    │   │   ├── Camera
    │   │   ├── Voice
    │   │   └── Text
    │   │
    │   └── content: Box
    │       └── Column
    │           ├── Card[Account]
    │           │   └── Row + Icon
    │           │
    │           ├── Card[DarkMode]
    │           │   ├── Row
    │           │   ├── Switch
    │           │   └── Icon
    │           │
    │           ├── Card[Favorite]
    │           │   └── Row + Icon
    │           │
    │           └── Card[Logout]
    │               └── Row + Icon (Red)
    │
    ├── SettingsViewModel
    │   └── uiState.isDarkMode
    │
    ├── AuthViewModel
    │   └── signOut()
    │
    └── AuthRepository
        └── getCurrentUser()
```

---

## آلية عمل Dark Mode

```
┌─────────────────────────────────┐
│   User Toggles DarkMode Switch  │
└────────────────┬────────────────┘
                 │
                 ▼
    ┌──────────────────────────┐
    │SettingsViewModel         │
    │  .setDarkMode(it)        │
    └────────┬─────────────────┘
             │
             ▼
    ┌──────────────────────────┐
    │ settingsState            │
    │ .isDarkMode = true/false │
    └────────┬─────────────────┘
             │
             ▼
    ┌──────────────────────────┐
    │ MaterialTheme            │
    │ يتغير تلقائياً           │
    └────────┬─────────────────┘
             │
             ▼
    ┌──────────────────────────┐
    │ جميع العناصر تتحدث         │
    │ - الخلفيات               │
    │ - الألوان               │
    │ - الأيقونات             │
    │ - النصوص               │
    └──────────────────────────┘
```

---

## حالات الاستخدام

### السيناريو 1: المستخدم العادي
```
1. تشغيل التطبيق
   ↓
2. تسجيل الدخول
   ↓
3. رؤية صفحة الهوم الجديدة
   ↓
4. اختيار إحدى الخيارات
   ↓
5. استخدام الميزات
```

### السيناريو 2: تفعيل Dark Mode
```
1. في صفحة الهوم
   ↓
2. اضغط على Switch في "DarkMod"
   ↓
3. التطبيق يتغير للوضع الداكن فوراً
   ↓
4. الانتقال إلى شاشة أخرى
   ↓
5. تطبيق Dark Mode على تلك الشاشة أيضاً
```

### السيناريو 3: تسجيل الخروج
```
1. في صفحة الهوم
   ↓
2. اضغط على "Log out"
   ↓
3. استدعاء viewModel.signOut()
   ↓
4. تنظيف Backstack
   ↓
5. الانتقال إلى Login Screen
```

---

## الألوان والأنماط

### الألوان الديناميكية:
```
┌─────────────────────────┐
│   Material Theme 3      │
├─────────────────────────┤
│ Primary                 │
│ PrimaryContainer        │
│ Secondary               │
│ SecondaryContainer      │
│ Tertiary                │
│ TertiaryContainer       │
│ Surface                 │
│ SurfaceVariant          │
│ Background              │
│ Error (RED)             │
└─────────────────────────┘
```

### الأشكال:
```
Cards: RoundedCornerShape(12.dp)
Icons: Size(24.dp)
Switch: Scale(0.8f)
```

### المسافات:
```
Padding Top/Bottom/Left/Right: 16.dp
Card Spacing: 12.dp
Row/Column Spacing: 12.dp
```

---

## مثال على الكود

### إنشاء بطاقة:
```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable { navController.navigate(Screen.Account.route) },
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
        Text("Account", style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Default.AccountCircle, contentDescription = "Account")
    }
}
```

---

## الأداء والتحسينات

### قبل التحديث:
- 516 سطر كود
- 3+ Composables
- 4+ Animations
- استهلاك عالي للموارد

### بعد التحديث:
- 238 سطر كود (↓ 54%)
- 1 Composable (↓ 67%)
- 0 Animations (↓ 100%)
- استهلاك منخفض للموارد

---

## الخلاصة البصرية

```
┌─────────────────────────────────┐
│  ✨ Home Screen الجديد         ║
├─────────────────────────────────┤
│                                 │
│  ✅ بسيط وواضح                 │
│  ✅ سهل الاستخدام              │
│  ✅ أداء ممتازة                 │
│  ✅ مدعوم Dark Mode             │
│  ✅ نظيف وحديث                  │
│                                 │
├─────────────────────────────────┤
│  🎨 التصميم احترافي وعملي      │
│  🚀 جاهز للإنتاج               │
└─────────────────────────────────┘
```

---
