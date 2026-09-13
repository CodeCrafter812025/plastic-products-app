# قوانین ثابت این پروژه — قبل از هر کاری بخون

## ساختار ریپو
- برنچ `main`: فقط بک‌اند (Django)، از پوشه‌ی جدا `E:\plastic-products-backend`
  (یه git worktree، نه کلون جدا) کار می‌کنیم.
- برنچ `feature/android-scaffold`: کل کد اندروید، از پوشه‌ی
  `E:\plastic-products-app`. پوشه‌ی backend/ داخل این برنچ **قدیمیه
  و هیچ‌وقت به‌روز نمی‌شه** — این دو برنچ دیگه هیچ‌وقت با هم merge
  نمی‌شن (قبلاً این کار رو کردیم و کلی مشکل ساخت).
- برای دیدن کد واقعی بک‌اند از داخل برنچ اندروید، همیشه از این
  استفاده کن: `git show origin/main:backend/<path>`
- فیکس‌های بک‌اند: یه برنچ جدید از main، بعد از تست/تأیید،
  push → PR → Squash and merge → توی worktree بک‌اند git pull +
  ری‌استارت سرور.

## قوانین کد
- هر متن فارسیِ ثابت باید توی `strings.xml` باشه (با `stringResource`)،
  هیچ‌وقت hardcoded توی فایل `.kt`.
- Repositoryها همیشه از الگوی `AuthResult<T>` (Success/Error/
  RateLimited/NetworkError) استفاده می‌کنن.
- تاریخ‌ها همیشه با `util/PersianDateFormatter.kt` (شمسی، self-contained،
  بدون کتابخونه‌ی خارجی) نمایش داده می‌شن؛ مقدار خام میلادی که به
  API می‌ره تغییر نمی‌کنه.
- توی `LazyColumn`/`LazyRow` که بیش از یک نوع آیتم داره، هر بخش
  باید key با پیشوند جدا داشته باشه (مثلاً `"item_${id}"` و
  `"history_${id}"`) — یه‌بار کرش گرفتیم چون دو جدول مختلف می‌تونن
  id یکسان داشته باشن.
- قبل از فرض‌کردن شکل یه API، همیشه از `git show origin/main:...`
  کد واقعی رو چک کن — مستندات قدیمی (`docs/source/api-specification.md`
  و `endpoint-list.md`) با کد واقعی فرق دارن، چندین‌بار همین باعث
  اشتباه شده.

## قوانین Git
- همیشه «commit و push» با هم — هیچ‌وقت فقط commit بدون push (یه‌بار
  یه کامیت push‌نشده توی یه session ابری گیر افتاد).
- هیچ‌وقت `git push --force` نزن.
- قبل از merge کردن یه تداخل، محتوای هر دو طرف رو کامل ببین، کورکورانه
  `--ours`/`--theirs` انتخاب نکن.

## قوانین ویندوز/انکودینگ
- برای خوندن/نوشتن هر فایلی که متن فارسی داره، هیچ‌وقت `Get-Content`/
  `Set-Content` ساده‌ی PowerShell استفاده نکن (BOM/انکودینگ خراب
  می‌شه). همیشه:
```powershell
  [System.IO.File]::WriteAllText(path, content, (New-Object System.Text.UTF8Encoding $false))
```

## محیط build
- `init.gradle` توی `%USERPROFILE%\.gradle\` از قبل mirror ایرانی
  (myket.ir) برای دسترسی به Maven گوگل تنظیم شده — لازم نیست دوباره
  درگیرش بشی مگر یه dependency خاص روش resolve نشه.
- `kotlin.compiler.execution.strategy=in-process` توی
  `android/gradle.properties` برای مشکل daemon هست.
- Release build (signing + R8/minify) کار می‌کنه و تست شده
  (`gradlew.bat assembleRelease`).

## وضعیت فعلی پروژه
تمام فازهای ۰ تا ۷ (اسکلت، Auth، محصولات، سبد خرید/سفارش، ویزیتور،
تاریخچه/فاکتور/اعلان، مدیریت ادمین، صیقل نهایی) کامل و تست‌شده‌ن.