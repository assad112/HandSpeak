#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
تدريب نموذج LSTM للتعرف على الحروف العربية في لغة الإشارة
Arabic Sign Language Letter Recognition - LSTM Model Training
"""

import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import json
import os

# الحروف العربية (28 حرف)
ARABIC_LETTERS = [
    "أ", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", 
    "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", 
    "ق", "ك", "ل", "م", "ن", "هـ", "و", "ي"
]

# إعدادات النموذج
INPUT_SIZE = 63  # 21 نقطة × 3 إحداثيات (x, y, z)
SEQUENCE_LENGTH = 5  # عدد الإطارات (للحروف الثابتة)
NUM_CLASSES = len(ARABIC_LETTERS)
LSTM_UNITS = 128
DROPOUT_RATE = 0.3

def create_lstm_model():
    """إنشاء نموذج LSTM للتعرف على الحروف"""
    
    model = keras.Sequential([
        # Input layer
        layers.Input(shape=(SEQUENCE_LENGTH, INPUT_SIZE)),
        
        # LSTM layers
        layers.LSTM(LSTM_UNITS, return_sequences=True),
        layers.Dropout(DROPOUT_RATE),
        
        layers.LSTM(LSTM_UNITS // 2, return_sequences=False),
        layers.Dropout(DROPOUT_RATE),
        
        # Dense layers
        layers.Dense(64, activation='relu'),
        layers.Dropout(DROPOUT_RATE),
        
        layers.Dense(NUM_CLASSES, activation='softmax')
    ])
    
    model.compile(
        optimizer='adam',
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    
    return model

def generate_sample_data(num_samples=100):
    """توليد بيانات تجريبية للاختبار"""
    print(f"📊 توليد {num_samples} عينة تجريبية لكل حرف...")
    
    X = []
    y = []
    
    for label_idx, letter in enumerate(ARABIC_LETTERS):
        for _ in range(num_samples):
            # توليد تسلسل عشوائي (محاكاة landmarks)
            # في الواقع، يجب استبدال هذا ببيانات حقيقية
            sequence = np.random.rand(SEQUENCE_LENGTH, INPUT_SIZE).astype(np.float32)
            
            # إضافة نمط فريد لكل حرف
            sequence[:, label_idx % INPUT_SIZE] += label_idx * 0.1
            
            X.append(sequence)
            y.append(label_idx)
    
    X = np.array(X)
    y = keras.utils.to_categorical(y, NUM_CLASSES)
    
    return X, y

def train_model(epochs=50, batch_size=32):
    """تدريب النموذج"""
    
    print("🚀 بدء تدريب نموذج الحروف العربية...")
    print(f"   عدد الحروف: {NUM_CLASSES}")
    print(f"   طول التسلسل: {SEQUENCE_LENGTH}")
    print(f"   حجم الإدخال: {INPUT_SIZE}")
    
    # توليد بيانات (في الإنتاج، استخدم بيانات حقيقية)
    X_train, y_train = generate_sample_data(num_samples=100)
    X_test, y_test = generate_sample_data(num_samples=20)
    
    print(f"\n📦 حجم البيانات:")
    print(f"   تدريب: {X_train.shape}")
    print(f"   اختبار: {X_test.shape}")
    
    # إنشاء النموذج
    model = create_lstm_model()
    model.summary()
    
    # التدريب
    print("\n🎯 بدء التدريب...")
    history = model.fit(
        X_train, y_train,
        validation_data=(X_test, y_test),
        epochs=epochs,
        batch_size=batch_size,
        verbose=1
    )
    
    # التقييم
    print("\n📈 تقييم النموذج:")
    test_loss, test_acc = model.evaluate(X_test, y_test, verbose=0)
    print(f"   دقة الاختبار: {test_acc * 100:.2f}%")
    print(f"   خسارة الاختبار: {test_loss:.4f}")
    
    return model, history

def convert_to_tflite(model, output_path):
    """تحويل النموذج إلى TensorFlow Lite"""
    
    print("\n🔄 تحويل إلى TensorFlow Lite...")
    
    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    converter._experimental_lower_tensor_list_ops = False
    
    tflite_model = converter.convert()
    
    # حفظ الملف
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    file_size = os.path.getsize(output_path) / (1024 * 1024)
    print(f"✅ تم حفظ النموذج: {output_path}")
    print(f"   حجم الملف: {file_size:.2f} MB")
    
    return output_path

def save_labels(output_path):
    """حفظ قائمة الحروف"""
    
    print(f"\n💾 حفظ قائمة الحروف...")
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(ARABIC_LETTERS, f, ensure_ascii=False, indent=2)
    
    print(f"✅ تم حفظ الحروف: {output_path}")
    print(f"   عدد الحروف: {len(ARABIC_LETTERS)}")

def test_tflite_model(model_path):
    """اختبار نموذج TFLite"""
    
    print("\n🧪 اختبار النموذج...")
    
    # تحميل النموذج
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"   شكل الإدخال: {input_details[0]['shape']}")
    print(f"   شكل الإخراج: {output_details[0]['shape']}")
    
    # اختبار بعينة عشوائية
    test_input = np.random.rand(1, SEQUENCE_LENGTH, INPUT_SIZE).astype(np.float32)
    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()
    
    output = interpreter.get_tensor(output_details[0]['index'])
    predicted_class = np.argmax(output[0])
    confidence = output[0][predicted_class]
    
    print(f"   تنبؤ تجريبي: {ARABIC_LETTERS[predicted_class]} (ثقة: {confidence * 100:.1f}%)")
    print("✅ النموذج يعمل بنجاح!")

def main():
    """الدالة الرئيسية"""
    
    print("=" * 60)
    print("🌟 تدريب نموذج الحروف العربية")
    print("   Arabic Sign Language Letter Recognition")
    print("=" * 60)
    
    # المسارات
    output_dir = "../app/src/main/assets"
    model_path = os.path.join(output_dir, "arabic_letters_lstm.tflite")
    labels_path = os.path.join(output_dir, "labels.json")
    
    # التدريب
    model, history = train_model(epochs=30, batch_size=32)
    
    # التحويل
    tflite_path = convert_to_tflite(model, model_path)
    
    # حفظ الحروف
    save_labels(labels_path)
    
    # الاختبار
    test_tflite_model(tflite_path)
    
    print("\n" + "=" * 60)
    print("✅ تم الانتهاء بنجاح!")
    print("=" * 60)
    print("\n📋 الخطوات التالية:")
    print("   1. استبدل البيانات التجريبية ببيانات حقيقية")
    print("   2. جمع عينات للحروف العربية (صور أو landmarks)")
    print("   3. أعد التدريب بالبيانات الحقيقية")
    print("   4. انسخ الملفات إلى assets في التطبيق")
    print("\n📁 الملفات المُنشأة:")
    print(f"   - {model_path}")
    print(f"   - {labels_path}")
    print("\n")

if __name__ == "__main__":
    main()
