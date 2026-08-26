# راه‌اندازی محیط توسعه‌ی اندروید در ایران

اگه از ایران به Maven گوگل (dl.google.com / maven.google.com) بدون
VPN دسترسی ندارید، این تنظیمات یک‌بار برای همیشه حلش می‌کنه.

## ۱. Mirror برای Maven گوگل

فایل `%USERPROFILE%\.gradle\init.gradle` (لینوکس/مک: `~/.gradle/init.gradle`)
رو با این محتوا بسازید:

```groovy
beforeSettings { settings ->
    settings.pluginManagement {
        repositories {
            maven { url 'https://maven.myket.ir' }
            gradlePluginPortal()
            google()
            mavenCentral()
        }
    }
    settings.dependencyResolutionManagement {
        repositories {
            maven { url 'https://maven.myket.ir' }
            google()
            mavenCentral()
        }
    }
}
```

⚠️ **نکته‌ی حیاتی روی ویندوز:** اگه این فایل رو با PowerShell
(`Set-Content -Encoding UTF8`) می‌سازید، یه BOM اضافه می‌کنه که باعث
خطای parse می‌شه («Unexpected character: '∩╗┐'»). به‌جاش از این استفاده کنید:

```powershell
[System.IO.File]::WriteAllText("$env:USERPROFILE\.gradle\init.gradle", $content, (New-Object System.Text.UTF8Encoding $false))
```

## ۲. اگه Kotlin daemon وصل نمی‌شه

اگه با خطای «Failed connecting to the daemon» مواجه شدید (معمولاً
به‌خاطر فایروال/آنتی‌ویروس که سوکت loopback رو مسدود می‌کنه)، این
خط رو به `android/gradle.properties` اضافه کنید:

```properties
kotlin.compiler.execution.strategy=in-process
```

## ۳. اگه یه dependency خاص resolve نمی‌شه ولی unresolved reference می‌ده

بعضی کتابخونه‌های قدیمی (مثل fork شخصی retrofit2-kotlinx-serialization-converter)
ممکنه با ابزار Gradle جدید خوب کار نکنن، حتی وقتی از mavenCentral
دانلود می‌شن. اگه به یه dependency خاص برخوردید که resolve می‌شه ولی
class‌هاش پیدا نمی‌شن، اول با نسخه‌ی رسمی/فعلی همون کتابخونه امتحان
کنید (مثلاً برای این مورد خاص: com.squareup.retrofit2:converter-kotlinx-serialization
به‌جای com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter).
