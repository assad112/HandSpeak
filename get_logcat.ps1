# سكريبت PowerShell للحصول على Logcat
# استخدم: .\get_logcat.ps1

Write-Host "=== HandSpeak Logcat Tool ===" -ForegroundColor Green
Write-Host ""

# التحقق من ADB
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB غير موجود في: $adbPath" -ForegroundColor Red
    Write-Host "تأكد من تثبيت Android SDK" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ ADB موجود" -ForegroundColor Green
Write-Host ""

# التحقق من الاتصال
Write-Host "التحقق من الاتصال بالجهاز..." -ForegroundColor Yellow
$devices = & $adbPath devices

if ($devices -match "device$") {
    Write-Host "✅ جهاز متصل" -ForegroundColor Green
} else {
    Write-Host "❌ لا يوجد جهاز متصل" -ForegroundColor Red
    Write-Host "تأكد من:" -ForegroundColor Yellow
    Write-Host "  1. توصيل الجهاز بالكمبيوتر" -ForegroundColor Yellow
    Write-Host "  2. تفعيل USB Debugging" -ForegroundColor Yellow
    Write-Host "  3. الموافقة على الاتصال على الجهاز" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# مسح السجل القديم
Write-Host "مسح السجل القديم..." -ForegroundColor Yellow
& $adbPath logcat -c
Write-Host "✅ تم مسح السجل" -ForegroundColor Green
Write-Host ""

# بدء التسجيل
Write-Host "بدء تسجيل Logcat..." -ForegroundColor Yellow
Write-Host "اضغط Ctrl+C لإيقاف التسجيل" -ForegroundColor Cyan
Write-Host ""

$logFile = "logcat.txt"

try {
    & $adbPath logcat > $logFile
} catch {
    Write-Host "تم إيقاف التسجيل" -ForegroundColor Green
}

Write-Host ""
Write-Host "✅ تم حفظ السجل في: $PWD\$logFile" -ForegroundColor Green
Write-Host ""

# فتح الملف
$open = Read-Host "هل تريد فتح الملف الآن؟ (y/n)"
if ($open -eq "y" -or $open -eq "Y") {
    notepad $logFile
}

