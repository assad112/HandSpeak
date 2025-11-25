#!/usr/bin/env python3
"""
سكريبت لتحميل صور الإشارات من Google Drive أو مصادر أخرى

الاستخدام:
    python download_sign_images.py

المتطلبات:
    pip install gdown requests pillow
"""

import os
import json
import requests
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import gdown

# مسار المجلدات
ASSETS_DIR = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "signs"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)

# قائمة المجلدات المطلوبة (من sign_map.json)
REQUIRED_FOLDERS = [
    # الحروف (28 حرف)
    "alef", "baa", "taa", "thaa", "jeem", "haa", "khaa",
    "daal", "thal", "raa", "zaay", "seen", "sheen",
    "saad", "daad", "taa2", "dhaa", "ain", "ghain",
    "faa", "qaaf", "kaaf", "laam", "meem", "noon",
    "haa2", "waaw", "yaa",
    # الكلمات (10 كلمات)
    "marhaba", "shokran", "naam", "la", "min_fadlak",
    "asef", "sabah_alkhair", "masaa_alkhair", "kaif_halak", "bikhair"
]

def create_placeholder_image(folder_name: str, index: int, output_path: Path):
    """
    إنشاء صورة placeholder بسيطة
    """
    # إنشاء صورة 512x512
    img = Image.new('RGB', (512, 512), color='white')
    draw = ImageDraw.Draw(img)
    
    # رسم مربع بسيط
    margin = 50
    draw.rectangle(
        [margin, margin, 512 - margin, 512 - margin],
        outline='blue',
        width=5
    )
    
    # إضافة نص
    try:
        # محاولة استخدام خط عربي
        font = ImageFont.truetype("arial.ttf", 40)
    except:
        font = ImageFont.load_default()
    
    text = f"{folder_name}\n{index}"
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    
    position = ((512 - text_width) // 2, (512 - text_height) // 2)
    draw.text(position, text, fill='blue', font=font)
    
    # حفظ الصورة
    img.save(output_path, 'PNG')
    print(f"✅ Created placeholder: {output_path}")

def download_from_google_drive(folder_id: str, output_dir: Path):
    """
    تحميل مجلد من Google Drive
    """
    try:
        url = f"https://drive.google.com/drive/folders/{folder_id}"
        gdown.download_folder(url, output=str(output_dir), quiet=False, use_cookies=False)
        return True
    except Exception as e:
        print(f"❌ Error downloading from Google Drive: {e}")
        return False

def download_from_url(image_url: str, output_path: Path):
    """
    تحميل صورة من URL
    """
    try:
        response = requests.get(image_url, timeout=10)
        if response.status_code == 200:
            with open(output_path, 'wb') as f:
                f.write(response.content)
            print(f"✅ Downloaded: {output_path}")
            return True
        else:
            print(f"❌ Failed to download: {image_url} (Status: {response.status_code})")
            return False
    except Exception as e:
        print(f"❌ Error downloading {image_url}: {e}")
        return False

def create_placeholder_images():
    """
    إنشاء صور placeholder لجميع المجلدات
    """
    print("🎨 Creating placeholder images...")
    
    for folder in REQUIRED_FOLDERS:
        folder_path = ASSETS_DIR / folder
        folder_path.mkdir(parents=True, exist_ok=True)
        
        # إنشاء 5 صور placeholder لكل مجلد
        for i in range(1, 6):
            image_path = folder_path / f"{i}.png"
            
            # تخطي إذا كانت الصورة موجودة
            if image_path.exists():
                print(f"⏭️  Skipping existing: {image_path}")
                continue
            
            create_placeholder_image(folder, i, image_path)
    
    print("✅ Placeholder images created!")

def download_from_rgb_dataset(dataset_path: str):
    """
    تحميل الصور من RGB_ArSL_dataset
    يتطلب أن يكون المجلد متاحاً محلياً أو على Google Drive
    """
    print("📥 Downloading from RGB_ArSL_dataset...")
    
    # إذا كان المجلد محلياً
    if os.path.exists(dataset_path):
        print(f"📁 Found local dataset: {dataset_path}")
        # يمكن إضافة منطق النسخ هنا
        return True
    
    # إذا كان على Google Drive
    # يمكن إضافة folder_id هنا
    return False

def main():
    """
    الوظيفة الرئيسية
    """
    print("=" * 60)
    print("📥 HandSpeak - Sign Images Downloader")
    print("=" * 60)
    print()
    
    # خيار 1: إنشاء صور placeholder
    print("1️⃣ Creating placeholder images...")
    create_placeholder_images()
    print()
    
    # خيار 2: تحميل من Google Drive (إذا كان متاحاً)
    print("2️⃣ To download from Google Drive:")
    print("   - Uncomment the code below")
    print("   - Add your Google Drive folder ID")
    print("   - Run: python download_sign_images.py")
    print()
    
    # مثال:
    # folder_id = "YOUR_GOOGLE_DRIVE_FOLDER_ID"
    # download_from_google_drive(folder_id, ASSETS_DIR)
    
    # خيار 3: تحميل من URLs
    print("3️⃣ To download from URLs:")
    print("   - Create a JSON file with URLs")
    print("   - Format: {\"alef\": [\"url1\", \"url2\", ...], ...}")
    print()
    
    print("=" * 60)
    print("✅ Done!")
    print("=" * 60)
    print()
    print("📝 Next steps:")
    print("   1. Replace placeholder images with real sign images")
    print("   2. Use the ImageDownloadSettingsScreen in the app")
    print("   3. Or manually copy images to: app/src/main/assets/signs/")

if __name__ == "__main__":
    main()

