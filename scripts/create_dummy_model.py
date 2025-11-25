#!/usr/bin/env python3
"""
سكريبت لإنشاء نموذج TFLite تجريبي لاختبار تطبيق HandSpeak

هذا النموذج التجريبي لن يعطي نتائج دقيقة، لكنه يسمح باختبار التطبيق
قبل تدريب النموذج الحقيقي على البيانات الفعلية.

الاستخدام:
    python create_dummy_model.py

الملف الناتج: arabic_sign_lstm.tflite
"""

import tensorflow as tf
import numpy as np
import os

print("🔧 إنشاء نموذج TFLite تجريبي...")

# إنشاء نموذج Dense Neural Network (256 → 128 → 64)
# Input: 63 features (21 landmarks × 3 coordinates)
# Output: 28 classes (28 حرف) - يمكن تغييره حسب labels.json

model = tf.keras.Sequential([
    tf.keras.layers.InputLayer(input_shape=(63,), name='input'),
    # Layer 1: 256 units
    tf.keras.layers.Dense(256, activation='relu', name='dense_1'),
    tf.keras.layers.Dropout(0.3, name='dropout_1'),
    # Layer 2: 128 units
    tf.keras.layers.Dense(128, activation='relu', name='dense_2'),
    tf.keras.layers.Dropout(0.3, name='dropout_2'),
    # Layer 3: 64 units
    tf.keras.layers.Dense(64, activation='relu', name='dense_3'),
    tf.keras.layers.Dropout(0.2, name='dropout_3'),
    # Output layer: 28 classes (حسب labels.json)
    tf.keras.layers.Dense(28, activation='softmax', name='output')
])

# تجميع النموذج
model.compile(
    optimizer='adam',
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# طباعة ملخص النموذج
print("\n📊 ملخص النموذج:")
model.summary()

# تدريب تجريبي (مع بيانات عشوائية للاختبار فقط)
print("\n🎓 تدريب تجريبي...")
X_dummy = np.random.rand(100, 63).astype(np.float32)
y_dummy = np.random.rand(100, 28).astype(np.float32)  # 28 تصنيف (حسب labels.json)
# Normalize y to probabilities
y_dummy = y_dummy / y_dummy.sum(axis=1, keepdims=True)

model.fit(
    X_dummy,
    y_dummy,
    epochs=1,
    batch_size=32,
    verbose=1
)

# تحويل إلى TFLite
print("\n🔄 تحويل إلى TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# تحويل
tflite_model = converter.convert()

# حفظ الملف
output_path = "arabic_sign_lstm.tflite"
with open(output_path, 'wb') as f:
    f.write(tflite_model)

file_size = os.path.getsize(output_path) / (1024 * 1024)  # MB
print(f"\n✅ تم إنشاء النموذج التجريبي بنجاح!")
print(f"📁 الملف: {output_path}")
print(f"📦 الحجم: {file_size:.2f} MB")
print(f"\n⚠️  ملاحظة: هذا نموذج تجريبي للاختبار فقط!")
print(f"   للحصول على نتائج دقيقة، قم بتدريب النموذج على البيانات الفعلية.")
print(f"\n📋 الخطوة التالية:")
print(f"   1. انسخ الملف إلى: app/src/main/assets/arabic_sign_lstm.tflite")
print(f"   2. قم بـ Clean و Rebuild للمشروع")
print(f"   3. اختبر التطبيق")

