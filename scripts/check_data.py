#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
سكريبت لفحص بيانات التدريب
"""

import sys
import os
import pandas as pd

# إصلاح مشكلة encoding في Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

csv_path = "user_training_data.csv"

if not os.path.exists(csv_path):
    print(f"[ERROR] الملف غير موجود: {csv_path}")
    sys.exit(1)

print("=" * 60)
print("فحص بيانات التدريب")
print("=" * 60)

df = pd.read_csv(csv_path)
print(f"\n[OK] تم قراءة {len(df)} عينة")

# عرض إحصائيات
print("\n[INFO] إحصائيات البيانات:")
label_counts = df['label'].value_counts()
print(f"   إجمالي العينات: {len(df)}")
print(f"   عدد التصنيفات: {len(label_counts)}")
print(f"\n   التوزيع حسب التصنيف:")
for label, count in label_counts.items():
    print(f"      {label}: {count} عينة")

# التحقق من جودة البيانات
print("\n[INFO] جودة البيانات:")
if len(df) < 100:
    print("   ⚠️  عدد العينات قليل جداً!")
    print("   للحصول على دقة جيدة، تحتاج على الأقل 50-100 عينة لكل حرف")
else:
    print("   ✅ عدد العينات جيد")

# التحقق من التوزيع
min_samples = label_counts.min()
max_samples = label_counts.max()
print(f"\n   أقل عدد عينات لحرف: {min_samples}")
print(f"   أكثر عدد عينات لحرف: {max_samples}")

if min_samples < 20:
    print("   ⚠️  بعض الحروف لديها عينات قليلة جداً!")
    print("   حاول جمع المزيد من البيانات لهذه الحروف:")
    for label, count in label_counts.items():
        if count < 20:
            print(f"      - {label}: {count} عينة (يحتاج 50-100)")

print("\n" + "=" * 60)
print("جاهز للتدريب!")
print("=" * 60)

