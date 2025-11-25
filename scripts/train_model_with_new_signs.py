"""
سكريبت لتدريب نموذج لغة الإشارة العربية على الحروف والإشارات الجديدة
يدعم LSTM و Dense Neural Network
"""

import tensorflow as tf
import numpy as np
import json
import os
from pathlib import Path

print("🚀 سكريبت تدريب نموذج لغة الإشارة العربية")
print("=" * 60)

# قراءة labels.json لتحديد عدد التصنيفات
labels_file = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "labels.json"
if labels_file.exists():
    with open(labels_file, 'r', encoding='utf-8') as f:
        labels = json.load(f)
    num_classes = len(labels)
    print(f"✅ تم تحميل {num_classes} تصنيف من labels.json")
    print(f"   التصنيفات: {', '.join(labels[:5])}... (+ {num_classes - 5} أكثر)")
else:
    # استخدام القيمة الافتراضية
    num_classes = 48  # 28 حرف + 20 إشارة جديدة
    print(f"⚠️  لم يتم العثور على labels.json، استخدام القيمة الافتراضية: {num_classes}")

# إعدادات النموذج
INPUT_SIZE = 63  # 21 landmarks × 3 (x, y, z)
SEQUENCE_LENGTH = 10  # طول التسلسل للـ LSTM
USE_LSTM = True  # True للـ LSTM، False للـ Dense

print(f"\n📊 إعدادات النموذج:")
print(f"   - عدد التصنيفات: {num_classes}")
print(f"   - حجم المدخل: {INPUT_SIZE} (21 landmarks × 3)")
print(f"   - نوع النموذج: {'LSTM' if USE_LSTM else 'Dense NN'}")
if USE_LSTM:
    print(f"   - طول التسلسل: {SEQUENCE_LENGTH}")

def create_lstm_model(num_classes: int, sequence_length: int = 10):
    """إنشاء نموذج LSTM"""
    model = tf.keras.Sequential([
        tf.keras.layers.InputLayer(input_shape=(sequence_length, INPUT_SIZE)),
        tf.keras.layers.LSTM(256, return_sequences=True, name='lstm_1'),
        tf.keras.layers.Dropout(0.3, name='dropout_1'),
        tf.keras.layers.LSTM(128, return_sequences=True, name='lstm_2'),
        tf.keras.layers.Dropout(0.3, name='dropout_2'),
        tf.keras.layers.LSTM(64, name='lstm_3'),
        tf.keras.layers.Dropout(0.2, name='dropout_3'),
        tf.keras.layers.Dense(128, activation='relu', name='dense_1'),
        tf.keras.layers.Dropout(0.2, name='dropout_4'),
        tf.keras.layers.Dense(64, activation='relu', name='dense_2'),
        tf.keras.layers.Dense(num_classes, activation='softmax', name='output')
    ])
    return model

def create_dense_model(num_classes: int):
    """إنشاء نموذج Dense Neural Network"""
    model = tf.keras.Sequential([
        tf.keras.layers.InputLayer(input_shape=(INPUT_SIZE,)),
        tf.keras.layers.Dense(256, activation='relu', name='dense_1'),
        tf.keras.layers.Dropout(0.3, name='dropout_1'),
        tf.keras.layers.Dense(128, activation='relu', name='dense_2'),
        tf.keras.layers.Dropout(0.3, name='dropout_2'),
        tf.keras.layers.Dense(64, activation='relu', name='dense_3'),
        tf.keras.layers.Dropout(0.2, name='dropout_3'),
        tf.keras.layers.Dense(num_classes, activation='softmax', name='output')
    ])
    return model

# إنشاء النموذج
print(f"\n🔧 إنشاء النموذج...")
if USE_LSTM:
    model = create_lstm_model(num_classes, SEQUENCE_LENGTH)
    model_name = "arabic_sign_lstm.tflite"
else:
    model = create_dense_model(num_classes)
    model_name = "arabic_sign_dense.tflite"

# تجميع النموذج
model.compile(
    optimizer='adam',
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# طباعة ملخص النموذج
print("\n📊 ملخص النموذج:")
model.summary()

# حساب عدد المعاملات
total_params = model.count_params()
print(f"\n📈 إجمالي المعاملات: {total_params:,}")

# إنشاء بيانات تجريبية للتدريب (للتأكد من أن النموذج يعمل)
print("\n🧪 إنشاء بيانات تجريبية...")
if USE_LSTM:
    # بيانات LSTM: [batch_size, sequence_length, features]
    X_dummy = np.random.random((100, SEQUENCE_LENGTH, INPUT_SIZE))
    y_dummy = np.random.random((100, num_classes))
else:
    # بيانات Dense: [batch_size, features]
    X_dummy = np.random.random((100, INPUT_SIZE))
    y_dummy = np.random.random((100, num_classes))

# Normalize labels to probabilities
y_dummy = y_dummy / y_dummy.sum(axis=1, keepdims=True)

# تدريب تجريبي (epoch واحد فقط للاختبار)
print("\n🏋️  تدريب تجريبي (epoch واحد للاختبار)...")
history = model.fit(
    X_dummy, 
    y_dummy, 
    epochs=1, 
    verbose=1,
    validation_split=0.2
)

# تحويل إلى TFLite
print("\n🔄 تحويل إلى TFLite...")
try:
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # LSTM يتطلب Select TF Ops
    if USE_LSTM:
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS
        ]
        converter._experimental_lower_tensor_list_ops = False
        print("   ℹ️  استخدام Select TF Ops للـ LSTM")
    
    tflite_model = converter.convert()
    
    # حفظ النموذج
    output_file = Path(__file__).parent / model_name
    with open(output_file, 'wb') as f:
        f.write(tflite_model)
    
    file_size_kb = len(tflite_model) / 1024
    file_size_mb = file_size_kb / 1024
    
    print(f"\n✅ تم إنشاء النموذج بنجاح!")
    print(f"   📁 الملف: {output_file}")
    print(f"   📦 الحجم: {file_size_kb:.2f} KB ({file_size_mb:.2f} MB)")
    print(f"\n📋 الخطوات التالية:")
    print(f"   1. انسخ الملف إلى: app/src/main/assets/{model_name}")
    print(f"   2. أعد بناء التطبيق (Clean & Rebuild)")
    print(f"   3. اختبر التطبيق")
    
    print(f"\n⚠️  ملاحظة مهمة:")
    print(f"   هذا نموذج تجريبي للاختبار فقط!")
    print(f"   للاستخدام الفعلي، درّب النموذج على بيانات حقيقية:")
    print(f"   - استخدم Google Colab للتدريب")
    print(f"   - أو استخدم بيانات تدريب من dataset حقيقي")
    print(f"   - راجع SETUP_MODELS.md للتفاصيل")
    
except Exception as e:
    print(f"\n❌ خطأ في تحويل النموذج: {e}")
    print(f"   تأكد من تثبيت TensorFlow بشكل صحيح")

print("\n" + "=" * 60)
print("✅ اكتمل!")

