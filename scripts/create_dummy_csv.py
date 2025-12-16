#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
إنشاء ملف CSV تجريبي للاختبار
Create a dummy CSV file for testing
"""

import csv
import random
import sys
import os
from datetime import datetime

# إصلاح مشكلة encoding في Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

print("=" * 60)
print("إنشاء ملف CSV تجريبي")
print("=" * 60)

# قائمة التصنيفات (48 فئة)
labels = [
    "أ", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "هـ", "و", "ي",
    "أحبك", "حقاً_أحبك", "أراك_لاحقاً", "أضحكتني", "سؤال", "اقتباس", "هذا_رهيب", "أنا_أراقبك", "عمل_جيد", "أتمنى_لك_حياة_سعيدة", "موافق", "أنت", "لا", "هذا_ممتاز", "لست_متأكد", "مرحباً", "شجرة", "بطة", "قطة", "هاتف"
]

# عدد العينات لكل تصنيف (افتراضي 10)
samples_per_label = 10

print(f"\n[INFO] إنشاء {samples_per_label} عينة لكل تصنيف من {len(labels)} تصنيف")
print(f"   إجمالي العينات: {samples_per_label * len(labels)}")

# إنشاء Header
header = ["label"]
for i in range(21):
    header.extend([f"x{i}", f"y{i}", f"z{i}"])
header.append("timestamp")

# إنشاء الملف
output_file = "user_training_data.csv"
print(f"\n[INFO] إنشاء الملف: {output_file}")

with open(output_file, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    
    # كتابة Header
    writer.writerow(header)
    
    # إنشاء بيانات تجريبية
    for label in labels:
        for sample in range(samples_per_label):
            row = [label]
            
            # إنشاء landmarks عشوائية (لكن منطقية)
            # x, y بين 0 و 1
            # z بين -0.5 و 0.5
            for i in range(21):
                row.append(random.uniform(0.0, 1.0))  # x
                row.append(random.uniform(0.0, 1.0))  # y
                row.append(random.uniform(-0.5, 0.5))  # z
            
            # timestamp
            row.append(datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
            
            writer.writerow(row)
    
    print(f"[OK] تم إنشاء {samples_per_label * len(labels)} عينة")

file_size = os.path.getsize(output_file) / (1024 * 1024)
print(f"[OK] تم حفظ الملف: {output_file}")
print(f"   الحجم: {file_size:.2f} MB")

print("\n" + "=" * 60)
print("[OK] تم الانتهاء!")
print("=" * 60)
print("\n[INFO] يمكنك الآن استخدام هذا الملف للتدريب:")
print("   python train_model.py")
print("   (أدخل: user_training_data.csv)")
print("\n[WARNING] هذا ملف تجريبي - النتائج لن تكون دقيقة!")
print("   للحصول على نتائج حقيقية، قم بجمع بيانات حقيقية من التطبيق.\n")
