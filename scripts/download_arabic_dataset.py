#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
تنزيل وتحضير dataset الحروف العربية للغة الإشارة
Download and Prepare Arabic Sign Language Dataset
"""

import os
import json
import requests
from pathlib import Path

def download_arsl_dataset():
    """
    تنزيل dataset من مصادر مختلفة
    """
    
    print("=" * 70)
    print("📦 مصادر datasets للحروف العربية في لغة الإشارة")
    print("=" * 70)
    
    datasets = [
        {
            "name": "ArSL2018 Dataset",
            "source": "King Saud University",
            "size": "54,049 images",
            "classes": "32 (28 letters + 4 words)",
            "url": "https://www.kaggle.com/datasets/ammarsayedtaha/arabic-sign-language",
            "description": "أفضل dataset للحروف العربية"
        },
        {
            "name": "Arabic Alphabets Sign Language Dataset",
            "source": "Kaggle",
            "size": "14,400 images",
            "classes": "28 letters",
            "url": "https://www.kaggle.com/datasets/grassknoted/asl-alphabet",
            "description": "dataset بسيط ومرتب"
        },
        {
            "name": "ArSLVD Dataset",
            "source": "ResearchGate",
            "size": "5,000+ videos",
            "classes": "28 letters + words",
            "url": "https://www.researchgate.net/publication/Arabic_Sign_Language",
            "description": "فيديوهات بدل صور"
        },
        {
            "name": "Arabic Sign Language MNIST",
            "source": "GitHub",
            "size": "2,000 images",
            "classes": "28 letters",
            "url": "https://github.com/examples/arabic-sign-language",
            "description": "مناسب للبداية"
        }
    ]
    
    print("\n📚 Datasets المتاحة:\n")
    
    for i, dataset in enumerate(datasets, 1):
        print(f"{i}. {dataset['name']}")
        print(f"   المصدر: {dataset['source']}")
        print(f"   الحجم: {dataset['size']}")
        print(f"   الأصناف: {dataset['classes']}")
        print(f"   الوصف: {dataset['description']}")
        print(f"   🔗 الرابط: {dataset['url']}")
        print()
    
    print("-" * 70)
    print("\n⚠️  ملاحظة: معظم datasets تحتاج تسجيل في Kaggle")
    print()
    print("📋 خطوات التنزيل اليدوي:")
    print("   1. سجل حساب في Kaggle (مجاني)")
    print("   2. ادخل على الرابط أعلاه")
    print("   3. اضغط 'Download'")
    print("   4. فك ضغط الملف")
    print("   5. ضع المجلد في: scripts/dataset/")
    print()

def check_dataset_structure():
    """
    التحقق من بنية dataset
    """
    
    print("=" * 70)
    print("📁 بنية Dataset المتوقعة")
    print("=" * 70)
    
    structure = """
scripts/
├── dataset/
│   ├── أ/
│   │   ├── image_001.jpg
│   │   ├── image_002.jpg
│   │   └── ...
│   ├── ب/
│   │   ├── image_001.jpg
│   │   └── ...
│   ├── ت/
│   └── ...
│
└── train_arabic_letters_model.py
    """
    
    print(structure)
    print()
    
    dataset_dir = Path("dataset")
    
    if dataset_dir.exists():
        print("✅ مجلد dataset موجود!")
        
        # عد المجلدات (الحروف)
        letters = [d for d in dataset_dir.iterdir() if d.is_dir()]
        print(f"   عدد الحروف: {len(letters)}")
        
        # عد الصور في كل مجلد
        total_images = 0
        for letter_dir in letters:
            images = list(letter_dir.glob("*.jpg")) + list(letter_dir.glob("*.png"))
            total_images += len(images)
            if len(images) > 0:
                print(f"   {letter_dir.name}: {len(images)} صورة")
        
        print(f"\n   إجمالي الصور: {total_images}")
        
        if total_images > 0:
            print("\n✅ Dataset جاهز للاستخدام!")
        else:
            print("\n⚠️  المجلدات فارغة - نزّل الصور")
    else:
        print("❌ مجلد dataset غير موجود")
        print(f"   أنشئ المجلد: {dataset_dir.absolute()}")

def create_sample_dataset():
    """
    إنشاء dataset تجريبي صغير
    """
    
    print("\n" + "=" * 70)
    print("🎨 إنشاء Dataset تجريبي")
    print("=" * 70)
    
    dataset_dir = Path("dataset_sample")
    dataset_dir.mkdir(exist_ok=True)
    
    arabic_letters = ["أ", "ب", "ت", "ث", "ج"]
    
    print(f"\n📁 إنشاء مجلدات ل {len(arabic_letters)} حروف...")
    
    for letter in arabic_letters:
        letter_dir = dataset_dir / letter
        letter_dir.mkdir(exist_ok=True)
        
        # إنشاء ملف README في كل مجلد
        readme = letter_dir / "README.txt"
        readme.write_text(
            f"ضع هنا صور حرف '{letter}' في لغة الإشارة العربية\n"
            f"عدد الصور المطلوب: 50-100 صورة\n"
            f"الصيغة: .jpg أو .png\n",
            encoding="utf-8"
        )
    
    print(f"✅ تم إنشاء {len(arabic_letters)} مجلدات في: {dataset_dir.absolute()}")
    print("\n📸 الآن:")
    print("   1. استخدم الكاميرا لالتقاط صور اليد")
    print("   2. احفظ كل حرف في مجلده")
    print("   3. 50-100 صورة لكل حرف")
    print("   4. استخدم إضاءة جيدة وخلفية واضحة")

def alternative_approach():
    """
    بديل: استخدام MediaPipe لجمع landmarks مباشرة
    """
    
    print("\n" + "=" * 70)
    print("💡 بديل: جمع Landmarks بدل الصور")
    print("=" * 70)
    
    print("""
الطريقة الأسرع: استخدم التطبيق نفسه لجمع البيانات!

1. في SignToTextViewModel.kt، أضف:

    fun saveLandmarksForTraining(landmarks: List<HandLandmark>, label: String) {
        val data = mapOf(
            "label" to label,
            "landmarks" to landmarks.map { 
                mapOf("x" to it.x, "y" to it.y, "z" to it.z) 
            },
            "timestamp" to System.currentTimeMillis()
        )
        
        // حفظ في ملف JSON
        val file = File(context.filesDir, "training_data.json")
        file.appendText(Json.encodeToString(data) + "\\n")
    }

2. في الشاشة، أضف زر "حفظ للتدريب":

    Button(onClick = { 
        viewModel.saveLandmarksForTraining(landmarks, selectedLetter)
    }) {
        Text("💾 حفظ حرف: $selectedLetter")
    }

3. اجمع 50-100 عينة لكل حرف

4. استخرج الملف:
    adb pull /data/data/com.example.handspeak/files/training_data.json

5. استخدمه في train_arabic_letters_model.py

ميزات هذه الطريقة:
✅ سريعة
✅ دقيقة (landmarks جاهزة)
✅ موحدة (نفس المعالجة)
✅ لا تحتاج صور كثيرة
    """)

def main():
    """
    الدالة الرئيسية
    """
    
    print("\n")
    print("🌟" * 35)
    print("       دليل تحضير Dataset للحروف العربية")
    print("🌟" * 35)
    
    # عرض مصادر datasets
    download_arsl_dataset()
    
    # التحقق من البنية
    check_dataset_structure()
    
    # إنشاء dataset تجريبي
    create_sample_dataset()
    
    # البديل
    alternative_approach()
    
    print("\n" + "=" * 70)
    print("✅ انتهى الدليل!")
    print("=" * 70)
    print("\n🎯 الخطوات الموصى بها:")
    print("   1. استخدم البديل (جمع landmarks من التطبيق)")
    print("   2. أو نزّل dataset من Kaggle")
    print("   3. درّب النموذج باستخدام train_arabic_letters_model.py")
    print("   4. انسخ النموذج الجديد إلى assets/")
    print("\n")

if __name__ == "__main__":
    main()
