#!/usr/bin/env python3
"""
إعداد سريع لتحميل الصور من Google Drive

الاستخدام:
    1. ارفع الصور على Google Drive
    2. شارك المجلد (أو الملفات) مع "Anyone with the link"
    3. احصل على Folder ID أو File ID
    4. شغّل السكريبت:
    
    python setup_images_from_drive.py --folder-id YOUR_FOLDER_ID
"""

import os
import gdown
import argparse
from pathlib import Path

# مسار assets
ASSETS_DIR = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "signs"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)

def download_from_drive_folder(folder_id: str):
    """
    تحميل مجلد كامل من Google Drive
    """
    try:
        url = f"https://drive.google.com/drive/folders/{folder_id}"
        print(f"📥 جاري التحميل من Google Drive...")
        print(f"🔗 الرابط: {url}")
        print("=" * 60)
        
        gdown.download_folder(
            url,
            output=str(ASSETS_DIR),
            quiet=False,
            use_cookies=False
        )
        
        print("=" * 60)
        print(f"✅ تم التحميل بنجاح!")
        print(f"📁 الموقع: {ASSETS_DIR}")
        return True
        
    except Exception as e:
        print(f"❌ خطأ في التحميل: {e}")
        print("\n💡 نصائح:")
        print("   1. تأكد من أن المجلد مشترك (Anyone with the link)")
        print("   2. تأكد من صحة Folder ID")
        print("   3. جرب تثبيت gdown: pip install gdown")
        return False

def download_single_file(file_id: str, folder_name: str, index: int):
    """
    تحميل ملف واحد من Google Drive
    """
    try:
        url = f"https://drive.google.com/uc?id={file_id}"
        folder_path = ASSETS_DIR / folder_name
        folder_path.mkdir(parents=True, exist_ok=True)
        
        output_path = folder_path / f"{index}.png"
        
        gdown.download(url, str(output_path), quiet=False)
        
        print(f"✅ تم تحميل: {folder_name}/{index}.png")
        return True
        
    except Exception as e:
        print(f"❌ خطأ: {e}")
        return False

def main():
    parser = argparse.ArgumentParser(
        description='تحميل الصور من Google Drive',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
أمثلة:
  # تحميل مجلد كامل
  python setup_images_from_drive.py --folder-id 1ABC123xyz789
  
  # تحميل ملف واحد
  python setup_images_from_drive.py --file-id 1ABC123xyz789 --folder alef --index 1

كيفية الحصول على Folder ID:
  1. افتح Google Drive
  2. انقر بزر الماوس الأيمن على المجلد
  3. اختر "Get link" أو "الحصول على رابط"
  4. الرابط سيكون: https://drive.google.com/drive/folders/FOLDER_ID
  5. انسخ FOLDER_ID
        """
    )
    
    parser.add_argument('--folder-id', type=str, help='Google Drive Folder ID')
    parser.add_argument('--file-id', type=str, help='Google Drive File ID (للملف الواحد)')
    parser.add_argument('--folder', type=str, help='اسم المجلد (مع --file-id)')
    parser.add_argument('--index', type=int, help='رقم الصورة (مع --file-id)')
    
    args = parser.parse_args()
    
    if args.folder_id:
        download_from_drive_folder(args.folder_id)
    elif args.file_id and args.folder and args.index:
        download_single_file(args.file_id, args.folder, args.index)
    else:
        parser.print_help()
        print("\n❌ يرجى تحديد --folder-id أو (--file-id + --folder + --index)")

if __name__ == "__main__":
    main()

