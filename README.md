# 🏭 سامانه فروش مستقیم محصولات پلاستیکی (Plastic Products B2B App)

این پروژه یک راهکار جامع **B2B (کارخانه به خریدار)** برای مدیریت، ثبت سفارش و فروش مستقیم محصولات پلاستیکی است. این سامانه شامل **بک‌اند مبتنی بر Django REST Framework**، **اپلیکیشن نیتیو اندروید با Kotlin و Jetpack Compose**، اسکریپت‌های پایگاه داده و مستندات کامل مهندسی نرم‌افزار می‌باشد.

---

## ✨ ویژگی‌های کلیدی

### 📱 اپلیکیشن اندروید (`android`)
- **توسعه مدرن با Kotlin و Jetpack Compose:** طراحی رابط کاربری واکنش‌گرا و پویا با رعایت اصول Material Design 3.
- **معماری MVVM و لایه‌بندی تمیز:** تفکیک کامل لایه‌های `data` (شبکه، مدل‌ها، مخازن داده و ذخیره‌سازی محلی)، `domain` و `ui`.
- **احراز هویت امن و مدیریت خودکار توکن:**
  - فرآیند کامل ورود با شماره موبایل و تایید کد یکبارمصرف (OTP).
  - ذخیره‌سازی امن توکن‌های JWT توسط `TokenManager`.
  - تمدید خودکار توکن‌های منقضی‌شده (Silent Token Refresh) با استفاده از `TokenAuthenticator` در OkHttp.
- **مدیریت پروفایل و کاتالوگ محصولات:** زیرساخت ارتباطی کامل با APIها جهت مشاهده، فیلتر محصولات (`ProductFilter`) و مدیریت اطلاعات کاربری.
- **ساختار یکپارچه دریافت پاسخ‌ها:** پردازش استاندارد پاسخ‌های سرور از طریق `ApiEnvelope`.

### ⚙️ بک‌اند و API (`backend`)
- **مدیریت همزمانی با Pessimistic Locking:** جلوگیری از بروز Race Condition و فروش بیش از موجودی (Overselling) در سفارشات همزمان.
- **پشتیبانی از موجودی و مقادیر اعشاری:** محاسبه دقیق اوزان، موجودی انبار و مبالغ سفارشات به همراه ثبت تاریخچه تغییرات قیمت (`PriceHistory`).
- **ماشین وضعیت سفارشات (Order State Machine):** مدیریت چرخه حیات سفارش و ثبت اسنپ‌شات اقلام سفارش (`items_snapshot`) در لحظه صدور فاکتور.
- **صدور فاکتور رسمی PDF:** تولید فاکتورهای فارسی با پشتیبانی از فونت `Vazirmatn`.
- **امنیت و محدودیت نرخ درخواست (Throttling):** احراز هویت مبتنی بر JWT و OTP، پین امنیتی ادمین (`admin_pin`)، کنترل سطوح دسترسی (`permissions`) و محافظت در برابر حملات Brute-Force.
- **یکپارچگی پاسخ‌ها و خطاها:** استفاده از Renderer و Exception Handler سفارشی جهت ارائه خروجی‌های استاندارد JSON.
- **تست بار و استرس (Load Testing):** مجهز به سناریوهای تست بار با استفاده از ابزار **Locust** (`locustfile.py`).

---

## 🏗️ ساختار پروژه

```text
plastic-products-app/
├── android/                        # پروژه اپلیکیشن اندروید (Kotlin + Jetpack Compose)
│   └── app/src/main/java/ir/codecrafter/plasticproducts/
│       ├── data/
│       │   ├── local/              # مدیریت توکن در حافظه محلی (TokenManager)
│       │   ├── model/              # مدل‌های داده (Product, UserProfile, OtpModels, ...)
│       │   ├── network/            # تنظیمات Retrofit, OkHttp, ApiEnvelope, TokenAuthenticator
│       │   └── repository/         # مخازن داده (AuthRepository, ProductRepository, ProfileRepository)
│       ├── domain/                 # لایه منطق دامنه
│       └── ui/
│           ├── auth/               # صفحات و ViewModel احراز هویت و تایید OTP
│           ├── navigation/         # گراف‌های مسیریابی (AppNavHost, AuthGraph)
│           ├── profile/            # صفحه و ViewModel پروفایل کاربری
│           └── theme/              # تنظیمات پوسته و تم برنامه
├── backend/                        # پروژه بک‌اند (Django & DRF)
│   ├── core/                       # تنظیمات پایه، Rendererها، مدیریت خطا و سرویس اعلان‌ها
│   ├── users/                      # مدیریت کاربران، احراز هویت OTP، سطوح دسترسی و Throttling
│   ├── products/                   # مدیریت محصولات و تاریخچه قیمت‌ها (PriceHistory)
│   ├── orders/                     # ثبت سفارشات، قفل بدبینانه و تولید فاکتور PDF
│   ├── plastic_products/           # تنظیمات اصلی پروژه جنگو (settings, urls, wsgi, asgi)
│   ├── static/fonts/               # فونت فارسی وزیرمتن جهت رندر فاکتورهای PDF
│   ├── seed_test_data.py           # اسکریپت تولید داده‌های آزمایشی
│   ├── generate_tokens.py          # اسکریپت تولید توکن‌های تست
│   └── locustfile.py               # سناریوهای تست بار با Locust
├── database/                       # اسکریپت‌های SQL پایگاه داده
│   ├── schema.sql                  # ساختار جداول (DDL)
│   ├── seed.sql                    # داده‌های اولیه (DML)
│   └── down.sql                    # حذف جداول و بازگردانی
├── docs/                           # مستندات کامل مهندسی نرم‌افزار
│   ├── android-dev-setup-iran.md   # راهنمای تنظیم محیط توسعه اندروید در ایران
│   ├── source/                     # فایل‌های منبع مستندات (Markdown و PlantUML)
│   └── export/                     # خروجی‌های گرافیکی و PDF (SRS, ERD, معماری، API و Use Case)
└── start.bat                       # اسکریپت راه‌اندازی سریع در ویندوز

🛠️ تکنولوژی‌های استفاده‌شدهبخشتکنولوژی‌ها و ابزارهااپلیکیشن اندرویدKotlin, Jetpack Compose, Material 3, ViewModel & StateFlow, Navigation Compose, Retrofit, OkHttp, Gradle Version Catalog (libs.versions.toml)بک‌اندPython, Django, Django REST Framework (DRF), SimpleJWT, ReportLab / PDF Generationپایگاه دادهPostgreSQL / SQLite (توسعه و تست)تست و مستندسازیDjango Test Suite, Locust (Load Testing), PlantUML, Markdown🚀 راهنمای نصب و راه‌اندازی۱. راه‌اندازی بک‌اند (backend)۱. وارد پوشه بک‌اند شوید و محیط مجازی پایتون را بسازید:Bashcd backend
python -m venv venv
۲. محیط مجازی را فعال کنید:در ویندوز:Bashvenv\Scripts\activate
در لینوکس و مک:Bashsource venv/bin/activate
۳. وابستگی‌ها را نصب کنید:Bashpip install -r requirements.txt
۴. مهاجرت‌های پایگاه داده (Migrations) را اعمال کرده و در صورت نیاز داده‌های آزمایشی را بارگذاری کنید:Bashpython manage.py migrate
python seed_test_data.py
۵. سرور توسعه را اجرا کنید:Bashpython manage.py runserver
(در سیستم‌عامل ویندوز می‌توانید از فایل start.bat در ریشه پروژه نیز استفاده کنید).۲. راه‌اندازی اپلیکیشن اندروید (android)۱. نرم‌افزار Android Studio را باز کرده و گزینه Open را انتخاب کنید.۲. پوشه android موجود در پروژه را انتخاب نمایید.3. صبر کنید تا فرآیند Gradle Sync تکمیل شود.💡 نکته مهم برای توسعه‌دهندگان در ایران: در صورت بروز خطا در دانلود وابستگی‌ها یا تحریم‌های شبکه، حتماً فایل راهنمای docs/android-dev-setup-iran.md را مطالعه کنید.آدرس سرور (BASE_URL) را در فایل NetworkModule.kt متناسب با محیط اجرای خود (مثلاً http://10.0.2.2:8000/ برای شبیه‌ساز اندروید) بررسی کنید.اپلیکیشن را روی شبیه‌ساز (Emulator) یا دستگاه فیزیکی اجرا (Run) نمایید.🧪 اجرای تست‌هاتست‌های واحد و یکپارچه‌سازی بک‌اندبرای اجرای تست‌های خودکار در ماژول‌های core، users، products و orders:Bashcd backend
python manage.py test
تست بار و همزمانی (Locust)برای بررسی عملکرد سیستم و مکانیزم قفل‌گذاری هنگام ثبت سفارش‌های همزمان:Bashcd backend
python generate_tokens.py
locust -f locustfile.py
📚 مستندات مهندسی و معماری (docs)تمامی مستندات تحلیل و طراحی سیستم در پوشه docs/ قرار دارند:📄 سند نیازمندی‌های نرم‌افزار (SRS): نسخه Markdown | نسخه PDF📄 مشخصات نیازمندی‌ها: نسخه Markdown | نسخه PDF🔗 مستندات کامل API و لیست اندپوینت‌ها:مشخصات API (Markdown) | نسخه PDFلیست اندپوینت‌ها (Markdown) | نسخه PDF🏛️ نمودار معماری سیستم: تصویر معماری🗄️ طراحی پایگاه داده (ERD & DDL): نمودار ERD (PNG) | مستند ERD (PDF) | سند DDL (PDF)🔄 نمودار فعالیت (Activity Diagram): تصویر نمودار | نسخه PDF👥 نمودارها و مشخصات Use Case:مشخصات Use Caseها (PDF)نقش ادمین (PNG) | نقش خریدار (PNG) | نقش بازدیدکننده (PNG)🛠️ راهنمای محیط توسعه اندروید در ایران: مشاهده راهنما
