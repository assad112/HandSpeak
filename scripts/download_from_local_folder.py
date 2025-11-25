#!/usr/bin/env python3
"""
سكريبت لنسخ الصور من مجلد محلي إلى assets

الاستخدام:
    python download_from_local_folder.py --source "C:/path/to/images" --target "app/src/main/assets/signs"

أو:
    python download_from_local_folder.py --source "C:/Users/HP/Desktop/صور الإشارات"
"""

import os
import shutil
import argparse
from pathlib import Path

def copy_images_from_folder(source_dir: str, target_dir: str):
    """
    نسخ الصور من مجلد محلي إلى assets
    """
    source = Path(source_dir)
    target = Path(target_dir)
    
    if not source.exists():
        print(f"❌ المجلد المصدر غير موجود: {source}")
        return False
    
    target.mkdir(parents=True, exist_ok=True)
    
    print(f"📁 المصدر: {source}")
    print(f"📁 الهدف: {target}")
    print("=" * 60)
    
    # خريطة أسماء المجلدات
    folder_map = {
        "أ": "alef", "ب": "baa", "ت": "taa", "ث": "thaa",
        "ج": "jeem", "ح": "haa", "خ": "khaa", "د": "daal",
        "ذ": "thal", "ر": "raa", "ز": "zaay", "س": "seen",
        "ش": "sheen", "ص": "saad", "ض": "daad", "ط": "taa2",
        "ظ": "dhaa", "ع": "ain", "غ": "ghain", "ف": "faa",
        "ق": "qaaf", "ك": "kaaf", "ل": "laam", "م": "meem",
        "ن": "noon", "ه": "haa2", "و": "waaw", "ي": "yaa",
        "مرحبا": "marhaba", "شكرا": "shokran", "نعم": "naam",
        "لا": "la", "من_فضلك": "min_fadlak", "من فضلك": "min_fadlak",
        "آسف": "asef", "صباح_الخير": "sabah_alkhair", "صباح الخير": "sabah_alkhair",
        "مساء_الخير": "masaa_alkhair", "مساء الخير": "masaa_alkhair",
        "كيف_حالك": "kaif_halak", "كيف حالك": "kaif_halak",
        "بخير": "bikhair"
    }
    
    copied_count = 0
    
    # البحث في المجلد المصدر
    for item in source.iterdir():
        if item.is_dir():
            # محاولة مطابقة اسم المجلد
            folder_name = item.name
            
            # البحث في الخريطة
            target_folder = None
            if folder_name in folder_map:
                target_folder = folder_map[folder_name]
            else:
                # البحث بدون مسافات
                folder_name_no_spaces = folder_name.replace(" ", "_")
                if folder_name_no_spaces in folder_map:
                    target_folder = folder_map[folder_name_no_spaces]
                else:
                    # البحث في القيم
                    for key, value in folder_map.items():
                        if value == folder_name or value == folder_name_no_spaces:
                            target_folder = value
                            break
            
            if target_folder:
                target_path = target / target_folder
                target_path.mkdir(parents=True, exist_ok=True)
                
                # نسخ الصور
                image_files = sorted([f for f in item.iterdir() 
                                    if f.suffix.lower() in ['.png', '.jpg', '.jpeg']])
                
                for idx, image_file in enumerate(image_files, start=1):
                    target_image = target_path / f"{idx}.png"
                    
                    # تحويل JPG إلى PNG إذا لزم الأمر
                    if image_file.suffix.lower() == '.png':
                        shutil.copy2(image_file, target_image)
                    else:
                        # نسخ مع تغيير الامتداد
                        shutil.copy2(image_file, target_image)
                    
                    print(f"✅ نسخ: {folder_name}/{image_file.name} → {target_folder}/{idx}.png")
                    copied_count += 1
            else:
                print(f"⚠️  مجلد غير معروف: {folder_name}")
    
    print("=" * 60)
    print(f"✅ تم نسخ {copied_count} صورة!")
    return True

def main():
    parser = argparse.ArgumentParser(description='نسخ الصور من مجلد محلي')
    parser.add_argument('--source', type=str, required=True,
                       help='المجلد المصدر (مثال: C:/Users/HP/Desktop/صور الإشارات)')
    parser.add_argument('--target', type=str, 
                       default='app/src/main/assets/signs',
                       help='المجلد الهدف (افتراضي: app/src/main/assets/signs)')
    
    args = parser.parse_args()
    
    # الحصول على المسار المطلق
    script_dir = Path(__file__).parent.parent
    target_path = script_dir / args.target
    
    copy_images_from_folder(args.source, str(target_path))

if __name__ == "__main__":
    main()

