#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
سكريبت بسيط لإنشاء ملف arabic_sign_lstm.tflite
Simple script to create arabic_sign_lstm.tflite file
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

print("=" * 60)
print("إنشاء ملف arabic_sign_lstm.tflite")
print("=" * 60)

# التحقق من تثبيت TensorFlow
try:
    # محاولة استيراد TensorFlow
    import tensorflow as tf
    
    # محاولة الحصول على الإصدار
    try:
        tf_version = tf.__version__
        print(f"[OK] TensorFlow version: {tf_version}")
        
        # تحذير إذا كان الإصدار جديداً جداً
        try:
            major, minor = map(int, tf_version.split('.')[:2])
            if major > 2 or (major == 2 and minor > 16):
                print("[WARNING] إصدار TensorFlow جديد جداً!")
                print("   للحصول على أفضل توافق، استخدم TensorFlow 2.16.1:")
                print("   pip install tensorflow==2.16.1")
                print("   (سيتم المتابعة مع الإصدار الحالي...)")
        except:
            pass  # تجاهل خطأ في التحقق من الإصدار
    except AttributeError:
        # إذا لم يكن __version__ متاحاً، جرب طريقة أخرى
        try:
            tf_version = tf.version.VERSION
            print(f"[OK] TensorFlow version: {tf_version}")
        except:
            print("[OK] TensorFlow مثبت (لا يمكن تحديد الإصدار)")
except ImportError as e:
    print("[ERROR] خطأ: TensorFlow غير مثبت!")
    print(f"   الخطأ: {e}")
    print("   قم بتثبيته: pip install tensorflow==2.16.1")
    sys.exit(1)
except Exception as e:
    print(f"[WARNING] تحذير: {e}")
    print("   سيتم المتابعة...")

# إعدادات النموذج
INPUT_SIZE = 63  # 21 نقطة × 3 إحداثيات
NUM_CLASSES = 48  # 48 فئة (28 حرف + 20 كلمة/عبارة)

print(f"\n[INFO] إعدادات النموذج:")
print(f"   المدخلات: {INPUT_SIZE} (21 نقطة × 3 إحداثيات)")
print(f"   المخرجات: {NUM_CLASSES} (48 فئة: 28 حرف + 20 كلمة/عبارة)")

# إنشاء النموذج
print("\n[INFO] إنشاء النموذج...")

# محاولة استيراد Keras بشكل صحيح
try:
    # محاولة استخدام tf.keras أولاً
    if hasattr(tf, 'keras'):
        keras = tf.keras
        layers = tf.keras.layers
        print("[OK] تم استيراد Keras من tensorflow.keras")
    else:
        # محاولة استيراد مباشر
        from tensorflow import keras
        from tensorflow.keras import layers
        print("[OK] تم استيراد Keras من tensorflow")
except (ImportError, AttributeError):
    try:
        import keras
        from keras import layers
        print("[OK] تم استيراد Keras مباشرة")
    except ImportError:
        print("[ERROR] لا يمكن استيراد Keras!")
        sys.exit(1)

# استخدام tf.keras مباشرة
model = tf.keras.Sequential([
    tf.keras.layers.InputLayer(shape=(INPUT_SIZE,), name='input'),
    tf.keras.layers.Dense(256, activation='relu', name='dense_1'),
    tf.keras.layers.Dropout(0.3, name='dropout_1'),
    tf.keras.layers.Dense(128, activation='relu', name='dense_2'),
    tf.keras.layers.Dropout(0.3, name='dropout_2'),
    tf.keras.layers.Dense(64, activation='relu', name='dense_3'),
    tf.keras.layers.Dropout(0.2, name='dropout_3'),
    tf.keras.layers.Dense(NUM_CLASSES, activation='softmax', name='output')
])

# تجميع النموذج
model.compile(
    optimizer='adam',
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

print("[OK] تم إنشاء النموذج")

# تدريب تجريبي (مع بيانات عشوائية)
print("\n[INFO] تدريب تجريبي (بيانات عشوائية)...")
X_dummy = np.random.rand(100, INPUT_SIZE).astype(np.float32)
y_dummy = np.random.rand(100, NUM_CLASSES).astype(np.float32)
y_dummy = y_dummy / y_dummy.sum(axis=1, keepdims=True)  # Normalize

model.fit(X_dummy, y_dummy, epochs=1, batch_size=32, verbose=0)
print("[OK] تم التدريب")

# تحويل إلى TFLite
print("\n[INFO] تحويل إلى TensorFlow Lite...")
print("   استخدام إصدار متوافق مع TensorFlow Lite 2.16.1...")

# استخدام SavedModel بدلاً من Keras model مباشرة
try:
    # حفظ النموذج كـ SavedModel أولاً باستخدام model.export()
    import tempfile
    with tempfile.TemporaryDirectory() as tmpdir:
        saved_model_path = os.path.join(tmpdir, "saved_model")
        model.export(saved_model_path)
        print("[OK] تم حفظ النموذج كـ SavedModel")
        
        # تحويل SavedModel إلى TFLite
        converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_path)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS
        ]
        tflite_model = converter.convert()
        print("[OK] تم تحويل النموذج إلى TFLite")
except Exception as e:
    print(f"[WARNING] فشل استخدام SavedModel: {e}")
    print("   محاولة استخدام ConcreteFunction...")
    try:
        # محاولة استخدام ConcreteFunction
        @tf.function
        def model_func(x):
            return model(x)
        
        # إنشاء input signature
        input_shape = (1, INPUT_SIZE)
        concrete_func = model_func.get_concrete_function(
            tf.TensorSpec(shape=input_shape, dtype=tf.float32)
        )
        
        converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS
        ]
        tflite_model = converter.convert()
        print("[OK] تم تحويل النموذج إلى TFLite باستخدام ConcreteFunction")
    except Exception as e2:
        print(f"[ERROR] فشل جميع المحاولات: {e2}")
        sys.exit(1)

# حفظ الملف
output_path = "arabic_sign_lstm.tflite"
with open(output_path, 'wb') as f:
    f.write(tflite_model)

file_size = os.path.getsize(output_path) / (1024 * 1024)  # MB
print(f"[OK] تم حفظ الملف: {output_path}")
print(f"   الحجم: {file_size:.2f} MB")

# نسخ إلى assets
assets_path = "../app/src/main/assets/arabic_sign_lstm.tflite"
try:
    # إنشاء المجلد إذا لم يكن موجوداً
    os.makedirs(os.path.dirname(assets_path), exist_ok=True)
    
    # نسخ الملف
    import shutil
    shutil.copy(output_path, assets_path)
    print(f"[OK] تم نسخ الملف إلى: {assets_path}")
except Exception as e:
    print(f"[WARNING] لم يتم النسخ تلقائياً: {e}")
    print(f"   انسخ الملف يدوياً من: {output_path}")
    print(f"   إلى: {assets_path}")

print("\n" + "=" * 60)
print("[OK] تم الانتهاء بنجاح!")
print("=" * 60)
print("\n[INFO] الخطوات التالية:")
print("   1. في Android Studio: Build > Clean Project")
print("   2. Build > Rebuild Project")
print("   3. شغّل التطبيق واختبره")
print("\n[NOTE] ملاحظة:")
print("   هذا نموذج تجريبي للاختبار فقط!")
print("   للحصول على نتائج دقيقة، قم بتدريبه على بيانات حقيقية.")
print("\n")

