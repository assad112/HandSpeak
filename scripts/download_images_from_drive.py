#!/usr/bin/env python3
"""
سكريبت متقدم لتحميل صور الإشارات من Google Drive

الاستخدام:
    python download_images_from_drive.py --folder-id YOUR_FOLDER_ID

أو:
    python download_images_from_drive.py --config config.json
"""

import os
import json
import argparse
from pathlib import Path
import gdown
from tqdm import tqdm

# مسار المجلدات
ASSETS_DIR = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "signs"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)

def download_folder_from_drive(folder_id: str, output_dir: Path):
    """
    تحميل مجلد كامل من Google Drive
    """
    try:
        url = f"https://drive.google.com/drive/folders/{folder_id}"
        print(f"📥 Downloading from: {url}")
        
        gdown.download_folder(
            url,
            output=str(output_dir),
            quiet=False,
            use_cookies=False
        )
        
        print(f"✅ Downloaded to: {output_dir}")
        return True
    except Exception as e:
        print(f"❌ Error: {e}")
        return False

def download_file_from_drive(file_id: str, output_path: Path):
    """
    تحميل ملف واحد من Google Drive
    """
    try:
        url = f"https://drive.google.com/uc?id={file_id}"
        gdown.download(url, str(output_path), quiet=False)
        return True
    except Exception as e:
        print(f"❌ Error downloading {file_id}: {e}")
        return False

def organize_downloaded_images(download_dir: Path):
    """
    تنظيم الصور المحمّلة حسب المجلدات
    """
    print("📁 Organizing downloaded images...")
    
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
        "لا": "la", "من_فضلك": "min_fadlak", "آسف": "asef",
        "صباح_الخير": "sabah_alkhair", "مساء_الخير": "masaa_alkhair",
        "كيف_حالك": "kaif_halak", "بخير": "bikhair"
    }
    
    # البحث عن الصور وتنظيمها
    for root, dirs, files in os.walk(download_dir):
        for file in files:
            if file.lower().endswith(('.png', '.jpg', '.jpeg')):
                file_path = Path(root) / file
                
                # محاولة تحديد المجلد من اسم الملف أو المسار
                # يمكن تخصيص هذا المنطق حسب هيكل البيانات
                # ...
                
                pass

def main():
    parser = argparse.ArgumentParser(description='Download sign images from Google Drive')
    parser.add_argument('--folder-id', type=str, help='Google Drive folder ID')
    parser.add_argument('--config', type=str, help='JSON config file')
    parser.add_argument('--output', type=str, default=str(ASSETS_DIR), help='Output directory')
    
    args = parser.parse_args()
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    if args.config:
        # تحميل من ملف config
        with open(args.config, 'r', encoding='utf-8') as f:
            config = json.load(f)
            
        if 'folder_id' in config:
            download_folder_from_drive(config['folder_id'], output_dir)
        elif 'files' in config:
            for file_info in config['files']:
                file_id = file_info['id']
                folder_name = file_info['folder']
                file_index = file_info.get('index', 1)
                
                folder_path = output_dir / folder_name
                folder_path.mkdir(parents=True, exist_ok=True)
                
                output_path = folder_path / f"{file_index}.png"
                download_file_from_drive(file_id, output_path)
    
    elif args.folder_id:
        # تحميل مجلد كامل
        download_folder_from_drive(args.folder_id, output_dir)
    
    else:
        print("❌ Please provide --folder-id or --config")
        parser.print_help()

if __name__ == "__main__":
    main()

