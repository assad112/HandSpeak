#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
سكريبت لتدريب النموذج على بيانات حقيقية
Script to train the model on real data
"""

import sys
import os

# إصلاح مشكلة encoding في Windows
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

import tensorflow as tf
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import json
import shutil

print("=" * 60)
print("تدريب النموذج على بيانات حقيقية")
print("=" * 60)

# التحقق من TensorFlow
try:
    import tensorflow as tf
    tf_version = tf.__version__
    print(f"[OK] TensorFlow version: {tf_version}")
except ImportError:
    print("[ERROR] TensorFlow غير مثبت!")
    print("   قم بتثبيته: pip install tensorflow==2.16.1")
    sys.exit(1)

# إعدادات النموذج
INPUT_SIZE = 63  # 21 نقطة × 3 إحداثيات
NUM_CLASSES = 48  # 48 فئة (28 حرف + 20 كلمة/عبارة)

print(f"\n[INFO] إعدادات النموذج:")
print(f"   المدخلات: {INPUT_SIZE} (21 نقطة × 3 إحداثيات)")
print(f"   المخرجات: {NUM_CLASSES} (48 فئة)")

# مسار ملف CSV
csv_path = input("\n[INPUT] أدخل مسار ملف CSV (أو اضغط Enter للبحث في scripts/): ").strip()
if not csv_path:
    csv_path = "user_training_data.csv"

if not os.path.exists(csv_path):
    print(f"[ERROR] الملف غير موجود: {csv_path}")
    print("\n[INFO] كيفية الحصول على البيانات:")
    print("   1. شغّل التطبيق")
    print("   2. افتح صفحة الكاميرا")
    print("   3. اعط إشارة واكتب اسمها")
    print("   4. كرر العملية لعدة إشارات")
    print("   5. ابحث عن الملف في:")
    print("      /data/data/com.example.handspeak/files/training_data/user_training_data.csv")
    print("   6. انسخ الملف إلى scripts/")
    sys.exit(1)

print(f"[OK] تم العثور على الملف: {csv_path}")

# قراءة البيانات
print("\n[INFO] قراءة البيانات...")
try:
    df = pd.read_csv(csv_path)
    print(f"[OK] تم قراءة {len(df)} عينة")
    
    # عرض إحصائيات
    print("\n[INFO] إحصائيات البيانات:")
    label_counts = df['label'].value_counts()
    print(f"   إجمالي العينات: {len(df)}")
    print(f"   عدد التصنيفات: {len(label_counts)}")
    print(f"   أعلى 10 تصنيفات:")
    for label, count in label_counts.head(10).items():
        print(f"      {label}: {count} عينة")
    
    # التحقق من البيانات
    if len(df) < 10:
        print("\n[WARNING] عدد العينات قليل جداً!")
        print("   للحصول على نتائج جيدة، تحتاج على الأقل:")
        print("   - 50 عينة لكل حرف (28 × 50 = 1400 عينة)")
        print("   - 100 عينة لكل كلمة (20 × 100 = 2000 عينة)")
        print("   - إجمالي: ~3400 عينة")
        response = input("\n   هل تريد المتابعة؟ (y/n): ").strip().lower()
        if response != 'y':
            sys.exit(0)
    
    # تحضير البيانات
    print("\n[INFO] تحضير البيانات...")
    
    # استخراج الميزات (features)
    feature_columns = []
    for i in range(21):
        feature_columns.extend([f'x{i}', f'y{i}', f'z{i}'])
    
    X = df[feature_columns].values.astype(np.float32)
    y = df['label'].values
    
    print(f"[OK] الميزات: {X.shape}")
    print(f"[OK] التصنيفات: {len(np.unique(y))} تصنيف فريد")
    
    # Label Encoding
    label_encoder = LabelEncoder()
    y_encoded = label_encoder.fit_transform(y)
    
    # تحويل إلى one-hot encoding
    y_onehot = tf.keras.utils.to_categorical(y_encoded, num_classes=len(label_encoder.classes_))
    
    print(f"[OK] تم تحويل التصنيفات إلى one-hot encoding: {y_onehot.shape}")
    
    # تقسيم البيانات
    print("\n[INFO] تقسيم البيانات...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y_onehot, test_size=0.2, random_state=42, stratify=y_encoded
    )
    
    print(f"[OK] بيانات التدريب: {X_train.shape}")
    print(f"[OK] بيانات الاختبار: {X_test.shape}")
    
    # إنشاء النموذج
    print("\n[INFO] إنشاء النموذج...")
    model = tf.keras.Sequential([
        tf.keras.layers.InputLayer(shape=(INPUT_SIZE,), name='input'),
        tf.keras.layers.Dense(256, activation='relu', name='dense_1'),
        tf.keras.layers.Dropout(0.3, name='dropout_1'),
        tf.keras.layers.Dense(128, activation='relu', name='dense_2'),
        tf.keras.layers.Dropout(0.3, name='dropout_2'),
        tf.keras.layers.Dense(64, activation='relu', name='dense_3'),
        tf.keras.layers.Dropout(0.2, name='dropout_3'),
        tf.keras.layers.Dense(len(label_encoder.classes_), activation='softmax', name='output')
    ])
    
    model.compile(
        optimizer='adam',
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    
    print("[OK] تم إنشاء النموذج")
    model.summary()
    
    # تدريب النموذج
    print("\n[INFO] بدء التدريب...")
    epochs = int(input("   عدد الـ epochs (افتراضي 50): ").strip() or "50")
    batch_size = int(input("   حجم الـ batch (افتراضي 32): ").strip() or "32")
    
    history = model.fit(
        X_train, y_train,
        epochs=int(epochs),
        batch_size=int(batch_size),
        validation_data=(X_test, y_test),
        verbose=1
    )
    
    # تقييم النموذج
    print("\n[INFO] تقييم النموذج...")
    test_loss, test_accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"[OK] دقة الاختبار: {test_accuracy*100:.2f}%")
    print(f"[OK] خسارة الاختبار: {test_loss:.4f}")
    
    # حفظ النموذج
    print("\n[INFO] حفظ النموذج...")
    
    # تحويل إلى TFLite
    print("[INFO] تحويل إلى TensorFlow Lite...")
    try:
        import tempfile
        with tempfile.TemporaryDirectory() as tmpdir:
            saved_model_path = os.path.join(tmpdir, "saved_model")
            model.export(saved_model_path)
            
            converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_path)
            converter.optimizations = [tf.lite.Optimize.DEFAULT]
            converter.target_spec.supported_ops = [
                tf.lite.OpsSet.TFLITE_BUILTINS,
                tf.lite.OpsSet.SELECT_TF_OPS
            ]
            tflite_model = converter.convert()
            
            # حفظ الملف
            output_path = "arabic_sign_lstm.tflite"
            with open(output_path, 'wb') as f:
                f.write(tflite_model)
            
            file_size = os.path.getsize(output_path) / (1024 * 1024)
            print(f"[OK] تم حفظ الملف: {output_path}")
            print(f"   الحجم: {file_size:.2f} MB")
            
            # نسخ إلى assets
            assets_path = "../app/src/main/assets/arabic_sign_lstm.tflite"
            try:
                os.makedirs(os.path.dirname(assets_path), exist_ok=True)
                shutil.copy(output_path, assets_path)
                print(f"[OK] تم نسخ الملف إلى: {assets_path}")
            except Exception as e:
                print(f"[WARNING] لم يتم النسخ تلقائياً: {e}")
                print(f"   انسخ الملف يدوياً من: {output_path}")
                print(f"   إلى: {assets_path}")
    
    except Exception as e:
        print(f"[ERROR] فشل تحويل النموذج: {e}")
        sys.exit(1)
    
    # حفظ labels (للتحقق)
    labels_path = "../app/src/main/assets/labels.json"
    try:
        labels_list = label_encoder.classes_.tolist()
        with open(labels_path, 'w', encoding='utf-8') as f:
            json.dump(labels_list, f, ensure_ascii=False, indent=2)
        print(f"[OK] تم حفظ labels في: {labels_path}")
    except Exception as e:
        print(f"[WARNING] لم يتم حفظ labels: {e}")
    
    print("\n" + "=" * 60)
    print("[OK] تم الانتهاء بنجاح!")
    print("=" * 60)
    print("\n[INFO] الخطوات التالية:")
    print("   1. في Android Studio: Build > Clean Project")
    print("   2. Build > Rebuild Project")
    print("   3. شغّل التطبيق واختبره")
    print("\n")

except Exception as e:
    print(f"[ERROR] خطأ: {e}")
    import traceback
    traceback.print_exc()
    sys.exit(1)

