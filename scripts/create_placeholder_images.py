#!/usr/bin/env python3
"""
إنشاء صور placeholder لجميع الإشارات

الاستخدام:
    python create_placeholder_images.py
"""

import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

# مسار المجلدات
ASSETS_DIR = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "signs"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)

# قائمة المجلدات
FOLDERS = [
    # الحروف
    ("alef", "أ"), ("baa", "ب"), ("taa", "ت"), ("thaa", "ث"),
    ("jeem", "ج"), ("haa", "ح"), ("khaa", "خ"), ("daal", "د"),
    ("thal", "ذ"), ("raa", "ر"), ("zaay", "ز"), ("seen", "س"),
    ("sheen", "ش"), ("saad", "ص"), ("daad", "ض"), ("taa2", "ط"),
    ("dhaa", "ظ"), ("ain", "ع"), ("ghain", "غ"), ("faa", "ف"),
    ("qaaf", "ق"), ("kaaf", "ك"), ("laam", "ل"), ("meem", "م"),
    ("noon", "ن"), ("haa2", "ه"), ("waaw", "و"), ("yaa", "ي"),
    # الكلمات
    ("marhaba", "مرحبا"), ("shokran", "شكرا"), ("naam", "نعم"),
    ("la", "لا"), ("min_fadlak", "من فضلك"), ("asef", "آسف"),
    ("sabah_alkhair", "صباح الخير"), ("masaa_alkhair", "مساء الخير"),
    ("kaif_halak", "كيف حالك"), ("bikhair", "بخير"),
    ("assalamu_alaikum", "السلام عليكم")
]

def create_image(folder_name: str, label: str, index: int, output_path: Path):
    """إنشاء صورة placeholder"""
    # إنشاء صورة 512x512 بخلفية بيضاء
    img = Image.new('RGB', (512, 512), color='#F5F5F5')
    draw = ImageDraw.Draw(img)
    
    # رسم إطار
    margin = 20
    draw.rectangle(
        [margin, margin, 512 - margin, 512 - margin],
        outline='#2196F3',
        width=4
    )
    
    # رسم دائرة في المنتصف
    center = 256
    radius = 150
    draw.ellipse(
        [center - radius, center - radius, center + radius, center + radius],
        outline='#2196F3',
        width=3
    )
    
    # إضافة النص
    try:
        # محاولة استخدام خط أكبر
        font_size = 60
        font = ImageFont.truetype("arial.ttf", font_size)
    except:
        font = ImageFont.load_default()
    
    # نص الإشارة
    text = label
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    
    text_x = (512 - text_width) // 2
    text_y = center - text_height // 2 - 20
    draw.text((text_x, text_y), text, fill='#2196F3', font=font)
    
    # رقم الصورة
    index_text = str(index)
    bbox2 = draw.textbbox((0, 0), index_text, font=ImageFont.load_default())
    index_width = bbox2[2] - bbox2[0]
    index_x = (512 - index_width) // 2
    index_y = center + 40
    draw.text((index_x, index_y), index_text, fill='#757575', font=ImageFont.load_default())
    
    # حفظ الصورة
    img.save(output_path, 'PNG', optimize=True)
    print(f"✅ Created: {output_path}")

def main():
    print("🎨 Creating placeholder images for all signs...")
    print("=" * 60)
    
    total_created = 0
    
    for folder_name, label in FOLDERS:
        folder_path = ASSETS_DIR / folder_name
        folder_path.mkdir(parents=True, exist_ok=True)
        
        # إنشاء 5 صور لكل إشارة
        for i in range(1, 6):
            image_path = folder_path / f"{i}.png"
            
            # تخطي إذا كانت موجودة
            if image_path.exists():
                print(f"⏭️  Skipping: {image_path.name}")
                continue
            
            create_image(folder_name, label, i, image_path)
            total_created += 1
    
    print("=" * 60)
    print(f"✅ Created {total_created} placeholder images!")
    print(f"📁 Location: {ASSETS_DIR}")
    print()
    print("💡 Next steps:")
    print("   1. Replace placeholder images with real sign images")
    print("   2. Images should be in PNG format, 512x512 or larger")
    print("   3. Use ImageDownloadSettingsScreen in the app to download more")

if __name__ == "__main__":
    main()

