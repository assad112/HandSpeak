"""
سكريبت لإنشاء نموذج Dense Neural Network للاختبار
هذا النموذج يعادل النموذج المدرب في Colab
"""

import tensorflow as tf
import numpy as np

print("🔧 إنشاء نموذج Dense Neural Network...")

# البنية المعمارية (Dense NN)
model = tf.keras.Sequential([
    tf.keras.layers.InputLayer(input_shape=(63,)),  # 21 landmarks × 3
    tf.keras.layers.Dense(256, activation='relu', name='dense_1'),
    tf.keras.layers.Dropout(0.3, name='dropout_1'),
    tf.keras.layers.Dense(128, activation='relu', name='dense_2'),
    tf.keras.layers.Dropout(0.3, name='dropout_2'),
    tf.keras.layers.Dense(64, activation='relu', name='dense_3'),
    tf.keras.layers.Dropout(0.2, name='dropout_3'),
    tf.keras.layers.Dense(28, activation='softmax', name='output')  # 28 تصنيف
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

# إنشاء بيانات تجريبية للتدريب (للتأكد من أن النموذج يعمل)
print("\n🧪 إنشاء بيانات تجريبية...")
X_dummy = np.random.random((100, 63))  # 100 عينة × 63 features
y_dummy = np.random.random((100, 28))  # 100 عينة × 28 تصنيف
y_dummy = y_dummy / y_dummy.sum(axis=1, keepdims=True)  # Normalize to probabilities

# تدريب تجريبي (epoch واحد فقط للاختبار)
print("🏋️ تدريب تجريبي...")
model.fit(X_dummy, y_dummy, epochs=1, verbose=1)

# تحويل إلى TFLite
print("\n🔄 تحويل إلى TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# حفظ
output_file = 'arabic_sign_dense.tflite'
with open(output_file, 'wb') as f:
    f.write(tflite_model)

print(f"\n✅ تم إنشاء النموذج: {output_file}")
print(f"📦 الحجم: {len(tflite_model) / 1024:.2f} KB")
print("\n💡 ملاحظة: هذا نموذج تجريبي للاختبار فقط!")
print("   للاستخدام الفعلي، درّب النموذج على بيانات حقيقية في Colab.")

